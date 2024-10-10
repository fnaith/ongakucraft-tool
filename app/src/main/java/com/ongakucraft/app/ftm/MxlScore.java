package com.ongakucraft.app.ftm;

import com.ongakucraft.core.OcException;
import lombok.extern.slf4j.Slf4j;
import org.audiveris.proxymusic.ScorePartwise;
import org.audiveris.proxymusic.mxl.Mxl;
import org.audiveris.proxymusic.util.Marshalling;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public final class MxlScore {
    final List<MxlPart> parts;

    public MxlScore(ScorePartwise scorePartwise) {
//        mxlDivisions4List = new ArrayList<>(); TODO
//        mxlRepeatCount = 0; TODO
        parts = Collections.unmodifiableList(parse(scorePartwise));
        checkMeasureSizeIsUnique(parts);
    }

    private static List<MxlPart> parse(ScorePartwise scorePartwise) {
        final List<MxlPart> parts = new ArrayList<>();
        for (int p = 0; p < scorePartwise.getPart().size(); ++p) {
            final var part = scorePartwise.getPart().get(p);
            parts.add(new MxlPart(p, part));
        }
        return parts;
    }

    private static void checkMeasureSizeIsUnique(List<MxlPart> parts) {
        final var measureSizes = parts.stream().map(MxlPart::getMeasureSize).collect(Collectors.toSet());
        if (1 != measureSizes.size()) {
            throw new OcException("part measure size must be unique : %s", measureSizes.toString());
        }
    }
}
