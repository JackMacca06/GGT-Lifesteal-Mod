package io.github.jackmacca06.ggtlifesteal.mixin;

import io.github.jackmacca06.ggtlifesteal.WardenRules;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileLaunchMixin {
    @Inject(method = "shoot", at = @At("HEAD"))
    private void ggtls$capture(double x, double y, double z, float power, float inaccuracy, CallbackInfo ci) {
        WardenRules.capture((Projectile) (Object) this);
    }
}
