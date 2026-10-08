package io.github.jackmacca06.ggtlifesteal.mixin;

import io.github.jackmacca06.ggtlifesteal.FuryVaultBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityType.class)
public abstract class FuryVaultTypeMixin {
    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    private void ggtls$eventVault(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == BlockEntityTypes.VAULT && state.getBlock() instanceof FuryVaultBlock) cir.setReturnValue(true);
    }
}
