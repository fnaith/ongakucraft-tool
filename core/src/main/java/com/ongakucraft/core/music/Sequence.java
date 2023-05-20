package com.ongakucraft.core.music;

import com.ongakucraft.core.midi.MidiNote;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class Sequence {
    public static Sequence of(int id, Staff staff, List<MidiNote> beatToNote) {
        return new Sequence(id, staff, Collections.unmodifiableList(beatToNote));
    }

    private final int id;
    private final Staff staff;
    private final List<MidiNote> beatToNote;
    @Getter(lazy = true) private final int minKey = minKey();
    @Getter(lazy = true) private final int maxKey = maxKey();
    @Getter(lazy = true) private final int count = count();

    private int minKey() {
        return beatToNote.stream().filter(Objects::nonNull).map(MidiNote::getKey).min(Integer::compare).orElse(0);
    }

    private int maxKey() {
        return beatToNote.stream().filter(Objects::nonNull).map(MidiNote::getKey).max(Integer::compare).orElse(128);
    }

    private int count() {
        return (int) beatToNote.stream().filter(Objects::nonNull).count();
    }
}
