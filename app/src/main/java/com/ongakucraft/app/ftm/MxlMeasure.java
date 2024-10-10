package com.ongakucraft.app.ftm;

import com.ongakucraft.core.OcException;
import org.audiveris.proxymusic.ScorePartwise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class MxlMeasure {
    public MxlMeasure(ScorePartwise.Part.Measure measure) {
        parse(measure);
    }

    private static void parse(ScorePartwise.Part.Measure measure) {
        // TODO
    }
}
