package io.github.jackmacca06.ggtlifesteal.mixin;

import io.github.jackmacca06.ggtlifesteal.HeartRewards;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class DeathAttributionMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void ggtls$captureCredit(DamageSource source, CallbackInfo ci) {
        HeartRewards.captureDeath((ServerPlayer) (Object) this);
        io.github.jackmacca06.ggtlifesteal.FuryRules.onDeath((ServerPlayer) (Object) this);
    }
}
