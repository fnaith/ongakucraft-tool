package com.ongakucraft.core.ftm;

import com.ongakucraft.core.OcException;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
public class FtmNote {
    public static final FtmNote NOTE_CUT = FtmNote.builder().key(-1000).effects(List.of()).build();

    private final int key;

    @With private final List<FtmEffect> effects;
    @With private final int instrument;
    @With private final int volume;
    @With private final boolean tieStop;

    public boolean isNoteCut() {
        return NOTE_CUT.key == key;
    }

    public FtmNote addChord(List<Integer> chord) {
        if (1 == chord.size()) {
            return this;
        }
        if (key != chord.get(0)) {
            throw new OcException("chord should start with key : %d %s", key, chord);
        }
        final List<FtmEffect> newEffects = new ArrayList<>(effects);
        switch (chord.size()) {
            case 2 -> newEffects.add(FtmEffect.arpeggio(chord.get(1) - key, 0));
            case 3 -> newEffects.add(FtmEffect.arpeggio(chord.get(1) - key, chord.get(2) - key));
            case 4 -> newEffects.add(FtmEffect.arpeggio(chord.get(1) - key, chord.get(3) - key));
            default -> throw new OcException("chord too many : %d %d", key, chord);
        }
        return FtmNote.builder().key(key).effects(newEffects).build();
    }
}
