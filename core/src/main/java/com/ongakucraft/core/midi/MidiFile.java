package com.ongakucraft.core.midi;

import java.util.Collections;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class MidiFile {
    private final String filePath;
    private final String sourceUrl;
    private final int msDuration;
    private final int wholeNoteTicks;
    private final List<MidiTrack> trackList;
    private final List<MidiTempo> tempoList;

    public static MidiFile of(String filePath, String sourceUrl,
                              int msDuration, int wholeNoteTicks,
                              List<MidiTrack> trackList, List<MidiTempo> tempoList) {
        return new MidiFile(filePath, sourceUrl, msDuration, wholeNoteTicks,
                            Collections.unmodifiableList(trackList), Collections.unmodifiableList(tempoList));
    }

    public boolean containsMultipleTempo() {
        return 1 < tempoList.size();
    }

    public boolean containsUnmatchedNoteOn() {
        return trackList.stream().anyMatch(MidiTrack::containsUnmatchedNoteOn);
    }

    public boolean containsUnmatchedNoteOff() {
        return trackList.stream().anyMatch(MidiTrack::containsUnmatchedNoteOff);
    }

    public boolean isValid() {
        return trackList.stream().allMatch(MidiTrack::isValid);
    }
}
