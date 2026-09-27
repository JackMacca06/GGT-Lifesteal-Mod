package io.github.jackmacca06.ggtlifesteal;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;

public final class WardenRules {
    private static final Map<ServerLevel, Set<Warden>> WARDENS = new IdentityHashMap<>();
    private static final AttachmentType<List<String>> ANGRY_AT_LAUNCH = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(Ggtlifesteal.MOD_ID, "angry_wardens_at_launch"),
            b -> b.persistent(Codec.STRING.listOf()));
    private WardenRules() {}

    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> WARDENS.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> WARDENS.clear());
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof Warden warden) WARDENS.computeIfAbsent(level, l -> new HashSet<>()).add(warden);
            if (entity instanceof Projectile projectile) capture(projectile);
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof Warden warden) {
                Set<Warden> wardens = WARDENS.get(level);
                if (wardens != null) wardens.remove(warden);
            }
        });
        LootTableEvents.MODIFY_DROPS.register((holder, context, drops) -> {
            if (holder.unwrapKey().map(key -> key.identifier().equals(
                    Identifier.fromNamespaceAndPath("minecraft", "entities/warden"))).orElse(false)) {
                // Exactly one, even when another pool or Looting has added a mace.
                drops.removeIf(stack -> stack.is(Items.MACE));
                drops.add(new ItemStack(Items.MACE));
            }
        });
    }

    /** Snapshot before the projectile can provoke anger; persist it through chunk unloads. */
    public static void capture(Projectile projectile) {
        if (!(projectile.level() instanceof ServerLevel level)) return;
        AttachmentTarget target = (AttachmentTarget) projectile;
        if (target.getAttached(ANGRY_AT_LAUNCH) != null) return;
        List<String> angry = WARDENS.getOrDefault(level, Set.of()).stream()
                .filter(warden -> warden.isAlive() && warden.getAngerLevel().isAngry())
                .map(warden -> warden.getUUID().toString()).toList();
        target.setAttached(ANGRY_AT_LAUNCH, angry);
    }

    public static float projectileDamage(Warden warden, DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.IS_PROJECTILE) && !(source.getDirectEntity() instanceof Projectile)) return amount;
        if (source.getDirectEntity() instanceof Projectile projectile) {
            List<String> angry = ((AttachmentTarget) projectile).getAttached(ANGRY_AT_LAUNCH);
            if (angry != null && angry.contains(warden.getUUID().toString())) return amount;
        }
        // Includes all unprovoked shots, hence also out-of-detection-range shots and dispensers.
        return amount * 0.05F;
    }
}
