package io.github.jackmacca06.ggtlifesteal;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

public final class FuryItem extends Item {
    public static final String CHARGE = "ggtls_fury_charge";
    public static final int FULL_CHARGE = 12001;
    public FuryItem(Properties properties) { super(properties); }
    // A visual durability meter, so repair mechanics cannot bypass the saved cooldown.
    private float chargeFraction(ItemStack stack) {
        int charge = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getIntOr(CHARGE, FULL_CHARGE);
        return Math.clamp((float) charge / FULL_CHARGE, 0, 1);
    }
    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) {
        return Math.max(1, Math.round(MAX_BAR_WIDTH * chargeFraction(stack)));
    }
    @Override public int getBarColor(ItemStack stack) {
        return net.minecraft.util.Mth.hsvToRgb(chargeFraction(stack) / 3.0F, 1.0F, 1.0F);
    }
    @Override public boolean canFitInsideContainerItems() { return false; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 32; }
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.DRINK; }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSpectator() || !player.isAlive()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer && !FuryRules.canDrink(serverPlayer, player.getItemInHand(hand)))
            return InteractionResult.FAIL;
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) FuryRules.drink(player, stack);
        return stack;
    }
}
