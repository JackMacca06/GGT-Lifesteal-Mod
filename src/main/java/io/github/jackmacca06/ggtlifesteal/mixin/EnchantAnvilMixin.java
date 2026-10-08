package io.github.jackmacca06.ggtlifesteal.mixin;

import io.github.jackmacca06.ggtlifesteal.EnchantRules;
import io.github.jackmacca06.ggtlifesteal.ModState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class EnchantAnvilMixin extends ItemCombinerMenu {
    // These protected fields belong to ItemCombinerMenu, not AnvilMenu.
    protected EnchantAnvilMixin(MenuType<?> type, int id, Inventory inventory,
                                ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slots) {
        super(type, id, inventory, access, slots);
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void ggtls$enchants(CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer
                && !ModState.enchantsOpen(serverPlayer.level().getServer())
                && EnchantRules.addsEnchantments(inputSlots.getItem(0), resultSlots.getItem(0))) {
            resultSlots.setItem(0, ItemStack.EMPTY);
            serverPlayer.sendOverlayMessage(Component.translatable("message.ggtlifesteal.anvil_locked"));
        }
    }
}
