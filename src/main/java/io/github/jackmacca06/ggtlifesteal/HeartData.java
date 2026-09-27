package io.github.jackmacca06.ggtlifesteal;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Stores permanent heart capacity independently of temporary health effects. */
public final class HeartData {
    public static final int STARTING_HEARTS = 10;
    public static final int MIN_HEARTS = 3;
    public static final int MAX_HEARTS = 20;

    private static final Identifier HEALTH_MODIFIER =
            Identifier.fromNamespaceAndPath(Ggtlifesteal.MOD_ID, "heart_capacity");

    private static final AttachmentType<Integer> HEARTS = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(Ggtlifesteal.MOD_ID, "hearts"),
            builder -> builder.initializer(() -> STARTING_HEARTS)
                    .persistent(Codec.intRange(MIN_HEARTS, MAX_HEARTS))
                    .copyOnDeath()
    );
    private static final AttachmentType<Integer> MAKESHIFT_HEARTS = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(Ggtlifesteal.MOD_ID, "makeshift_capacity"),
            builder -> builder.initializer(() -> 0).persistent(Codec.intRange(0, 17)).copyOnDeath()
    );

    private HeartData() {
    }

    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) ->
                applyCapacity(listener.player));

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            // Fabric transfers attachments in AFTER_RESPAWN. Copy ours before the
            // new player is tracked, so health restoration cannot read defaults
            // regardless of AFTER_RESPAWN listener registration order.
            AttachmentTarget target = (AttachmentTarget) newPlayer;
            target.setAttached(HEARTS, getHearts(oldPlayer));
            target.setAttached(MAKESHIFT_HEARTS, getMakeshiftHearts(oldPlayer));
            applyCapacity(newPlayer);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            applyCapacity(newPlayer);
            if (!alive) {
                // Vanilla respawn health is assigned before our modifier is restored.
                newPlayer.setHealth(newPlayer.getMaxHealth());
            }
        });
    }

    public static int getHearts(ServerPlayer player) {
        return ((AttachmentTarget) player).getAttachedOrCreate(HEARTS);
    }

    /** Called only on the logical server; failure does not change stored capacity. */
    public static boolean tryAddHeart(ServerPlayer player) {
        return tryAddHeart(player, false);
    }

    public static boolean tryAddHeart(ServerPlayer player, boolean makeshift) {
        int hearts = getHearts(player);
        if (hearts >= (makeshift ? STARTING_HEARTS : MAX_HEARTS)
                || player.getAttribute(Attributes.MAX_HEALTH) == null) {
            return false;
        }

        ((AttachmentTarget) player).setAttached(HEARTS, hearts + 1);
        if (makeshift) {
            ((AttachmentTarget) player).setAttached(MAKESHIFT_HEARTS, getMakeshiftHearts(player) + 1);
        }
        applyCapacity(player);
        // Fill the new heart, without restoring all previously missing health.
        player.heal(2.0F);
        return true;
    }

    public static int getMakeshiftHearts(ServerPlayer player) {
        return ((AttachmentTarget) player).getAttachedOrCreate(MAKESHIFT_HEARTS);
    }

    public static int withdrawableHearts(ServerPlayer player) {
        int hearts = getHearts(player);
        return Math.max(0, Math.min(hearts - STARTING_HEARTS,
                hearts - MIN_HEARTS - getMakeshiftHearts(player)));
    }

    public static boolean withdraw(ServerPlayer player, int count) {
        if (count <= 0 || count > withdrawableHearts(player)) return false;
        ((AttachmentTarget) player).setAttached(HEARTS, getHearts(player) - count);
        applyCapacity(player);
        return true;
    }

    /** Operator override of permanent capacity; does not heal existing injuries. */
    public static void setHearts(ServerPlayer player, int count) {
        if (count < MIN_HEARTS || count > MAX_HEARTS) {
            throw new IllegalArgumentException("Heart count must be between 3 and 20.");
        }
        AttachmentTarget target = (AttachmentTarget) player;
        target.setAttached(HEARTS, count);
        target.setAttached(MAKESHIFT_HEARTS, 0);
        applyCapacity(player);
    }

    public static boolean loseHeart(ServerPlayer player) {
        if (getHearts(player) <= MIN_HEARTS) return false;
        ((AttachmentTarget) player).setAttached(HEARTS, getHearts(player) - 1);
        int makeshift = getMakeshiftHearts(player);
        if (makeshift > 0) ((AttachmentTarget) player).setAttached(MAKESHIFT_HEARTS, makeshift - 1);
        applyCapacity(player);
        return true;
    }

    private static void applyCapacity(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }

        // Persist the modifier: vanilla loads saved attributes BEFORE saved health.
        // A transient modifier is absent at that point and clips health to 20 HP.
        // Replace only our own modifier. Keep vanilla effects and other mods intact.
        health.removeModifier(HEALTH_MODIFIER);
        int extraHearts = getHearts(player) - STARTING_HEARTS;
        if (extraHearts != 0) {
            health.addPermanentModifier(new AttributeModifier(
                    HEALTH_MODIFIER,
                    extraHearts * 2.0,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }
}
