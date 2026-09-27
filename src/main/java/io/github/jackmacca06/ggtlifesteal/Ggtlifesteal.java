package io.github.jackmacca06.ggtlifesteal;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

public class Ggtlifesteal implements ModInitializer {

    public static final String MOD_ID = "ggtlifesteal";
    public static Item HEART;
    public static Item MAKESHIFT_HEART;

    @Override
    public void onInitialize() {
        HeartData.initialize();
        // Give the item its unique name: ggtlifesteal:heart.
        Identifier heartId = Identifier.fromNamespaceAndPath(MOD_ID, "heart");
        ResourceKey<Item> heartKey = ResourceKey.create(Registries.ITEM, heartId);

        // Make the heart stackable and give it an enchantment glint.
        Item.Properties heartSettings = new Item.Properties()
                .setId(heartKey)
                .stacksTo(64)
                .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .component(
                        DataComponents.CUSTOM_NAME,
                        Component.translatable("item.ggtlifesteal.heart")
                                .withStyle(ChatFormatting.DARK_RED)
                                .withStyle(style -> style.withBold(true))
                                .withStyle(style -> style.withItalic(true))
                )
                .component(DataComponents.LORE, new ItemLore(List.of(
                        Component.translatable("tooltip.ggtlifesteal.heart.use")
                                .withStyle(ChatFormatting.GRAY)
                                .withStyle(style -> style.withItalic(false)),
                        Component.translatable("tooltip.ggtlifesteal.heart.limit", HeartData.MAX_HEARTS)
                                .withStyle(ChatFormatting.DARK_GRAY)
                                .withStyle(style -> style.withItalic(false))
                )));

        // Add the item to Minecraft's item registry.
        HEART = Registry.register(
                BuiltInRegistries.ITEM,
                heartKey,
                new HeartItem(heartSettings));

        ResourceKey<Item> makeshiftKey = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, "makeshift_heart"));
        MAKESHIFT_HEART = Registry.register(BuiltInRegistries.ITEM, makeshiftKey,
                new MakeshiftHeartItem(new Item.Properties().setId(makeshiftKey).stacksTo(64)
                        .component(DataComponents.CUSTOM_NAME,
                                Component.translatable("item.ggtlifesteal.makeshift_heart")
                                        .withStyle(ChatFormatting.GREEN).withStyle(s -> s.withItalic(false)))
                        .component(DataComponents.LORE, new ItemLore(List.of(
                                Component.translatable("tooltip.ggtlifesteal.makeshift_heart")
                                        .withStyle(ChatFormatting.GRAY).withStyle(s -> s.withItalic(false)))))));

        ModState.initialize();
        CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath("minecraft", "ingredients"))).register(tab -> {
            tab.accept(HEART);
            tab.accept(MAKESHIFT_HEART);
        });
        CombatRules.initialize();
        HeartRewards.initialize();
        PlaytimeRules.initialize();
        EndRules.initialize();
        GgtCommands.initialize();
        WardenRules.initialize();
        ShriekerRules.initialize();
    }
}
