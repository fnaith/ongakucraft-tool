package com.ongakucraft.app.mxl;

import com.ongakucraft.core.OcException;
import lombok.Getter;
import org.audiveris.proxymusic.ScorePartwise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public final class MxlScore {
    private final List<MxlPart> parts;

    public MxlScore(String filePath, ScorePartwise scorePartwise) {
        final var parts = parse(filePath, scorePartwise);
        fixEndingNoteOnlyOnFirstPart(filePath, scorePartwise, parts);
        checkMeasureSizeIsUnique(parts);
        checkBeatSizeIsUnique(parts);
        this.parts = Collections.unmodifiableList(removeEmptyPart(parts));
    }

    private static List<MxlPart> parse(String filePath, ScorePartwise scorePartwise) {
        final List<MxlPart> parts = new ArrayList<>();
        for (int p = 0; p < scorePartwise.getPart().size(); ++p) {
            final var part = scorePartwise.getPart().get(p);
            parts.add(new MxlPart(filePath, p, part));
        }
        return parts;
    }

    private static void fixEndingNoteOnlyOnFirstPart(String filePath, ScorePartwise scorePartwise, List<MxlPart> parts) {
        final var measureSizes = parts.stream().map(MxlPart::getMeasureSize).collect(Collectors.toSet());
        if (2 != measureSizes.size()) {
            return;
        }
        final var firstMeasureSize = parts.get(0).getMeasureSize();
        for (var p = 1; p < parts.size(); ++p) {
            final var part = parts.get(p);
            if (part.getMeasureSize() < firstMeasureSize) {
                parts.set(p, new MxlPart(filePath, part.getId(), scorePartwise.getPart().get(p), parts.get(0).getMeasureIndexList()));
            }
        }
    }

    private static void checkMeasureSizeIsUnique(List<MxlPart> parts) {
        final var measureSizes = parts.stream().map(MxlPart::getMeasureSize).collect(Collectors.toSet());
        if (1 != measureSizes.size()) {
            throw new OcException("part measure size must be unique : %s", measureSizes.toString());
        }
    }

    private static void checkBeatSizeIsUnique(List<MxlPart> parts) {
        final var beatSizes = parts.stream().map(MxlPart::getBeatSize).collect(Collectors.toSet());
        if (1 != beatSizes.size()) {
            throw new OcException("part beat size must be unique : %s", beatSizes.toString());
        }
    }

    private static List<MxlPart> removeEmptyPart(List<MxlPart> parts) {
        return parts.stream().filter(part -> !part.isEmpty()).toList();
    }
}
