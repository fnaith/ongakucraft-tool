package com.ongakucraft.app.data;

import com.ongakucraft.app.midi.MidiReader;
import com.ongakucraft.core.OcException;
import com.ongakucraft.core.ftm.FtmChannel;
import com.ongakucraft.core.ftm.FtmEffect;
import com.ongakucraft.core.ftm.FtmNote;
import com.ongakucraft.core.ftm.FtmSong;
import com.ongakucraft.core.midi.MidiNote;
import lombok.extern.slf4j.Slf4j;
import org.audiveris.proxymusic.*;
import org.audiveris.proxymusic.mxl.Mxl;
import org.audiveris.proxymusic.util.Marshalling;

import java.io.File;
import java.lang.String;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public final class FamiTrackerApp {
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
                    throw new OcException("note on should be int : %s/%d", note.getOn(), ticksPerRow);
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
        final var song = FtmSong.of(146, channelList);
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

    private static String pitchToName(Pitch pitch) {
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
        final var name = pitchToName(note.getPitch());
        if (name.equals(FtmNote.keyToName(ftmNote.getKey()))) {
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
            parseMxl(scorePartwise, FamiTrackerApp::printMxlScore, FamiTrackerApp::printMxlPart, FamiTrackerApp::printMxlMeasure);
        } else {
            parseMxl(scorePartwise, FamiTrackerApp::printMxlScore, FamiTrackerApp::printMxlPart, null);
        }
    }

    private static List<Integer> mxlDivisions16List;
    private static List<Integer> mxlMeasureSizeList;

    private static void checkMxlScore(ScorePartwise scorePartwise) {
        mxlDivisions16List = new ArrayList<>();
        mxlMeasureSizeList = new ArrayList<>();
    }

    private static void checkMxlPart(int p, ScorePartwise.Part part) {
        if (!mxlMeasureSizeList.contains(part.getMeasure().size())) {
            mxlMeasureSizeList.add(part.getMeasure().size());
        }
        if (1 != mxlMeasureSizeList.size()) {
            throw new OcException("part %d measure size should be unique : %s", p, mxlMeasureSizeList.toString());
        }
    }

    private static void checkMxlMeasure(int[] p_m, ScorePartwise.Part.Measure measure) {
        for (final var noteOrBackupOrForward : measure.getNoteOrBackupOrForward()) {
            if (noteOrBackupOrForward instanceof final Barline barline) {
                if (null != barline.getRepeat()) {
                    throw new OcException("part %d measure %d repeat unsupported : %s", p_m[0], p_m[1]);
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
                    final var divisions16 = divisions4 / 4;
                    if (divisions4 != divisions16 * 4) {
                        throw new OcException("part %d measure %d divisions16 should be int : %d/4", p_m[0], p_m[1], divisions4);
                    }
                    if (!mxlDivisions16List.contains(divisions16)) {
                        mxlDivisions16List.add(divisions16);
                    }
                    if (1 != mxlDivisions16List.size()) {
                        throw new OcException("part %d measure %d divisions16 should be unique : %s", p_m[0], p_m[1], mxlDivisions16List.toString());
                    }
                }
            } else if (noteOrBackupOrForward instanceof final Note note) {
                if (0 != note.getDuration().scale()) {
                    throw new OcException("part %d measure %d note duration should be int : %s", p_m[0], p_m[1], note.getDuration().toString());
                }
            }
        }
    }

    private static void checkMxl(String filePath) {
        final var scorePartwise = loadMxl(filePath);
        parseMxl(scorePartwise, FamiTrackerApp::checkMxlScore, FamiTrackerApp::checkMxlPart, FamiTrackerApp::checkMxlMeasure);
//        log.info("mxlDivisions16List : {}", mxlDivisions16List);
//        log.info("mxlMeasureSizeList : {}", mxlMeasureSizeList);
    }

    public static List<List<String>> groupMxlNoteNameByRow(String filePath, int measures, int divisions16) {
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
                        rowIndex -= duration / divisions16;
                    } else if (noteOrBackupOrForward instanceof final Forward forward) {
                        final var duration = forward.getDuration().intValue();
                        rowIndex += duration / divisions16;
                    } else if (noteOrBackupOrForward instanceof final Note note) {
                        final var isChord = null != note.getChord();
                        if (!isChord) {
                            rowIndex += prevRows;
                        }
                        final var duration = note.getDuration().intValue();
                        if (0 != duration % divisions16) {
                            throw new OcException("part %d measure %d note rows should be int : %s/%d", p, m, duration, mxlDivisions16);
                        }
                        if (!isTieStop(note)) {
                            final var pitch = note.getPitch();
                            if (null != pitch) {
                                rows.get(rowIndex).add(pitchToName(pitch));
                            }
                        }
                        prevRows = duration / divisions16;
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
    private static int mxlDivisions16;
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
        mxlDivisions16 = mxlDivisions16List.get(0);
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
                final var rows = duration / mxlDivisions16;
                if (duration != rows * mxlDivisions16) {
                    throw new OcException("part %d measure %d backup rows should be int : %s/%d", p_m[0], p_m[1], duration, mxlDivisions16);
                }
                mxlRowIndex -= rows;
            } else if (noteOrBackupOrForward instanceof final Forward forward) {
                final var duration = forward.getDuration().intValue();
                final var rows = duration / mxlDivisions16;
                if (duration != rows * mxlDivisions16) {
                    throw new OcException("part %d measure %d forward rows should be int : %s/%d", p_m[0], p_m[1], duration, mxlDivisions16);
                }
                mxlRowIndex += rows;
            } else if (noteOrBackupOrForward instanceof final Note note) {
                if (note.getStaff().intValue() != mxlVoiceToStaff.computeIfAbsent(note.getVoice(), k -> note.getStaff().intValue())) {
                    throw new OcException("part %d measure %d voice %s staff not match : %d/%d", p_m[0], p_m[1], note.getVoice(), mxlVoiceToStaff.get(note.getVoice()), note.getStaff().intValue());
                }
                final var isChord = null != note.getChord();
                if (!isChord) {
                    mxlRowIndex += mxlPrevRows;
                }
                final var duration = note.getDuration().intValue();
                final var rows = duration / mxlDivisions16;
                var additionRows = 0;
                var isTuplet = false;
                if (duration != rows * mxlDivisions16) {
                    if (mxlDivisions16 * 4 == duration * 3) {
                        isTuplet = true;
                    }
                    if (!isTuplet) {
                        throw new OcException("part %d measure %d note rows should be int : %s/%d", p_m[0], p_m[1], duration, mxlDivisions16);
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
                    if (!pitchToName(note.getPitch()).equals(pitchToName(tieStartNote.getPitch()))) {
                        throw new OcException("part %d measure %d can't match tie start pitch : %s", p_m[0], p_m[1], pitchToName(tieStartNote.getPitch()), pitchToName(note.getPitch()));
                    }
                    final var ftmNote = mxlChannel.get(mxlRowIndex - 1);
                    if (!isSamePitch(ftmNote, tieStartNote)) {
                        throw new OcException("part %d measure %d can't match tie start key : %s %s", p_m[0], p_m[1], pitchToName(tieStartNote.getPitch()), FtmNote.keyToName(ftmNote.getKey()));
                    }
                    fillNote(mxlChannel, mxlRowIndex, mxlRowIndex + rows, ftmNote);
                } else {
                    final var pitch = note.getPitch();
                    if (null == pitch) {
                        for (int i = 0; i < rows; ++i) {
                            mxlChannel.set(mxlRowIndex + i, FtmNote.REST);
                        }
                    } else {
                        if (isChord) {
                            final var ftmNote = mxlChannel.get(mxlRowIndex);
                            ftmNote.addChord(pitchToName(pitch));
                        } else {
                            final var ftmNote = FtmNote.of(pitchToName(pitch));
                            ftmNote.setPedal(mxlStaffToPedal.getOrDefault(note.getStaff().intValue(), false));
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
                                        // TODO start / stpo
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
    private static void blueClapperFromMxl(String filePath) {
        final var scorePartwise = loadMxl(filePath);
        parseMxl(scorePartwise, FamiTrackerApp::processMxlScore, FamiTrackerApp::processMxlPart, FamiTrackerApp::processMxlMeasure);
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
                            case 1:
                                ftmNote.setEffect(arpeggioIdx, FtmEffect.arpeggio(ftmNote.getChord().get(0) - ftmNote.getKey(), 0));
                                break;
                            case 2:
                                ftmNote.setEffect(arpeggioIdx, FtmEffect.arpeggio(ftmNote.getChord().get(0) - ftmNote.getKey(), ftmNote.getChord().get(1) - ftmNote.getKey()));
                                break;
                            case 3:
                                ftmNote.setEffect(arpeggioIdx, FtmEffect.arpeggio(ftmNote.getChord().get(0) - ftmNote.getKey(), ftmNote.getChord().get(2) - ftmNote.getKey()));
                                break;
                            default:
                                throw new OcException("row %d chord too many : %d %d", row, ftmNote.getKey(), ftmNote.getChord());
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
        for (final var mxlChannelList : mxlPartToVoiceToChannel.values()) {
            final var channel1 = mxlChannelList.get("1");
            final var channel2 = mxlChannelList.get("2");
            final var channel3 = mxlChannelList.get("3");
            final var channel5 = mxlChannelList.get("5");
            final var channel6 = mxlChannelList.get("6");
            setChannel(channel1, 4, 8);
            setChannel(channel2, 4, 8);
            setChannel(channel3, 7, 7);
            setChannel(channel5, 11, 6);
            setChannel(channel6, 11, 6);
            final List<FtmChannel> channelList = new ArrayList<>();
            channelList.add(FtmChannel.of(channel1));
            channelList.add(FtmChannel.of(channel2));
            channelList.add(FtmChannel.of(channel3));
            channelList.add(null);
            channelList.add(null);
            channelList.add(FtmChannel.of(channel5));
            channelList.add(FtmChannel.of(channel6));
            channelList.add(null);
            final var ftmSong = FtmSong.of(146, channelList);
//            log.info("channelList : {}", channelList.size());
            log.info("{}", ftmSong);
        }
    }

    public static void main(String[] args) {
        try {
//            final var rootDirPath = "/Users/wilson/Downloads";
            final var rootDirPath = "D:/Sync/Ongakucraft/midi/blue clapper";
            final var measures = 149;
            final var midiFilePath = rootDirPath + "/BLUE_CLAPPER__Hololive_IDOL_PROJECT-clean.mid";
//            final var midiFilePath = rootDirPath + "/BLUE_CLAPPER__Hololive_IDOL_PROJECT-split.mid";
//            final var midiFilePath = rootDirPath + "/BLUE_CLAPPER__Hololive_IDOL_PROJECT-cut.mid";
//            final var midiFilePath = rootDirPath + "/BLUE_CLAPPER__Hololive_IDOL_PROJECT-split2.mid";
//            final var mxlFilePath = rootDirPath + "/BLUE_CLAPPER__Hololive_IDOL_PROJECT-clean.mxl";
            final var mxlFilePath = rootDirPath + "/BLUE_CLAPPER__Hololive_IDOL_PROJECT.mxl";

//            printMidiFile(midiFilePath);
//            final var midiRows = groupMidiNoteNameByRow(midiFilePath, measures);
//            blueClapperFromMidi(midiFilePath);

//            printMxl(mxlFilePath, false);
//            printMxl(mxlFilePath, true);
            checkMxl(mxlFilePath);
//            final var mxlRows = groupMxlNoteNameByRow(mxlFilePath, measures, mxlDivisions16List.get(0));
//            diffNoteGroupByRow(midiRows, mxlRows, measures);

            blueClapperFromMxl(mxlFilePath);
        } catch (Exception e) {
            log.error("FamiTrackerApp", e);
        }
    }

    private FamiTrackerApp() {}
}
