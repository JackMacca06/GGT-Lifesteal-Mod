package io.github.jackmacca06.ggtlifesteal;

import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public final class PlaytimeRules {
    private static final AttachmentType<DailyPlaytime> USAGE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(Ggtlifesteal.MOD_ID, "daily_playtime"),
            b -> b.initializer(() -> new DailyPlaytime(DailyPlaytime.dayAt(System.currentTimeMillis()), 0))
                    .persistent(DailyPlaytime.CODEC).copyOnDeath());
    private static final Map<UUID, Long> LAST_ACCOUNTED = new HashMap<>();
    private static final Map<UUID, Long> LAST_REMAINING = new HashMap<>();
    private PlaytimeRules() {}

    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> { LAST_ACCOUNTED.clear(); LAST_REMAINING.clear(); });
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> {
            LAST_ACCOUNTED.put(listener.player.getUUID(), System.currentTimeMillis());
            check(listener.player);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) check(player);
        });
        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) ->
                server.getPlayerList().getPlayers().forEach(PlaytimeRules::account));
        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                server.getPlayerList().getPlayers().forEach(PlaytimeRules::account));
    }
    public static void onDisconnect(ServerPlayer player) {
        account(player);
        LAST_ACCOUNTED.remove(player.getUUID());
        LAST_REMAINING.remove(player.getUUID());
    }
    private static DailyPlaytime account(ServerPlayer player) {
        long now = System.currentTimeMillis();
        AttachmentTarget target = (AttachmentTarget) player;
        DailyPlaytime previous = target.getAttachedOrCreate(USAGE);
        DailyPlaytime updated = previous.accrue(LAST_ACCOUNTED.getOrDefault(player.getUUID(), now), now);
        LAST_ACCOUNTED.put(player.getUUID(), now);
        if (!updated.equals(previous)) target.setAttached(USAGE, updated);
        return updated;
    }
    public static long remainingMillis(ServerPlayer player) {
        DailyPlaytime usage = account(player);
        int hours = ModState.dailyHours(player.level().getServer());
        return hours == 0 ? Long.MAX_VALUE : Math.max(0, hours * 3_600_000L - usage.millis());
    }
    private static void check(ServerPlayer player) {
        long remaining = remainingMillis(player);
        Long previous = LAST_REMAINING.put(player.getUUID(), remaining);
        if (remaining == 0) {
            CombatRules.exemptDisconnect(player);
            player.connection.disconnect(Component.literal("Daily playtime limit reached. You can return at midnight ACST (UTC+09:30)."));
            return;
        }
        for (long warning : new long[]{300_000L, 60_000L}) {
            if (remaining <= warning && (previous == null || previous > warning)) {
                player.sendSystemMessage(Component.literal("Daily playtime remaining: " + ((remaining + 59_999) / 60_000) + " minute(s)."));
            }
        }
    }
}
