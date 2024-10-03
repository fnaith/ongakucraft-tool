package com.ongakucraft.app.mxl;

import com.ongakucraft.core.OcException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.audiveris.proxymusic.Attributes;
import org.audiveris.proxymusic.Barline;
import org.audiveris.proxymusic.ScorePartwise;
import org.audiveris.proxymusic.StartStopDiscontinue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Getter
public class MxlPart {
    final int id;
    final List<Integer> measureIndexList;
    final List<MxlMeasure> measures;

    public MxlPart(String filePath, int id, ScorePartwise.Part part) {
        this(filePath, id, part, parseMeasureIndexList(filePath, id, part));
    }

    public MxlPart(String filePath, int id, ScorePartwise.Part part, List<Integer> measureIndexList) {
        this.id = id;
        this.measureIndexList = measureIndexList;
        final var divisions4 = getDivisions4(id, part);
        measures = buildMeasures(filePath, id, part, divisions4, measureIndexList);
    }

    public int getMeasureSize() {
        return measures.size();
    }

    public int getBeatSize() {
        return measures.stream().mapToInt(MxlMeasure::getBeatSize).sum();
    }

    public boolean isEmpty() {
        return measures.stream().allMatch(MxlMeasure::isEmpty);
    }

    public List<String> getVoices() {
        return getVoices(measures);
    }

    private static void checkRepeatAndEnding(String filePath, int id, ScorePartwise.Part part) {
        boolean foundRepeat = false;
        Integer repeatStart = null;
        Integer repeatEnd = null;
        for (var m = 0; m < part.getMeasure().size(); ++m) {
            final var measure = part.getMeasure().get(m);
            for (final var noteOrBackupOrForward : measure.getNoteOrBackupOrForward()) {
                if (noteOrBackupOrForward instanceof final Barline barline) {
                    if (null != barline.getRepeat()) {
                        final var repeat = barline.getRepeat();
                        if (null != repeat.getTimes()) {
                            throw new OcException("can't support repeat more than once : %d %d", id, m);
                        }
                        switch (repeat.getDirection()) {
                            case FORWARD -> {
                                if (null != repeatStart) {
                                    throw new OcException("already in repeat : %d %d %d", id, m, repeatStart);
                                }
                                foundRepeat = true;
                                repeatStart = m;
                            }
                            case BACKWARD -> {
                                if (!foundRepeat) {
                                    foundRepeat = true;
                                    repeatStart = 0;
                                }
                                if (null == repeatStart) {
                                    log.warn("not in repeat : {} {} {} {}", filePath, id, m, repeatStart);
                                }
                                repeatStart = null;
                                repeatEnd = m;
                            }
                        }
                    }
                    if (null != barline.getEnding()) {
                        final var ending = barline.getEnding();
                        switch (ending.getNumber()) {
                            case "1" -> {
                                if (StartStopDiscontinue.STOP == ending.getType()) {
                                    if (null == repeatEnd || repeatEnd != m) {
                                        throw new OcException("mismatch ending 1's stop : %d %d %d", id, m, repeatEnd);
                                    }
                                }
                            }
                            case "2" -> {
                                if (StartStopDiscontinue.START == ending.getType()) {
                                    if (null == repeatEnd || repeatEnd != (m - 1)) {
                                        throw new OcException("mismatch ending 2's start : %d %d %d", id, m, repeatEnd);
                                    }
                                }
                            }
                            default -> throw new OcException("can't support ending more than 2 : %d %d %s", id, m, ending.getNumber());
                        }
                    }
                }
            }
        }
    }

    private static void checkDivisions(int id, ScorePartwise.Part part) {
        final List<Integer> divisions4List = new ArrayList<>();
        for (var m = 0; m < part.getMeasure().size(); ++m) {
            final var measure = part.getMeasure().get(m);
            for (final var noteOrBackupOrForward : measure.getNoteOrBackupOrForward()) {
                if (noteOrBackupOrForward instanceof final Attributes attributes) {
                    final var divisions = attributes.getDivisions();
                    if (null != divisions) {
                        final var divisions4 = divisions.intValue();
                        if (!divisions4List.contains(divisions4)) {
                            divisions4List.add(divisions4);
                            if (1 != divisions4List.size()) {
                                throw new OcException("part %d measure %d divisions4 should be unique : %d %s", id, m, divisions4, divisions4List.toString());
                            }
                        }
                    }
                }
            }
        }
    }

