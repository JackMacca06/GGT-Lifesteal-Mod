package io.github.jackmacca06.ggtlifesteal;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class FuryVaultBlock extends VaultBlock {
    public FuryVaultBlock(Properties properties) { super(properties); }
    @Override public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                Player player, InteractionHand hand, BlockHitResult hit) {
        // Key-independent claim path is handled before vanilla interaction by UseBlockCallback.
        return InteractionResult.SUCCESS;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Reuse the vanilla client animation, but never run vanilla key/loot logic server-side.
        return level.isClientSide() ? super.getTicker(level, state, type) : null;
    }
    @Override protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion,
                                            BiConsumer<ItemStack, BlockPos> drops) {}
}
