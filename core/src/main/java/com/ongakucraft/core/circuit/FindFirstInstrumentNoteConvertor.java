package com.ongakucraft.core.circuit;

import com.ongakucraft.core.OcException;
import com.ongakucraft.core.midi.MidiNote;
import com.ongakucraft.core.music.Sequence;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.With;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class FindFirstInstrumentNoteConvertor implements NoteConvertor {
    public static final FindFirstInstrumentNoteConvertor DEFAULT = of(0, Instrument.BASS, Instrument.HARP, Instrument.BELL);

    public static FindFirstInstrumentNoteConvertor of(int octaveModifier, Instrument... instruments) {
        return new FindFirstInstrumentNoteConvertor(octaveModifier, instruments);
    }

    @With private final int octaveModifier;
    private final Instrument[] instruments;

    @Override
    public Note convert(Sequence sequence, int beat, MidiNote note) {
        final var key = note.getKey() + octaveModifier * 12;
        Instrument instrument = null;
        for (var currentInstrument : instruments) {
            if (currentInstrument.getKeyRange().contains(key)) {
                instrument = currentInstrument;
                break;
            }
        }
        if (null == instrument) {
            throw new OcException("[FindFirstInstrumentNoteConvertor][convert] fail : {}, {}, {}", sequence.getId(), beat, note.getKey());
        }
        return Note.of(instrument.getPath(), key - instrument.getKeyRange().getMin());
    }
}
