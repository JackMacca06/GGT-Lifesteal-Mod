package io.github.jackmacca06.ggtlifesteal.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.jackmacca06.ggtlifesteal.EnchantRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CraftingMenu.class)
public abstract class EnchantCraftingMixin {
    @WrapOperation(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;assemble(Lnet/minecraft/world/item/crafting/RecipeInput;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack ggtls$crafting(CraftingRecipe recipe, RecipeInput input,
                                            Operation<ItemStack> original, @Local(argsOnly = true) ServerLevel level) {
        return EnchantRules.craftingResult(original.call(recipe, input), level);
    }
}
