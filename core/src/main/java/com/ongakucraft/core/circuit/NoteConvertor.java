package com.ongakucraft.core.circuit;

import java.util.ArrayList;
import java.util.List;

import com.ongakucraft.core.midi.MidiNote;
import com.ongakucraft.core.music.Sequence;

@FunctionalInterface
public interface NoteConvertor {
    Note convert(Sequence sequence, int beat, MidiNote note);

    default List<Note> convert(Sequence sequence) {
        final List<Note> beatToNote = new ArrayList<>();
        final var beatToMidiNote = sequence.getBeatToNote();
        for (var beat = 0; beat < beatToMidiNote.size(); ++beat) {
            final var note = beatToMidiNote.get(beat);
            beatToNote.add(null == note ? null : convert(sequence, beat, note));
        }
        return beatToNote;
    }
}
