package com.ongakucraft.core.ftm;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class FtmInstrument {
    public static FtmInstrument of(String name, String type, String id, String content) {
        return new FtmInstrument(name, type, id, content,
                                 new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    private final String name;
    private final String type;
    private final String id;
    private final String content;
    private final List<FtmSequence> sequences;
    private final List<FtmSample> samples;
    private final List<String> keyDpcm;
    private final List<String> fds;
}
