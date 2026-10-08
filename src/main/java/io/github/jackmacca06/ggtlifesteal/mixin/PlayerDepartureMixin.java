package io.github.jackmacca06.ggtlifesteal.mixin;

import io.github.jackmacca06.ggtlifesteal.CombatRules;
import io.github.jackmacca06.ggtlifesteal.PlaytimeRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerDepartureMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void ggtls$beforeSave(ServerPlayer player, CallbackInfo ci) {
        CombatRules.onDisconnect(player);
        io.github.jackmacca06.ggtlifesteal.LocatorRules.onDisconnect(player);
        PlaytimeRules.onDisconnect(player);
    }
}
