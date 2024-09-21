package com.ongakucraft.core.ftm;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class FtmSample {
    public static FtmSample of(String id, String size, String name) {
        return new FtmSample(id, size, name, new ArrayList<>());
    }

    private final String id;
    private final String size;
    private final String name;
    private final List<String> dpcm;
}
