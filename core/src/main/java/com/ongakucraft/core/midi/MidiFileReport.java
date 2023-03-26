package com.ongakucraft.core.midi;

import java.util.Collections;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class MidiFileReport {
    public static final List<Integer> NOTE_DIVISIONS = List.of(4, 8, 16, 32);

    private final MidiFile file;
    private final List<MidiTrackReport> trackReportList;

    public static MidiFileReport of(MidiFile midiFile) {
        final var divisionList = buildDivisionList(midiFile.getWholeNoteTicks());
        final var trackReports = midiFile.getTrackList().stream().filter(track -> !track.getNoteList().isEmpty())
                                         .map(track -> MidiTrackReport.of(track, midiFile.getWholeNoteTicks(), divisionList)).toList();
        return new MidiFileReport(midiFile, Collections.unmodifiableList(trackReports));
    }

    private static List<Integer> buildDivisionList(int wholeNoteTicks) {
        return NOTE_DIVISIONS.stream().filter(division -> 0 == wholeNoteTicks % division).toList();
    }

    public boolean isNormal() {
        return !trackReportList.isEmpty() && trackReportList.stream().allMatch(MidiTrackReport::isNormal);
    }

    public boolean containsModifiedTrack() {
        return trackReportList.stream().anyMatch(trackReport -> 0 != trackReport.getDurationModifier());
    }

    public int getMinValidDivision() {
        return trackReportList.stream().map(MidiTrackReport::getMinValidDivision).max(Integer::compare).orElseThrow();
    }
}
