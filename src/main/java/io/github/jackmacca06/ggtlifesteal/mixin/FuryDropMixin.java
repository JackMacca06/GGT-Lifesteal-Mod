package io.github.jackmacca06.ggtlifesteal.mixin;

import io.github.jackmacca06.ggtlifesteal.FuryRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class FuryDropMixin {
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void ggtls$noFuryDrop(ItemStack stack, boolean random, Prediction prediction, CallbackInfoReturnable<ItemEntity> cir) {
        // A full inventory during menu close must not eject the bound item.
        // Its saved owner will receive a replacement on the next inventory reconciliation.
        if (FuryRules.isFury(stack)) cir.setReturnValue(null);
    }
}
