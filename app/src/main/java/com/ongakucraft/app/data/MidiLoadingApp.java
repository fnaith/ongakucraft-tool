package com.ongakucraft.app.data;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import com.ongakucraft.app.midi.MidiReader;
import com.ongakucraft.core.OcException;
import com.ongakucraft.core.midi.MidiFileReport;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MidiLoadingApp {
    private static void printMidiFile(String filePath) {
        log.info("filePath : {}", filePath);
        final var midiFile = MidiReader.read(filePath);
        log.info("msDuration : {}", midiFile.getMsDuration());
        log.info("wholeNoteTicks : {}", midiFile.getWholeNoteTicks());
        for (final var track : midiFile.getTrackList()) {
            log.info("track : {}", track.getId());
            for (final var note : track.getNoteList()) {
                log.info("note : {}", note);
            }
        }
    }

    private static void checkMidiFiles(String dirPath) {
        try {
            final var okCount = new AtomicInteger(0);
            final var containsMultipleTempoCount = new AtomicInteger(0);
            final var isNotNormalCount = new AtomicInteger(0);
            final var containsModifiedTrackCount = new AtomicInteger(0);
            final var failCount = new AtomicInteger(0);
            final var containsUnmatchedNoteOnCount = new AtomicInteger(0);
            final var containsUnmatchedNoteOffCount = new AtomicInteger(0);
            final var errorCount = new AtomicInteger(0);
            Files.walk(Paths.get(dirPath)).forEach(path -> {
                if (Files.isRegularFile(path)) {
                    final var filePath = path.toString();
                    final var fileName = path.getFileName().toString();
                    if (fileName.contains("mid") && !fileName.endsWith(".txt") && !fileName.endsWith(".dms")) {
                        try {
                            final var midiFile = MidiReader.read(filePath);
                            if (midiFile.isValid()) {
                                okCount.incrementAndGet();
                                if (midiFile.containsMultipleTempo()) {
                                    containsMultipleTempoCount.incrementAndGet();
                                }
                                final var midiFileReport = MidiFileReport.of(midiFile);
                                if (!midiFileReport.isNormal()) {
                                    isNotNormalCount.incrementAndGet();
                                }
                                if (midiFileReport.containsModifiedTrack()) {
                                    containsModifiedTrackCount.incrementAndGet();
                                }
                            } else {
//                                log.info(filePath);
                                failCount.incrementAndGet();
                                if (midiFile.containsUnmatchedNoteOn()) {
                                    containsUnmatchedNoteOnCount.incrementAndGet();
                                }
                                if (midiFile.containsUnmatchedNoteOff()) {
                                    containsUnmatchedNoteOffCount.incrementAndGet();
                                }
                            }
                        } catch (Exception ignored) {
                            errorCount.incrementAndGet();
                        }
                    }
                }
            });
            log.info("okCount : {}", okCount.get());
            log.info("containsMultipleTempoCount : {}", containsMultipleTempoCount.get());
            log.info("isNotNormalCount : {}", isNotNormalCount.get());
            log.info("containsModifiedTrackCount : {}", containsModifiedTrackCount.get());
            log.info("failCount : {}", failCount.get());
            log.info("containsUnmatchedNoteOnCount : {}", containsUnmatchedNoteOnCount.get());
            log.info("containsUnmatchedNoteOffCount : {}", containsUnmatchedNoteOffCount.get());
            log.info("errorCount : {}", errorCount.get());
        } catch (Exception e) {
            throw new OcException("checkMidiFiles fail : %s", e);
        }
    }

    public static void main(String[] args) {
        try {
//            printMidiFile("data/generated/input/Happy_Birthday/Happy_Birthday_for_Violin.mid");
//            printMidiFile("data/generated/input/Happy_Birthday/Happy_Birthday_for_Violin-align.mid");
            final var rootDirPath = "C:\\Users\\User\\Downloads\\midi";
//            checkMidiFiles(rootDirPath);
        } catch (Exception e) {
            log.error("MidiLoadingApp", e);
        }
    }

    private MidiLoadingApp() {}
}
