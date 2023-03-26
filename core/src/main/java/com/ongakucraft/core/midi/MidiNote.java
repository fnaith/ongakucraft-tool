package com.ongakucraft.core.midi;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(cacheStrategy = LAZY, exclude = "duration")
@Getter
@ToString
public final class MidiNote implements Comparable<MidiNote> {
    private final int key;
    private final int on;
    private final int off;
    private final int duration;

    public static MidiNote of(int key, int on, int off) {
        return new MidiNote(key, on, off, off - on);
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
