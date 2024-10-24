package com.ongakucraft.app.ftm;

import java.util.HashMap;
import java.util.Map;

public final class MxlNote {
    private static final Map<String, Integer> nameToKey;
    static {
        nameToKey = new HashMap<>();
        for (int key = 0; key < 128; ++key) {
            final var name = keyToName(key);
            nameToKey.put(name, key);
            if (name.startsWith("F-")) {
                nameToKey.put("E#" + name.substring(2), key);
            }
        }
    }

    public static String keyToName(int key) {
        final var octave = String.valueOf((key / 12) - 1);
        return "C-C#D-D#E-F-F#G-G#A-A#B-".substring((key % 12) * 2, (key % 12) * 2 + 2) + octave;
    }

    private static int nameToKey(String name) {
        return nameToKey.get(name);
    }

    public static MxlNote of(String name) {
        return new MxlNote(nameToKey(name));
    }

    private final int key;

    private MxlNote(int key) {
        this.key = key;
    }
}
