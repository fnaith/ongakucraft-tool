package com.ongakucraft.core.circuit;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum Instrument {
    BASS("oak_log", KeyRange.F_SHARP_1_3),
    SNARE_DRUM("sand", KeyRange.F_SHARP_3_5),
    HIHAT("glass", KeyRange.F_SHARP_2_4),
    BASS_DRUM("stone", KeyRange.F_SHARP_1_3),
    BELL("gold_block", KeyRange.F_SHARP_5_7),
    FLUTE("clay", KeyRange.F_SHARP_4_6),
    CHIME("packed_ice", KeyRange.F_SHARP_5_7),
    GUITAR("white_wool", KeyRange.F_SHARP_2_4),
    XYLOPHONE("bone_block", KeyRange.F_SHARP_5_7),
    IRON_XYLOPHONE("iron_block", KeyRange.F_SHARP_3_5),
    COW_BELL("soul_sand", KeyRange.F_SHARP_4_6),
    DIDGERIDOO("pumpkin", KeyRange.F_SHARP_1_3),
    SQUARE_WAVE("emerald_block", KeyRange.F_SHARP_3_5),
    BANJO("hay_block", KeyRange.F_SHARP_3_5),
    PLING("glowstone", KeyRange.F_SHARP_3_5),
    HARP("grass_block", KeyRange.F_SHARP_3_5);

    private final String path;
    private final KeyRange keyRange;
}
