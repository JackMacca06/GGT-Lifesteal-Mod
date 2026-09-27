package io.github.jackmacca06.ggtlifesteal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.*;

/** Immutable saved accounting value, independent of Fabric lifecycle callbacks. */
public record DailyPlaytime(long day, long millis) {
    public static final ZoneOffset ACST = ZoneOffset.ofHoursMinutes(9, 30);
    public static final Codec<DailyPlaytime> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("day").forGetter(DailyPlaytime::day),
            Codec.LONG.validate(value -> value >= 0 ? DataResult.success(value)
                    : DataResult.error(() -> "Playtime cannot be negative"))
                    .fieldOf("millis").forGetter(DailyPlaytime::millis)
    ).apply(i, DailyPlaytime::new));

    public static long dayAt(long now) {
        return Instant.ofEpochMilli(now).atOffset(ACST).toLocalDate().toEpochDay();
    }
    public DailyPlaytime accrue(long from, long now) {
        long today = dayAt(now);
        long start = LocalDate.ofEpochDay(today).atStartOfDay().toInstant(ACST).toEpochMilli();
        long elapsed = Math.max(0, now - Math.max(from, start));
        long previous = day == today ? millis : 0;
        return new DailyPlaytime(today, previous + elapsed);
    }
}
