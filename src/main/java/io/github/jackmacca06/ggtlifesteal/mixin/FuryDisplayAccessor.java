package io.github.jackmacca06.ggtlifesteal.mixin;

import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface FuryDisplayAccessor {
    @Invoker("setBillboardConstraints") void ggtls$billboard(Display.BillboardConstraints value);
    @Invoker("setViewRange") void ggtls$range(float range);
}
