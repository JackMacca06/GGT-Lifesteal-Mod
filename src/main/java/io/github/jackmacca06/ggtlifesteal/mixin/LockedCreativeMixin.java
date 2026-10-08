package io.github.jackmacca06.ggtlifesteal.mixin;

import io.github.jackmacca06.ggtlifesteal.InventoryLocks;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class LockedCreativeMixin {
    @Shadow public ServerPlayer player;
    @Inject(method = "handleSetCreativeModeSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER), cancellable = true)
    private void ggtls$protectBoundItems(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        int slot = packet.slotNum();
        if (io.github.jackmacca06.ggtlifesteal.FuryRules.isFury(packet.itemStack())) {
            io.github.jackmacca06.ggtlifesteal.FuryRules.moveCreative(player, slot, packet.itemStack());
            ci.cancel(); return;
        }
        if (slot >= 0 && slot < player.inventoryMenu.slots.size()
                && io.github.jackmacca06.ggtlifesteal.FuryRules.isFury(player.inventoryMenu.getSlot(slot).getItem())) {
            // Creative sends source removal and destination placement separately.
            // Keep the real item until the destination packet can move it atomically.
            ci.cancel(); return;
        }
        if (InventoryLocks.locked(player, packet.itemStack())
                || slot >= 0 && slot < player.inventoryMenu.slots.size()
                && InventoryLocks.locked(player, player.inventoryMenu.getSlot(slot).getItem())) {
            player.inventoryMenu.broadcastFullState();
            ci.cancel();
        }
    }
}
