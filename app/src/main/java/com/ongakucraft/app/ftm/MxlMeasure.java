package com.ongakucraft.app.ftm;

import com.ongakucraft.core.OcException;
import com.ongakucraft.core.ftm.FtmNote;
import org.audiveris.proxymusic.*;

import java.lang.String;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public final class MxlMeasure {
    public MxlMeasure(String filePath, int partId, int measureId, ScorePartwise.Part.Measure measure, int divisions4) {
        parse(filePath, partId, measureId, measure, divisions4);
    }

    private static void check(int partId, int measureId, ScorePartwise.Part.Measure measure, int divisions4) {
        for (final var noteOrBackupOrForward : measure.getNoteOrBackupOrForward()) {
            if (noteOrBackupOrForward instanceof final Barline barline) {
                if (null != barline.getRepeat()) {
                    final var repeat = barline.getRepeat();
                    if (null != repeat.getAfterJump()) {
                        throw new OcException("part %d measure %d repeat after jump unsupported : %s", partId, measureId, repeat);
                    }
                    if (null != repeat.getTimes()) {
                        throw new OcException("part %d measure %d repeat times unsupported : %s", partId, measureId, repeat);
                    }
                    if (null != repeat.getWinged()) {
                        throw new OcException("part %d measure %d repeat winged unsupported : %s", partId, measureId, repeat);
                    }
                }
            } else if (noteOrBackupOrForward instanceof final Backup backup) {
                if (0 != backup.getDuration().scale()) {
                    throw new OcException("part %d measure %d backup duration should be int : %s", partId, measureId, backup.getDuration().toString());
                }
                final var duration = backup.getDuration().intValue();
                final var rows = duration * 4 / divisions4;
                if (duration != rows * divisions4 / 4) {
                    throw new OcException("part %d measure %d backup rows should be int : %d/%d", partId, measureId, duration, divisions4);
                }
            } else if (noteOrBackupOrForward instanceof final Forward forward) {
                if (0 != forward.getDuration().scale()) {
                    throw new OcException("part %d measure %d forward duration should be int : %s", partId, measureId, forward.getDuration().toString());
                }
                final var duration = forward.getDuration().intValue();
                final var rows = duration * 4 / divisions4;
                if (duration != rows * divisions4 / 4) {
                    throw new OcException("part %d measure %d forward rows should be int : %d/%d", partId, measureId, duration, divisions4);
                }
            } else if (noteOrBackupOrForward instanceof final Attributes attributes) {
                final var divisions = attributes.getDivisions();
                if (null != divisions) {
                    if (0 != divisions.scale()) {
                        throw new OcException("part %d measure %d divisions should be int : %s", partId, measureId, divisions.toString());
                    }
                }
            } else if (noteOrBackupOrForward instanceof final Note note) {
                if (null != note.getType()) {
                    switch (note.getType().getValue()) {
                        case "whole", "half", "quarter", "eighth":
                            break;
                        case "16th":
                            if (null != note.getDot() && !note.getDot().isEmpty()) {
                                throw new OcException("part %d measure %d note type is dotted 16th", partId, measureId);
                            }
                            break;
                        default:
                            throw new OcException("part %d measure %d note type unknown : %s", partId, measureId, note.getType().getValue());
                    }
                }
                if (null != note.getDuration()) {
                    if (0 != note.getDuration().scale()) {
                        throw new OcException("part %d measure %d note duration should be int : %s", partId, measureId, note.getDuration().toString());
                    }
                }
                if (null != note.getGrace()) {
                    final var grace = note.getGrace();
                    if (null != grace.getStealTimePrevious()) {
                        throw new OcException("part %d measure %d unknown grace steal time previous : %s", partId, measureId, grace);
                    }
                    if (null != grace.getStealTimeFollowing()) {
                        throw new OcException("part %d measure %d unknown grace steal time following : %s", partId, measureId, grace);
                    }
                    if (null != grace.getMakeTime()) {
                        throw new OcException("part %d measure %d unknown grace make time : %s", partId, measureId, grace);
                    }
                }
            }
        }
    }

    private static int getDurationByType(int partId, int measureId, NoteType noteType, int divisions4) {
        return switch (noteType.getValue()) {
            case "whole" -> BigDecimal.valueOf(divisions4).intValue();
            case "half" -> BigDecimal.valueOf(divisions4).multiply(BigDecimal.valueOf(2)).intValue();
            case "quarter" -> BigDecimal.valueOf(divisions4).intValue();
            case "eighth" -> BigDecimal.valueOf(divisions4).divide(BigDecimal.valueOf(2), RoundingMode.UNNECESSARY).intValue();
            case "16th" -> BigDecimal.valueOf(divisions4).divide(BigDecimal.valueOf(4), RoundingMode.UNNECESSARY).intValue();
            default -> throw new OcException("part %d measure %d unknown note type : %s", partId, measureId);
        };
    }

    private static int getDuration(int partId, int measureId, Note note, int divisions4) {
        if (null != note.getDuration()) {
            return note.getDuration().intValue();
        }
        final var noteType = note.getType();
        if (null != noteType) {
            return getDurationByType(partId, measureId, noteType, divisions4);
        }
        throw new OcException("part %d measure %d unknown note duration : %s", partId, measureId);
    }

    private static String pitchToName(int partId, int measureId, Note note) {
        final var pitch = note.getPitch();
        if (null == pitch) {
            final var unpitched = note.getUnpitched();
            if (null == unpitched) {
                return null;
            }
            return unpitched.getDisplayStep().name() + '-' + unpitched.getDisplayOctave();
        }
        var step = pitch.getStep().name();
        var octave = pitch.getOctave();
        final var alter = null == pitch.getAlter() ? 0 : pitch.getAlter().intValue();
        var semitone = switch (step) {
            case "C" -> 0;
            case "D" -> 2;
            case "E" -> 4;
            case "F" -> 5;
            case "G" -> 7;
            case "A" -> 9;
            case "B" -> 11;
            default -> throw new OcException("part %d measure %d note step not unsupported : %s", partId, measureId, step);
        };
        semitone += alter;
        while (semitone < 0) {
            semitone += 12;
            --octave;
        }
        while (11 < semitone) {
            semitone -= 12;
            ++octave;
        }
        step = "C-C#D-D#E-F-F#G-G#A-A#B-".substring(semitone * 2, semitone * 2 + 2);
        return step + octave;
    }

    private static void parse(String filePath, int partId, int measureId, ScorePartwise.Part.Measure measure, int divisions4) {
        check(partId, measureId, measure, divisions4);
        // TODO
        var mxlRowIndex = 0;
        var mxlPrevRows = 0;
        final Map<String, Integer> mxlVoiceToTuplet = new HashMap<>();
        for (final var noteOrBackupOrForward : measure.getNoteOrBackupOrForward()) {
            if (noteOrBackupOrForward instanceof final Backup backup) {
                final var duration = backup.getDuration().intValue();
                final var rows = duration * 4 / divisions4;
                mxlRowIndex -= rows;
            } else if (noteOrBackupOrForward instanceof final Forward forward) {
                final var duration = forward.getDuration().intValue();
                final var rows = duration * 4 / divisions4;
                mxlRowIndex += rows;
            } else if (noteOrBackupOrForward instanceof final Note note) {
                final var staffId = null == note.getStaff() ? -1 : note.getStaff().intValue();
//                if (staffId != mxlVoiceToStaff.computeIfAbsent(note.getVoice(), k -> staffId)) {
//                    throw new OcException("part %d measure %d voice %s staff not match : %d/%d", partId, measureId, note.getVoice(), mxlVoiceToStaff.get(note.getVoice()), staffId);
//                }
                final var isChord = null != note.getChord();
                if (!isChord) {
                    mxlRowIndex += mxlPrevRows;
                }
                final var duration = getDuration(partId, measureId, note, divisions4);
                if (duration * 5 == divisions4) {
                    throw new OcException("part %d measure %d note is tuplet 5 : %d/%d", partId, measureId, duration, divisions4);
                }
                if (6 < duration && duration == divisions4 * 2 / 7) {
                    throw new OcException("part %d measure %d note is tuplet 7 : %d/%d", partId, measureId, duration, divisions4);
                }
                final var durationByType = null == note.getType() ? -1 : getDurationByType(partId, measureId, note.getType(), divisions4);
                final var rows = duration * 4 / divisions4;
                var additionRows = 0;
                final var isTriplet  = duration * 3 == durationByType * 2;
                if (!isTriplet) {
                    if (duration != rows * divisions4 / 4) {
                        throw new OcException("part %d measure %d note rows should be int : %d/%d", partId, measureId, duration, divisions4);
                    }
                }
//                final var mxlChannel = findMxlChannel(p_m, note.getVoice(), rows, isChord); TODO
                final var pitchName = pitchToName(partId, measureId, note);
                if (null == pitchName) {
                    for (int i = 0; i < rows; ++i) {
//                        mxlChannel.set(mxlRowIndex + i, FtmNote.REST); TODO
                    }
                } else {
                    if (isChord) {
//                        final var ftmNote = mxlChannel.get(mxlRowIndex); TODO
//                        ftmNote.addChord(pitchName); TODO
                    } else {
                        final var ftmNote = MxlNote.of(pitchName);
//                        ftmNote.setPedal(mxlStaffToPedal.getOrDefault(staffId, false)); TODO
                        final var tuplet = mxlVoiceToTuplet.getOrDefault(note.getVoice(), 0);/* TODO
                        for (final var notation : note.getNotations()) {
                            for (final var tiedOrSlurOrTuplet : notation.getTiedOrSlurOrTuplet()) {
                                if (tiedOrSlurOrTuplet instanceof final Articulations articulations) {
                                    for (final var accentOrStrongAccentOrStaccato : articulations.getAccentOrStrongAccentOrStaccato()) {
                                        switch (accentOrStrongAccentOrStaccato.getName().toString()) {
                                            case "accent" -> ftmNote.setAccent(true);
                                            case "staccato" -> ftmNote.setStaccato(true);
                                            case "tenuto" -> ftmNote.setTenuto(true);
                                            case "strong-accent" -> ftmNote.setTenuto(true);// TODO
                                            case "detached-legato" -> ftmNote.setTenuto(true);// TODO
                                            case "staccatissimo" -> ftmNote.setTenuto(true);// TODO
                                            case "falloff" -> ftmNote.setTenuto(true);// TODO
                                            case "caesura" -> ftmNote.setTenuto(true);// TODO
                                            case "doit" -> ftmNote.setTenuto(true);// TODO
                                            case "plop" -> ftmNote.setTenuto(true);// TODO
                                            default -> throw new OcException("part %d measure %d unknown notation : %s", partId, measureId, accentOrStrongAccentOrStaccato.getName().toString());
                                        }
                                    }
                                } else if (tiedOrSlurOrTuplet instanceof Arpeggiate) {
                                    ftmNote.setArpeggiate(true);
                                } else if (tiedOrSlurOrTuplet instanceof Tied) {
                                } else if (tiedOrSlurOrTuplet instanceof Tuplet) {
                                } else if (tiedOrSlurOrTuplet instanceof final Slur slur) {
                                    // TODO start / stop
                                } else if (tiedOrSlurOrTuplet instanceof final Fermata fermata) {
                                    // TODO
                                } else if (tiedOrSlurOrTuplet instanceof final Ornaments ornaments) {
                                    // TODO
                                } else if (tiedOrSlurOrTuplet instanceof final Slide slide) {
                                    // TODO
                                } else if (tiedOrSlurOrTuplet instanceof final Glissando glissando) {
                                    // TODO
                                } else if (tiedOrSlurOrTuplet instanceof final Technical technical) {
                                    // TODO
                                } else if (tiedOrSlurOrTuplet instanceof final NonArpeggiate nonArpeggiate) {
                                    // TODO
                                } else if (tiedOrSlurOrTuplet instanceof final OtherNotation otherNotation) {
                                    // TODO
                                } else {
                                    throw new OcException("part %d measure %d unknown notation : %s", partId, measureId, tiedOrSlurOrTuplet);
                                }
                            }
                        }
                        ftmNote.setTuplet(tuplet);*/
//                        fillNote(mxlChannel, mxlRowIndex, mxlRowIndex + rows, ftmNote); TODO
                    }
                    if (isTriplet) {
                        mxlVoiceToTuplet.merge(note.getVoice(), 1, Integer::sum);
                        if (3 == mxlVoiceToTuplet.get(note.getVoice())) {
                            mxlVoiceToTuplet.remove(note.getVoice());
                            additionRows += 1;
                        }
                    }
                }
                mxlPrevRows = rows + additionRows;
            }
            /*else if (noteOrBackupOrForward instanceof final Direction direction) { TODO
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
                        throw new OcException("part %d measure %d unknown direction : %s", partId, measureId, type);
                    }
                }
            }*/
        }
    }
}
