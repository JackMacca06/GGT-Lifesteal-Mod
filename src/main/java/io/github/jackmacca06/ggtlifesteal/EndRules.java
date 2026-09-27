package io.github.jackmacca06.ggtlifesteal;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;

public final class EndRules {
    private EndRules() {}
    public static boolean denied(ServerPlayer player, ServerLevel destination) {
        if (!destination.dimension().equals(Level.END) || ModState.endOpen(destination.getServer())) return false;
        player.sendOverlayMessage(Component.literal("The End has not been released yet."));
        return true;
    }
    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> {
            ServerPlayer player = listener.player;
            if (player.level().dimension().equals(Level.END) && !ModState.endOpen(server)) {
                player.teleport(TeleportTransition.createDefault(player, TeleportTransition.DO_NOTHING));
                player.sendSystemMessage(Component.literal("The End is locked. You have been returned to the world spawn."));
            }
        });
    }
}
