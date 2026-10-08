package io.github.jackmacca06.ggtlifesteal;

import com.mojang.serialization.Codec;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class LocatorRules {
    public static final long DURATION_NANOS = 300_000_000_000L;
    private static final String COMPASS_ID = "ggtls_locator_id";
    // A funded session holds one refundable charge until it is explicitly spent/forfeited.
    private static final AttachmentType<Boolean> REFUND = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(Ggtlifesteal.MOD_ID, "locator_refund"),
            b -> b.initializer(() -> false).persistent(Codec.BOOL).copyOnDeath());
    private record Session(UUID target, String compassId, long deadline, ServerBossEvent bar) {}
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, Long> LAST_WARNING = new HashMap<>();
    private static boolean stopping;
    private LocatorRules() {}

    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            SESSIONS.clear(); LAST_WARNING.clear(); stopping = false;
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            stopping = true;
            SESSIONS.values().forEach(s -> s.bar.removeAllPlayers());
            SESSIONS.clear(); // REFUND stays saved until the next join.
        });
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> deliverRefund(listener.player));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) finish(player, false, null);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            Session session = SESSIONS.get(newPlayer.getUUID());
            if (session != null) {
                session.bar.removePlayer(oldPlayer); session.bar.addPlayer(newPlayer);
            }
        });
        ServerTickEvents.START_SERVER_TICK.register(LocatorRules::tick);
    }

    public static boolean active(ServerPlayer player) { return SESSIONS.containsKey(player.getUUID()); }
    public static boolean locked(ServerPlayer player, ItemStack stack) {
        Session session = SESSIONS.get(player.getUUID());
        return session != null && stack.is(Ggtlifesteal.LOCATOR_COMPASS)
                && session.compassId.equals(id(stack));
    }
    private static String id(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr(COMPASS_ID, "");
    }
    private static ItemStack compass(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(Ggtlifesteal.LOCATOR_COMPASS)) return stack;
        }
        return ItemStack.EMPTY;
    }
    public static boolean start(ServerPlayer seeker, UUID targetId) {
        ServerPlayer target = seeker.level().getServer().getPlayerList().getPlayer(targetId);
        if (active(seeker) || !seeker.isAlive() || seeker.isSpectator() || target == null
                || target == seeker || target.isSpectator() || !target.isAlive()) return error(seeker, "locator.unavailable");
        ItemStack compass = compass(seeker);
        if (compass.isEmpty()) return error(seeker, "locator.no_compass");
        ItemStack charge = ItemStack.EMPTY;
        for (int i = 0; i < playerSlots(seeker); i++) {
            var stack = seeker.getInventory().getItem(i);
            if (stack.is(Ggtlifesteal.TRACKING_CHARGE)) { charge = stack; break; }
        }
        if (charge.isEmpty()) return error(seeker, "locator.no_fuel");
        String compassId = UUID.randomUUID().toString();
        CustomData.update(DataComponents.CUSTOM_DATA, compass, tag -> tag.putString(COMPASS_ID, compassId));
        charge.shrink(1); // Fuel is an explicit cost, including for an operator testing in Creative.
        ((AttachmentTarget) seeker).setAttached(REFUND, true);
        var bar = new ServerBossEvent(UUID.randomUUID(), Component.empty(), BossEvent.BossBarColor.BLUE,
                BossEvent.BossBarOverlay.PROGRESS);
        bar.addPlayer(seeker);
        SESSIONS.put(seeker.getUUID(), new Session(targetId, compassId, System.nanoTime() + DURATION_NANOS, bar));
        seeker.getInventory().setChanged();
        tick(seeker.level().getServer());
        return true;
    }
    private static int playerSlots(ServerPlayer player) { return player.getInventory().getContainerSize(); }

    public static void cancel(ServerPlayer seeker) { finish(seeker, false, "locator.cancelled"); }
    public static void onDisconnect(ServerPlayer player) {
        if (stopping) return;
        finish(player, false, null);
        MinecraftServer server = player.level().getServer();
        for (var entry : List.copyOf(SESSIONS.entrySet())) {
            if (entry.getValue().target.equals(player.getUUID())) {
                ServerPlayer seeker = server.getPlayerList().getPlayer(entry.getKey());
                if (seeker != null) finish(seeker, true, "locator.refunded");
            }
        }
    }
    private static void finish(ServerPlayer seeker, boolean refund, String message) {
        Session session = SESSIONS.remove(seeker.getUUID());
        if (session == null) return;
        session.bar.removeAllPlayers();
        if (refund) deliverRefund(seeker);
        else ((AttachmentTarget) seeker).setAttached(REFUND, false);
        if (message != null) seeker.sendSystemMessage(Component.translatable("message.ggtlifesteal." + message));
        if (SESSIONS.values().stream().noneMatch(s -> s.target.equals(session.target))) LAST_WARNING.remove(session.target);
    }
    private static void deliverRefund(ServerPlayer player) {
        AttachmentTarget data = (AttachmentTarget) player;
        if (!data.getAttachedOrCreate(REFUND)) return;
        data.setAttached(REFUND, false);
        // Manual insertion does not discard leftovers in Creative mode.
        ItemStack refund = new ItemStack(Ggtlifesteal.TRACKING_CHARGE);
        for (int i = 0; i < 36; i++) {
            ItemStack existing = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameComponents(existing, refund) && existing.getCount() < existing.getMaxStackSize()) {
                existing.grow(1); player.getInventory().setChanged(); return;
            }
        }
        for (int i = 0; i < 36; i++) if (player.getInventory().getItem(i).isEmpty()) {
            player.getInventory().setItem(i, refund); return;
        }
        HeartRewards.drop(player, refund);
    }
    private static void tick(MinecraftServer server) {
        long now = System.nanoTime();
        for (var entry : List.copyOf(SESSIONS.entrySet())) {
            ServerPlayer seeker = server.getPlayerList().getPlayer(entry.getKey());
            if (seeker == null) continue;
            Session session = entry.getValue();
            long remaining = session.deadline - now;
            if (remaining <= 0) { finish(seeker, false, "locator.expired"); continue; }
            if (!seeker.isAlive()) { finish(seeker, false, null); continue; }
            boolean present = false;
            for (int i = 0; i < playerSlots(seeker); i++) if (locked(seeker, seeker.getInventory().getItem(i))) present = true;
            if (!present) { finish(seeker, false, "locator.missing"); continue; }
            ServerPlayer target = server.getPlayerList().getPlayer(session.target);
            if (target == null) { finish(seeker, true, "locator.refunded"); continue; }
            boolean sameLevel = seeker.level() == target.level();
            double distance = seeker.position().distanceTo(target.position());
            if (sameLevel && distance <= 100 && target.isAlive() && !target.isSpectator() && !seeker.isSpectator()) {
                CombatRules.refreshProximity(seeker); CombatRules.refreshProximity(target);
                refreshGlow(seeker); refreshGlow(target);
            }
            int seconds = (int) ((remaining + 999_999_999L) / 1_000_000_000L);
            String location = sameLevel ? Math.round(distance) + "m"
                    : "? (" + target.level().dimension().identifier().getPath().replace("the_", "").toUpperCase(Locale.ROOT) + ")";
            session.bar.setName(Component.translatable("hud.ggtlifesteal.tracking", target.getGameProfile().name(), location, time(seconds)));
            session.bar.setProgress(Math.clamp((float) remaining / DURATION_NANOS, 0, 1));
            Long warning = LAST_WARNING.get(session.target);
            if (warning == null || now - warning >= 60_000_000_000L) {
                target.sendSystemMessage(Component.translatable("message.ggtlifesteal.locator.watched")
                        .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
                LAST_WARNING.put(session.target, now);
            }
        }
    }
    private static void refreshGlow(ServerPlayer player) {
        var glow = player.getEffect(net.minecraft.world.effect.MobEffects.GLOWING);
        // Short renewable effect expires naturally outside range, without clearing other sources of glowing.
        if (glow == null || !glow.isInfiniteDuration() && glow.getDuration() <= 20) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.GLOWING, 40, 0, false, false, false));
        }
    }
    public static String time(int seconds) { return String.format(Locale.ROOT, "%02dm%02ds", seconds / 60, seconds % 60); }
    private static boolean error(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable("message.ggtlifesteal." + key).withStyle(net.minecraft.ChatFormatting.RED));
        return false;
    }
}
