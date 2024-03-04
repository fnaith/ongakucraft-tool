package com.ongakucraft.core.ftm;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
public final class FtmNote {
    public static final FtmNote REST = new FtmNote(-1000);
    public static final int NOTE_CUT = -2000;
    private static final Map<String, Integer> nameToKey;
    static {
        nameToKey = new HashMap<>();
        for (int key = 0; key < 128; ++key) {
            nameToKey.put(keyToName(key), key);
        }
    }

    public static String keyToName(int key) {
        final var octave = String.valueOf((key / 12) - 1);
        return "C-C#D-D#E-F-F#G-G#A-A#B-".substring((key % 12) * 2, (key % 12) * 2 + 2) + octave;
    }

    private static int nameToKey(String name) {
        return nameToKey.get(name);
    }

    public static FtmNote of(int key) {
        return new FtmNote(key);
    }

    public static FtmNote of(String name) {
        return new FtmNote(nameToKey(name));
    }

    public static FtmNote noteCut() {
        return of(NOTE_CUT);
    }

    private int key;
    private final List<Integer> chord = new ArrayList<>();
    private int tuplet;
    private boolean accent;
    private boolean staccato;
    private boolean arpeggiate;
    private boolean pedal;
    private int instrument;
    private int volume;
    private final FtmEffect[] fx = new FtmEffect[4];

    private FtmNote(int key) {
        this.key = key;
    }

    public void addChord(int key) {
        chord.add(key);
    }

    public void addChord(String name) {
        chord.add(nameToKey(name));
    }

    public boolean isChord() {
        return !chord.isEmpty();
    }

    public void sortChord() {
        chord.add(key);
        chord.sort(Integer::compareTo);
        key = chord.remove(0);
    }

    public void setEffect(int idx, FtmEffect effect) {
        fx[idx] = effect;
    }

    public FtmEffect getEffect(int idx) {
        return fx[idx];
    }
}
