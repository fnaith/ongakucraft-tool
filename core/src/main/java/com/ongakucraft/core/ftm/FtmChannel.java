package com.ongakucraft.core.ftm;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class FtmChannel {
    public static FtmChannel of(List<FtmNote> ftmNotes) {
        var fxCount = 0;
        for (final var ftmNote : ftmNotes) {
            if (null != ftmNote) {
                fxCount = Math.max(fxCount, ftmNote.getEffects().size());
            }
        }
        fxCount = Math.max(fxCount, 1);
        return new FtmChannel(Collections.unmodifiableList(ftmNotes), fxCount);
    }

    private final List<FtmNote> notes;
    private final int fxCount;

    public int size() {
        return notes.size();
    }
}
