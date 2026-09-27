package io.github.jackmacca06.ggtlifesteal.mixin;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class ShriekerProtectionMixin {
    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void ggtls$protectMining(BlockState state, Player player, BlockGetter level,
                                     BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (state.is(Blocks.SCULK_SHRIEKER) && !player.isCreative()) cir.setReturnValue(0.0F);
    }

    @Inject(method = "onExplosionHit", at = @At("HEAD"), cancellable = true)
    private void ggtls$protectExplosion(BlockState state, ServerLevel level, BlockPos pos,
                                        Explosion explosion, BiConsumer<ItemStack, BlockPos> drops,
                                        CallbackInfo ci) {
        if (state.is(Blocks.SCULK_SHRIEKER)) ci.cancel();
    }
}
