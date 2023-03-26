package com.ongakucraft.core.midi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class MidiTrackReport {
    public static MidiTrackReport of(MidiTrack track,
                                     int wholeNoteTicks, List<Integer> divisionList) {
        final List<Integer> validDivisionList = new ArrayList<>();
        final Map<Integer, List<MidiNote>> divisionToUnalignedNoteList = new HashMap<>();
        for (final var division : divisionList) {
            final var unalignedNoteList = findUnalignedNoteList(track, division);
            if (unalignedNoteList.isEmpty()) {
                validDivisionList.add(division);
            } else {
                divisionToUnalignedNoteList.put(division, unalignedNoteList);
            }
        }
        final var durationModifier = getNoteDurationModifier(track, wholeNoteTicks, validDivisionList);
        validDivisionList.sort(Integer::compareTo);
        return new MidiTrackReport(modifyDuration(track, durationModifier), wholeNoteTicks, durationModifier,
                                   Collections.unmodifiableList(validDivisionList),
                                   Collections.unmodifiableMap(divisionToUnalignedNoteList));
    }

    private static List<MidiNote> findUnalignedNoteList(MidiTrack track, int divisionTicks) {
        final var noteList = track.getNoteList();
        final List<MidiNote> unalignedNoteList = new ArrayList<>();
        final var firstTick = noteList.get(0).getOn();
        for (var i = 1; i < noteList.size(); ++i) {
            final var note = noteList.get(i);
            final var tick = note.getOn();
            if (0 != (tick - firstTick) % divisionTicks) {
                unalignedNoteList.add(note);
            }
        }
        return unalignedNoteList;
    }

    private static int getNoteDurationModifier(MidiTrack track, int wholeNoteTicks, List<Integer> divisionList) {
        if (!track.isValid() || divisionList.isEmpty()) {
            return 0;
        }
        final var maxDivision = divisionList.stream().max(Integer::compareTo).get();
        final var divisionTicks = wholeNoteTicks / maxDivision;
        final var noModifyMatchedDurationCount = countMatchedDuration(track, divisionTicks, 0);
        final var plusOneMatchedDurationCount = countMatchedDuration(track, divisionTicks, 1);
        return plusOneMatchedDurationCount <= noModifyMatchedDurationCount ? 0 : 1;
    }

    private static long countMatchedDuration(MidiTrack track, int divisionTicks, int durationModifier) {
        return track.getNoteList().stream().filter(note -> 0 == (note.getDuration() + durationModifier) % divisionTicks).count();
    }

    private static MidiTrack modifyDuration(MidiTrack track, int durationModifier) {
        if (0 == durationModifier) {
            return track;
        }
        final var noteList = track.getNoteList().stream().map(note -> MidiNote.of(note.getKey(), note.getOn(), note.getOff() + durationModifier)).toList();
        return MidiTrack.of(track.getId(), noteList,track.getUnmatchedNoteOnList(), track.getUnmatchedNoteOffList(), track.getInstruments());
    }

    private final MidiTrack track;
    private final int wholeNoteTicks;
    private final int durationModifier;
    private final List<Integer> validDivisionList;
    private final Map<Integer, List<MidiNote>> divisionToUnalignedNoteList;

    public boolean isNormal() {
        return !validDivisionList.isEmpty();
    }

    public int getMinValidDivision() {
        return validDivisionList.get(0);
    }
}
