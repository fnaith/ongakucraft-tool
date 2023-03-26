package com.ongakucraft.core.midi;

import java.util.Collections;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class MidiTrack {
    public static MidiTrack of(int id, List<MidiNote> notes,
                               List<MidiNote> unmatchedNoteOnList, List<MidiNote> unmatchedNoteOffList,
                               List<String> instruments) {
        return new MidiTrack(id, Collections.unmodifiableList(notes),
                             Collections.unmodifiableList(unmatchedNoteOnList),
                             Collections.unmodifiableList(unmatchedNoteOffList),
                             Collections.unmodifiableList(instruments));
    }

    private final int id;
    private final List<MidiNote> noteList;
    private final List<MidiNote> unmatchedNoteOnList;
    private final List<MidiNote> unmatchedNoteOffList;
    private final List<String> instruments;

    public boolean containsUnmatchedNoteOn() {
        return !unmatchedNoteOnList.isEmpty();
    }

    public boolean containsUnmatchedNoteOff() {
        return !unmatchedNoteOffList.isEmpty();
    }

    public boolean isValid() {
        return !containsUnmatchedNoteOn() && !containsUnmatchedNoteOff();
    }
}
