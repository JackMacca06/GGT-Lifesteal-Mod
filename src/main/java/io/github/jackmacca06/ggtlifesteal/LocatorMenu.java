package io.github.jackmacca06.ggtlifesteal;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ResolvableProfile;

/** Read-only server menu: display stacks can never enter the player's inventory. */
public final class LocatorMenu extends ChestMenu {
    private final ServerPlayer viewer;
    private final Map<Integer, UUID> targets = new HashMap<>();
    private final int page;
    private final UUID selected;
    private final boolean cancelling;
    private final int pages;

    public static void open(ServerPlayer player, int page, UUID selected) {
        boolean cancelling = LocatorRules.active(player);
        player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new LocatorMenu(id, player, page, selected),
                Component.translatable(cancelling ? "menu.ggtlifesteal.locator.cancel"
                        : selected == null ? "menu.ggtlifesteal.locator.select" : "menu.ggtlifesteal.locator.confirm")));
    }
    private LocatorMenu(int id, ServerPlayer player, int page, UUID selected) {
        super(MenuType.GENERIC_9x6, id, player.getInventory(), new SimpleContainer(54), 6);
        this.viewer = player; this.selected = selected; this.cancelling = LocatorRules.active(player);
        var players = player.level().getServer().getPlayerList().getPlayers().stream()
                .filter(p -> p != player && !p.isSpectator() && p.isAlive())
                .sorted(Comparator.comparing(p -> p.getGameProfile().name())).toList();
        pages = Math.max(1, (players.size() + 44) / 45);
        this.page = Math.clamp(page, 0, pages - 1);
        if (cancelling || selected != null) {
            icon(21, Items.EMERALD, cancelling ? "menu.ggtlifesteal.locator.cancel_yes" : "menu.ggtlifesteal.locator.start");
            icon(23, Items.BARRIER, "menu.ggtlifesteal.locator.back");
            if (selected != null) {
                ServerPlayer target = player.level().getServer().getPlayerList().getPlayer(selected);
                if (target != null) head(13, target);
            }
        } else {
            int start = this.page * 45;
            for (int i = start; i < Math.min(start + 45, players.size()); i++) head(i - start, players.get(i));
            if (this.page > 0) icon(45, Items.ARROW, "menu.ggtlifesteal.locator.previous");
            if (this.page + 1 < pages) icon(53, Items.ARROW, "menu.ggtlifesteal.locator.next");
            icon(49, Items.BARRIER, "menu.ggtlifesteal.locator.close");
        }
    }
    private void head(int slot, ServerPlayer target) {
        var stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(target.getGameProfile()));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(target.getGameProfile().name())
                .withStyle(s -> s.withItalic(false)));
        getSlot(slot).set(stack); targets.put(slot, target.getUUID());
    }
    private void icon(int slot, Item item, String key) {
        var stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(key).withStyle(s -> s.withItalic(false)));
        getSlot(slot).set(stack);
    }
    @Override public void clicked(int slot, int button, ContainerInput input, Player player) {
        if (player != viewer || input != ContainerInput.PICKUP || slot < 0 || slot >= 54) return;
        if (cancelling || selected != null) {
            if (slot == 21) {
                viewer.closeContainer();
                if (cancelling) LocatorRules.cancel(viewer); else LocatorRules.start(viewer, selected);
            } else if (slot == 23) {
                viewer.closeContainer(); if (!cancelling) open(viewer, page, null);
            }
        } else if (targets.containsKey(slot)) open(viewer, page, targets.get(slot));
        else if (slot == 45 && page > 0) open(viewer, page - 1, null);
        else if (slot == 53 && page + 1 < pages) open(viewer, page + 1, null);
        else if (slot == 49) viewer.closeContainer();
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return player == viewer && player.isAlive(); }
}
