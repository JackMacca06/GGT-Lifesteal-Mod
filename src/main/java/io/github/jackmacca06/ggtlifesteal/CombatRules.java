package io.github.jackmacca06.ggtlifesteal;

import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class CombatRules {
    private static final long DURATION_NANOS = 60_000_000_000L;
    private static final Map<UUID, Long> UNTIL = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<UUID, Integer> DISPLAYED = new HashMap<>();
    private static final Set<UUID> EXEMPT_DISCONNECT = new HashSet<>();
    private static boolean stopping;

    private CombatRules() {}
    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            UNTIL.clear(); DISPLAYED.clear(); EXEMPT_DISCONNECT.clear(); stopping = false;
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> stopping = true);
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, before, taken, blocked) -> {
            if (entity instanceof ServerPlayer victim && taken > 0) tagHit(victim, source);
        });
        EntityElytraEvents.ALLOW.register(entity -> !(entity instanceof Player p) || !active(p));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                int seconds = secondsLeft(player);
                Integer previous = DISPLAYED.get(player.getUUID());
                if (seconds > 0) {
                    player.stopFallFlying();
                    if (previous == null || previous != seconds) {
                        player.sendOverlayMessage(Component.literal("IN COMBAT: " + seconds).withStyle(ChatFormatting.RED));
                        DISPLAYED.put(player.getUUID(), seconds);
                    }
                } else if (previous != null) {
                    player.sendOverlayMessage(Component.literal("You are no longer in combat.").withStyle(ChatFormatting.GREEN));
                    clear(player);
                }
            }
        });
    }

    /** Called on the server thread before PlayerList saves and removes this player. */
    public static void onDisconnect(ServerPlayer player) {
            boolean exempt = EXEMPT_DISCONNECT.remove(player.getUUID());
            boolean punish = !stopping && !exempt && active(player) && player.isAlive();
            clear(player);
            if (!punish) return;

            // Remove each original stack before spawning its single replacement in the world.
            ItemStack cursor = player.containerMenu.getCarried();
            player.containerMenu.setCarried(ItemStack.EMPTY);
            HeartRewards.drop(player, cursor);
            // Return temporary crafting inputs before emptying the carried inventory.
            player.doCloseContainer();
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                player.getInventory().setItem(slot, ItemStack.EMPTY);
                HeartRewards.drop(player, stack);
            }
            if (HeartData.loseHeart(player)) HeartRewards.drop(player, new ItemStack(Ggtlifesteal.HEART));
    }

    private static void tagHit(ServerPlayer victim, DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim
                && !attacker.isSpectator() && !victim.isSpectator()) {
            tag(attacker);
            if (victim.isAlive()) tag(victim);
        }
    }
    public static void onDeath(ServerPlayer victim, DamageSource source) {
        tagHit(victim, source); // AFTER_DAMAGE excludes lethal hits.
        clear(victim);
    }
    private static void tag(ServerPlayer player) {
        UNTIL.put(player.getUUID(), System.nanoTime() + DURATION_NANOS);
        DISPLAYED.remove(player.getUUID());
        player.stopFallFlying();
    }
    public static int secondsLeft(Player player) {
        Long deadline = UNTIL.get(player.getUUID());
        if (deadline == null) return 0;
        long remaining = deadline - System.nanoTime();
        return (int) Math.max(0, Math.min(60, (remaining + 999_999_999L) / 1_000_000_000L));
    }
    public static boolean active(Player player) { return secondsLeft(player) > 0; }
    public static boolean denyEquip(LivingEntity entity, ItemStack stack) {
        return entity instanceof Player player && active(player) && stack.has(DataComponents.GLIDER);
    }
    public static void exemptDisconnect(ServerPlayer player) { EXEMPT_DISCONNECT.add(player.getUUID()); }
    private static void clear(Player player) { UNTIL.remove(player.getUUID()); DISPLAYED.remove(player.getUUID()); }
}
