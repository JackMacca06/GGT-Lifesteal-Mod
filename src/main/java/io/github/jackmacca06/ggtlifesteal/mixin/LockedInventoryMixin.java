package io.github.jackmacca06.ggtlifesteal.mixin;

import io.github.jackmacca06.ggtlifesteal.InventoryLocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class LockedInventoryMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void ggtls$locked(int slot, int button, ContainerInput input, Player player, CallbackInfo ci) {
        var menu = (AbstractContainerMenu) (Object) this;
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && io.github.jackmacca06.ggtlifesteal.FuryRules.craftClick(menu, slot, input, serverPlayer)) {
            menu.broadcastFullState(); ci.cancel(); return;
        }
        if (InventoryLocks.rejectClick(menu, slot, button, input, player)) {
            menu.broadcastFullState(); ci.cancel();
        }
    }
}
