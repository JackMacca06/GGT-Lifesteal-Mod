package io.github.jackmacca06.ggtlifesteal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Empty owner means queued at spawn. Order in the saved list is event order. */
public record FuryEntry(String id, String owner, int cooldown) {
    public static final Codec<FuryEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(FuryEntry::id),
            Codec.STRING.fieldOf("owner").forGetter(FuryEntry::owner),
            Codec.intRange(0, 12000).fieldOf("cooldown").forGetter(FuryEntry::cooldown)
    ).apply(i, FuryEntry::new));
    public FuryEntry withOwner(String owner) { return new FuryEntry(id, owner, 0); }
    public FuryEntry tick() { return cooldown > 0 ? new FuryEntry(id, owner, cooldown - 1) : this; }
}
