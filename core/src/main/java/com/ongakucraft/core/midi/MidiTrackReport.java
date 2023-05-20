package com.ongakucraft.core.midi;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class MidiTrackReport {
    private static final int KEY_PER_OCTAVE = 12;

    public static MidiTrackReport of(MidiTrack track,
                                     int wholeNoteTicks, List<Integer> divisionList) {
        final List<Integer> validDivisionList = new ArrayList<>();
        final Map<Integer, List<MidiNote>> divisionToUnalignedNoteList = new HashMap<>();
        for (final var division : divisionList) {
            final var unalignedNoteList = findUnalignedNoteList(track, wholeNoteTicks / division);
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

    public int getMinValidDivision() {
        return validDivisionList.get(0);
    }

    public boolean hasValidDivision() {
        return !validDivisionList.isEmpty();
    }

    public List<MidiNote> getLowerThanKeyNotes(int key) {
        return track.getNoteList().stream().filter(note -> note.getKey() < key).toList();
    }

    public List<MidiNote> getHigherThanKeyNotes(int key) {
        return track.getNoteList().stream().filter(note -> key < note.getKey()).toList();
    }

    private List<MidiNote> getNormalKeyRangeNotes(int low, int high) {
        return track.getNoteList().stream().filter(note -> low <= note.getKey() && note.getKey() <= high).toList();
    }

    public List<Integer> calculateAdjustableOctaves(int low, int high) {
        final var octaves = new ArrayList<Integer>();
        final var min = getMinKeyBetween(low, high);
        final var max = getMaxKeyBetween(low, high);
        if (max - min <= high - low) {
            for (var octave = -1; low <= min + octave * KEY_PER_OCTAVE; --octave) {
                octaves.add(octave);
            }
            for (var octave = 1; max + octave * KEY_PER_OCTAVE <= high; ++octave) {
                octaves.add(octave);
            }
        }
        Collections.sort(octaves);
        return Collections.unmodifiableList(octaves);
    }

    public int getMinKeyBetween(int low, int high) {
        final var keyDistribution = getKeyDistribution(low, high);
        for (var key = 0; key < keyDistribution.size(); ++key) {
            final var count = keyDistribution.get(key);
            if (0 < count) {
                return key;
            }
        }
        return low;
    }

    public int getMaxKeyBetween(int low, int high) {
        final var keyDistribution = getKeyDistribution(low, high);
        for (var key = keyDistribution.size() - 1; 0 <= key ; --key) {
            final var count = keyDistribution.get(key);
            if (0 < count) {
                return key;
            }
        }
        return high;
    }

    public List<Integer> getKeyDistribution(int low, int high) {
        final var keyCounts = new int[high + 1];
        for (var note : getNormalKeyRangeNotes(low, high)) {
            ++keyCounts[note.getKey()];
        }
        return Arrays.stream(keyCounts).boxed().toList();
    }
}
