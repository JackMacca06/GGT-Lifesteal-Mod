package io.github.jackmacca06.ggtlifesteal.mixin;

import io.github.jackmacca06.ggtlifesteal.InventoryLocks;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class LockedDropMixin {
    @Inject(method = "drop(Z)V", at = @At("HEAD"), cancellable = true)
    private void ggtls$drop(boolean wholeStack, CallbackInfo ci) {
        var player = (ServerPlayer) (Object) this;
        if (InventoryLocks.locked(player, player.getMainHandItem())) {
            player.containerMenu.broadcastFullState(); ci.cancel();
        }
    }
}
