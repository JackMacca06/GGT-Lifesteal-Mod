package io.github.jackmacca06.ggtlifesteal;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.ChatFormatting;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;




public class Ggtlifesteal implements ModInitializer {

    public static final String MOD_ID = "ggtlifesteal";
    public static Item HEART;
    public static Item MAKESHIFT_HEART;
    public static Item VAULT_RESTOCK;
    public static Item OMINOUS_VAULT_RESTOCK;
    public static Item LOCATOR_COMPASS;
    public static Item TRACKING_CHARGE;
    public static Item DRAGONS_FURY;
    public static net.minecraft.world.level.block.Block FURY_VAULT;

    @Override
    public void onInitialize() {
        HeartData.initialize();
        // Give the item its unique name: ggtlifesteal:heart.
        Identifier heartId = Identifier.fromNamespaceAndPath(MOD_ID, "heart");
        ResourceKey<Item> heartKey = ResourceKey.create(Registries.ITEM, heartId);

        Item.Properties heartSettings = ItemPresentation.describe(
                ItemPresentation.head(new Item.Properties().setId(heartKey).stacksTo(64)
                        .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true), ItemPresentation.HEART_TEXTURE),
                "item.ggtlifesteal.heart", ChatFormatting.RED,
                "tooltip.ggtlifesteal.heart.use", "tooltip.ggtlifesteal.heart.capacity");
        HEART = Registry.register(BuiltInRegistries.ITEM, heartKey, new HeartItem(heartSettings));

        ResourceKey<Item> makeshiftKey = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, "makeshift_heart"));
        Item.Properties makeshiftSettings = ItemPresentation.describe(
                ItemPresentation.head(new Item.Properties().setId(makeshiftKey).stacksTo(64),
                        ItemPresentation.MAKESHIFT_TEXTURE),
                "item.ggtlifesteal.makeshift_heart", ChatFormatting.GREEN,
                "tooltip.ggtlifesteal.makeshift.use", "tooltip.ggtlifesteal.makeshift.limit");
        MAKESHIFT_HEART = Registry.register(BuiltInRegistries.ITEM, makeshiftKey,
                new MakeshiftHeartItem(makeshiftSettings));
        VAULT_RESTOCK = registerRestock("vault_restock", false, ItemPresentation.VAULT_TEXTURE);
        OMINOUS_VAULT_RESTOCK = registerRestock("ominous_vault_restock", true, ItemPresentation.OMINOUS_VAULT_TEXTURE);
        VaultRestockItem.initialize();
        ResourceKey<Item> locatorKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "locator_compass"));
        LOCATOR_COMPASS = Registry.register(BuiltInRegistries.ITEM, locatorKey, new LocatorItem(ItemPresentation.describe(
                new Item.Properties().setId(locatorKey).stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true),
                "item.ggtlifesteal.locator_compass", ChatFormatting.AQUA,
                "tooltip.ggtlifesteal.locator.use", "tooltip.ggtlifesteal.locator.fuel", "tooltip.ggtlifesteal.locator.lock")));
        ResourceKey<Item> chargeKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "tracking_charge"));
        TRACKING_CHARGE = Registry.register(BuiltInRegistries.ITEM, chargeKey, new Item(ItemPresentation.describe(
                new Item.Properties().setId(chargeKey).stacksTo(64).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true),
                "item.ggtlifesteal.tracking_charge", ChatFormatting.BLUE, "tooltip.ggtlifesteal.charge.use")));
        LocatorRules.initialize();
        ResourceKey<Item> furyKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "dragons_fury"));
        DRAGONS_FURY = Registry.register(BuiltInRegistries.ITEM, furyKey, new FuryItem(ItemPresentation.describe(
                new Item.Properties().setId(furyKey).stacksTo(1)
                        .component(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("minecraft", "dragon_breath")),
                "item.ggtlifesteal.dragons_fury", ChatFormatting.LIGHT_PURPLE,
                "tooltip.ggtlifesteal.fury.effects", "tooltip.ggtlifesteal.fury.ready", "tooltip.ggtlifesteal.fury.bound")));
        var vaultKey = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "fury_vault"));
        FURY_VAULT = Registry.register(BuiltInRegistries.BLOCK, vaultKey, new FuryVaultBlock(
                net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.VAULT)
                        .setId(vaultKey).strength(-1.0F, 3600000.0F).noLootTable()
                        .pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)));
        FuryRules.initialize();
        FuryVaultRules.initialize();
        InventoryLocks.initialize();
        ModState.initialize();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath(MOD_ID, "items"),
                net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder()
                        .title(net.minecraft.network.chat.Component.translatable("itemGroup.ggtlifesteal"))
                        .icon(() -> new net.minecraft.world.item.ItemStack(HEART)).build());
        CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath(MOD_ID, "items"))).register(output -> {
                            output.accept(HEART);
                            output.accept(MAKESHIFT_HEART);
                            output.accept(LOCATOR_COMPASS);
                            output.accept(TRACKING_CHARGE);
                            output.accept(DRAGONS_FURY);
                            output.accept(VAULT_RESTOCK);
                            output.accept(OMINOUS_VAULT_RESTOCK);
                        });
        CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath("minecraft", "ingredients"))).register(tab -> {
            tab.accept(HEART);
            tab.accept(MAKESHIFT_HEART);
            tab.accept(VAULT_RESTOCK);
            tab.accept(OMINOUS_VAULT_RESTOCK);
            tab.accept(LOCATOR_COMPASS);
            tab.accept(TRACKING_CHARGE);
        });
        CombatRules.initialize();
        HeartRewards.initialize();
        PlaytimeRules.initialize();
        EndRules.initialize();
        GgtCommands.initialize();
        WardenRules.initialize();
        ShriekerRules.initialize();
        EnchantRules.initialize();
    }

    private static Item registerRestock(String path, boolean ominous, String texture) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, path));
        Item.Properties properties = ItemPresentation.describe(
                ItemPresentation.head(new Item.Properties().setId(key).stacksTo(64), texture),
                "item.ggtlifesteal." + path, ominous ? ChatFormatting.AQUA : ChatFormatting.GOLD,
                "tooltip.ggtlifesteal." + path + ".use", "tooltip.ggtlifesteal.restock.key");
        return Registry.register(BuiltInRegistries.ITEM, key, new VaultRestockItem(properties, ominous));
    }
}
