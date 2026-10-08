package io.github.jackmacca06.ggtlifesteal;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;

public final class EndRules {
    private EndRules() {}
    private static String lockedDimension(ServerLevel destination) {
        if (destination.dimension().equals(Level.END) && !ModState.endOpen(destination.getServer())) return "End";
        if (destination.dimension().equals(Level.NETHER) && !ModState.netherOpen(destination.getServer())) return "Nether";
        return null;
    }
    public static boolean denied(ServerPlayer player, ServerLevel destination) {
        String name = lockedDimension(destination);
        if (name == null) return false;
        player.sendOverlayMessage(Component.translatable("message.ggtlifesteal.dimension_locked", name));
        return true;
    }
    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> {
            ServerPlayer player = listener.player;
            String name = lockedDimension(player.level());
            if (name != null) {
                player.teleport(overworldSpawn(player));
                player.sendSystemMessage(Component.translatable("message.ggtlifesteal.dimension_return", name));
            }
        });
    }

    public static TeleportTransition overworldSpawn(ServerPlayer player) {
        TeleportTransition normal = TeleportTransition.createDefault(player, TeleportTransition.DO_NOTHING);
        if (normal.newLevel().dimension().equals(Level.OVERWORLD)) return normal;
        // An operator may have configured the global spawn in a locked dimension.
        ServerLevel overworld = player.level().getServer().overworld();
        var spawn = overworld.getRespawnData().pos();
        int y = overworld.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                spawn.getX(), spawn.getZ());
        return new TeleportTransition(overworld,
                new net.minecraft.world.phys.Vec3(spawn.getX() + 0.5, y, spawn.getZ() + 0.5),
                net.minecraft.world.phys.Vec3.ZERO, 0, 0, TeleportTransition.DO_NOTHING);
    }
}
