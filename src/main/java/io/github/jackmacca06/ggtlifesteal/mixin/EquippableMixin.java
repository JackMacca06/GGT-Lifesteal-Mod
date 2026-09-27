package io.github.jackmacca06.ggtlifesteal.mixin;

import io.github.jackmacca06.ggtlifesteal.CombatRules;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Equippable.class)
public abstract class EquippableMixin {
    @Inject(method = "swapWithEquipmentSlot", at = @At("HEAD"), cancellable = true)
    private void ggtls$equipment(ItemStack stack, Player player, CallbackInfoReturnable<InteractionResult> cir) {
        if (CombatRules.denyEquip(player, stack)) cir.setReturnValue(InteractionResult.FAIL);
    }
}
