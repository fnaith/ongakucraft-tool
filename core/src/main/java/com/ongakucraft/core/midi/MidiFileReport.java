package com.ongakucraft.core.midi;

import java.util.Collections;
import java.util.HashMap;
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
    private final int minUnalignedTrackCount;
    private final int minUnalignedNoteCount;
    private final int minUnalignedDivision;

    public static MidiFileReport of(MidiFile midiFile) {
        final var divisionList = buildDivisionList(midiFile.getWholeNoteTicks());
        final var trackReports = midiFile.getTrackList().stream().filter(track -> !track.getNoteList().isEmpty())
                                         .map(track -> MidiTrackReport.of(track, midiFile.getWholeNoteTicks(), divisionList)).toList();
        final var unalignedTrackCounts = new HashMap<Integer, Integer>();
        final var unalignedNoteCounts = new HashMap<Integer, Integer>();
        var minUnalignedTrackCount = Integer.MAX_VALUE;
        var minUnalignedNoteCount = Integer.MAX_VALUE;
        var minUnalignedDivision = Integer.MAX_VALUE;
        for (final var trackReport : trackReports) {
            for (final var entry : trackReport.getDivisionToUnalignedNoteList().entrySet()) {
                final var unalignedNoteCount = entry.getValue().size();
                final var trackCount = unalignedTrackCounts.getOrDefault(entry.getKey(), 0);
                unalignedTrackCounts.put(entry.getKey(), trackCount + (0 == unalignedNoteCount ? 0 : 1));
            }
        }
        for (final var trackReport : trackReports) {
            for (final var entry : trackReport.getDivisionToUnalignedNoteList().entrySet()) {
                final var division = entry.getKey();
                final var unalignedNoteCount = entry.getValue().size();
                final var trackCount = unalignedTrackCounts.get(entry.getKey());
                final var noteCount = unalignedNoteCounts.getOrDefault(entry.getKey(), 0);
                unalignedNoteCounts.put(entry.getKey(), noteCount + unalignedNoteCount);
                if (trackCount < minUnalignedTrackCount ||
                    (trackCount == minUnalignedTrackCount && noteCount < minUnalignedNoteCount)) {
                    minUnalignedTrackCount = trackCount;
                    minUnalignedNoteCount = noteCount;
                    minUnalignedDivision = division;
                }
            }
        }
        return new MidiFileReport(midiFile, Collections.unmodifiableList(trackReports),
                                  minUnalignedTrackCount, minUnalignedNoteCount, minUnalignedDivision);
    }

    private static List<Integer> buildDivisionList(int wholeNoteTicks) {
        return NOTE_DIVISIONS.stream().filter(division -> 0 == wholeNoteTicks % division).toList();
    }

    public boolean isEmpty() {
        return trackReportList.isEmpty();
    }

    public boolean containsModifiedTrack() {
        return trackReportList.stream().anyMatch(trackReport -> 0 != trackReport.getDurationModifier());
    }

    public int getMinValidDivision() {
        return trackReportList.stream().map(MidiTrackReport::getMinValidDivision).max(Integer::compare).orElseThrow();
    }

    public boolean hasValidDivision() {
        return trackReportList.stream().allMatch(MidiTrackReport::hasValidDivision);
    }
}
