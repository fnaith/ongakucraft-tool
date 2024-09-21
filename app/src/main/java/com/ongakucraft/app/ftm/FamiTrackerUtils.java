package com.ongakucraft.app.ftm;

import com.ongakucraft.app.midi.MidiReader;
import com.ongakucraft.core.OcException;
import com.ongakucraft.core.ftm.*;
import com.ongakucraft.core.midi.MidiNote;
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
public final class FamiTrackerUtils {
    private static final MidiNote PLACEHOLDER_NOTE = MidiNote.of(-1, -1, -1);

    public static void printMidiFile(String filePath) {
        log.info("filePath : {}", filePath);
        final var midiFile = MidiReader.read(filePath);
        log.info("msDuration : {}", midiFile.getMsDuration());
        log.info("wholeNoteTicks : {}", midiFile.getWholeNoteTicks());
        final var duration16 = midiFile.getWholeNoteTicks() / 16;
        final Map<Integer, Integer> durations = new TreeMap<>();
        final Map<Integer, Integer> velocities = new TreeMap<>();
        int noteCount = 0;
        for (final var track : midiFile.getTrackList()) {
            log.info("track : {}", track.getId());
            for (final var note : track.getNoteList()) {
                if (0 != note.getOn() % duration16) {
                    log.info("note : {}", note);
                }
                if (-159 == note.getDuration()) {
                    log.info("note : {} {}", note.getOn() / midiFile.getWholeNoteTicks() + 1, note);
                }
                durations.merge(note.getDuration(), 1, Integer::sum);
                velocities.merge(note.getVelocity(), 1, Integer::sum);
                ++noteCount;
            }
        }
        log.info("durations : {}", durations);
        log.info("velocities : {}", velocities);
        log.info("noteCount : {}", noteCount);
    }

    public static List<List<String>> groupMidiNoteNameByRow(String filePath, int measures) {
        final List<List<String>> rows = new ArrayList<>();
        for (int m = 0; m < measures; ++m) {
            for (int i = 0; i < 16; ++i) {
                rows.add(new ArrayList<>());
            }
        }
        final var midiFile = MidiReader.read(filePath);
        final var ticksPerRow = midiFile.getWholeNoteTicks() / 16;
        for (final var track : midiFile.getTrackList()) {
            for (final var note : track.getNoteList()) {
                if (0 != note.getOn() % ticksPerRow) {
                    throw new OcException("note on should be int : %d/%d", note.getOn(), ticksPerRow);
                }
                final var row = rows.get(note.getOn() / ticksPerRow);
                row.add(FtmNote.keyToName(note.getKey()));
            }
        }
        return rows;
    }

    public static void blueClapperFromMidi(String filePath) {
        final Set<Integer> staccatoDurations = Set.of(59, /*119,*/ 239);
        final Map<Integer, Integer> durationToRows = new HashMap<>();
        durationToRows.putAll(Map.of(
                59, 1, 113, 1,  119, 1, // or 119,2
                159, 1, // 1/3
               227, 2,
                239, 2, // 0r 239,4
                341, 3,
                455, 4, 479, 4,
                683, 6
        ));
        durationToRows.putAll(Map.of(
                911, 8, 958, 8, 959, 8,
                1139, 10,
                1367, 12, 1439, 12,
                1823, 16, 1919, 16,
                2033, 17
        ));
        final var midiFile = MidiReader.read(filePath);
        final var ticksPerRow = midiFile.getWholeNoteTicks() / 16;
        final var rows = midiFile.getTrackList().stream().map(track -> {
            return track.getNoteList().stream().map(note -> {
                final var row = note.getOn() / ticksPerRow;
                return row + durationToRows.get(note.getDuration()) - 1;
            }).max(Integer::compareTo).orElse(0);
        }).max(Integer::compareTo).orElse(0) + 1;
        final List<List<MidiNote>> allMonoTracks = new ArrayList<>();
        for (final var track : midiFile.getTrackList()) {
            final Map<Integer, List<MidiNote>> rowToNoteList = new TreeMap<>();
            for (final var note : track.getNoteList()) {
                final var row = note.getOn() / ticksPerRow;
                final var noteList = rowToNoteList.computeIfAbsent(row, k -> new ArrayList<>());
                noteList.add(note);
            }
            final List<List<MidiNote>> monoTracks = new ArrayList<>();
            for (final var entry : rowToNoteList.entrySet()) {
                final var beginRow = entry.getKey();
                final var noteList = entry.getValue();
                noteList.sort(Comparator.comparing(MidiNote::getKey).thenComparing(MidiNote::getDuration));
                for (final var note : noteList) {
                    final var endRow = beginRow + durationToRows.get(note.getDuration()) - 1;
                    List<MidiNote> foundMonoTrack = null;
                    for (final var monoTrack : monoTracks) {
                        var allNull = true;
                        for (int i = beginRow; i <= endRow; ++i) {
                            if (null != monoTrack.get(i)) {
                                allNull = false;
                                break;
                            }
                        }
                        if (allNull) {
                            foundMonoTrack = monoTrack;
                            break;
                        }
                    }
                    if (null == foundMonoTrack) {
                        foundMonoTrack = newMonoTrack(rows);
                        monoTracks.add(foundMonoTrack);
                    }
                    for (int i = beginRow; i <= endRow; ++i) {
                        foundMonoTrack.set(i, PLACEHOLDER_NOTE);
                    }
                    foundMonoTrack.set(beginRow, note);
                }
            }
            log.info("monoTracks : {}", monoTracks.size());
            allMonoTracks.addAll(monoTracks);
//            break;
        }
        log.info("allMonoTracks : {}", allMonoTracks.size());
        // 24 : Arpeggio
        // 42,82 : triplet + fadeout for 32th notes
        // 18,19,20,21 : staccato + fadeout for 32th notes
        final List<FtmChannel> channelList = new ArrayList<>();
        channelList.add(FtmChannel.of(toFtmTrack(allMonoTracks.get(0), durationToRows, 4)));
        channelList.add(FtmChannel.of(toFtmTrack(allMonoTracks.get(1), durationToRows, 4)));
        channelList.add(null);
        channelList.add(null);
        channelList.add(null);
        channelList.add(FtmChannel.of(toFtmTrack(allMonoTracks.get(2), durationToRows, 14)));
        channelList.add(FtmChannel.of(toFtmTrack(allMonoTracks.get(3), durationToRows, 14)));
        channelList.add(null);
        final var song = FtmSong.of("BLUE CLAPPER", "Ongakucraft", "COVER Corp.",
                                    146, List.of(), channelList);
        log.info(song.toString());
    }

    private static List<MidiNote> newMonoTrack(int rows) {
        final List<MidiNote> monoTrack = new ArrayList<>();
        for (var i = 0; i < rows; ++i) {
            monoTrack.add(null);
        }
        return monoTrack;
    }

    private static List<FtmNote> toFtmTrack(List<MidiNote> monoTrack, Map<Integer, Integer> durationToRows, int instrument) {
        final List<FtmNote> ftmTrack = new ArrayList<>();
        final var rows = monoTrack.size();
        for (var i = 0; i < rows; ++i) {
            ftmTrack.add(null);
        }
        for (var beginRow = 0; beginRow < rows; ++beginRow) {
            final var midiNote = monoTrack.get(beginRow);
            if (null == midiNote || PLACEHOLDER_NOTE == midiNote) {
                continue;
            }
            final var endRow = beginRow + durationToRows.get(midiNote.getDuration()) - 1;
            final var ftmNote = FtmNote.of(midiNote.getKey());
            ftmNote.setInstrument(instrument);
            ftmNote.setVolume(8);
            ftmTrack.set(beginRow, ftmNote);
            if (endRow + 1 < rows) {
                ftmTrack.set(endRow + 1, FtmNote.noteCut());
            }
        }
        return ftmTrack;
    }

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

    private static void parseMxl(ScorePartwise scorePartwise,
                                 Consumer<ScorePartwise> scoreFunc,
                                 BiConsumer<Integer, ScorePartwise.Part> partFunc,
                                 BiConsumer<int[], ScorePartwise.Part.Measure> measureFunc) {
        if (null != scoreFunc) {
            scoreFunc.accept(scorePartwise);
        }
        for (int p = 0; p < scorePartwise.getPart().size(); ++p) {
            final var part = scorePartwise.getPart().get(p);
            if (null != partFunc) {
                partFunc.accept(p, part);
            }
            if (null != measureFunc) {
                for (int m = 0; m < part.getMeasure().size(); ++m) {
                    final var measure = part.getMeasure().get(m);
                    measureFunc.accept(new int[]{p, m}, measure);
                }
            }
        }
    }