    private static int getDivisions4(int id, ScorePartwise.Part part) {
        for (var m = 0; m < part.getMeasure().size(); ++m) {
            final var measure = part.getMeasure().get(m);
            for (final var noteOrBackupOrForward : measure.getNoteOrBackupOrForward()) {
                if (noteOrBackupOrForward instanceof final Attributes attributes) {
                    final var divisions = attributes.getDivisions();
                    if (null != divisions) {
                        return divisions.intValue();
                    }
                }
            }
        }
        throw new OcException("part %d does not have divisions4 info : %d %s", id);
    }

    private static List<Integer> parseMeasureIndexList(String filePath, int id, ScorePartwise.Part part) {
        checkRepeatAndEnding(filePath, id, part);
        checkDivisions(id, part);
        final List<Integer> measureIndexList = new ArrayList<>();
        var foundRepeat = false;
        Integer repeatStart = null;
        Integer endingStart = null;
        Integer endingStop = null;
        for (var m = 0; m < part.getMeasure().size(); ++m) {
            measureIndexList.add(m);
            final var measure = part.getMeasure().get(m);
            for (final var noteOrBackupOrForward : measure.getNoteOrBackupOrForward()) {
                if (noteOrBackupOrForward instanceof final Barline barline) {
                    if (null != barline.getRepeat()) {
                        final var repeat = barline.getRepeat();
                        switch (repeat.getDirection()) {
                            case FORWARD -> {
                                foundRepeat = true;
                                repeatStart = m;
                            }
                            case BACKWARD -> {
                                if (!foundRepeat) {
                                    foundRepeat = true;
                                    repeatStart = 0;
                                }
                                if (null != endingStart) {
                                    if (null == endingStop) {
                                        endingStop = m;
                                    }
                                    for (var r = repeatStart; r <= m; ++r) {
                                        if (!(endingStart <= r && r <= endingStop)) {
                                            measureIndexList.add(r);
                                        }
                                    }
                                    endingStart = null;
                                    endingStop = null;
                                }
                            }
                        }
                    }
                    if (null != barline.getEnding()) {
                        final var ending = barline.getEnding();
                        if (ending.getNumber().equals("1")) {
                            if (StartStopDiscontinue.START == ending.getType()) {
                                endingStart = m;
                            }
                            if (StartStopDiscontinue.STOP == ending.getType()) {
                                endingStop = m;
                            }
                        }
                    }
                }
            }
        }
        return measureIndexList;
    }

    private static List<MxlMeasure> buildMeasures(String filePath, int id, ScorePartwise.Part part, int divisions4,
                                                  List<Integer> measureIndexList) {
        final List<MxlMeasure> measures = new ArrayList<>();
        for (var m = 0; m < part.getMeasure().size(); ++m) {
            final var measure = part.getMeasure().get(m);
            measures.add(new MxlMeasure(filePath, id, m, measure, divisions4));
        }
        removeEmptyVoice(measures);
        final List<MxlMeasure> repeatedMeasures = new ArrayList<>();
        for (final var m : measureIndexList) {
            final var measure = measures.get(m);
            repeatedMeasures.add(measure);
        }
        return repeatedMeasures;
    }

    private static List<String> getVoices(List<MxlMeasure> measures) {
        final Set<String> voices = new HashSet<>();
        for (final var measure : measures) {
            voices.addAll(measure.getVoiceToMxlNotes().keySet());
        }
        return voices.stream().sorted().toList();
    }

    private static boolean hasVoice(List<MxlMeasure> measures, String voice) {
        return measures.stream().anyMatch(measure -> measure.hasVoice(voice));
    }

    private static void removeEmptyVoice(List<MxlMeasure> measures) {
        final var voices = getVoices(measures);
        for (final var voice : voices) {
            if (!hasVoice(measures, voice)) {
                measures.replaceAll(mxlMeasure -> mxlMeasure.removeVoice(voice));
            }
        }
    }
}
