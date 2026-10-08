package io.github.jackmacca06.ggtlifesteal;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class LocatorItem extends Item {
    public LocatorItem(Properties properties) { super(properties); }
    @Override public boolean canFitInsideContainerItems() { return false; }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSpectator() || !player.isAlive()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) LocatorMenu.open(serverPlayer, 0, null);
        return InteractionResult.SUCCESS;
    }
}
