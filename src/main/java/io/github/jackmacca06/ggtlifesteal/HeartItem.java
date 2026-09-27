package io.github.jackmacca06.ggtlifesteal;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Instantly redeems one heart, with all inventory and health changes on the server. */
public final class HeartItem extends Item {
    public HeartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSpectator() || !player.isAlive()) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        if (HeartData.getHearts(serverPlayer) >= HeartData.MAX_HEARTS) {
            serverPlayer.sendOverlayMessage(Component.translatable(
                    "message.ggtlifesteal.heart_limit", HeartData.MAX_HEARTS
            ).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (!HeartData.tryAddHeart(serverPlayer)) {
            return InteractionResult.FAIL;
        }

        // consume() preserves the stack for creative players, as vanilla items do.
        player.getItemInHand(hand).consume(1, player);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.0F, 1.0F);
        serverPlayer.sendOverlayMessage(Component.translatable(
                "message.ggtlifesteal.heart_added",
                HeartData.getHearts(serverPlayer), HeartData.MAX_HEARTS
        ).withStyle(ChatFormatting.GREEN));
        return InteractionResult.SUCCESS;
    }
}
