package io.github.jackmacca06.ggtlifesteal.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface FuryTextAccessor {
    @Invoker("setText") void ggtls$text(Component text);
    @Invoker("setFlags") void ggtls$flags(byte flags);
    @Invoker("setLineWidth") void ggtls$width(int width);
}
