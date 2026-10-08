package io.github.jackmacca06.ggtlifesteal;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;

/** Shared names, concise lore and fixed head profiles for functional custom items. */
public final class ItemPresentation {
    public static final String HEART_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWI3NmI0ZWU5ODg1NzIyOTdjYmQ4NzQ2ODNiZWU5NmFlM2M1NWNlOTRjMDA0ZTUxYWRjODJjZWUxNmNkMGIwYyJ9fX0=";
    public static final String MAKESHIFT_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjUwYWFjYzE3YTA5ZDkwMzNkNTQ1NzliNjNiMTY0OGI3MzZkMjc0MDUwZmU4N2JkNGMxMDExNjYyMjhkZjEzMCJ9fX0=";
    public static final String VAULT_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTE3NjNiZThkYzA3NGZiMWRhYmE3YjFlYTIwZDE3MjhmY2I4ODM4ZTBlMTFmMWFiZmJjMDBlZmQ5MDRhODBhIn19fQ==";
    public static final String OMINOUS_VAULT_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjQ2ODUwMzJhZDRiNjFlYzE3NDljYjJmN2Y1ZmQ4MDhmZjVkN2Q3ZDg2YjJjM2M3NjE5ZjMzZGE4MzUyYTQzYSJ9fX0=";

    private ItemPresentation() {}

    public static ResolvableProfile headProfile(String texture) {
        UUID id = UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8));
        PropertyMap properties = new PropertyMap(ImmutableMultimap.of(
                "textures", new Property("textures", texture)));
        return ResolvableProfile.createResolved(new GameProfile(id, "GGTItem", properties));
    }

    public static Item.Properties head(Item.Properties properties, String texture) {
        // Render as a head without inheriting BlockItem placement or head equipment behavior.
        return properties.component(DataComponents.PROFILE, headProfile(texture));
    }

    public static Item.Properties describe(Item.Properties properties, String name,
                                            ChatFormatting color, String... loreKeys) {
        return properties.component(DataComponents.CUSTOM_NAME,
                        Component.translatable(name).withStyle(color).withStyle(s -> s.withItalic(false)))
                .component(DataComponents.LORE, new ItemLore(Arrays.stream(loreKeys)
                        .<Component>map(key -> Component.translatable(key).withStyle(ChatFormatting.GRAY)
                                .withStyle(s -> s.withItalic(false))).toList()));
    }
}