    private static void parseMxlWithRepeat(ScorePartwise scorePartwise,
                                           Consumer<ScorePartwise> scoreFunc,
                                           BiConsumer<Integer, ScorePartwise.Part> partFunc,
                                           BiConsumer<int[], ScorePartwise.Part.Measure> measureFunc) {
        if (null != scoreFunc) {
            scoreFunc.accept(scorePartwise);
        }
        for (int p = 0; p < scorePartwise.getPart().size(); ++p) {
            final var part = scorePartwise.getPart().get(p);
            partFunc.accept(p, part);
            final Set<Integer> usedRepeatForward = new HashSet<>();
            Integer repeatForward = null;
            Integer ending1Start = null;
            Integer ending1Stop = null;
            final List<Integer> mList = new ArrayList<>();
            for (int m = 0; m < part.getMeasure().size();) {
                mList.add(m);
                var needContinue = false;
                final var measure = part.getMeasure().get(m);
                for (final var noteOrBackupOrForward : measure.getNoteOrBackupOrForward()) {
                    if (noteOrBackupOrForward instanceof final Barline barline) {
                        if (null != barline.getEnding()) {
                            final var ending = barline.getEnding();
                            if ("1".equals(ending.getNumber())) {
                                if (StartStopDiscontinue.START == ending.getType()) {
                                    ending1Start = m;
                                } else {
                                    ending1Stop = m;
                                }
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
                if (null != ending1Start && ending1Start == m) {
                    m = ending1Stop + 1;
                    ending1Start = null;
                    ending1Stop = null;
                }
            }
            mxlMeasureSize = mList.size();
            for (final int m : mList) {
                final var measure = part.getMeasure().get(m);
                measureFunc.accept(new int[]{p, m}, measure);
            }
        }
    }

    private static BigDecimal getDuration(Note note) { // TODO replace all note.getDuration()
        final var duration = note.getDuration();
        if (null != duration) {
            return duration;
        }
        if (null != note.getGrace()) {
            final var type = note.getType().getValue();
            if (null != type) {
                return switch (type) {
                    case "half" -> BigDecimal.valueOf(mxlDivisions4).multiply(BigDecimal.valueOf(2));
                    case "quarter" -> BigDecimal.valueOf(mxlDivisions4);
                    case "eighth" -> BigDecimal.valueOf(mxlDivisions4).divide(BigDecimal.valueOf(2), RoundingMode.UNNECESSARY);
                    case "16th" -> BigDecimal.valueOf(mxlDivisions4).divide(BigDecimal.valueOf(4), RoundingMode.UNNECESSARY);
                    default -> null;
                };
            }
        }
        return null;
    }

    private static String pitchToName(Note note) {
        final var pitch = note.getPitch();
        final var unpitched = note.getUnpitched();
        if (null == pitch) {
            if (null == unpitched) {
                return null;
            }
            return unpitched.getDisplayStep().name() + '-' + unpitched.getDisplayOctave();
        }
        final var alter = null == pitch.getAlter() ? '-' : switch (pitch.getAlter().toString()) {
            case "-1" -> 'b';
            case "1" -> '#';
            default -> throw new OcException("alter should be in [-1, 0, -] : %s", pitch.getAlter().toString());
        };
        var sa = pitch.getStep().name() + alter;
        if ('b' == alter) {
            sa = switch (sa) {
                case "Ab" -> "G#";
                case "Bb" -> "A#";
                case "Db" -> "C#";
                case "Eb" -> "D#";
                case "Gb" -> "F#";
                default -> throw new OcException("minor not unsupported : %s", sa);
            };
        }
        return sa + pitch.getOctave();
    }

    private static boolean isSamePitch(FtmNote ftmNote, Note note) {
        final var name = pitchToName(note);
        if (FtmNote.keyToName(ftmNote.getKey()).equals(name)) {
            return true;
        }
        for (final var key : ftmNote.getChord()) {
            if (name.equals(FtmNote.keyToName(key))) {
                return true;
            }
        }
        return false;
    }

    private static void printMxlScore(ScorePartwise scorePartwise) {
        log.info("version : {}", scorePartwise.getVersion());
        final var work = scorePartwise.getWork();
        if (null != work) {
            log.info("workNumber : {}", work.getWorkNumber());
            log.info("workTitle : {}", work.getWorkTitle());
            log.info("opus : {}", work.getOpus());
        }
        log.info("movementNumber : {}", scorePartwise.getMovementNumber());
        final var identification = scorePartwise.getIdentification();
        if (null != identification) {
            for (final var typedText : identification.getCreator()) {
                log.info("creator : {} {}", typedText.getValue(), typedText.getType());
            }
            for (final var typedText : identification.getRights()) {
                log.info("right : {} {}", typedText.getValue(), typedText.getType());
            }
            final var encoding = identification.getEncoding();
            if (null != encoding) {
                for (final var element : identification.getEncoding().getEncodingDateOrEncoderOrSoftware()) {
                    final var name = element.getName().toString();
                    switch (name) {
                        case "software" -> log.info("encoding : {} {}", name, element.getValue());
                        case "encoding-date" -> log.info("encoding : {} {}", name, element.getValue());
                        case "supports" -> log.info("encoding : {} {} {}", name, ((Supports) element.getValue()).getElement(), ((Supports) element.getValue()).getAttribute());
                        default -> throw new OcException("unknown encoding : %s %s", name, element.getValue());
                    }
                }
            }
            final var source = identification.getSource();
            if (null != source) {
                log.info("source : {}", source);
            }
            for (final var typedText : identification.getRelation()) {
                log.info("relation : {} {}", typedText.getValue(), typedText.getType());
            }
            final var miscellaneous = identification.getMiscellaneous();
            if (null != miscellaneous) {
                for (final var field : miscellaneous.getMiscellaneousField()) {
                    log.info("miscellaneous : {} {}", field.getName(), field.getValue());
                }
            }
        }
        for (final var credit : scorePartwise.getCredit()) {
            for (final var creditTypeOrLinkOrBookmark : credit.getCreditTypeOrLinkOrBookmark()) {
                if (creditTypeOrLinkOrBookmark.getValue() instanceof final FormattedText formattedText) {
                    log.info("credit : {} {} {}", credit.getPage(), creditTypeOrLinkOrBookmark.getName(), formattedText.getValue());
                } else {
                    log.info("credit : {} {} {}", credit.getPage(), creditTypeOrLinkOrBookmark.getName(), creditTypeOrLinkOrBookmark.getValue());
                }
            }
        }
    }

    private static void printMxlPart(int p, ScorePartwise.Part part) {
        final var id = (ScorePart) part.getId();
        if (null != id) {
            log.info("part {} id : {}", p, id.getId());
            log.info("part {} partName : {}", p, id.getPartName().getValue());
            for (final var scoreInstrument : id.getScoreInstrument()) {
                log.info("part {} scoreInstrument : {}", p, scoreInstrument.getInstrumentName());
            }
        }
        log.info("part {} measure size : {}", p, part.getMeasure().size());
    }

    private static void printMxlMeasure(int[] p_m, ScorePartwise.Part.Measure measure) {
        log.info("part {} measure {} : {}", p_m[0], p_m[1], measure.getId());
        for (final var noteOrBackupOrForward : measure.getNoteOrBackupOrForward()) {
            if (noteOrBackupOrForward instanceof final Print print) {
                log.info("print page : {}", print.getPageNumber());
            } else if (noteOrBackupOrForward instanceof final Barline barline) {
                log.info("barline location : {}", barline.getLocation());
            } else if (noteOrBackupOrForward instanceof final Attributes attributes) {
                log.info("attributes clef : {}", attributes.getClef());
                log.info("attributes divisions : {}", attributes.getDivisions());
                log.info("attributes key : {}", attributes.getKey());
                log.info("attributes staves : {}", attributes.getStaves());
                log.info("attributes time : {}", attributes.getTime());
            } else if (noteOrBackupOrForward instanceof final Backup backup) {
                log.info("backup : {}", backup.getDuration());
            } else if (noteOrBackupOrForward instanceof final Forward forward) {
                log.info("forward : {}", forward.getDuration());
            } else if (noteOrBackupOrForward instanceof final Direction direction) {
                log.info("noteOrBackupOrForward : {}", noteOrBackupOrForward.getClass().getName());
                if (null != direction.getSound()) {
                    if (null != direction.getSound().getTempo()) {
                        log.info("direction.getSound().getTempo() : {}", direction.getSound().getTempo());
                    }
                }
                for (final var type : direction.getDirectionType()) {
                    if (null != type.getRehearsal()) {
                        for (final var rehearsal : type.getRehearsal()) {
                            log.info("staff {} rehearsal : {}", direction.getStaff().intValue(), rehearsal.getValue());
                        }
                    }
                    if (null != type.getDynamics()) {
                        for (final var dynamic : type.getDynamics()) {
                            for (final var prop : dynamic.getPOrPpOrPpp()) {
                                log.info("staff {} dynamic : {}", direction.getStaff().intValue(), prop.getName());
                            }
                        }
                    }
                    if (null != type.getMetronome()) {
                        log.info("staff {} beat : {} {}", direction.getStaff().intValue(), type.getMetronome().getBeatUnit(), direction.getSound().getTempo());
                    }
                }
            } else if (noteOrBackupOrForward instanceof final Note note) {
                final var duration = note.getDuration();
                final var pitch = note.getPitch();
                if (null == pitch) {
                    log.info("note : {}", duration);
                } else {
                    final var isChord = null != note.getChord();
                    final var alter = null == pitch.getAlter() ? "-" : switch (pitch.getAlter().toString()) {
                        case "-1" -> "b";
                        case "1" -> "#";
                        default -> throw new OcException("part %d measure %d alter should be in [-1, 0, -] : %s", pitch.getAlter().toString());
                    };
                    final List<String> notations = new ArrayList<>();
                    for (final var notation : note.getNotations()) {
                        if (null != notation.getTiedOrSlurOrTuplet()) {
                            for (final var tiedOrSlurOrTuplet : notation.getTiedOrSlurOrTuplet()) {
                                if (tiedOrSlurOrTuplet instanceof final Articulations articulations) {
                                    if (null != articulations.getAccentOrStrongAccentOrStaccato()) {
                                        for (final var accentOrStrongAccentOrStaccato : articulations.getAccentOrStrongAccentOrStaccato()) {
                                            notations.add(accentOrStrongAccentOrStaccato.getName().toString());
                                        }
                                    }
                                } else if (tiedOrSlurOrTuplet instanceof final Tied tied) {
                                    notations.add("tied " + tied.getType());
                                }
                            }
                        }
                    }
                    log.info("note : {} {}{}{} {}{}", duration, pitch.getStep(), alter, pitch.getOctave(), isChord, notations);
                }
            } else {
                throw new OcException("part %d measure %d unknown noteOrBackupOrForward : %s", noteOrBackupOrForward.getClass().getName());
            }
        }
    }

    public static void printMxl(String filePath, boolean printMeasures) {
        final var scorePartwise = loadMxl(filePath);
        if (printMeasures) {
            parseMxl(scorePartwise, FamiTrackerUtils::printMxlScore, FamiTrackerUtils::printMxlPart, FamiTrackerUtils::printMxlMeasure);
        } else {
            parseMxl(scorePartwise, FamiTrackerUtils::printMxlScore, FamiTrackerUtils::printMxlPart, null);
        }
    }

    private static List<Integer> mxlDivisions4List;
    private static List<Integer> mxlMeasureSizeList;
    private static int mxlRepeatCount;

    private static void checkMxlScore(ScorePartwise scorePartwise) {
        mxlDivisions4List = new ArrayList<>();
        mxlMeasureSizeList = new ArrayList<>();
        mxlRepeatCount = 0;
    }

    private static void checkMxlPart(int p, ScorePartwise.Part part) {
        if (!mxlMeasureSizeList.contains(part.getMeasure().size())) {
            mxlMeasureSizeList.add(part.getMeasure().size());
        }
        if (1 != mxlMeasureSizeList.size()) {
            throw new OcException("part %d measure size should be unique : %s", p, mxlMeasureSizeList.toString());
        }
    }

    private static int lastEndingStop;
    private static void checkMxlMeasure(int[] p_m, ScorePartwise.Part.Measure measure) {
        for (final var noteOrBackupOrForward : measure.getNoteOrBackupOrForward()) {
            if (noteOrBackupOrForward instanceof final Barline barline) {
                if (null != barline.getRepeat()) {
                    final var repeat = barline.getRepeat();
                    switch (repeat.getDirection()) {
                        case FORWARD -> {
                            ++mxlRepeatCount;
                            if (mxlRepeatCount <= 0) {
                                throw new OcException("part %d measure %d unmatched repeat forward : %s", p_m[0], p_m[1], repeat);
                            }
                        }
                        case BACKWARD -> {
                            if (0 < mxlRepeatCount) {
                                --mxlRepeatCount;
                            }
                        }
                        default -> throw new OcException("part %d measure %d unknown repeat direction : %s", p_m[0], p_m[1], repeat);
                    }
                    if (null != repeat.getAfterJump()) {
                        throw new OcException("part %d measure %d repeat after jump unsupported : %s", p_m[0], p_m[1], repeat);
                    }
                    if (null != repeat.getTimes()) {
                        throw new OcException("part %d measure %d repeat times unsupported : %s", p_m[0], p_m[1], repeat);
                    }
                    if (null != repeat.getWinged()) {
                        throw new OcException("part %d measure %d repeat winged unsupported : %s", p_m[0], p_m[1], repeat);
                    }
                }
                if (null != barline.getEnding()) {
                    final var ending = barline.getEnding();
                    switch (ending.getNumber()) {
                        case "1" -> {
                            switch (ending.getType()) {
                                case START -> {}
                                case STOP -> {
                                    if (null == barline.getRepeat() || BackwardForward.BACKWARD != barline.getRepeat().getDirection()) {
                                        throw new OcException("part %d measure %d unmatched ending stop : %s", p_m[0], p_m[1], ending);
                                    }
                                    lastEndingStop = p_m[1];
                                }
                                case DISCONTINUE -> {
                                    lastEndingStop = p_m[1];
                                }
                            }
                        }
                        case "2" -> {
                            if (StartStopDiscontinue.START == ending.getType()) {
                                if (lastEndingStop + 1 != p_m[1]) {
                                    throw new OcException("part %d measure %d unmatched ending start : %s", p_m[0], p_m[1], ending);
                                }
                            }
                        }
                        default -> throw new OcException("part %d measure %d unknown ending number : %s", p_m[0], p_m[1], ending);
                    }
                } else {
                    if (null != barline.getRepeat() && BackwardForward.BACKWARD == barline.getRepeat().getDirection()) {
                        lastEndingStop = p_m[1];
                    } else {
                        lastEndingStop = -1;
                    }
                }
            } else if (noteOrBackupOrForward instanceof final Backup backup) {
                if (0 != backup.getDuration().scale()) {
                    throw new OcException("part %d measure %d backup duration should be int : %s", p_m[0], p_m[1], backup.getDuration().toString());
                }
            } else if (noteOrBackupOrForward instanceof final Forward forward) {
                if (0 != forward.getDuration().scale()) {
                    throw new OcException("part %d measure %d forward duration should be int : %s", p_m[0], p_m[1], forward.getDuration().toString());
                }
            } else if (noteOrBackupOrForward instanceof final Attributes attributes) {
                final var divisions = attributes.getDivisions();
                if (null != divisions) {
                    if (0 != divisions.scale()) {
                        throw new OcException("part %d measure %d divisions should be int : %s", p_m[0], p_m[1], divisions.toString());
                    }
                    final var divisions4 = divisions.intValue();
                    if (!mxlDivisions4List.contains(divisions4)) {
                        mxlDivisions4List.add(divisions4);
                    }
                    if (1 != mxlDivisions4List.size()) {
                        throw new OcException("part %d measure %d divisions4 should be unique : %s", p_m[0], p_m[1], mxlDivisions4List.toString());
                    }
                }
            } else if (noteOrBackupOrForward instanceof final Note note) {
                if (null != note.getGrace()) {
                    final var grace = note.getGrace();
                    if (null != grace.getStealTimePrevious()) {
                        throw new OcException("part %d measure %d unknown grace steal time previous : %s", p_m[0], p_m[1], grace);
                    }
                    if (null != grace.getStealTimeFollowing()) {
                        throw new OcException("part %d measure %d unknown grace steal time following : %s", p_m[0], p_m[1], grace);
                    }
                    if (null != grace.getMakeTime()) {
                        throw new OcException("part %d measure %d unknown grace make time : %s", p_m[0], p_m[1], grace);
                    }
                    switch (note.getType().getValue()) {
                        case "half", "quarter", "eighth", "16th":
                            break;
                        default:
                            throw new OcException("part %d measure %d grace note type unknown : %s", p_m[0], p_m[1], note.getType().getValue());
                    }
                } else if (0 != note.getDuration().scale()) {
                    throw new OcException("part %d measure %d note duration should be int : %s", p_m[0], p_m[1], note.getDuration().toString());
                }
            }
        }
    }

    private static void checkMxl(String filePath) {
        final var scorePartwise = loadMxl(filePath);
        parseMxl(scorePartwise, FamiTrackerUtils::checkMxlScore, FamiTrackerUtils::checkMxlPart, FamiTrackerUtils::checkMxlMeasure);
//        log.info("mxlDivisions16List : {}", mxlDivisions16List);
//        log.info("mxlMeasureSizeList : {}", mxlMeasureSizeList);
    }

    public static List<List<String>> groupMxlNoteNameByRow(String filePath, int measures, int divisions4) {
        final List<List<String>> rows = new ArrayList<>();
        for (int m = 0; m < measures; ++m) {
            for (int i = 0; i < 16; ++i) {
                rows.add(new ArrayList<>());
            }
        }
        final var scorePartwise = loadMxl(filePath);
        for (int p = 0; p < scorePartwise.getPart().size(); ++p) {
            final var part = scorePartwise.getPart().get(p);
            var rowIndex = 0;
            var prevRows = 0;
            for (int m = 0; m < part.getMeasure().size(); ++m) {
                final var measure = part.getMeasure().get(m);
                for (final var noteOrBackupOrForward : measure.getNoteOrBackupOrForward()) {
                    if (noteOrBackupOrForward instanceof final Backup backup) {
                        final var duration = backup.getDuration().intValue();
                        rowIndex -= duration * 4 / divisions4;
                    } else if (noteOrBackupOrForward instanceof final Forward forward) {
                        final var duration = forward.getDuration().intValue();
                        rowIndex += duration * 4 / divisions4;
                    } else if (noteOrBackupOrForward instanceof final Note note) {
                        final var isChord = null != note.getChord();
                        if (!isChord) {
                            rowIndex += prevRows;
                        }
                        final var duration = note.getDuration().intValue();
                        if (0 != (duration * 4) % divisions4) {
                            throw new OcException("part %d measure %d note rows should be int : %d/%d", p, m, duration, divisions4);
                        }
                        if (!isTieStop(note)) {
                            final var pitchName = pitchToName(note);
                            if (null != pitchName) {
                                rows.get(rowIndex).add(pitchName);
                            }
                        }
                        prevRows = duration * 4 / divisions4;
                    }
                }
            }
        }
        return rows;
    }

    public static void diffNoteGroupByRow(List<List<String>> midiRows, List<List<String>> mxlRows, int measures) {
        int noteDiff = 0;
        for (int m = 0; m < measures; ++m) {
            for (int i = 0; i < 16; ++i) {
                final int row = i + m * 16;
                final var midiNotes = midiRows.get(row);
                final var mxlNotes = mxlRows.get(row);
                final var diff = new ArrayList<>(midiNotes);
                diff.removeAll(mxlNotes);
                if (!diff.isEmpty()) {
                    log.info("{} {} : {} : {}", m + 1, i, midiNotes, mxlNotes);
                }
                noteDiff += midiNotes.size();
                noteDiff -= mxlNotes.size();
            }
        }
        log.info("noteDiff : {}", noteDiff);
    }

    // process per score
    private static int mxlDivisions4;
    private static int mxlMeasureSize;
    private static Map<Integer, Map<String, List<FtmNote>>> mxlPartToVoiceToChannel;
    private static Map<Integer, Map<String, Integer>> mxlPartToVoiceToStaff;
    // process per part
    private static Map<String, List<FtmNote>> mxlVoiceToChannel;
    private static Map<String, Integer> mxlVoiceToStaff;
    private static Map<Integer, Boolean> mxlStaffToPedal;
    private static int mxlRowIndex;
    private static int mxlPrevRows;
    private static Map<String, Queue<Note>> mxlVoiceToTieStartNoteQueue;
    private static Map<String, Integer> mxlVoiceToTuplet;

    private static List<FtmNote> newMxlChannel() {
        final List<FtmNote> mxlChannel = new ArrayList<>();
        for (int i = 0; i < mxlMeasureSize * 16; ++i) {
            mxlChannel.add(null);
        }
        return mxlChannel;
    }

    private static void processMxlScore(ScorePartwise scorePartwise) {
        mxlDivisions4 = mxlDivisions4List.get(0);
        mxlMeasureSize = mxlMeasureSizeList.get(0);
        mxlPartToVoiceToChannel = new LinkedHashMap<>();
        mxlPartToVoiceToStaff = new LinkedHashMap<>();
    }

    private static void processMxlPart(int p, ScorePartwise.Part part) {
        mxlVoiceToChannel = new LinkedHashMap<>();
        mxlPartToVoiceToChannel.put(p, mxlVoiceToChannel);
        mxlVoiceToStaff = new HashMap<>();
        mxlStaffToPedal = new HashMap<>();
        mxlPartToVoiceToStaff.put(p, mxlVoiceToStaff);
        mxlRowIndex = 0;
        mxlPrevRows = 0;
        mxlVoiceToTieStartNoteQueue = new HashMap<>();
        mxlVoiceToTuplet = new HashMap<>();
    }

    private static boolean isTieStart(Note note) {
        return note.getTie().stream().anyMatch(tie -> StartStop.START == tie.getType());
    }

    private static boolean isTieStop(Note note) {
        return note.getTie().stream().anyMatch(tie -> StartStop.STOP == tie.getType());
    }

    private static void offerTieStartNote(Note note) {
        final var noteQueue = mxlVoiceToTieStartNoteQueue.computeIfAbsent(note.getVoice(), k -> new ArrayDeque<>());
        noteQueue.offer(note);
    }

    private static Note pollTieStartNote(String voice) {
        final var noteQueue = mxlVoiceToTieStartNoteQueue.computeIfAbsent(voice, k -> new ArrayDeque<>());
        return noteQueue.poll();
    }

    private static List<FtmNote> findMxlChannel(int[] p_m, String voice, int rows, boolean isChord) {
        final var mxlChannel = mxlVoiceToChannel.computeIfAbsent(voice, k -> newMxlChannel());
        if (isChord) {
            final var ftmNote = mxlChannel.get(mxlRowIndex);
            for (int i = 0; i < rows; ++i) {
                final var existNote = mxlChannel.get(mxlRowIndex + i);
                if (null == existNote) {
                    throw new OcException("part %d measure %d voice %s row %d should be exist", p_m[0], p_m[1], voice, mxlRowIndex + i);
                }
                if (ftmNote != mxlChannel.get(mxlRowIndex + i)) {
                    throw new OcException("part %d measure %d voice %s row %d should be same", p_m[0], p_m[1], voice, mxlRowIndex + i);
                }
            }
        } else {
            for (int i = 0; i < rows; ++i) {
                if (null != mxlChannel.get(mxlRowIndex + i)) {
                    throw new OcException("part %d measure %d voice %s row %d should be empty", p_m[0], p_m[1], voice, mxlRowIndex + i);
                }
            }
        }
        return mxlChannel;
    }

    private static void fillNote(List<FtmNote> mxlChannel, int begin, int end, FtmNote note) {
        for (int row = begin; row < end; ++row) {
            mxlChannel.set(row, note);
        }
    }

    private static void processMxlMeasure(int[] p_m, ScorePartwise.Part.Measure measure) {
        for (final var noteOrBackupOrForward : measure.getNoteOrBackupOrForward()) {
            if (noteOrBackupOrForward instanceof final Backup backup) {
                final var duration = backup.getDuration().intValue();
                final var rows = duration * 4 / mxlDivisions4;
                if (duration != rows * mxlDivisions4 / 4) {
                    throw new OcException("part %d measure %d backup rows should be int : %d/%d", p_m[0], p_m[1], duration, mxlDivisions4);
                }
                mxlRowIndex -= rows;
            } else if (noteOrBackupOrForward instanceof final Forward forward) {
                final var duration = forward.getDuration().intValue();
                final var rows = duration * 4 / mxlDivisions4;
                if (duration != rows * mxlDivisions4 / 4) {
                    throw new OcException("part %d measure %d forward rows should be int : %d/%d", p_m[0], p_m[1], duration, mxlDivisions4);
                }
                mxlRowIndex += rows;
            } else if (noteOrBackupOrForward instanceof final Note note) {
                final var staffId = null == note.getStaff() ? -1 : note.getStaff().intValue();
                if (staffId != mxlVoiceToStaff.computeIfAbsent(note.getVoice(), k -> staffId)) {
                    throw new OcException("part %d measure %d voice %s staff not match : %d/%d", p_m[0], p_m[1], note.getVoice(), mxlVoiceToStaff.get(note.getVoice()), staffId);
                }
                final var isChord = null != note.getChord();
                if (!isChord) {
                    mxlRowIndex += mxlPrevRows;
                }
                // TODO grace
                final var duration = getDuration(note).intValue();
                final var rows = duration * 4 / mxlDivisions4;
                var additionRows = 0;
                var isTuplet = false;
                if (duration != rows * mxlDivisions4 / 4) {
                    if (mxlDivisions4 == duration * 3) {
                        isTuplet = true;
                    }
                    if (!isTuplet) {
                        throw new OcException("part %d measure %d note rows should be int : %d/%d", p_m[0], p_m[1], duration, mxlDivisions4);
                    }
                }
                final var mxlChannel = findMxlChannel(p_m, note.getVoice(), rows, isChord);
                if (isTieStart(note)) {
                    offerTieStartNote(note);
                }
                if (isTieStop(note)) {
                    final var tieStartNote = pollTieStartNote(note.getVoice());
                    if (null == tieStartNote) {
                        throw new OcException("part %d measure %d can't find tie start", p_m[0], p_m[1]);
                    }
                    if (!Objects.equals(pitchToName(note), pitchToName(tieStartNote))) {
                        throw new OcException("part %d measure %d can't match tie start pitch : %s", p_m[0], p_m[1], pitchToName(tieStartNote), pitchToName(note));
                    }
                    final var ftmNote = mxlChannel.get(mxlRowIndex - 1);
                    if (!isSamePitch(ftmNote, tieStartNote)) {
                        throw new OcException("part %d measure %d can't match tie start key : %s %s", p_m[0], p_m[1], pitchToName(tieStartNote), FtmNote.keyToName(ftmNote.getKey()));
                    }
                    fillNote(mxlChannel, mxlRowIndex, mxlRowIndex + rows, ftmNote);
                } else {
                    final var pitchName = pitchToName(note);
                    if (null == pitchName) {
                        for (int i = 0; i < rows; ++i) {
                            mxlChannel.set(mxlRowIndex + i, FtmNote.REST);
                        }
                    } else {
                        if (isChord) {
                            final var ftmNote = mxlChannel.get(mxlRowIndex);
                            ftmNote.addChord(pitchName);
                        } else {
                            final var ftmNote = FtmNote.of(pitchName);
                            ftmNote.setPedal(mxlStaffToPedal.getOrDefault(staffId, false));
                            final var tuplet = mxlVoiceToTuplet.getOrDefault(note.getVoice(), 0);
                            for (final var notation : note.getNotations()) {
                                for (final var tiedOrSlurOrTuplet : notation.getTiedOrSlurOrTuplet()) {
                                    if (tiedOrSlurOrTuplet instanceof final Articulations articulations) {
                                        for (final var accentOrStrongAccentOrStaccato : articulations.getAccentOrStrongAccentOrStaccato()) {
                                            switch (accentOrStrongAccentOrStaccato.getName().toString()) {
                                                case "accent" -> ftmNote.setAccent(true);
                                                case "staccato" -> ftmNote.setStaccato(true);
                                                default -> throw new OcException("part %d measure %d unknown notation : %s", p_m[0], p_m[1], accentOrStrongAccentOrStaccato.getName().toString());
                                            }
                                        }
                                    } else if (tiedOrSlurOrTuplet instanceof Arpeggiate) {
                                        ftmNote.setArpeggiate(true);
                                    } else if (tiedOrSlurOrTuplet instanceof Tied) {
                                    } else if (tiedOrSlurOrTuplet instanceof Tuplet) {
                                    } else if (tiedOrSlurOrTuplet instanceof final Slur slur) {
                                        // TODO start / stop
                                    } else {
                                        throw new OcException("part %d measure %d unknown notation : %s", p_m[0], p_m[1], tiedOrSlurOrTuplet);
                                    }
                                }
                            }
                            ftmNote.setTuplet(tuplet);
                            fillNote(mxlChannel, mxlRowIndex, mxlRowIndex + rows, ftmNote);
                        }
                        if (isTuplet) {
                            mxlVoiceToTuplet.merge(note.getVoice(), 1, Integer::sum);
                            if (3 == mxlVoiceToTuplet.get(note.getVoice())) {
                                mxlVoiceToTuplet.remove(note.getVoice());
                                additionRows += 1;
                            }
                        }
                    }
                }
                mxlPrevRows = rows + additionRows;
            } else if (noteOrBackupOrForward instanceof final Direction direction) {
                for (final var type : direction.getDirectionType()) {
                    var foundType = false;
                    if (null != type.getMetronome()) {
                        foundType = true;
                    }
                    if (null != type.getRehearsal()) {
                        foundType = true;
                    }
                    if (null != type.getWedge()) {
                        // TODO staff : crescendo,diminuendo/stop
                        foundType = true;
                    }
                    if (null != type.getPedal()) {
                        if (PedalType.START == type.getPedal().getType()) {
                            mxlStaffToPedal.put(direction.getStaff().intValue(), true);
                        }
                        if (PedalType.STOP == type.getPedal().getType()) {
                            mxlStaffToPedal.put(direction.getStaff().intValue(), false);
                        }
                        foundType = true;
                    }
                    if (!foundType) {
                        throw new OcException("part %d measure %d unknown direction : %s", p_m[0], p_m[1], type);
                    }
                }
            }
        }
    }

    private static void setChannel(List<FtmNote> mxlChannel, int instrument, int volume) {
        for (final var ftmNote : mxlChannel) {
            if (null != ftmNote) {
                ftmNote.setInstrument(instrument);
                ftmNote.setVolume(volume);
            }
        }
    }

    private static void processMxl(String filePath) {
        final var scorePartwise = loadMxl(filePath);
        parseMxlWithRepeat(scorePartwise, FamiTrackerUtils::processMxlScore, FamiTrackerUtils::processMxlPart, FamiTrackerUtils::processMxlMeasure);
        // remove empty channel
        for (final var part : mxlPartToVoiceToChannel.keySet().stream().toList()) {
            final var mxlVoiceToChannel = mxlPartToVoiceToChannel.get(part);
            for (final var voice : mxlVoiceToChannel.keySet().stream().toList()) {
                final var channel = mxlVoiceToChannel.get(voice);
                if (channel.stream().allMatch(note -> null == note || FtmNote.REST == note)) {
                    mxlVoiceToChannel.remove(voice);
                }
            }
            if (mxlVoiceToChannel.isEmpty()) {
                mxlPartToVoiceToChannel.remove(part);
            }
        }
        // log channel
        for (final var part : mxlPartToVoiceToChannel.keySet().stream().toList()) {
            final var mxlVoiceToChannel = mxlPartToVoiceToChannel.get(part);
            for (final var voice : mxlVoiceToChannel.keySet().stream().toList()) {
                log.info("channel : {} {}", part, voice);
            }
        }
        // remove rest note
        for (final var mxlVoiceToChannel : mxlPartToVoiceToChannel.values()) {
            for (final var mxlChannel : mxlVoiceToChannel.values()) {
                for (int row = 0; row < mxlChannel.size(); ++row) {
                    final var ftmNote = mxlChannel.get(row);
                    if (FtmNote.REST == ftmNote) {
                        mxlChannel.set(row, null);
                    }
                }
            }
        }
        // sort chord
        for (final var mxlVoiceToChannel : mxlPartToVoiceToChannel.values()) {
            for (final var mxlChannel : mxlVoiceToChannel.values()) {
                for (int row = 0; row < mxlChannel.size(); ++row) {
                    final var ftmNote = mxlChannel.get(row);
                    if (null != ftmNote && ftmNote.isChord()) {
                        ftmNote.sortChord();
                        if (16 < ftmNote.getChord().get(ftmNote.getChord().size() - 1) - ftmNote.getKey()) {
                            throw new OcException("row %d chord too wide : %d %d", row, ftmNote.getKey(), ftmNote.getChord());
                        }
                    }
                }
            }
        }
        // apply arpeggio
        final var arpeggioIdx = 0;
        for (final var mxlVoiceToChannel : mxlPartToVoiceToChannel.values()) {
            for (final var mxlChannel : mxlVoiceToChannel.values()) {
                final Set<FtmNote> ftmNoteSet = new HashSet<>();
                for (int row = 0; row < mxlChannel.size(); ++row) {
                    final var ftmNote = mxlChannel.get(row);
                    if (null != ftmNote && ftmNote.isChord()) {
                        if (ftmNoteSet.contains(ftmNote)) {
                            continue;
                        }
                        ftmNoteSet.add(ftmNote);
                        if (null != ftmNote.getEffect(arpeggioIdx)) {
                            throw new OcException("row %d fx should be null : %d", row, arpeggioIdx);
                        }
                        switch (ftmNote.getChord().size()) {
                            case 1 -> ftmNote.setEffect(arpeggioIdx, FtmEffect.arpeggio(ftmNote.getChord().get(0) - ftmNote.getKey(), 0));
                            case 2 -> ftmNote.setEffect(arpeggioIdx, FtmEffect.arpeggio(ftmNote.getChord().get(0) - ftmNote.getKey(), ftmNote.getChord().get(1) - ftmNote.getKey()));
                            case 3 -> ftmNote.setEffect(arpeggioIdx, FtmEffect.arpeggio(ftmNote.getChord().get(0) - ftmNote.getKey(), ftmNote.getChord().get(2) - ftmNote.getKey()));
                            default -> throw new OcException("row %d chord too many : %d %d", row, ftmNote.getKey(), ftmNote.getChord());
                        }
                    }
                }
            }
        }
        // add note cut
        for (final var mxlVoiceToChannel : mxlPartToVoiceToChannel.values()) {
            for (final var mxlChannel : mxlVoiceToChannel.values()) {
                for (int row = 1; row < mxlChannel.size(); ++row) {
                    final var ftmNote = mxlChannel.get(row);
                    final var prevNote = mxlChannel.get(row - 1);
                    if (null == ftmNote && null != prevNote && FtmNote.NOTE_CUT != prevNote.getKey()) {
                        mxlChannel.set(row, FtmNote.noteCut());
                    }
                }
            }
        }
        // disable effect
        for (final var mxlVoiceToChannel : mxlPartToVoiceToChannel.values()) {
            for (final var mxlChannel : mxlVoiceToChannel.values()) {
                for (int row = 1; row < mxlChannel.size(); ++row) {
                    final var ftmNote = mxlChannel.get(row);
                    final var prevNote = mxlChannel.get(row - 1);
                    if (null != ftmNote && null != prevNote && prevNote != ftmNote) {
                        for (int idx = 0; idx < prevNote.getFx().length; ++idx) {
                            if (null != prevNote.getEffect(idx) && null == ftmNote.getEffect(idx) &&
                                    prevNote.getEffect(idx).isForChannel() && !prevNote.getEffect(idx).isDisabled()) {
                                ftmNote.setEffect(idx, prevNote.getEffect(idx).disable());
                            }
                        }
                    }
                }
            }
        }
        // remove note duration
        for (final var mxlVoiceToChannel : mxlPartToVoiceToChannel.values()) {
            for (final var mxlChannel : mxlVoiceToChannel.values()) {
                for (int row = mxlChannel.size() - 1; 1 <= row; --row) {
                    final var ftmNote = mxlChannel.get(row);
                    final var prevNote = mxlChannel.get(row - 1);
                    if (prevNote == ftmNote) {
                        mxlChannel.set(row, null);
                    }
                }
            }
        }
    }

    private static void blueClapperFromMxl(String filePath, Map<String, FtmInstrument> nameToInstrument) {
        processMxl(filePath);
        final List<FtmInstrument> instrumentList = new ArrayList<>();
        instrumentList.add(nameToInstrument.get("cookie-snow-2a03-piano"));
        instrumentList.add(nameToInstrument.get("trojan-mage-2a03-Tri Bass 1"));
        instrumentList.add(nameToInstrument.get("cookie-snow-2a03-kick bass"));
        instrumentList.add(nameToInstrument.get("cookie-smile-vrc6-chimes"));
        instrumentList.add(nameToInstrument.get("isabelle-trouble-fds-Bass"));
        for (final var mxlChannelList : mxlPartToVoiceToChannel.values()) {
            final var channel1 = mxlChannelList.get("1");
            final var channel2 = mxlChannelList.get("2");
            final var channel3 = mxlChannelList.get("3");
            final var channel5 = mxlChannelList.get("5");
            final var channel6 = mxlChannelList.get("6");
            setChannel(channel1, 0, 8);
            setChannel(channel2, 0, 8);
            setChannel(channel3, 1, 7);
            setChannel(channel5, 3, 6);
            setChannel(channel6, 3, 6);
            final List<FtmChannel> channelList = new ArrayList<>();
            channelList.add(FtmChannel.of(channel1));
            channelList.add(FtmChannel.of(channel2));
            channelList.add(FtmChannel.of(channel3));
            channelList.add(null);
            channelList.add(null);
            channelList.add(FtmChannel.of(channel5));
            channelList.add(FtmChannel.of(channel6));
            channelList.add(null);
            final var ftmSong = FtmSong.of("BLUE CLAPPER", "Ongakucraft", "COVER Corp.",
                                           146, instrumentList, channelList);
//            log.info("channelList : {}", channelList.size());
            log.info("{}", ftmSong);
        }
    }

    private static void laLionFromMxl(String filePath, Map<String, FtmInstrument> nameToInstrument) {
        processMxl(filePath);
        final List<FtmInstrument> instrumentList = new ArrayList<>();
        instrumentList.add(nameToInstrument.get("cookie-snow-2a03-piano"));
        instrumentList.add(nameToInstrument.get("trojan-mage-2a03-Tri Bass 1"));
        instrumentList.add(nameToInstrument.get("cookie-snow-2a03-kick bass"));
        instrumentList.add(nameToInstrument.get("cookie-smile-vrc6-chimes"));
        instrumentList.add(nameToInstrument.get("isabelle-trouble-fds-Bass"));
        final var channel0 = mxlPartToVoiceToChannel.get(0).get("1");
        final var channel1 = mxlPartToVoiceToChannel.get(1).get("1");
        final var channel2 = mxlPartToVoiceToChannel.get(2).get("1");
        final var channel3 = mxlPartToVoiceToChannel.get(2).get("5");
        final var channel4 = mxlPartToVoiceToChannel.get(3).get("1");
        final var channel5 = mxlPartToVoiceToChannel.get(3).get("2");
        final var channel6 = mxlPartToVoiceToChannel.get(4).get("1");
        final var channel7 = mxlPartToVoiceToChannel.get(5).get("1");
        setChannel(channel0, 0, 8);
        setChannel(channel1, 0, 8);
        setChannel(channel2, 1, 7);
        setChannel(channel3, 1, 7);
        final List<FtmChannel> channelList = new ArrayList<>();
        channelList.add(FtmChannel.of(channel0));
        channelList.add(FtmChannel.of(channel1));
        channelList.add(null);
        channelList.add(null);
        channelList.add(null);
        channelList.add(FtmChannel.of(channel2));
        channelList.add(FtmChannel.of(channel3));
        channelList.add(null);
        final var ftmSong = FtmSong.of("La Lion", "Ongakucraft", "COVER Corp.",
                120, instrumentList, channelList);
//            log.info("channelList : {}", channelList.size());
        log.info("{}", ftmSong);
    }

    private static void shinySmilyStoryFromMxl(String filePath, Map<String, FtmInstrument> nameToInstrument) {
        processMxl(filePath);
        final List<FtmInstrument> instrumentList = new ArrayList<>();
        instrumentList.add(nameToInstrument.get("cookie-snow-2a03-piano"));
        instrumentList.add(nameToInstrument.get("trojan-mage-2a03-Tri Bass 1"));
        instrumentList.add(nameToInstrument.get("cookie-snow-2a03-kick bass"));
        instrumentList.add(nameToInstrument.get("cookie-smile-vrc6-chimes"));
        instrumentList.add(nameToInstrument.get("isabelle-trouble-fds-Bass"));
        for (final var mxlChannelList : mxlPartToVoiceToChannel.values()) {
            final var channel1 = mxlChannelList.get("1");
            final var channel5 = mxlChannelList.get("5");
            setChannel(channel1, 0, 8);
            setChannel(channel5, 3, 6);
            final List<FtmChannel> channelList = new ArrayList<>();
            channelList.add(FtmChannel.of(channel1));
            channelList.add(null);
            channelList.add(null);
            channelList.add(null);
            channelList.add(null);
            channelList.add(FtmChannel.of(channel5));
            channelList.add(null);
            channelList.add(null);
            final var ftmSong = FtmSong.of("Shiny Smily Story", "Ongakucraft", "COVER Corp.",
                    168, instrumentList, channelList);
//            log.info("channelList : {}", channelList.size());
            log.info("{}", ftmSong);
        }
    }

    private static void captureTheMomentFromMxl(String filePath, Map<String, FtmInstrument> nameToInstrument) {
        processMxl(filePath);
        final List<FtmInstrument> instrumentList = new ArrayList<>();
        instrumentList.add(nameToInstrument.get("cookie-snow-2a03-piano"));
        instrumentList.add(nameToInstrument.get("trojan-mage-2a03-Tri Bass 1"));
        instrumentList.add(nameToInstrument.get("cookie-snow-2a03-kick bass"));
        instrumentList.add(nameToInstrument.get("cookie-smile-vrc6-chimes"));
        instrumentList.add(nameToInstrument.get("isabelle-trouble-fds-Bass"));
        for (final var mxlChannelList : mxlPartToVoiceToChannel.values()) {
            final var channel1 = mxlChannelList.get("1");
            final var channel5 = mxlChannelList.get("5");
            setChannel(channel1, 0, 8);
            setChannel(channel5, 3, 6);
            final List<FtmChannel> channelList = new ArrayList<>();
            channelList.add(FtmChannel.of(channel1));
            channelList.add(null);
            channelList.add(null);
            channelList.add(null);
            channelList.add(null);
            channelList.add(FtmChannel.of(channel5));
            channelList.add(null);
            channelList.add(null);
            final var ftmSong = FtmSong.of("Shiny Smily Story", "Ongakucraft", "COVER Corp.",
                    168, instrumentList, channelList);
//            log.info("channelList : {}", channelList.size());
            log.info("{}", ftmSong);
        }
    }

    private static void check0th(String rootDirPath) {
        checkMxl(rootDirPath + "0th/Step and Go - Tokino Sora/Step_and_Go____Tokino_Sora.mxl");
        checkMxl(rootDirPath + "0th/Kotonoha - Roboco-san/____Kotonoha_-_Roboco-san.mxl");
        checkMxl(rootDirPath + "0th/afterglow - AZKi/afterglow.mxl");
        checkMxl(rootDirPath + "0th/Inochi - AZkI/Inochi_-_AZkI_WHiTE.mxl");
        checkMxl(rootDirPath + "0th/Sakura Kaz - Sakura Miko/mxl (1).mxl");
        checkMxl(rootDirPath + "0th/Sakura Kaz - Sakura Miko/Sakura_Kaze.mxl");
        checkMxl(rootDirPath + "0th/Sakura Kaz - Sakura Miko/Sakura_Kaze_Piano_Ver._-_Sakura_Miko.mxl");
        checkMxl(rootDirPath + "0th/wii-wii-woo - Hoshimachi Suisei/_wii-wii-woo.mxl");
        checkMxl(rootDirPath + "0th/bibidiba - Hoshimachi Suisei/_-_.mxl");
        checkMxl(rootDirPath + "0th/bibidiba - Hoshimachi Suisei/BIBIDIBA___Saxophone_ver..mxl");
        checkMxl(rootDirPath + "0th/bibidiba - Hoshimachi Suisei/Bibbidiba.mxl");
        checkMxl(rootDirPath + "0th/Kakero - Hoshimachi Suisei/__.mxl");
        checkMxl(rootDirPath + "0th/SUICHAN-NO-MAINTENANCE - Hoshimachi Suisei/.mxl");
        checkMxl(rootDirPath + "0th/soiree - Hoshimachi Suisei/.mxl");
        checkMxl(rootDirPath + "0th/soiree - Hoshimachi Suisei/_-_.mxl");
        checkMxl(rootDirPath + "0th/Michizure - Hoshimachi Suisei/.mxl");
        checkMxl(rootDirPath + "0th/Michizure - Hoshimachi Suisei/_-_.mxl");
        checkMxl(rootDirPath + "0th/Michizure - Hoshimachi Suisei/_-_Ayase.mxl");
        checkMxl(rootDirPath + "0th/Michizure - Hoshimachi Suisei/_Michizure.mxl");
        checkMxl(rootDirPath + "0th/Bye Bye Rainy - Hoshimachi Suisei/.mxl");
        checkMxl(rootDirPath + "0th/GHOST - Hoshimachi Suisei/GHOST.mxl");
        checkMxl(rootDirPath + "0th/GHOST - Hoshimachi Suisei/GHOST__.mxl");
        checkMxl(rootDirPath + "0th/GHOST - Hoshimachi Suisei/GHOST (1).mxl");
        checkMxl(rootDirPath + "0th/GHOST - Hoshimachi Suisei/GHOST__Hoshimachi_Suisei.mxl");
        checkMxl(rootDirPath + "0th/GHOST - Hoshimachi Suisei/GHOST_-_HOSHIMACHI_SUISEI.mxl");
        checkMxl(rootDirPath + "0th/GHOST - Hoshimachi Suisei/GHOST__Hoshimachi_Suisei_by_Cassi.mxl");
        checkMxl(rootDirPath + "0th/GHOST - Hoshimachi Suisei/GHOST_-_____Shiroha.mxl");
        checkMxl(rootDirPath + "0th/Stellar Stellar - Hoshimachi Suisei/Stellar_Stellar.mxl");
        checkMxl(rootDirPath + "0th/Stellar Stellar - Hoshimachi Suisei/Stellar_Stellar (1).mxl");
        checkMxl(rootDirPath + "0th/Stellar Stellar - Hoshimachi Suisei/Stellar_Stellar (2).mxl");
        checkMxl(rootDirPath + "0th/Stellar Stellar - Hoshimachi Suisei/Stellar_Stellar_-_____Shiroha.mxl");
        checkMxl(rootDirPath + "0th/Stellar Stellar - Hoshimachi Suisei/Stellar_Stellar_-_Hoshimachi_Suisei.mxl");
        checkMxl(rootDirPath + "0th/Stellar Stellar - Hoshimachi Suisei/Stellar_Stellar__Hoshimachi_Suisei__THE_FIRST_TAKE_Version.mxl");
        checkMxl(rootDirPath + "0th/Tenkyuu Suisei wa Yoru wo Mataide - Hoshimachi Suisei/.mxl");
        checkMxl(rootDirPath + "0th/Tenkyuu Suisei wa Yoru wo Mataide - Hoshimachi Suisei/__.mxl");
        checkMxl(rootDirPath + "0th/Tenkyuu Suisei wa Yoru wo Mataide - Hoshimachi Suisei/Tenkyuu.mxl");
        checkMxl(rootDirPath + "0th/Next Color Planet - Hoshimachi Suisei/New_Colour_Planet.mxl");
        checkMxl(rootDirPath + "0th/Next Color Planet - Hoshimachi Suisei/NEXT_COLOR_PLANET.mxl");
        checkMxl(rootDirPath + "0th/Next Color Planet - Hoshimachi Suisei/NEXT_COLOR_PLANET_-_____Shiro.mxl");
        checkMxl(rootDirPath + "0th/Next Color Planet - Hoshimachi Suisei/Next_Color_Planet_-_Hoshimachi_Suisei (1).mxl");
        checkMxl(rootDirPath + "0th/Next Color Planet - Hoshimachi Suisei/Next_Color_Planet_-_Hoshimachi_Suisei.mxl");
        checkMxl(rootDirPath + "0th/Next Color Planet - Hoshimachi Suisei/Next_Color_Planet__Hoshimachi_Suisei_for_String_Orchestra.mxl");
        checkMxl(rootDirPath + "0th/Next Color Planet - Hoshimachi Suisei/Next_Colour_Planet.mxl");
    }

    private static void check1th(String rootDirPath) {
        checkMxl(rootDirPath + "1th/Shallys - Aki Rosenthal/Shallys.mxl");
        checkMxl(rootDirPath + "1th/Shallys - Aki Rosenthal/Shallys_-_Aki_Rosenthal.mxl");
        checkMxl(rootDirPath + "1th/Heroine Audition - Aki Rosenthal/Heroine_Audition__Aki_Rosenthal.mxl");
        checkMxl(rootDirPath + "1th/Your Destiny Situation - Aki Rosenthal/___Aki_Rosenthal.mxl");
        checkMxl(rootDirPath + "1th/REDHEART - Akai Haato/REDHEART.mxl");
        checkMxl(rootDirPath + "1th/REDHEART - Akai Haato/REDHEART_-_Akai_HaatoHaachama.mxl");
        checkMxl(rootDirPath + "1th/REDHEART - Akai Haato/REDHEART_Jazz_ver..mxl");
        checkMxl(rootDirPath + "1th/Infinity - Akai Haato/Infinity_-_Akai_Haato_x_Haachama.mxl");
        // TODO 夏色まつり
        // TODO 夜空メル
    }

    private static void check2th(String rootDirPath) {
        checkMxl(rootDirPath + "2th/Mage of Violet - Murasaki Shion/Mage_of_Violet_-_Murasaki_Shion.mxl");
        checkMxl(rootDirPath + "2th/Docchi Docchi Song - Nakiri Ayame/Extended.mxl");
        // TODO 癒月ちょこ
        checkMxl(rootDirPath + "2th/Pleiades - Oozora Subaru/mxl (1).mxl");
        checkMxl(rootDirPath + "2th/Aqua iro palette - Aqua Minato/_Aqua_Colored_Palette_-_Minato_Aqua.mxl");
        checkMxl(rootDirPath + "2th/Aqua iro palette - Aqua Minato/_Aqua-Coloured_Palette_-__Minato_Aqua.mxl");
        checkMxl(rootDirPath + "2th/Aqua iro palette - Aqua Minato/Aqua_iro_palette_-_Aqua_Minato.mxl");
        checkMxl(rootDirPath + "2th/Aqua iro palette - Aqua Minato/arr-.mxl");
        checkMxl(rootDirPath + "2th/Kira Kira Minato Aqua/_-_.mxl");
        checkMxl(rootDirPath + "2th/Kira Kira Minato Aqua/mxl (2).mxl");
        checkMxl(rootDirPath + "2th/I Wanna - Minato Aqua/I_Wanna_-_Minato_Aqua.mxl");
        checkMxl(rootDirPath + "2th/I Wanna - Minato Aqua/mxl (1).mxl");
        checkMxl(rootDirPath + "2th/Imada Aoi - Minato Aqua/__.mxl");
        checkMxl(rootDirPath + "2th/Imada Aoi - Minato Aqua/imada_aoi.mxl");
        checkMxl(rootDirPath + "2th/Imada Aoi - Minato Aqua/Imada_Aoi___-_Minato_Aqua_.mxl");
        checkMxl(rootDirPath + "2th/uni-birth - Minato Aqua/uni-birth.mxl");
    }

    private static void check3th(String rootDirPath) {
        checkMxl(rootDirPath + "3th/Iiwake bunny - pekora/.mxl");
        checkMxl(rootDirPath + "3th/Pekorandom Brain - Usada Pekora/Pekorandom_Brain.mxl");
        checkMxl(rootDirPath + "3th/Atelier - Shiranui Flare/_Atelier_-_Shiranui_Flare.mxl");
        checkMxl(rootDirPath + "3th/Homenobi - Shirogane Noel/__.mxl");
        checkMxl(rootDirPath + "3th/ririkaru monster - Shirogane Noel/Monster.mxl");
        checkMxl(rootDirPath + "3th/Im Your Treasure Box - Houshou Marine/Im_Your_Treasure_Box.mxl");
        checkMxl(rootDirPath + "3th/Im Your Treasure Box - Houshou Marine/Im_Your_Treasure_Box___ (1).mxl");
        checkMxl(rootDirPath + "3th/Im Your Treasure Box - Houshou Marine/Im_Your_Treasure_Box___.mxl");
        checkMxl(rootDirPath + "3th/Ahoy We are the Houshou Pirates - Houshou Marine/Ahoy_.mxl");
        checkMxl(rootDirPath + "3th/Ahoy We are the Houshou Pirates - Houshou Marine/Ahoy___.mxl");
        checkMxl(rootDirPath + "3th/Ahoy We are the Houshou Pirates - Houshou Marine/Ahoy___-_Marine_Hosho_Hololive.mxl");
        checkMxl(rootDirPath + "3th/Ahoy We are the Houshou Pirates - Houshou Marine/c28d004cf9c4162124a77d4434b62a0d20cc589d.mxl");
        checkMxl(rootDirPath + "3th/Unison - Houshou Marine/UNISON.mxl");
        checkMxl(rootDirPath + "3th/Marine Set Sail - Houshou Marine/.mxl");
        checkMxl(rootDirPath + "3th/omoikou - Uruha Rushia/.mxl");
        checkMxl(rootDirPath + "3th/omoikou - Uruha Rushia/omoikou_-_Uruha_Rushia.mxl");
    }

    private static void check4th(String rootDirPath) {
        checkMxl(rootDirPath + "4th/Tokusya-Seizon Wonder-la-der - Amane Kanata/.mxl");
        checkMxl(rootDirPath + "4th/Oracle - Amane Kanata/Oracle___Amane_Kanata.mxl");
        checkMxl(rootDirPath + "4th/Dreamy Sheep - Tsunomaki watame/.mxl");
        checkMxl(rootDirPath + "4th/Everlasting Soul - Tsunomaki Watame/Everlasting_Soul.mxl");
        checkMxl(rootDirPath + "4th/mayday mayday - Tsunomaki Watame/mayday_mayday.mxl");
        checkMxl(rootDirPath + "4th/My Song - Tsunomaki Watame/My_song_-_Horie_Shota.mxl");
        checkMxl(rootDirPath + "4th/My Song - Tsunomaki Watame/My_Song_-_Tsunomaki_Watame.mxl");
        checkMxl(rootDirPath + "4th/My Song - Tsunomaki Watame/My_Song_WIP_-_Watame.mxl");
        checkMxl(rootDirPath + "4th/Ai-mai Chocolate - Tsunomaki Watame/Ai-mai_Chocolate_-_Tsunomaki_Watame.mxl");
        checkMxl(rootDirPath + "4th/Cloudy Sheep - Tsunomaki Watame/_Cloudy_Sheep_-_Tsunomaki_Watame.mxl");
        checkMxl(rootDirPath + "4th/FACT - Tokoyami Towa/FACT.mxl");
        // TODO 姫森ルーナ
        checkMxl(rootDirPath + "4th/Weather Hackers - Kiryu Coco/Weather_Hackers.mxl");
        checkMxl(rootDirPath + "4th/Weather Hackers - Kiryu Coco/Weather_Hackers__Kiryu_Coco.mxl");
        checkMxl(rootDirPath + "4th/Weather Hackers - Kiryu Coco/Weather_Hackers_-_Kiryu_Coco.mxl");
        checkMxl(rootDirPath + "4th/Weather Hackers - Kiryu Coco/Weather_Hackers__Kiryu_Coco__Instrumental.mxl");
        checkMxl(rootDirPath + "4th/Weather Hackers - Kiryu Coco/Weather_Hackers_Full_Band.mxl");
        checkMxl(rootDirPath + "4th/Kiseki Knot - hololive 4th Generation/_Kiseki_Knot__Hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "4th/Kiseki Knot - hololive 4th Generation/Kiseki_Knot.mxl");
        checkMxl(rootDirPath + "4th/Kiseki Knot - hololive 4th Generation/Kiseki_Knot_-_hololive_4th_Generation.mxl");
        checkMxl(rootDirPath + "4th/Kiseki Knot - hololive 4th Generation/.mxl");
    }

    private static void check5th(String rootDirPath) {
        checkMxl(rootDirPath + "5th/Asu e no Kyoukaisen - Yukihana Lamy/Asu_e_no_Taisen_-_Yukihana_Lamy.mxl");
        checkMxl(rootDirPath + "5th/Lunch with me - Momosuzu Nene/Lunch_with_Me.mxl");
        checkMxl(rootDirPath + "5th/Lunch with me - Momosuzu Nene/Lunch_with_me (1).mxl");
        checkMxl(rootDirPath + "5th/Nenenenenenenene Daibakusou - Momosuzu Nene/.mxl");
        checkMxl(rootDirPath + "5th/Congrachumarch - Momosuzu Nene/CHU__Congrachu_March.mxl");
        checkMxl(rootDirPath + "5th/La-Lion/La-Lion_A_song_for_Nene_made_for_Shishiro_Botan.mxl");
        checkMxl(rootDirPath + "5th/HOLOGRAM CIRCUS - Omaru Polka/HOLOGRAM_CIRCUS.mxl");
        checkMxl(rootDirPath + "5th/HOLOGRAM CIRCUS - Omaru Polka/HOLOGRAM_CIRCUS_-_Omaru_Polka.mxl");
        checkMxl(rootDirPath + "5th/Saikyoutic Polka/.mxl");
        checkMxl(rootDirPath + "5th/BLUE_CLAPPER/BLUE_CLAPPER__Hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "5th/Twinkle 4 You/Twinkle_4_You_-_NePoLaBo.mxl");
    }

    private static void check6th(String rootDirPath) {
        checkMxl(rootDirPath + "6th/drop candy - La+ Darknesss/drop_candy.mxl");
        // TODO 鷹嶺ルイ
        checkMxl(rootDirPath + "6th/WAO - Hakui Koyori/WAO.mxl");
        checkMxl(rootDirPath + "6th/WAO - Hakui Koyori/WAO_-_.mxl");
        checkMxl(rootDirPath + "6th/Paralyze - Sakamata Chloe/Paralyze_-_Sakamata_Chloe.mxl");
        checkMxl(rootDirPath + "6th/IrohaStep - kazama iroha/.mxl");
    }

    private static void check7th(String rootDirPath) {
        // TODO 火威青
        // TODO 音乃瀬奏
        // TODO 一条莉々華
        // TODO 儒烏風亭らでん
        // TODO 轟はじめ
    }

    private static void checkEn1(String rootDirPath) {
        checkMxl(rootDirPath + "en1/Ijimekko Bully - Mori Calliope/_Bully__Ijimekko_Bully_-_Mori_Calliope_.mxl");
        checkMxl(rootDirPath + "en1/Red - Calliope Mori/Red.mxl");
        checkMxl(rootDirPath + "en1/Red - Calliope Mori/Red (1).mxl");
        checkMxl(rootDirPath + "en1/Red - Calliope Mori/Red (1).mxl");
        checkMxl(rootDirPath + "en1/Excuse My Rudeness But Could You Please RIP - Calliope Mori/Excuse_My_Rudeness_But_Could_You_Please_RIP__WIP.mxl");
        checkMxl(rootDirPath + "en1/Guh - Calliope Mori/guh.mxl");
        checkMxl(rootDirPath + "en1/Dead Beats - Mori Calliope/Dead_Beats_-_Mori_Calliope.mxl");
        checkMxl(rootDirPath + "en1/DO U - Takanashi Kiara/DO_U.mxl");
        checkMxl(rootDirPath + "en1/DO U - Takanashi Kiara/DO_U_-_KIRA__Takanashi_Kiara_Orchestral_Arrangement_by_Deemo_Harlos.mxl");
        checkMxl(rootDirPath + "en1/SPARKS - Takanashi Kiara/SPARKS__Takanashi_Kiara.mxl");
        checkMxl(rootDirPath + "en1/HINOTORI - Takanashi Kiara/Hinotori.mxl");
        checkMxl(rootDirPath + "en1/HINOTORI - Takanashi Kiara/Hinotori_by_Takanashi_Kiara.mxl");
        checkMxl(rootDirPath + "en1/HINOTORI - Takanashi Kiara/Hinotori_O.mxl");
        checkMxl(rootDirPath + "en1/Fever Night - Takanashi Kiara/Fever_Night_-_Takanashi_Kiara.mxl");
        checkMxl(rootDirPath + "en1/Violet - Ninomae Ina nis/Violet (1).mxl");
        checkMxl(rootDirPath + "en1/Violet - Ninomae Ina nis/VIOLET (2).mxl");
        checkMxl(rootDirPath + "en1/Violet - Ninomae Ina nis/Violet (3).mxl");
        checkMxl(rootDirPath + "en1/Violet - Ninomae Ina nis/Violet.mxl");
        checkMxl(rootDirPath + "en1/Violet - Ninomae Ina nis/Violet__seibin.mxl");
        checkMxl(rootDirPath + "en1/Violet - Ninomae Ina nis/HL__I.mxl");
        checkMxl(rootDirPath + "en1/MECONOPSIS - Ninomae Ina nis/MECONOPSIS.mxl");
        checkMxl(rootDirPath + "en1/REFLECT - Gawr Gura/REFLECT_Preview_Size_-_Gawr_Gura.mxl");
        checkMxl(rootDirPath + "en1/REFLECT - Gawr Gura/REFLECT_-_Gura__Test.mxl");
        checkMxl(rootDirPath + "en1/REFLECT - Gawr Gura/REFLECT__Gawr_Gura_Reflect_-_Gawr_Gura.mxl");
        checkMxl(rootDirPath + "en1/REFLECT - Gawr Gura/REFLECT_-_Gawr_Gura.mxl");
        checkMxl(rootDirPath + "en1/REFLECT - Gawr Gura/REFLECT.mxl");
        checkMxl(rootDirPath + "en1/REFLECT - Gawr Gura/Reflect (1).mxl");
        checkMxl(rootDirPath + "en1/REFLECT - Gawr Gura/ORIGINAL_REFLECT_-_Gawr_Gura_-_Farhan_Sarasin.mxl");
        checkMxl(rootDirPath + "en1/Tokyo Wabi Sabi Lullaby - Gawr Gura/Tokyo_Wabi_Sabi_Lullaby.mxl");
        checkMxl(rootDirPath + "en1/ChikuTaku - Watson Amelia/ChikuTaku.mxl");
        checkMxl(rootDirPath + "en1/Non-Fiction/Non-Fiction.mxl");
        checkMxl(rootDirPath + "en1/Non-Fiction/Non-Fiction__hololive_English_-Myth-.mxl");
    }

    private static void checkEn2(String rootDirPath) {
        checkMxl(rootDirPath + "en2/Let Me Stay Here - Ceres Fauna/Let_Me_Say_Here.mxl");
        checkMxl(rootDirPath + "en2/Daydream - Ouro Kronii/Daydream (1).mxl");
        checkMxl(rootDirPath + "en2/Daydream - Ouro Kronii/Daydream.mxl");
        checkMxl(rootDirPath + "en2/Daydream - Ouro Kronii/Daydream_-_Ouro_Kronii (1).mxl");
        checkMxl(rootDirPath + "en2/Daydream - Ouro Kronii/Daydream_-_Ouro_Kronii.mxl");
        checkMxl(rootDirPath + "en2/A New Start - Nanashi Mumei/a_new_start (1).mxl");
        checkMxl(rootDirPath + "en2/A New Start - Nanashi Mumei/A_New_Start (2).mxl");
        checkMxl(rootDirPath + "en2/A New Start - Nanashi Mumei/A_New_Start.mxl");
        checkMxl(rootDirPath + "en2/A New Start - Nanashi Mumei/A_New_Start_-_Nanashi_Mumei.mxl");
        checkMxl(rootDirPath + "en2/A New Start - Nanashi Mumei/A_New_Start__Nanashi_Mumei_Ch..mxl");
        checkMxl(rootDirPath + "en2/mumei - Nanashi Mumei/mumei_-_TKN__Nanashi_Mumei_Piano_Solo_Arr._Harlos.mxl");
        checkMxl(rootDirPath + "en2/mumei - Nanashi Mumei/Mumei.mxl");
        checkMxl(rootDirPath + "en2/PLAY DICE - Hakos Baelz/PLAY_DICE__Hakos_Baelz.mxl");
        checkMxl(rootDirPath + "en2/PLAY DICE - Hakos Baelz/PLAY_DICE_-_Hakos_Baelz.mxl");
        checkMxl(rootDirPath + "en2/MESS - Hakos Baelz/MESS.mxl");
        checkMxl(rootDirPath + "en2/MESS - Hakos Baelz/MESS_-_Hakos_Baelz.mxl");
        checkMxl(rootDirPath + "en2/Astrogirl - Tsukumo Sana/Astrogirl (1).mxl");
        checkMxl(rootDirPath + "en2/Astrogirl - Tsukumo Sana/Astrogirl.mxl");
        checkMxl(rootDirPath + "en2/Astrogirl - Tsukumo Sana/Astrogirl__Tsukumo_Sana.mxl");
    }

    private static void checkEn3(String rootDirPath) {
        // TODO シオリ・ノヴェラ
        // TODO 古石ビジュー
        // TODO ネリッサ・レイヴンクロフト
        // TODO フワワ・アビスガード
        // TODO モココ・アビスガード
    }

    private static void checkEn4(String rootDirPath) {
        // TODO エリザベス・ローズ・ブラッドフレイム
        // TODO ジジ・ムリン
        // TODO セシリア・イマーグリーン
        // TODO ラオーラ・パンテーラ
    }

    private static void checkGamers(String rootDirPath) {
        checkMxl(rootDirPath + "gamer/Say Fanfare - Shirakami Fubuki/Playable_Solo_Piano_Say_Say_Fanfare_-_Shirakami_Fubuki_.mxl");
        checkMxl(rootDirPath + "gamer/Say Fanfare - Shirakami Fubuki/Say_Fanfare.mxl");
        checkMxl(rootDirPath + "gamer/Say Fanfare - Shirakami Fubuki/Say_Fanfare__Shirakami_Fubuki.mxl");
        checkMxl(rootDirPath + "gamer/Say Fanfare - Shirakami Fubuki/Shirakami_Fubuki_-_Say.mxl");
        checkMxl(rootDirPath + "gamer/LETTER - Shirakami Fubuki/LETTER.mxl");
        checkMxl(rootDirPath + "gamer/LETTER - Shirakami Fubuki/LETTER_-_Shirakami_Fubuki.mxl");
        checkMxl(rootDirPath + "gamer/Hi Fine FOX - Shirakami Fubuki/Hi_Fine_FOX__Shirakami_Fubuki.mxl");
        checkMxl(rootDirPath + "gamer/KINGWORLD - Shirakami Fubuki/KINGWORLD_-_sasakure.UK___Fubuki_Piano_Solo_Arr._Harlos.mxl");
        checkMxl(rootDirPath + "gamer/KONKON Beats - Shirakami Fubuki/KONKON_Beats.mxl");
        // TODO 大神ミオ
        checkMxl(rootDirPath + "gamer/Mogu Mogu Yummy - Nekomata Okayu/YUMMY.mxl");
        checkMxl(rootDirPath + "gamer/Mogu Mogu Yummy - Nekomata Okayu/YUMMY (1).mxl");
        checkMxl(rootDirPath + "gamer/Mogu Mogu Yummy - Nekomata Okayu/Mogu_Mogu_Yummy__Nekomata_Okayu_MOGU_MOGU_YUMMY__YUMMY.mxl");
        checkMxl(rootDirPath + "gamer/Saikyou Tensai Wonderful World of Korone - Inugami Korone/korones_saikyou_tensai_wonderful_world.mxl");
        checkMxl(rootDirPath + "gamer/Saikyou Tensai Wonderful World of Korone - Inugami Korone/Saikyou_Tensai_Wonderful_World_of_Korone.mxl");
        checkMxl(rootDirPath + "gamer/Doggy Gods Street - Inugami Korone/Doggy_Gods_Street_-_for_Sax_Quartet.mxl");
        checkMxl(rootDirPath + "gamer/Haro Haro Nariyansu - Inugami Korone/HALO_HALO_NARIYANSU_ONDO.mxl");
    }

    private static void checkHope(String rootDirPath) {
        // TODO IRyS
    }

    private static void checkId1(String rootDirPath) {
        checkMxl(rootDirPath + "id1/ALiCE&u - Ayunda Risu/Aliceu.mxl");
        checkMxl(rootDirPath + "id1/ALiCE&u - Ayunda Risu/ALiCEu_-_Ayunda_Risu.mxl");
        checkMxl(rootDirPath + "id1/High Tide - Moona Hoshinova/High_Tide (1).mxl");
        checkMxl(rootDirPath + "id1/High Tide - Moona Hoshinova/High_Tide.mxl");
        checkMxl(rootDirPath + "id1/High Tide - Moona Hoshinova/High_Tide__Moona_Hoshinova.mxl");
        checkMxl(rootDirPath + "id1/High Tide - Moona Hoshinova/High_Tide_String_Quartet_Arrangement.mxl");
        checkMxl(rootDirPath + "id1/Taut Hati – Moona Hoshinova/Taut_Hati.mxl"); // TODO fix grace note type unknown : half
        // TODO Bersama Ioforia - Airani Iofifteen
        checkMxl(rootDirPath + "id1/Dramatic XViltration - AREA 15/Dramatic_XViltration___XViltrasi_Dramatis.mxl");
    }

    private static void checkId2(String rootDirPath) {
        // TODO Kureiji Ollie
        // TODO Anya Melfissa
        // TODO Pavolia Reine
    }

    private static void checkId3(String rootDirPath) {
        checkMxl(rootDirPath + "id3/You're Mine - Vestia Zeta/Youre_Mine_-_Vestia_Zeta.mxl");
        checkMxl(rootDirPath + "id3/BACKSEAT - Kaela Kovalskia/BACKSEAT__Kaela_Kovalskia.mxl");
        checkMxl(rootDirPath + "id3/Mantra Hujan - Kobo Kaneru/Mantra_Hujan.mxl");
        checkMxl(rootDirPath + "id3/Mantra Hujan - Kobo Kaneru/Mantra_Hujan__Kobo_Kanaeru.mxl");
        checkMxl(rootDirPath + "id3/Mantra Hujan - Kobo Kaneru/Mantra_Hujan_-_Kobo_Kanaeru.mxl");
        checkMxl(rootDirPath + "id3/Mantra Hujan - Kobo Kaneru/Mantra_Hujan_-_Kobo_Kaneru.mxl");
        checkMxl(rootDirPath + "id3/Mantra Hujan - Kobo Kaneru/-Mantra_Hujan_-_Kobo_Kanaeru.mxl");
        checkMxl(rootDirPath + "id3/HELP - Kobo Kanaeru/HELP.mxl");
        checkMxl(rootDirPath + "id3/HELP - Kobo Kanaeru/HELP__Kobo_Kanaeru (1).mxl");
        checkMxl(rootDirPath + "id3/HELP - Kobo Kanaeru/HELP__Kobo_Kanaeru.mxl");
        checkMxl(rootDirPath + "id3/HELP - Kobo Kanaeru/HELP__Kobo_Kanaeru__Full_Ensemble_Transcription_almost.mxl");
    }

    private static void checkIdol(String rootDirPath) {
        checkMxl(rootDirPath + "idol/Asuiro ClearSky/ClearSky_-_hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "idol/Candy-Go-Round/Candy-Go-Round.mxl");
        checkMxl(rootDirPath + "idol/Capture the Moment/Capture_the_Moments.mxl");
        checkMxl(rootDirPath + "idol/Capture the Moment/Capture_the_Moment__Hololive_IDOL_PROJECT_Hololive_5th_Fes..mxl");
        checkMxl(rootDirPath + "idol/DAILY DIARY/DAILY_DIARY_-_hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "idol/DAILY DIARY/DAILY_DIARY_short_ver._-_Matsuri_Subaru_Miko_Noel_Marine.mxl");
        checkMxl(rootDirPath + "idol/Dreaming Days/Dreaming_Days (1).mxl");
        checkMxl(rootDirPath + "idol/Dreaming Days/DREAMING_DAYS.mxl");
        checkMxl(rootDirPath + "idol/Dreaming Days/Dreaming_Days__Hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "idol/Hyakkaryoran Hanafubuki/__Hyakka_Ryouran_Hanafubuki__hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "idol/Hyakkaryoran Hanafubuki/__Hyakkaryoran_Hanafubuki_-_hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "idol/Hyakkaryoran Hanafubuki/_Hyakkaryouran_Hanafubuki__hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "idol/Hyakkaryoran Hanafubuki/hanafubuki.mxl");
        checkMxl(rootDirPath + "idol/Kirameki Rider/Kirameki_Rider.mxl");
        checkMxl(rootDirPath + "idol/Kirameki Rider/Kirameki_Rider_.mxl");
        checkMxl(rootDirPath + "idol/Koyoi wa Halloween Night/Halloween_Night.mxl");
        checkMxl(rootDirPath + "idol/Koyoi wa Halloween Night/Halloween_Night_-_hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "idol/Non-Fiction/Non-Fiction.mxl");
        checkMxl(rootDirPath + "idol/Non-Fiction/Non-Fiction__hololive_English_-Myth-.mxl");
        checkMxl(rootDirPath + "idol/Plasmagic Seasons/Plasmagic_Seasons.mxl");
        checkMxl(rootDirPath + "idol/Plasmagic Seasons/Plasmagic_Seasons_-_hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "idol/Prism Melody/Prism_Melody_-_hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "idol/Shijoshugi Adtruck/_Shijoshugi_Adtruck__Hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "idol/Shijoshugi Adtruck/_Shijoshugi_Adtruck_-_hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "idol/Shiny_Smily_Story/Shiny_Smily_Story (2).mxl");
        checkMxl(rootDirPath + "idol/Shiny_Smily_Story/Shiny_Smily_Story.mxl");
        checkMxl(rootDirPath + "idol/Shiny_Smily_Story/Shiny_Smily_Story_.mxl");
        checkMxl(rootDirPath + "idol/Shiny_Smily_Story/Shiny_Smily_Story_-_Hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "idol/Shiny_Smily_Story/shiny_smily_story-hololive.mxl");
        checkMxl(rootDirPath + "idol/STARDUST SONG/STARDUST_SONG.mxl");
        checkMxl(rootDirPath + "idol/STARDUST SONG/STARDUST_SONG__hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "idol/STARDUST SONG/STARDUST_SONG_-_hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "idol/Suspect/Suspect_-_hololive_IDOL_PROJECT.mxl");
    }

    private static void checkUnit(String rootDirPath) {
        checkMxl(rootDirPath + "unit/Happiness World - BABACORN/Happiness_World.mxl");
        checkMxl(rootDirPath + "unit/PekoMiko Great War/PekoMiko_Great_War.mxl");
        checkMxl(rootDirPath + "unit/story time – Star Flower/story_time.mxl");
        checkMxl(rootDirPath + "unit/story time – Star Flower/story_time__full_ver..mxl");
        checkMxl(rootDirPath + "unit/story time – Star Flower/story_time_-_Star_Flower_Hoshimachi_Suisei_AZKi_Moona_Hoshinova_IRyS.mxl");
    }

    public static void main(String[] args) {
        try {
//            final var rootDirPath = "/Users/wilson/Downloads/mxl/";
            final var rootDirPath = "D:/Share/LoopHero/mxl/";

            check0th(rootDirPath);
            check1th(rootDirPath);
            check2th(rootDirPath);
            check3th(rootDirPath);
            check4th(rootDirPath);
            check5th(rootDirPath);
            check6th(rootDirPath);
            check7th(rootDirPath);
            checkEn1(rootDirPath);
            checkEn2(rootDirPath);
            checkEn3(rootDirPath);
            checkEn4(rootDirPath);
            checkGamers(rootDirPath);
            checkHope(rootDirPath);
            checkId1(rootDirPath);
            checkId2(rootDirPath);
            checkId3(rootDirPath);
            checkIdol(rootDirPath);
            checkUnit(rootDirPath);


            final var measures = 149;
            final var midiFilePath = rootDirPath + "/BLUE_CLAPPER__Hololive_IDOL_PROJECT-clean.mid";
//            final var midiFilePath = rootDirPath + "/BLUE_CLAPPER__Hololive_IDOL_PROJECT-split.mid";
//            final var midiFilePath = rootDirPath + "/BLUE_CLAPPER__Hololive_IDOL_PROJECT-cut.mid";
//            final var midiFilePath = rootDirPath + "/BLUE_CLAPPER__Hololive_IDOL_PROJECT-split2.mid";
//            final var mxlFilePath = rootDirPath + "/BLUE_CLAPPER__Hololive_IDOL_PROJECT-clean.mxl";
            final var mxlFilePath = rootDirPath + "5th/BLUE_CLAPPER/BLUE_CLAPPER__Hololive_IDOL_PROJECT.mxl";

//            printMidiFile(midiFilePath);
//            final var midiRows = groupMidiNoteNameByRow(midiFilePath, measures);
//            blueClapperFromMidi(midiFilePath);

//            printMxl(mxlFilePath, false);
//            printMxl(mxlFilePath, true);
//            final var mxlRows = groupMxlNoteNameByRow(mxlFilePath, measures, mxlDivisions4List.get(0));
//            diffNoteGroupByRow(midiRows, mxlRows, measures);

            // load instrument
//            final var instrumentRootDirPath = "/Users/wilson/Downloads/_instrument_txt/";
            final var instrumentRootDirPath = "D:/Share/LoopHero/8bits/_instrument_txt";
            final var nameToInstrument = FtmInstrumentUtils.loadFtmInstruments(instrumentRootDirPath);
//            checkMxl(mxlFilePath);
//            blueClapperFromMxl(mxlFilePath, nameToInstrument);

//            checkMxl(rootDirPath + "5th/La-Lion/La-Lion_A_song_for_Nene_made_for_Shishiro_Botan.mxl");// TODO fix empty drum channel
//            laLionFromMxl(rootDirPath + "5th/La-Lion/La-Lion_A_song_for_Nene_made_for_Shishiro_Botan.mxl", nameToInstrument);
//            checkMxl(rootDirPath + "idol/Shiny_Smily_Story/Shiny_Smily_Story_-_Hololive_IDOL_PROJECT.mxl");
//            shinySmilyStoryFromMxl(rootDirPath + "idol/Shiny_Smily_Story/Shiny_Smily_Story_-_Hololive_IDOL_PROJECT.mxl", nameToInstrument);
//            checkMxl(rootDirPath + "idol/Capture the Moment/Capture_the_Moment__Hololive_IDOL_PROJECT_Hololive_5th_Fes..mxl");// TODO fix grace
//            captureTheMomentFromMxl(rootDirPath + "idol/Capture the Moment/Capture_the_Moment__Hololive_IDOL_PROJECT_Hololive_5th_Fes..mxl", nameToInstrument);
//            checkMxl(rootDirPath + "idol/Capture the Moment/Capture_the_Moments.mxl");
        } catch (Exception e) {
            log.error("FamiTrackerApp", e);
        }
    }

    private FamiTrackerUtils() {}
}
