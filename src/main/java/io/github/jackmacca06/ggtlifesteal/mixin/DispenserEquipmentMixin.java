package io.github.jackmacca06.ggtlifesteal.mixin;

import io.github.jackmacca06.ggtlifesteal.CombatRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class DispenserEquipmentMixin {
    @Inject(method = "canEquipWithDispenser", at = @At("HEAD"), cancellable = true)
    private void ggtls$equipment(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (CombatRules.denyEquip((LivingEntity) (Object) this, stack)) cir.setReturnValue(false);
    }
}
