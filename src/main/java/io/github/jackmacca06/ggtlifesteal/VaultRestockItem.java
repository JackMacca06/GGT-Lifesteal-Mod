package io.github.jackmacca06.ggtlifesteal;

import io.github.jackmacca06.ggtlifesteal.mixin.VaultServerDataAccessor;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultState;

public final class VaultRestockItem extends Item {
    private final boolean ominous;
    public VaultRestockItem(Properties properties, boolean ominous) {
        super(properties);
        this.ominous = ominous;
    }

    public static void initialize() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            var stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof VaultRestockItem restock) || player.isSpectator()) {
                return InteractionResult.PASS;
            }
            var state = level.getBlockState(hit.getBlockPos());
            // Only vanilla vaults qualify; the separate Fury event block cannot be reset.
            if (!state.is(Blocks.VAULT)) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
            if (state.getValue(VaultBlock.OMINOUS) != restock.ominous) {
                return fail(serverPlayer, "message.ggtlifesteal.restock.wrong_type");
            }
            var vaultState = state.getValue(VaultBlock.STATE);
            if (vaultState == VaultState.UNLOCKING || vaultState == VaultState.EJECTING) {
                return fail(serverPlayer, "message.ggtlifesteal.restock.busy");
            }
            if (!(level.getBlockEntity(hit.getBlockPos()) instanceof VaultBlockEntity vault)
                    || vault.getServerData() == null) return InteractionResult.FAIL;
            var data = (VaultServerDataAccessor) vault.getServerData();
            if (!data.ggtls$rewardedPlayers().remove(player.getUUID())) {
                return fail(serverPlayer, "message.ggtlifesteal.restock.unclaimed");
            }
            data.ggtls$markChanged();
            vault.setChanged();
            stack.consume(1, player);
            level.playSound(null, hit.getBlockPos(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 1.0F, restock.ominous ? 0.7F : 1.0F);
            serverPlayer.sendOverlayMessage(Component.translatable("message.ggtlifesteal.restock.success"));
            return InteractionResult.SUCCESS;
        });
    }

    private static InteractionResult fail(ServerPlayer player, String key) {
        player.sendOverlayMessage(Component.translatable(key).withStyle(net.minecraft.ChatFormatting.RED));
        return InteractionResult.FAIL;
    }
}
