package io.github.jackmacca06.ggtlifesteal;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.world.level.block.Blocks;

public final class ShriekerRules {
    private ShriekerRules() {}

    public static void initialize() {
        // Server-side enforcement also covers clients without the mining-progress hook.
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) ->
                !state.is(Blocks.SCULK_SHRIEKER) || player.isCreative());
    }
}
