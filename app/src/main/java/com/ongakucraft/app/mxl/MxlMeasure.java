package com.ongakucraft.app.mxl;

import com.ongakucraft.core.OcException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.audiveris.proxymusic.*;

import java.lang.String;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Getter
public final class MxlMeasure {
    final int beatSize;
    final Map<String, List<MxlNote>> voiceToMxlNotes;

    public MxlMeasure(String filePath, int partId, int measureId, ScorePartwise.Part.Measure measure, int divisions4) {
        this(filePath, partId, measureId, measure, divisions4, checkBeatSize(partId, measureId, measure, divisions4));
    }

    private MxlMeasure(String filePath, int partId, int measureId, ScorePartwise.Part.Measure measure, int divisions4, int beatSize) {
        this(beatSize, parse(filePath, partId, measureId, measure, divisions4, beatSize));
    }

    private MxlMeasure(int beatSize, Map<String, List<MxlNote>> voiceToMxlNotes) {
        this.beatSize = beatSize;
        this.voiceToMxlNotes = Collections.unmodifiableMap(voiceToMxlNotes);
    }

    public boolean hasVoice(String voice) {
        if (!voiceToMxlNotes.containsKey(voice)) {
            return false;
        }
        final var mxlNotes = voiceToMxlNotes.get(voice);
        return !mxlNotes.stream().allMatch(mxlNote -> null == mxlNote || mxlNote.isRest());
    }

    public MxlMeasure removeVoice(String voice) {
        if (!voiceToMxlNotes.containsKey(voice)) {
            return this;
        }
        final Map<String, List<MxlNote>> newVoiceToMxlNotes = new HashMap<>(voiceToMxlNotes);
        newVoiceToMxlNotes.remove(voice);
        return new MxlMeasure(beatSize, newVoiceToMxlNotes);
    }

    public boolean isEmpty() {
        return voiceToMxlNotes.isEmpty();
    }

