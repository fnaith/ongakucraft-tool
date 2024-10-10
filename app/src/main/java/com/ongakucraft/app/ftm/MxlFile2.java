package com.ongakucraft.app.ftm;

import com.ongakucraft.core.OcException;
import com.ongakucraft.core.ftm.FtmEffect;
import com.ongakucraft.core.ftm.FtmNote;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.audiveris.proxymusic.*;
import org.audiveris.proxymusic.mxl.Mxl;
import org.audiveris.proxymusic.util.Marshalling;

import java.io.File;
import java.lang.String;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public final class MxlFile2 {
    // https://www.w3.org/2021/06/musicxml40/musicxml-reference/elements/note/
    private static ScorePartwise loadMxl(String filePath) {
        try (final var mif = new Mxl.Input(new File(filePath))) {
            final List<ScorePartwise> scorePartwiseList = new ArrayList<>();
            for (final var rootFile : mif.getRootFiles()) {
                final var zipEntry = mif.getEntry(rootFile.fullPath);
                final var is = mif.getInputStream(zipEntry);
                scorePartwiseList.add((ScorePartwise) Marshalling.unmarshal(is));
            }
            if (1 != scorePartwiseList.size()) {
                throw new OcException("[FamiTrackerApp][loadMxl] : %d", scorePartwiseList.size());
            }
            return scorePartwiseList.get(0);
        } catch (Exception e) {
            throw new OcException("[FamiTrackerApp][loadMxl] : %s", e.getMessage());
        }
    }

//    private final ScorePartwise scorePartwise;

    public MxlFile2(String filePath) {
        final var scorePartwise = loadMxl(filePath);
        parseScorePartwise(scorePartwise);
    }

    public MxlFile2(ScorePartwise scorePartwise) {
        parseScorePartwise(scorePartwise);
    }

    private void parseScorePartwise(ScorePartwise scorePartwise) {
        for (int p = 0; p < scorePartwise.getPart().size(); ++p) {
            final var part = scorePartwise.getPart().get(p);
            parsePart(part);
        }
    }

    private void parsePart(ScorePartwise.Part part) {
    }
}
