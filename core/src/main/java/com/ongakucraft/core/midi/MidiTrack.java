package com.ongakucraft.core.midi;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class MidiTrack {
    public static MidiTrack of(int id, List<MidiNote> noteList,
                               List<MidiNote> unmatchedNoteOnList, List<MidiNote> unmatchedNoteOffList,
                               List<String> instruments) {
        noteList = noteList.stream().sorted().toList();
        final var minTickOn = noteList.isEmpty() ? 0 : noteList.get(0).getOn();
        final var maxTickOff = noteList.stream().map(MidiNote::getOff).max(Integer::compareTo).orElse(0);
        return new MidiTrack(id, Collections.unmodifiableList(noteList),
                             Collections.unmodifiableList(unmatchedNoteOnList),
                             Collections.unmodifiableList(unmatchedNoteOffList),
                             Collections.unmodifiableList(instruments),
                             minTickOn, maxTickOff);
    }

    private final int id;
    private final List<MidiNote> noteList;
    private final List<MidiNote> unmatchedNoteOnList;
    private final List<MidiNote> unmatchedNoteOffList;
    private final List<String> instruments;
    private final int minTickOn;
    private final int maxTickOff;

    public boolean containsUnmatchedNoteOn() {
        return !unmatchedNoteOnList.isEmpty();
    }

    public boolean containsUnmatchedNoteOff() {
        return !unmatchedNoteOffList.isEmpty();
    }

    public boolean isValid() {
        return !containsUnmatchedNoteOn();// && !containsUnmatchedNoteOff();
    }
}
