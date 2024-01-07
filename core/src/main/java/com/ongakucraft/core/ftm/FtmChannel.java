package com.ongakucraft.core.ftm;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class FtmChannel {
    public static FtmChannel of(List<FtmNote> noteList) {
        var maxFxIdx = -1;
        for (final var note : noteList) {
            if (null != note) {
                for (int idx = 0; idx < 4; ++idx) {
                    if (null != note.getEffect(idx)) {
                        maxFxIdx = Math.max(maxFxIdx, idx);
                    }
                }
            }
        }
        maxFxIdx = Math.max(maxFxIdx, 0);
        return new FtmChannel(Collections.unmodifiableList(noteList), maxFxIdx + 1);
    }

    private final List<FtmNote> noteList;
    private final int fxCount;

    public int size() {
        return noteList.size();
    }
}
