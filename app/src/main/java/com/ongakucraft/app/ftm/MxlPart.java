package com.ongakucraft.app.ftm;

import lombok.Getter;
import org.audiveris.proxymusic.Barline;
import org.audiveris.proxymusic.ScorePartwise;
import org.audiveris.proxymusic.StartStopDiscontinue;

import java.util.*;

@Getter
public class MxlPart {
    final int id;
    final List<MxlMeasure> measures;

    public MxlPart(int id, ScorePartwise.Part part) {
        this.id = id;
        measures = parse(part);
    }

    public int getMeasureSize() {
        return measures.size();
    }

    private List<MxlMeasure> parse(ScorePartwise.Part part) {
        final Set<Integer> usedRepeatForward = new HashSet<>();
        Integer repeatForward = null;
        final Map<String, Integer> numberToEndingStart = new HashMap<>();
        final Map<String, Integer> numberToEndingStop = new HashMap<>();
        final List<Integer> mList = new ArrayList<>();
        for (var m = 0; m < part.getMeasure().size();) {
            mList.add(m);
            var needContinue = false;
            final var measure = part.getMeasure().get(m);
            for (final var noteOrBackupOrForward : measure.getNoteOrBackupOrForward()) {
                if (noteOrBackupOrForward instanceof final Barline barline) {
                    if (null != barline.getEnding()) {
                        final var ending = barline.getEnding();
                        if (StartStopDiscontinue.START == ending.getType()) {
                            numberToEndingStart.put(ending.getNumber(), m);
                        } else {
                            numberToEndingStop.put(ending.getNumber(), m);
                        }
                    }
                    if (null != barline.getRepeat()) {
                        final var repeat = barline.getRepeat();
                        switch (repeat.getDirection()) {
                            case FORWARD -> {
                                if (!usedRepeatForward.contains(m)) {
                                    usedRepeatForward.add(m);
                                    repeatForward = m;
                                }
                            }
                            case BACKWARD -> {
                                if (!usedRepeatForward.contains(m)) {
                                    usedRepeatForward.add(m);
                                    if (null == repeatForward) {
                                        repeatForward = 0;
                                    }
                                    m = repeatForward;
                                    repeatForward = null;
                                    needContinue = true;
                                }
                            }
                        }
                    }
                }
            }
            if (needContinue) {
                continue;
            }
            ++m;
//            if (null != ending1Start && ending1Start == m) {
//                m = ending1Stop + 1;
//                ending1Start = null;
//                ending1Stop = null;
//            }
        }
//        mxlMeasureSize = mList.size();
        final List<MxlMeasure> measures = new ArrayList<>();
        for (final int m : mList) {
            final var measure = part.getMeasure().get(m);
            measures.add(new MxlMeasure(measure));
        }
        return measures;
    }
}
