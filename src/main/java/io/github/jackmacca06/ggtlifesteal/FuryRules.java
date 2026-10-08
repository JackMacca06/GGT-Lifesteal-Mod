package io.github.jackmacca06.ggtlifesteal;

import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.component.*;

/** The saved ownership ledger is authoritative; the inventory stack is its usable representation. */
public final class FuryRules {
    private static final String ID = "ggtls_fury_id";
    private static final AttachmentType<List<FuryEntry>> ENTRIES = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(Ggtlifesteal.MOD_ID, "fury_entries"),
            b -> b.initializer(List::of).persistent(FuryEntry.CODEC.listOf()));
    private static final Map<UUID, Long> PENALTY_AT = new HashMap<>();
    private static int tickCounter;
    private FuryRules() {}
    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> { tickCounter = 0; PENALTY_AT.clear(); });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            List<FuryEntry> entries = new ArrayList<>(entries(server));
            boolean changed = false;
            for (int i = 0; i < entries.size(); i++) {
                FuryEntry entry = entries.get(i);
                if (entry.owner().isEmpty()) continue;
                ServerPlayer owner = server.getPlayerList().getPlayer(UUID.fromString(entry.owner()));
                if (owner != null) {
                    if (entry.cooldown() > 0) { entry = entry.tick(); entries.set(i, entry); changed = true; }
                    if (owner.isAlive()) reconcile(owner, entry, tickCounter % 20 == 0);
                }
            }
            if (changed) save(server, entries);
            FuryVaultRules.tick(server);
        });
    }
    public static List<FuryEntry> entries(MinecraftServer server) {
        return ((AttachmentTarget) server.overworld()).getAttachedOrCreate(ENTRIES);
    }
    public static void save(MinecraftServer server, List<FuryEntry> entries) {
        ((AttachmentTarget) server.overworld()).setAttached(ENTRIES, List.copyOf(entries));
    }
    public static FuryEntry owned(ServerPlayer player) {
        return entries(player.level().getServer()).stream().filter(e -> e.owner().equals(player.getUUID().toString())).findFirst().orElse(null);
    }
    public static boolean isFury(ItemStack stack) { return stack.getItem() instanceof FuryItem; }
    public static void moveCreative(ServerPlayer player, int slot, ItemStack requested) {
        FuryEntry entry = owned(player);
        var menu = player.inventoryMenu;
        if (!player.hasInfiniteMaterials() || slot < 0 || slot >= menu.slots.size()) return;
        Slot destination = menu.getSlot(slot);
        if (destination.container != player.getInventory() || !destination.mayPlace(requested)) return;
        if (entry == null && id(requested).isEmpty()) {
            if (InventoryLocks.locked(player, destination.getItem())) return;
            // The Creative tab supplies an unbound template. Bind exactly one real potion on acquisition.
            FuryEntry created = new FuryEntry(UUID.randomUUID().toString(), player.getUUID().toString(), 0);
            var entries = new ArrayList<>(entries(player.level().getServer()));
            entries.add(created);
            save(player.level().getServer(), entries);
            destination.set(stack(created));
            menu.broadcastFullState();
            return;
        }
        if (entry == null || !entry.id().equals(id(requested))) {
            menu.broadcastFullState();
            return;
        }
        for (Slot source : menu.slots) {
            ItemStack original = source.getItem();
            if (source.container != player.getInventory() || !isFury(original) || !entry.id().equals(id(original))) continue;
            if (source == destination) return;
            ItemStack displaced = destination.getItem();
            if (InventoryLocks.locked(player, displaced) || !source.mayPlace(displaced)) return;
            // Move the server's real stack, never trust a client-supplied duplicate or modified potion.
            source.set(displaced);
            destination.set(original);
            menu.broadcastFullState();
            return;
        }
    }
    private static String id(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr(ID, "");
    }
    public static boolean canDrink(ServerPlayer player, ItemStack stack) {
        FuryEntry entry = owned(player);
        if (entry == null || !entry.id().equals(id(stack))) { message(player, "fury.not_owned"); return false; }
        if (entry.cooldown() > 0) {
            player.sendOverlayMessage(Component.translatable("message.ggtlifesteal.fury.cooldown", LocatorRules.time((entry.cooldown() + 19) / 20)));
            return false;
        }
        return true;
    }
    public static void drink(ServerPlayer player, ItemStack stack) {
        if (!canDrink(player, stack)) return;
        FuryEntry entry = owned(player);
        var entries = new ArrayList<>(entries(player.level().getServer()));
        entries.replaceAll(e -> e.id().equals(entry.id()) ? new FuryEntry(e.id(), e.owner(), 12000) : e);
        save(player.level().getServer(), entries);
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 6000, 0));
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 6000, 0));
        decorate(stack, new FuryEntry(entry.id(), entry.owner(), 12000));
    }
    public static boolean craftClick(AbstractContainerMenu menu, int slot, ContainerInput input, ServerPlayer player) {
        if (!(menu instanceof CraftingMenu crafting) || slot != 0 || !isFury(menu.getSlot(0).getItem())) return false;
        if (input != ContainerInput.PICKUP && input != ContainerInput.QUICK_MOVE && input != ContainerInput.SWAP) return true;
        if (owned(player) != null) {
            long now = System.nanoTime();
            Long last = PENALTY_AT.get(player.getUUID());
            if (last == null || now - last >= 1_000_000_000L) {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
                PENALTY_AT.put(player.getUUID(), now);
            }
            message(player, "fury.only_one"); return true;
        }
        if (freeSlot(player) < 0) { message(player, "fury.make_room"); return true; }
        // This recipe has no remainders. Complete one craft atomically instead of
        // trusting onCraftedBy, which vanilla also calls before failed shift-clicks.
        for (Slot ingredient : crafting.getInputGridSlots()) {
            if (!ingredient.getItem().isEmpty()) ingredient.remove(1);
        }
        crafting.slotsChanged(crafting.getInputGridSlots().getFirst().container);
        var entries = new ArrayList<>(entries(player.level().getServer()));
        FuryEntry entry = new FuryEntry(UUID.randomUUID().toString(), player.getUUID().toString(), 0);
        entries.add(entry); save(player.level().getServer(), entries);
        reconcile(player, entry, true);
        player.getInventory().setChanged(); menu.broadcastFullState();
        return true;
    }
    public static int freeSlot(ServerPlayer player) {
        for (int i = 0; i < 36; i++) if (player.getInventory().getItem(i).isEmpty()) return i;
        return -1;
    }
    private static void reconcile(ServerPlayer owner, FuryEntry entry, boolean refresh) {
        boolean found = false;
        for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
            ItemStack stack = owner.getInventory().getItem(i);
            if (!isFury(stack)) continue;
            if (!entry.id().equals(id(stack)) || found) { owner.getInventory().setItem(i, ItemStack.EMPTY); continue; }
            found = true; stack.setCount(1); if (refresh) decorate(stack, entry);
        }
        ItemStack cursor = owner.containerMenu.getCarried();
        if (isFury(cursor)) {
            if (!found && entry.id().equals(id(cursor))) { found = true; cursor.setCount(1); if (refresh) decorate(cursor, entry); }
            else owner.containerMenu.setCarried(ItemStack.EMPTY);
        }
        if (!found) {
            int slot = freeSlot(owner);
            if (slot >= 0) owner.getInventory().setItem(slot, stack(entry));
            else if (refresh) message(owner, "fury.make_room");
        }
    }
    public static ItemStack stack(FuryEntry entry) {
        var stack = new ItemStack(Ggtlifesteal.DRAGONS_FURY);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(ID, entry.id()));
        decorate(stack, entry); return stack;
    }
    private static void decorate(ItemStack stack, FuryEntry entry) {
        boolean ready = entry.cooldown() == 0;
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt(FuryItem.CHARGE, FuryItem.FULL_CHARGE - entry.cooldown()));
        stack.set(DataComponents.TOOLTIP_DISPLAY, stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT)
                .withHidden(DataComponents.POTION_CONTENTS, true));
        stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("minecraft", ready ? "dragon_breath" : "potion"));
        if (ready) stack.remove(DataComponents.POTION_CONTENTS);
        else stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(),
                Optional.of(0x484D48), List.of(), Optional.empty()));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("tooltip.ggtlifesteal.fury.effects").withStyle(ChatFormatting.GRAY).withStyle(s -> s.withItalic(false)),
                Component.translatable(ready ? "tooltip.ggtlifesteal.fury.ready" : "tooltip.ggtlifesteal.fury.recharging")
                        .withStyle(ready ? ChatFormatting.GREEN : ChatFormatting.RED).withStyle(s -> s.withItalic(false)),
                Component.translatable("tooltip.ggtlifesteal.fury.bound").withStyle(ChatFormatting.DARK_GRAY).withStyle(s -> s.withItalic(false)))));
    }
    private static void removeRepresentations(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            if (isFury(player.getInventory().getItem(i))) player.getInventory().setItem(i, ItemStack.EMPTY);
        if (isFury(player.containerMenu.getCarried())) player.containerMenu.setCarried(ItemStack.EMPTY);
    }
    public static void onDeath(ServerPlayer victim) {
        UUID credited = HeartRewards.creditedPlayer(victim.getCombatTracker().getDeathMessage());
        ServerPlayer killer = credited == null ? null : victim.level().getServer().getPlayerList().getPlayer(credited);
        transfer(victim, killer == victim ? null : killer);
    }
    public static void onCombatLogout(ServerPlayer player) { transfer(player, null); }
    private static void transfer(ServerPlayer victim, ServerPlayer killer) {
        FuryEntry entry = owned(victim);
        removeRepresentations(victim);
        if (entry == null) return;
        var server = victim.level().getServer();
        var entries = new ArrayList<>(entries(server)); entries.removeIf(e -> e.id().equals(entry.id()));
        if (killer != null && owned(killer) != null) dropMaterials(victim);
        else entries.add(entry.withOwner(killer == null ? "" : killer.getUUID().toString()));
        save(server, entries);
        if (killer != null && owned(killer) != null && owned(killer).id().equals(entry.id()) && killer.isAlive())
            reconcile(killer, owned(killer), true);
    }
    public static void dropMaterials(ServerPlayer player) {
        var contents = new BundleContents.Mutable();
        contents.tryInsert(new ItemStack(Items.DRAGON_EGG));
        contents.tryInsert(new ItemStack(Items.GLASS_BOTTLE));
        contents.tryInsert(new ItemStack(Items.DIAMOND_BLOCK, 2));
        contents.tryInsert(new ItemStack(Items.BLAZE_POWDER));
        var bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, contents.toImmutable());
        HeartRewards.drop(player, bundle);
    }
    public static void message(ServerPlayer player, String key) {
        player.sendOverlayMessage(Component.translatable("message.ggtlifesteal." + key));
    }
}
