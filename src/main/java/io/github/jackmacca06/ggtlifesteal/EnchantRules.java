package io.github.jackmacca06.ggtlifesteal;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;

public final class EnchantRules {
    private EnchantRules() {}
    public static void initialize() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player instanceof ServerPlayer serverPlayer && !player.isSpectator()
                    && level.getBlockState(hit.getBlockPos()).is(Blocks.ENCHANTING_TABLE)
                    && !ModState.enchantsOpen(serverPlayer.level().getServer())) {
                serverPlayer.sendOverlayMessage(Component.translatable("message.ggtlifesteal.enchants_locked"));
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    public static ItemStack craftingResult(ItemStack result, ServerLevel level) {
        return result.is(Items.ENCHANTING_TABLE) && !ModState.enchantsOpen(level.getServer())
                ? ItemStack.EMPTY : result;
    }

    public static boolean addsEnchantments(ItemStack input, ItemStack output) {
        return increased(input.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY),
                         output.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY))
                || increased(input.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY),
                             output.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
    }

    private static boolean increased(ItemEnchantments before, ItemEnchantments after) {
        return after.entrySet().stream().anyMatch(entry ->
                entry.getIntValue() > before.getLevel(entry.getKey()));
    }
}
