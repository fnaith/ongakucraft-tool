package com.ongakucraft.app.ftm;

import com.ongakucraft.core.OcException;
import com.ongakucraft.core.ftm.FtmNote;
import org.audiveris.proxymusic.*;

import java.lang.String;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class MxlMeasure {
    public MxlMeasure(String filePath, int partId, int measureId, ScorePartwise.Part.Measure measure, int divisions4) {
        parse(filePath, partId, measureId, measure, divisions4);
    }


//    private static BigDecimal getDuration(Note note) {
//        final var duration = note.getDuration();
//        if (null != duration) {
//            return duration;
//        }
//        if (null != note.getGrace()) {
//            final var type = note.getType().getValue();
//            if (null != type) {
//                return switch (type) {
//                    case "half" -> BigDecimal.valueOf(mxlDivisions4).multiply(BigDecimal.valueOf(2));
//                    case "quarter" -> BigDecimal.valueOf(mxlDivisions4);
//                    case "eighth" -> BigDecimal.valueOf(mxlDivisions4).divide(BigDecimal.valueOf(2), RoundingMode.UNNECESSARY);
//                    case "16th" -> BigDecimal.valueOf(mxlDivisions4).divide(BigDecimal.valueOf(4), RoundingMode.UNNECESSARY);
//                    default -> null;
//                };
//            }
//        }
//        return null;
//    }

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
                        case "whole", "half", "quarter", "eighth", "16th":
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

    private static void parse(String filePath, int partId, int measureId, ScorePartwise.Part.Measure measure, int divisions4) {
        check(partId, measureId, measure, divisions4);
        // TODO
        var mxlRowIndex = 0;
        for (final var noteOrBackupOrForward : measure.getNoteOrBackupOrForward()) {
            if (noteOrBackupOrForward instanceof final Backup backup) {
                final var duration = backup.getDuration().intValue();
                final var rows = duration * 4 / divisions4;
                mxlRowIndex -= rows;
            } else if (noteOrBackupOrForward instanceof final Forward forward) {
                final var duration = forward.getDuration().intValue();
                final var rows = duration * 4 / divisions4;
                mxlRowIndex += rows;
            } else if (noteOrBackupOrForward instanceof final Note note) {/* TODO
                final var staffId = null == note.getStaff() ? -1 : note.getStaff().intValue();
//                if (staffId != mxlVoiceToStaff.computeIfAbsent(note.getVoice(), k -> staffId)) {
//                    throw new OcException("part %d measure %d voice %s staff not match : %d/%d", p_m[0], p_m[1], note.getVoice(), mxlVoiceToStaff.get(note.getVoice()), staffId);
//                }
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
                                                case "tenuto" -> ftmNote.setTenuto(true);
                                                default -> throw new OcException("part %d measure %d unknown notation : %s", p_m[0], p_m[1], accentOrStrongAccentOrStaccato.getName().toString());
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
                mxlPrevRows = rows + additionRows;*/
            }
            /*else if (noteOrBackupOrForward instanceof final Direction direction) {
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
            }*/
        }
    }
}
