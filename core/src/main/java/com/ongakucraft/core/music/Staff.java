package com.ongakucraft.core.music;

import com.ongakucraft.core.midi.MidiNote;
import com.ongakucraft.core.midi.MidiTrackReport;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

@Getter
public final class Staff {
    public static Staff of(MidiTrackReport trackReport, int beats, int divisionTicks, int startSequenceId, int maxDuration) {
        final var beatToNoteList = generateBeatToNotes(beats);
        for (var note : trackReport.getTrack().getNoteList()) {
            for (var tick = note.getOn(); tick <= note.getOff() && (tick - note.getOn()) < maxDuration; ++tick) {
                final var beat = tick / divisionTicks;
                final var notes = beatToNoteList.get(beat);
                notes.add(note);
            }
        }
        final var sequenceCount = beatToNoteList.stream().map(ArrayList::size).max(Integer::compareTo).orElseThrow();
        final List<Sequence> sequenceList = new ArrayList<>();
        for (var i = 0; i < sequenceCount; ++i) {
            final List<MidiNote> beatToNote = new ArrayList<>();
            for (var beat = 0; beat < beatToNoteList.size(); ++beat) {
                final var noteList = beatToNoteList.get(beat);
                if (noteList.isEmpty()) {
                    beatToNote.add(null);
                } else {
                    beatToNote.add(noteList.remove(0));
                }
            }
            sequenceList.add(Sequence.of(startSequenceId + 1, null, beatToNote));
        }
        return new Staff(trackReport, sequenceList);
    }

    private final MidiTrackReport trackReport;
    private final List<Sequence> sequenceList;

    private Staff(MidiTrackReport trackReport, List<Sequence> sequenceList) {
        this.trackReport = trackReport;
        this.sequenceList = Collections.unmodifiableList(sequenceList.stream().map(
                sequence -> Sequence.of(sequence.getId(), sequence.getStaff(), sequence.getBeatToNote())).toList());
    }

    private static List<ArrayList<MidiNote>> generateBeatToNotes(int beats) {
        return IntStream.range(0, beats).mapToObj(beat -> new ArrayList<MidiNote>()).toList();
    }
}
