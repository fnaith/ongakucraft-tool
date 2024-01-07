package com.ongakucraft.core.midi;

import lombok.*;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(cacheStrategy = LAZY, exclude = "duration")
@Getter
@ToString
public final class MidiNote implements Comparable<MidiNote> {
    private static final int DEFAULT_VELOCITY = 100;

    private final int key;
    private final int on;
    private final int off;
    private final int velocity;
    private final int duration;

    public static MidiNote of(int key, int on, int off) {
        return of(key, on, off, DEFAULT_VELOCITY);
    }

    public static MidiNote of(int key, int on, int off, int velocity) {
        return new MidiNote(key, on, off, velocity, off - on);
    }

    @Override
    public int compareTo(MidiNote other) {
        final var cmp1 = Integer.compare(on, other.on);
        if (0 != cmp1) {
            return cmp1;
        }
        final var cmp2 = Integer.compare(key, other.key);
        if (0 != cmp2) {
            return cmp2;
        }
        return Integer.compare(off, other.off);
    }
}
