package io.github.jackmacca06.ggtlifesteal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record FuryVaultState(String id, BlockPos pos, int remaining, String label, boolean forcedByMod) {
    public static final Codec<FuryVaultState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(FuryVaultState::id),
            BlockPos.CODEC.fieldOf("pos").forGetter(FuryVaultState::pos),
            Codec.intRange(0, 12000).fieldOf("remaining").forGetter(FuryVaultState::remaining),
            Codec.STRING.fieldOf("label").forGetter(FuryVaultState::label),
            Codec.BOOL.fieldOf("forced_by_mod").forGetter(FuryVaultState::forcedByMod)
    ).apply(i, FuryVaultState::new));
    public FuryVaultState tick() { return new FuryVaultState(id, pos, Math.max(0, remaining - 1), label, forcedByMod); }
}
