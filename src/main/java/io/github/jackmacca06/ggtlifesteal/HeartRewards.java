package io.github.jackmacca06.ggtlifesteal;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class HeartRewards {
    private static final Set<UUID> PROCESSED_DEATHS = new HashSet<>();
    private static final Map<UUID, UUID> DEATH_CREDIT = new HashMap<>();
    private HeartRewards() {}

    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> { PROCESSED_DEATHS.clear(); DEATH_CREDIT.clear(); });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> PROCESSED_DEATHS.remove(newPlayer.getUUID()));
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> {
            ServerPlayer player = listener.player;
            PROCESSED_DEATHS.remove(player.getUUID());
            int pending = ModState.pending(server, player.getUUID());
            if (pending > 0) player.sendSystemMessage(Component.literal(
                    "You have " + pending + " stored Hearts. Use /ggtls claim_hearts."));
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayer victim)) return;
            CombatRules.onDeath(victim, source);
            UUID killerId = DEATH_CREDIT.remove(victim.getUUID());
            if (!PROCESSED_DEATHS.add(victim.getUUID()) || !HeartData.loseHeart(victim)) return;
            if (killerId != null && !killerId.equals(victim.getUUID())) {
                var server = victim.level().getServer();
                ServerPlayer online = server.getPlayerList().getPlayer(killerId);
                if (online != null && online.isAlive() && give(online, 1) == 1) {
                    online.sendSystemMessage(Component.literal("You received a Heart."));
                } else {
                    ModState.addReward(server, killerId);
                    if (online != null) online.sendSystemMessage(Component.literal(
                            "Your Heart was stored. Make room and use /ggtls claim_hearts."));
                }
            } else {
                drop(victim, new ItemStack(Ggtlifesteal.HEART));
            }
        });
    }

    /** Capture before vanilla clears the combat tracker in die(), regardless of showDeathMessages. */
    public static void captureDeath(ServerPlayer victim) {
        DEATH_CREDIT.remove(victim.getUUID());
        UUID killer = creditedPlayer(victim.getCombatTracker().getDeathMessage());
        if (killer != null && !killer.equals(victim.getUUID())) DEATH_CREDIT.put(victim.getUUID(), killer);
    }

    /** Read the attacker's structured UUID, not player-name text or the weapon-name argument. */
    public static UUID creditedPlayer(Component message) {
        if (message.getContents() instanceof TranslatableContents translation) {
            Object[] args = translation.getArgs();
            if (args.length > 1 && args[1] instanceof Component attacker
                    && attacker.getStyle().getHoverEvent() instanceof HoverEvent.ShowEntity hover
                    && hover.entity().type == EntityTypes.PLAYER) {
                return hover.entity().uuid;
            }
        }
        return null;
    }

    /** Main inventory only: never equips rewards or relies on creative-mode discard behavior. */
    public static int room(ServerPlayer player) {
        ItemStack example = new ItemStack(Ggtlifesteal.HEART);
        int room = 0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) room += 64;
            else if (ItemStack.isSameItemSameComponents(stack, example)) room += Math.max(0, 64 - stack.getCount());
        }
        return room;
    }

    public static int give(ServerPlayer player, int requested) {
        int remaining = Math.min(requested, room(player));
        int given = remaining;
        ItemStack example = new ItemStack(Ggtlifesteal.HEART);
        for (int pass = 0; pass < 2; pass++) {
            for (int slot = 0; slot < Inventory.INVENTORY_SIZE && remaining > 0; slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (pass == 0 && !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, example)) {
                    int amount = Math.min(remaining, Math.max(0, 64 - stack.getCount()));
                    stack.grow(amount);
                    remaining -= amount;
                } else if (pass == 1 && stack.isEmpty()) {
                    int amount = Math.min(remaining, 64);
                    player.getInventory().setItem(slot, new ItemStack(Ggtlifesteal.HEART, amount));
                    remaining -= amount;
                }
            }
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return given - remaining;
    }

    public static void drop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getY() + 0.3, player.getZ(), stack);
        entity.setDefaultPickUpDelay();
        player.level().addFreshEntity(entity);
    }
}
