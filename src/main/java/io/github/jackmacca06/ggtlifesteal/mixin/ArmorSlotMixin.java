package io.github.jackmacca06.ggtlifesteal.mixin;

import io.github.jackmacca06.ggtlifesteal.CombatRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorSlot.class)
public abstract class ArmorSlotMixin {
    @Shadow @Final private LivingEntity owner;
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void ggtls$equipment(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (CombatRules.denyEquip(owner, stack)) cir.setReturnValue(false);
    }
}
