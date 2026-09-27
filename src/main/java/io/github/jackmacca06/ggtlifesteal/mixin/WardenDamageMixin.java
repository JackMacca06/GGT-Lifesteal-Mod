package io.github.jackmacca06.ggtlifesteal.mixin;

import io.github.jackmacca06.ggtlifesteal.WardenRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Warden.class)
public abstract class WardenDamageMixin {
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float ggtls$projectileDamage(float amount, ServerLevel level, DamageSource source, float originalAmount) {
        return WardenRules.projectileDamage((Warden) (Object) this, source, amount);
    }
}
