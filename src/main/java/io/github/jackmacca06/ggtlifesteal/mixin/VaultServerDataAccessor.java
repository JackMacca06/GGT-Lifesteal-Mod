package io.github.jackmacca06.ggtlifesteal.mixin;

import java.util.Set;
import java.util.UUID;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(VaultServerData.class)
public interface VaultServerDataAccessor {
    @Accessor("rewardedPlayers") Set<UUID> ggtls$rewardedPlayers();
    @Invoker("markChanged") void ggtls$markChanged();
}
