package com.ongakucraft.core.circuit;

import lombok.Getter;

@Getter
public enum KeyRange {
    F_SHARP_1_3(0),
    F_SHARP_2_4(1),
    F_SHARP_3_5(2),
    F_SHARP_4_6(3),
    F_SHARP_5_7(4);

    public static final int LOWEST_KEY = 30;
    public static final int HIGHEST_KEY = F_SHARP_5_7.getMax();

    public static final int KEY_PER_OCTAVE = 12;
    private static final int KEY_PER_RANGE = KEY_PER_OCTAVE * 2 + 1;

    public static boolean isValid(int key) {
        return LOWEST_KEY <= key && key <= HIGHEST_KEY;
    }

    private final int min;
    private final int max;

    KeyRange(int index) {
        min = LOWEST_KEY + index * KEY_PER_OCTAVE;
        max = min - 1 + KEY_PER_RANGE;
    }

    public boolean contains(int key) {
        return min <= key && key <= max;
    }
}