    private static void check(String filePath, int partId, int measureId, ScorePartwise.Part.Measure measure, int divisions4) {
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
                            for (final var notation : note.getNotations()) {
                                for (final var tiedOrSlurOrTuplet : notation.getTiedOrSlurOrTuplet()) {
                                    if (tiedOrSlurOrTuplet instanceof Tuplet) {
                                        throw new OcException("part %d measure %d note type is tuplet 16th", partId, measureId);
                                    }
                                }
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
                        throw new OcException("part %d measure %d grace steal time previous : %s", partId, measureId, grace);
                    }
                    if (null != grace.getStealTimeFollowing()) {
                        throw new OcException("part %d measure %d grace steal time following : %s", partId, measureId, grace);
                    }
                    if (null != grace.getMakeTime()) {
                        throw new OcException("part %d measure %d grace make time : %s", partId, measureId, grace);
                    }
                    if (null != grace.getMakeTime()) {
                        if (YesNo.YES == grace.getSlash()) {
                            log.warn("grace slash acciaccatura : {} {} {}", filePath, partId, measureId);
                            continue;
                        }
                    }
                    log.warn("grace : {} {} {}", filePath, partId, measureId);
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

    private static List<MxlNote> newMxlNotes(int beatSize) {
        final List<MxlNote> mxlNotes = new ArrayList<>();
        for (int beat = 0; beat < beatSize; ++beat) {
            mxlNotes.add(null);
        }
        return mxlNotes;
    }

    private static void fillNotes(int partId, int measureId, String voice,
                                  List<MxlNote> mxlNotes, int startBeat, int beats, boolean isChord, MxlNote note) {
        final var endBeat = startBeat + beats;
        if (isChord) {
            final var mxlNote = mxlNotes.get(startBeat);
            for (int beat = startBeat; beat < endBeat; ++beat) {
                final var existNote = mxlNotes.get(beat);
                if (null == existNote) {
                    throw new OcException("part %d measure %d voice %s beat %d should be exist", partId, measureId, voice, startBeat);
                }
                if (mxlNote != mxlNotes.get(beat)) {
                    throw new OcException("part %d measure %d voice %s beat %d should be same", partId, measureId, voice, startBeat);
                }
            }
            note = mxlNote.addChord(note.getKey());
        } else {
            for (int beat = startBeat; beat < endBeat; ++beat) {
                if (beat < 0) {
                    throw new OcException("part %d measure %d voice %s beat %d should >= 0", partId, measureId, voice, startBeat);
                }
                if (mxlNotes.size() <= beat) {
                    throw new OcException("part %d measure %d voice %s beat %d should < 16", partId, measureId, voice, startBeat);
                }
                if (null != mxlNotes.get(beat)) {
                    throw new OcException("part %d measure %d voice %s beat %d should be empty", partId, measureId, voice, startBeat);
                }
            }
        }
        for (int beat = startBeat; beat < endBeat; ++beat) {
            mxlNotes.set(beat, note);
        }
    }

    private static int checkBeatSize(int partId, int measureId, ScorePartwise.Part.Measure measure, int divisions4) {
        var startBeat = 0;
        var tripletDurations = 0;
        var tripletCount = 0;
        for (final var noteOrBackupOrForward : measure.getNoteOrBackupOrForward()) {
            if (noteOrBackupOrForward instanceof final Backup backup) {
                break;
            } else if (noteOrBackupOrForward instanceof final Forward forward) {
                final var duration = forward.getDuration().intValue();
                final var beats = duration * 4 / divisions4;
                startBeat += beats;
            } else if (noteOrBackupOrForward instanceof final Note note) {
                if (null != note.getGrace()) {
                    continue;
                }

                final var duration = getDuration(partId, measureId, note, divisions4);
                final var durationByType = null == note.getType() ? -1 : getDurationByType(partId, measureId, note.getType(), divisions4);

                if (duration * 5 == divisions4) {
                    throw new OcException("part %d measure %d note is tuplet 5 : %d/%d", partId, measureId, duration, divisions4);
                }
                if (6 < duration && duration == divisions4 * 2 / 7) {
                    throw new OcException("part %d measure %d note is tuplet 7 : %d/%d", partId, measureId, duration, divisions4);
                }

                final var isChord = null != note.getChord();
                if (isChord) {
                    continue;
                }

                var beats = duration * 4 / divisions4;

                final var isTriplet = duration * 3 == durationByType * 4 / 2;
                if (!isTriplet) {
                    if (duration != beats * divisions4 / 4) {
                        throw new OcException("part %d measure %d note beats should be int : %d/%d", partId, measureId, duration, divisions4);
                    }
                }
                if (0 < tripletDurations) {
                    final var additionBeat = tripletDurations * 4 / divisions4;
                    startBeat += additionBeat;
                    tripletDurations -= additionBeat * divisions4 / 4;
                }
                final var tripletDelay = tripletDurations * 4 * 3 / divisions4;
                if (2 < tripletDelay) {
                    throw new OcException("part %d measure %d note triplet delay should be [0..2] : %d", partId, measureId, tripletDelay);
                }

                if (0 == tripletCount % 3) {
                    startBeat += (tripletDurations * 4 / divisions4);
                    tripletDurations = 0;
                }
                if (isTriplet) {
                    ++tripletCount;
                    beats = durationByType * 4 / 2 / divisions4;
                    tripletDurations += (duration - durationByType / 2);
                }

                startBeat += beats;
            }
        }

        if (0 == tripletCount % 3) {
            startBeat += (tripletDurations * 4 / divisions4);
        }

        return startBeat;
    }

    private static boolean isTieStop(Note note) {
        return note.getTie().stream().anyMatch(tie -> StartStop.STOP == tie.getType());
    }

    private static Map<String, List<MxlNote>> parse(String filePath, int partId, int measureId, ScorePartwise.Part.Measure measure,
                                                    int divisions4, int beatSize) {
        check(filePath, partId, measureId, measure, divisions4);
        var startBeat = 0;
        var prevStartBeat = 0;
        var prveBeats = 0;
        var tripletDurations = 0;
        var tripletCount = 0;
        final Map<String, List<MxlNote>> voiceToMxlNotes = new HashMap<>();
        for (final var noteOrBackupOrForward : measure.getNoteOrBackupOrForward()) {
            if (noteOrBackupOrForward instanceof final Backup backup) {
                final var duration = backup.getDuration().intValue();
                final var beats = duration * 4 / divisions4;
                startBeat -= beats;
            } else if (noteOrBackupOrForward instanceof final Forward forward) {
                final var duration = forward.getDuration().intValue();
                final var beats = duration * 4 / divisions4;
                startBeat += beats;
            } else if (noteOrBackupOrForward instanceof final Note note) {
                if (null != note.getGrace()) {
                    continue;
                }

                final var duration = getDuration(partId, measureId, note, divisions4);
                final var durationByType = null == note.getType() ? -1 : getDurationByType(partId, measureId, note.getType(), divisions4);

                final var pitchName = pitchToName(partId, measureId, note);
                final var mxlNotes = voiceToMxlNotes.computeIfAbsent(note.getVoice(), k -> newMxlNotes(beatSize));

                final var isChord = null != note.getChord();
                if (isChord) {
                    final var mxlNote = MxlNote.of(pitchName);
                    fillNotes(partId, measureId, note.getVoice(), mxlNotes, prevStartBeat, prveBeats, isChord, mxlNote);
                    continue;
                }

                var beats = duration * 4 / divisions4;

                final var isTriplet = duration * 3 == durationByType * 4 / 2;
                if (0 < tripletDurations) {
                    final var additionBeat = tripletDurations * 4 / divisions4;
                    startBeat += additionBeat;
                    tripletDurations -= additionBeat * divisions4 / 4;
                }
                final var tripletDelay = tripletDurations * 4 * 3 / divisions4;
                if (isTriplet) {
                    ++tripletCount;
                    beats = durationByType * 4 / 2 / divisions4;
                    tripletDurations += (duration - durationByType / 2);
                }

                if (null == pitchName) {
                    fillNotes(partId, measureId, note.getVoice(), mxlNotes, startBeat, beats, isChord, MxlNote.REST);
                } else {
                    var mxlNote = MxlNote.of(pitchName);
                    mxlNote = mxlNote.withTripletDelay(tripletDelay);
                    if (isTieStop(note)) {
                        mxlNote = mxlNote.withTieStop(isTieStop(note));
                    }
                    mxlNote = mxlNote.withTieStop(isTieStop(note));
                    for (final var notation : note.getNotations()) {
                        for (final var tiedOrSlurOrTuplet : notation.getTiedOrSlurOrTuplet()) {
                            if (tiedOrSlurOrTuplet instanceof final Articulations articulations) {
                                for (final var accentOrStrongAccentOrStaccato : articulations.getAccentOrStrongAccentOrStaccato()) {
                                    switch (accentOrStrongAccentOrStaccato.getName().toString()) {
                                        case "accent" -> mxlNote = mxlNote.withAccent(true);
                                        case "caesura" -> mxlNote = mxlNote.withCaesura(true);
                                        case "detached-legato" -> mxlNote = mxlNote.withDetachedLegato(true);
                                        case "doit" -> mxlNote = mxlNote.withDoit(true);
                                        case "falloff" -> mxlNote = mxlNote.withFalloff(true);
                                        case "plop" -> mxlNote = mxlNote.withPlop(true);
                                        case "staccatissimo" -> mxlNote = mxlNote.withStaccatissimo(true);
                                        case "staccato" -> mxlNote = mxlNote.withStaccato(true);
                                        case "strong-accent" -> mxlNote = mxlNote.withStrongAccent(true);
                                        case "tenuto" -> mxlNote = mxlNote.withTenuto(true);
                                        default -> throw new OcException("part %d measure %d unknown notation : %s", partId, measureId, accentOrStrongAccentOrStaccato.getName().toString());
                                    }
                                }
                            } else if (tiedOrSlurOrTuplet instanceof Arpeggiate) {
                                mxlNote = mxlNote.withArpeggiate(true);
                            } else if (tiedOrSlurOrTuplet instanceof Fermata) {
                                mxlNote = mxlNote.withFermata(true);
                            } else if (tiedOrSlurOrTuplet instanceof Glissando) {
                                mxlNote = mxlNote.withGlissando(true);
                            } else if (tiedOrSlurOrTuplet instanceof NonArpeggiate) {
                                mxlNote = mxlNote.withNonArpeggiate(true);
                            } else if (tiedOrSlurOrTuplet instanceof Ornaments) {
                                mxlNote = mxlNote.withOrnaments(true);
                            } else if (tiedOrSlurOrTuplet instanceof OtherNotation) {
                                mxlNote = mxlNote.withOtherNotation(true);
                            } else if (tiedOrSlurOrTuplet instanceof Slur) {
                                mxlNote = mxlNote.withSlur(true);
                            } else if (tiedOrSlurOrTuplet instanceof Slide) {
                                mxlNote = mxlNote.withSlide(true);
                            } else if (tiedOrSlurOrTuplet instanceof Tied) { // pass
                            } else if (tiedOrSlurOrTuplet instanceof Tuplet) { // pass
                            } else if (tiedOrSlurOrTuplet instanceof Technical) {
                                mxlNote = mxlNote.withTechnical(true);
                            } else {
                                throw new OcException("part %d measure %d unknown notation : %s", partId, measureId, tiedOrSlurOrTuplet);
                            }
                        }
                    }
                    fillNotes(partId, measureId, note.getVoice(), mxlNotes, startBeat, beats, isChord, mxlNote);
                }
                if (!isChord) {
                    prevStartBeat = startBeat;
                    prveBeats = beats;
                }
                startBeat += beats;
            } else if (noteOrBackupOrForward instanceof final Direction direction) {
                for (final var directionType : direction.getDirectionType()) {
                    var foundType = false;
                    if (null != directionType.getDynamics() && !directionType.getDynamics().isEmpty()) {
                        foundType = true;
                    }
                    if (null != directionType.getMetronome()) {
                        foundType = true;
                    }
                    if (null != directionType.getRehearsal()) {
                        foundType = true;
                    }
                    if (null != directionType.getPedal()) { // TODO
//                        final var staffId = null == direction.getStaff() ? -1 : direction.getStaff().intValue();
//                        var pedal = staffToPedal.getOrDefault(staffId, false);
//                        if (PedalType.START == directionType.getPedal().getType()) {
//                            if (pedal) {
//                                throw new OcException("part %d measure %d pedal should stopped : %b", partId, measureId, pedal);
//                            }
//                            staffToPedal.put(staffId, true);
//                        }
//                        if (PedalType.STOP == directionType.getPedal().getType()) {
//                            if (!pedal) {
//                                throw new OcException("part %d measure %d pedal should started : %b", partId, measureId, pedal);
//                            }
//                            staffToPedal.put(staffId, false);
//                        }
                        foundType = true;
                    }
                    if (null != directionType.getWedge()) { // TODO staff : crescendo,diminuendo/stop
                        foundType = true;
                    }
                    if (!foundType) {
                        throw new OcException("part %d measure %d unknown direction type : %s", partId, measureId, directionType);
                    }
                }
            }
        }
        if (0 == tripletCount % 3) {
            startBeat += (tripletDurations * 4 / divisions4);
        }
        if (beatSize != startBeat) {
            throw new OcException("part %d measure %d duration not filled", partId, measureId);
        }
        return voiceToMxlNotes;
    }
}
