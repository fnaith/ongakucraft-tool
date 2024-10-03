package com.ongakucraft.app.mxl;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.util.*;

@Builder
@Getter
public final class MxlNote {
    public static MxlNote REST = of(-1);

    private static final Map<String, Integer> pitchNameToMidiKey;
    static {
        pitchNameToMidiKey = new HashMap<>();
        for (int key = 21; key <= 108; ++key) {
            final var name = midiKeyToPitchName(key);
            pitchNameToMidiKey.put(name, key);
        }
    }

    private static String midiKeyToPitchName(int key) {
        final var octave = String.valueOf((key / 12) - 1);
        final var semitone = key % 12;
        return "C-C#D-D#E-F-F#G-G#A-A#B-".substring(semitone * 2, semitone * 2 + 2) + octave;
    }

    public static MxlNote of(String pitchName) {
        final var key = pitchNameToMidiKey.get(pitchName);
        return of(key);
    }

    private static MxlNote of(int key) {
        return MxlNote.builder().key(key).chord(List.of(key)).build();
    }

    private final int key;
    private final List<Integer> chord;

    @With private final int tripletDelay;
    @With private final boolean tieStop;

    @With private final boolean accent;
    @With private final boolean caesura;
    @With private final boolean detachedLegato;
    @With private final boolean doit;
    @With private final boolean falloff;
    @With private final boolean plop;
    @With private final boolean staccatissimo;
    @With private final boolean staccato;
    @With private final boolean strongAccent;
    @With private final boolean tenuto;

    @With private final boolean arpeggiate;
    @With private final boolean fermata;
    @With private final boolean glissando;
    @With private final boolean nonArpeggiate;
    @With private final boolean ornaments;
    @With private final boolean otherNotation;
    @With private final boolean slur;
    @With private final boolean slide;
    @With private final boolean technical;

    public boolean isRest() {
        return -1 == key;
    }

    public MxlNote addChord(int key) {
        final List<Integer> newChord = new ArrayList<>(chord);
        newChord.add(key);
        newChord.sort(Integer::compareTo);
        return MxlNote.builder().key(newChord.get(0)).chord(Collections.unmodifiableList(newChord))
                .tripletDelay(tripletDelay).tieStop(tieStop).build();
    }
}
