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

public final class MakeshiftHeartItem extends Item {
    public MakeshiftHeartItem(Properties properties) { super(properties); }
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSpectator() || !player.isAlive()) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        if (!HeartData.tryAddHeart(serverPlayer, true)) {
            player.sendOverlayMessage(Component.translatable("message.ggtlifesteal.makeshift_limit").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        player.getItemInHand(hand).consume(1, player);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BONE_BLOCK_BREAK, SoundSource.PLAYERS, 0.8F, 0.7F);
        player.sendOverlayMessage(Component.translatable("message.ggtlifesteal.makeshift_added",
                HeartData.getHearts(serverPlayer)).withStyle(ChatFormatting.GREEN));
        return InteractionResult.SUCCESS;
    }
}
