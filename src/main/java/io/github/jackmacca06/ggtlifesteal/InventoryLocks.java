package io.github.jackmacca06.ggtlifesteal;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public final class InventoryLocks {
    private InventoryLocks() {}
    public static void initialize() {
        // These interactions bypass container menus (item frames, allays and decorated pots).
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
                locked(player, player.getItemInHand(hand)) ? net.minecraft.world.InteractionResult.FAIL
                        : net.minecraft.world.InteractionResult.PASS);
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, level, hand, hit) ->
                locked(player, player.getItemInHand(hand)) ? net.minecraft.world.InteractionResult.FAIL
                        : net.minecraft.world.InteractionResult.PASS);
    }
    public static boolean locked(Player player, ItemStack stack) {
        return FuryRules.isFury(stack) || player instanceof ServerPlayer serverPlayer && LocatorRules.locked(serverPlayer, stack);
    }
    public static boolean rejectClick(AbstractContainerMenu menu, int slot, int button, ContainerInput input, Player player) {
        ItemStack carried = menu.getCarried();
        var target = slot >= 0 && slot < menu.slots.size() ? menu.getSlot(slot) : null;
        boolean ownSlot = target != null && target.container == player.getInventory();
        boolean swapFury = input == ContainerInput.SWAP && button >= 0
                && button < player.getInventory().getContainerSize()
                && FuryRules.isFury(player.getInventory().getItem(button));
        if (swapFury || target != null && FuryRules.isFury(target.getItem()) && input == ContainerInput.SWAP) {
            return !ownSlot || button < 0 || button >= player.getInventory().getContainerSize()
                    || locked(player, target.getItem()) && !FuryRules.isFury(target.getItem())
                    || locked(player, player.getInventory().getItem(button)) && !swapFury;
        }
        if (FuryRules.isFury(carried)) {
            return target == null || target.container != player.getInventory()
                    || input != ContainerInput.PICKUP || locked(player, target.getItem());
        }
        if (target != null && FuryRules.isFury(target.getItem())) {
            boolean inventoryShift = input == ContainerInput.QUICK_MOVE
                    && menu instanceof net.minecraft.world.inventory.InventoryMenu;
            return !ownSlot || input != ContainerInput.PICKUP && !inventoryShift || locked(player, carried);
        }
        if (locked(player, carried)) return true;
        if (target != null && locked(player, target.getItem())) return true;
        if (input == ContainerInput.SWAP && button >= 0 && button < player.getInventory().getContainerSize()
                && locked(player, player.getInventory().getItem(button))) return true;
        return input == ContainerInput.PICKUP_ALL && menu.slots.stream().anyMatch(s -> locked(player, s.getItem()));
    }
}
