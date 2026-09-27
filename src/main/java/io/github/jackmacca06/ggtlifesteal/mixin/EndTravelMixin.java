package io.github.jackmacca06.ggtlifesteal.mixin;

import io.github.jackmacca06.ggtlifesteal.EndRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Set;

@Mixin(ServerPlayer.class)
public abstract class EndTravelMixin {
    @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("HEAD"), cancellable = true)
    private void ggtls$portal(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> cir) {
        if (EndRules.denied((ServerPlayer) (Object) this, transition.newLevel())) cir.setReturnValue(null);
    }
    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z", at = @At("HEAD"), cancellable = true)
    private void ggtls$teleport(ServerLevel destination, double x, double y, double z, Set<Relative> relatives,
                               float yaw, float pitch, boolean resetCamera, CallbackInfoReturnable<Boolean> cir) {
        if (EndRules.denied((ServerPlayer) (Object) this, destination)) cir.setReturnValue(false);
    }
}
