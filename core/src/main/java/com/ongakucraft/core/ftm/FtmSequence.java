package com.ongakucraft.core.ftm;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class FtmSequence {
    public static FtmSequence of(String id, String type, String typeId, String sequenceNo, String params) {
        return new FtmSequence(id, type, typeId, sequenceNo, params);
    }

    private final String id;
    private final String type;
    private final String typeId;
    private final String sequenceNo;
    private final String params;
}
