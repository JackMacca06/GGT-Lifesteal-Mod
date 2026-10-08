package io.github.jackmacca06.ggtlifesteal;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import java.util.*;

/** World-scoped settings and offline rewards; immutable attachment values are replaced on mutation. */
public final class ModState {
    private static final AttachmentType<Boolean> END_OPEN = AttachmentRegistry.create(
            id("end_open"), b -> b.initializer(() -> false).persistent(Codec.BOOL));
    private static final AttachmentType<Boolean> NETHER_OPEN = AttachmentRegistry.create(
            id("nether_open"), b -> b.initializer(() -> false).persistent(Codec.BOOL));
    private static final AttachmentType<Boolean> ENCHANTS_OPEN = AttachmentRegistry.create(
            id("enchants_open"), b -> b.initializer(() -> false).persistent(Codec.BOOL));
    private static final AttachmentType<Integer> DAILY_HOURS = AttachmentRegistry.create(
            id("daily_hours"), b -> b.initializer(() -> 0).persistent(Codec.intRange(0, Integer.MAX_VALUE)));
    private static final AttachmentType<Map<String, Integer>> REWARDS = AttachmentRegistry.create(
            id("pending_hearts"), b -> b.initializer(Map::of)
                    .persistent(Codec.unboundedMap(Codec.STRING, Codec.intRange(0, Integer.MAX_VALUE))));

    private ModState() {}
    public static void initialize() {} // Register attachment types before worlds are loaded.
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(Ggtlifesteal.MOD_ID, path); }
    private static AttachmentTarget world(MinecraftServer server) { return (AttachmentTarget) server.overworld(); }
    public static boolean endOpen(MinecraftServer server) { return world(server).getAttachedOrCreate(END_OPEN); }
    public static void releaseEnd(MinecraftServer server) { world(server).setAttached(END_OPEN, true); }
    public static boolean netherOpen(MinecraftServer server) { return world(server).getAttachedOrCreate(NETHER_OPEN); }
    public static void releaseNether(MinecraftServer server) { world(server).setAttached(NETHER_OPEN, true); }
    public static boolean enchantsOpen(MinecraftServer server) { return world(server).getAttachedOrCreate(ENCHANTS_OPEN); }
    public static void releaseEnchants(MinecraftServer server) { world(server).setAttached(ENCHANTS_OPEN, true); }
    public static int dailyHours(MinecraftServer server) { return world(server).getAttachedOrCreate(DAILY_HOURS); }
    public static void dailyHours(MinecraftServer server, int hours) { world(server).setAttached(DAILY_HOURS, hours); }
    public static int pending(MinecraftServer server, UUID id) {
        return world(server).getAttachedOrCreate(REWARDS).getOrDefault(id.toString(), 0);
    }
    public static void pending(MinecraftServer server, UUID id, int amount) {
        var values = new HashMap<>(world(server).getAttachedOrCreate(REWARDS));
        if (amount == 0) values.remove(id.toString()); else values.put(id.toString(), amount);
        world(server).setAttached(REWARDS, Map.copyOf(values));
    }
    public static void addReward(MinecraftServer server, UUID id) {
        pending(server, id, Math.addExact(pending(server, id), 1));
    }
}
