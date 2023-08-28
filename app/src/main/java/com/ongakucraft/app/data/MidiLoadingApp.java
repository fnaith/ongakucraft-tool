package com.ongakucraft.app.data;

import com.ongakucraft.app.midi.MidiReader;
import com.ongakucraft.app.nbt.CircuitUtils;
import com.ongakucraft.app.nbt.NbtWriter;
import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.BlockDatasetVersion;
import com.ongakucraft.core.circuit.KeyRange;
import com.ongakucraft.core.midi.MidiFileReport;
import com.ongakucraft.core.midi.MidiTrackReport;
import com.ongakucraft.core.music.Music16;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public final class MidiLoadingApp {
    private static final BlockDatasetVersion VERSION = BlockDatasetVersion.of("1.18.2", 2975);

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
            final var isEmptyCount = new AtomicInteger(0);
            final var containsModifiedTrackCount = new AtomicInteger(0);
            final var failCount = new AtomicInteger(0);
            final var containsUnmatchedNoteOnCount = new AtomicInteger(0);
            final var containsUnmatchedNoteOffCount = new AtomicInteger(0);
            final var errorCount = new AtomicInteger(0);
            final var midiFilePathList = findMidiFilePathList(dirPath);
            for (final var filePath : midiFilePathList) {
                try {
                    final var midiFile = MidiReader.read(filePath);
                    if (midiFile.isValid()) {
                        okCount.incrementAndGet();
                        if (midiFile.containsMultipleTempo()) {
                            containsMultipleTempoCount.incrementAndGet();
                        }
                        final var midiFileReport = MidiFileReport.of(midiFile);
                        if (!midiFileReport.isEmpty()) {
                            isEmptyCount.incrementAndGet();
                        }
                        if (midiFileReport.containsModifiedTrack()) {
                            containsModifiedTrackCount.incrementAndGet();
                        }
                    } else {
//                        log.info(filePath);
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
            log.info("okCount : {}", okCount.get());
            log.info("containsMultipleTempoCount : {}", containsMultipleTempoCount.get());
            log.info("isEmptyCount : {}", isEmptyCount.get());
            log.info("containsModifiedTrackCount : {}", containsModifiedTrackCount.get());
            log.info("failCount : {}", failCount.get());
            log.info("containsUnmatchedNoteOnCount : {}", containsUnmatchedNoteOnCount.get());
            log.info("containsUnmatchedNoteOffCount : {}", containsUnmatchedNoteOffCount.get());
            log.info("errorCount : {}", errorCount.get());
        } catch (Exception e) {
            throw new OcException("checkMidiFiles fail : %s", e);
        }
    }

    private static List<MidiFileReport> findCandidates(String dirPath, boolean logFail, boolean logDetail) {
        final var midiFilePathList = findMidiFilePathList(dirPath);
        final List<MidiFileReport> okReportList = new ArrayList<>();
        final List<MidiFileReport> failReportList = new ArrayList<>();
        for (final var filePath : midiFilePathList) {
            try {
                final var midiFile = MidiReader.read(filePath);
                final var midiFileReport = MidiFileReport.of(midiFile);
                if (midiFile.isValid()) {
                    if (midiFileReport.isEmpty()) {
                        log.warn("isEmpty : {}", filePath);
                    } else if (!midiFileReport.hasValidDivision()) {
                        failReportList.add(midiFileReport);
                    } else if (16 < midiFileReport.getMinValidDivision()) {
                        log.warn("16 < division : {}", filePath);
                    } else {
                        okReportList.add(midiFileReport);
                    }
                } else {
                    log.warn("!isValid : {}", filePath);
                }
            } catch (Exception ignored) {
                log.warn("error : {}", filePath);
            }
        }
        okReportList.sort(Comparator.comparing(report -> report.getFile().getFilePath()));
        failReportList.sort(Comparator.comparing(MidiFileReport::getMinUnalignedTrackCount).thenComparing(MidiFileReport::getMinUnalignedNoteCount));
        log.info("ok/fail count : {} / {}", okReportList.size(), failReportList.size());
        log.info("-------------------------------------------------");
        for (var i = 0; i < okReportList.size(); ++i) {
            log.info("\n({}) {}", i, formatFileReportInfo(okReportList.get(i)));
            log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        }
        log.info("ok/fail count : {} / {}", okReportList.size(), failReportList.size());
        if (logFail) {
            for (final var report : failReportList) {
                log.info("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
                log.info(formatFileReportInfo(report) +
                         String.format("unaligned tracks/notes : %d / %d", report.getMinUnalignedTrackCount(), report.getMinUnalignedNoteCount()));
            }
        }
        if (logDetail) {
            log.info("-------------------------------------------------");
            log.info("ok/fail count : {} / {}", okReportList.size(), failReportList.size());
            log.info("-------------------------------------------------");
            for (var i = 0; i < okReportList.size(); ++i) {
                final var report = okReportList.get(i);
                final var music = Music16.of(report, 1);
                log.info("\nsequences : {}\n({}) {}", music.getSequenceList().size(), i, formatOkDetail(report));
                log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
            }
            log.info("ok/fail count : {} / {}", okReportList.size(), failReportList.size());
            if (logFail) {
                for (final var report : failReportList) {
                    log.info("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
                    log.info("\n{}", formatFailDetail(report));
                }
            }
        }
        return okReportList;
    }

    private static String formatOkDetail(MidiFileReport report) {
        final var sb = new StringBuilder();
        sb.append(formatFileReportInfo(report));
        for (final var trackReport : report.getTrackReportList()) {
            final var track = trackReport.getTrack();
            sb.append(String.format("\ttrack : %d %s%n", track.getId(), track.getInstruments()));
            final var lowerCount = trackReport.getLowerThanKeyNotes(KeyRange.LOWEST_KEY).size();
            final var higherCount = trackReport.getHigherThanKeyNotes(KeyRange.HIGHEST_KEY).size();
            if (0 < lowerCount || 0 < higherCount) {
                sb.append(String.format("!!!\tadjustable octaves %s%n", trackReport.calculateAdjustableOctaves(KeyRange.LOWEST_KEY, KeyRange.HIGHEST_KEY)));
                sb.append(String.format("!!!\tmin/max/lower/higher : %d / %d / %d / %d%n",
                                        trackReport.getMinKeyBetween(KeyRange.LOWEST_KEY, KeyRange.HIGHEST_KEY),
                                        trackReport.getMaxKeyBetween(KeyRange.LOWEST_KEY, KeyRange.HIGHEST_KEY),
                                        lowerCount, higherCount));
            }
            sb.append(String.format("\tticks/start/end : %d / %d / %d%n", track.getNoteList().size(), track.getMinTickOn() / 1000, track.getMaxTickOff() / 1000));
            sb.append(calculateOctaveDistribution(trackReport));
        }
        return sb.toString();
    }

    private static String formatFailDetail(MidiFileReport report) {
        final var sb = new StringBuilder();
        sb.append(formatFileReportInfo(report));
        for (final var trackReport : report.getTrackReportList()) {
            final var track = trackReport.getTrack();
            final var unalignedDivision = report.getMinUnalignedDivision();
            final var unalignedNotes = trackReport.getDivisionToUnalignedNoteList().get(unalignedDivision);
            sb.append(String.format("\ttrack : %d %s%n", track.getId(), track.getInstruments()));
            if (null != unalignedNotes) {
                final var unalignedTicks = unalignedNotes.stream().map(note -> note.getOn() / report.getFile().getWholeNoteTicks() + 1).toList();
                sb.append(String.format("\tnotes/unaligned : %d / %d%n", track.getNoteList().size(), unalignedTicks.size()));
                sb.append(String.format("\t\t%s%n", unalignedTicks));
            }
        }
        return sb.toString();
    }

    private static String calculateOctaveDistribution(MidiTrackReport trackReport) {
        final var octaveCount = (KeyRange.HIGHEST_KEY - KeyRange.LOWEST_KEY) / KeyRange.KEY_PER_OCTAVE + 1;
        final var header = IntStream.rangeClosed(1, octaveCount)
                                    .mapToObj(i -> String.format("|  F#%1d |", i))
                                    .collect(Collectors.joining("      "));
        final List<int[]> noteRangeList = new ArrayList<>();
        for (var i = 0; i < octaveCount; ++i) {
            noteRangeList.add(new int[]{ KeyRange.LOWEST_KEY + i * KeyRange.KEY_PER_OCTAVE, KeyRange.LOWEST_KEY + i * KeyRange.KEY_PER_OCTAVE, 0 });
            noteRangeList.add(new int[]{ KeyRange.LOWEST_KEY + i * KeyRange.KEY_PER_OCTAVE + 1, KeyRange.LOWEST_KEY + (i + 1) * KeyRange.KEY_PER_OCTAVE - 1, 0 });
        }
        noteRangeList.remove(noteRangeList.size() - 1);
        final var keyDistribution = trackReport.getKeyDistribution(KeyRange.LOWEST_KEY, KeyRange.HIGHEST_KEY);
        for (var key = 0; key < keyDistribution.size(); ++key) {
            final var count = keyDistribution.get(key);
            for (final var noteRange : noteRangeList) {
                if (noteRange[0] <= key && key <= noteRange[1]) {
                    noteRange[2] += count;
                    break;
                }
            }
        }
        final var octaveDistribution = noteRangeList
                .stream().map(noteRange -> 0 < noteRange[2] ? String.format(" %4d |", noteRange[2]) : "      |")
                .collect(Collectors.joining());
        final var sb = new StringBuilder();
        sb.append("\t\t").append(header).append(String.format("%n"));
        sb.append("\t\t|").append(octaveDistribution).append(String.format("%n"));
        return sb.toString();
    }

    private static String formatFileReportInfo(MidiFileReport report) {
        final var sb = new StringBuilder();
        sb.append(String.format("path : %s%n", report.getFile().getFilePath()));
        sb.append(String.format("url : %s%n", report.getFile().getSourceUrl()));
        sb.append(String.format("division / staffs / min. : %s / %d / %s%n",
                                report.hasValidDivision() ? report.getMinValidDivision() : "??",
                                report.getFile().getTrackList().size(), formatMinutes(report.getFile().getMsDuration())));
        return sb.toString();
    }

    private static String formatMinutes(int ms) {
        final var seconds = ms / 1000;
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private static List<String> findMidiFilePathList(String dirPath) {
        try (var walk = Files.walk(Paths.get(dirPath))) {
            final var midiFilePathList = new ArrayList<String>();
            walk.forEach(path -> {
                if (Files.isRegularFile(path)) {
                    final var filePath = path.toString();
                    final var fileName = path.getFileName().toString();
                    if (fileName.contains("mid") && !fileName.endsWith(".txt") && !fileName.endsWith(".dms")) {
                        midiFilePathList.add(filePath);
                    }
                }
            });
            return midiFilePathList;
        } catch (Exception e) {
            throw new OcException("[MidiLoadingApp][findMidiFilePathList]", e);
        }
    }

    public static void main(String[] args) {
        try {
//            printMidiFile("data/generated/input/Happy_Birthday/Happy_Birthday_for_Violin.mid");
//            printMidiFile("data/generated/input/Happy_Birthday/Happy_Birthday_for_Violin-align.mid");
//            final var rootDirPath = "C:\\Users\\User\\Downloads\\midi";
//            checkMidiFiles(rootDirPath);

//            final var rootDirPath = "D:\\Sync\\Ongakucraft\\case\\0041\\0059\\レクイエム - Kanaria and 星街すいせい";
//            final var rootDirPath = "D:\\Sync\\Ongakucraft\\midi\\Hopes and Dreams - Orchestra"; // ok/fail count : 24 / 13
//            final var rootDirPath = "D:\\Sync\\Ongakucraft\\midi\\Hopes and Dreams - Undertale"; // ok/fail count : 13 / 8
//            final var rootDirPath = "D:\\Sync\\Ongakucraft\\midi\\ヤキモチ - 高橋優";
//            final var rootDirPath = "D:\\Sync\\Ongakucraft\\midi\\NIGHT DANCER - imase";
//            final var rootDirPath = "D:\\Sync\\Ongakucraft\\midi\\Megalovania - Undertale\\Meglovania-Orchestra"; // 2041941
//            final var rootDirPath = "D:\\Sync\\Ongakucraft\\midi\\Megalovania - Undertale\\Meglovania-4-6"; // 1987616
//            final var rootDirPath = "D:\\Sync\\Ongakucraft\\midi\\Megalovania - Undertale\\Meglovania-"; // megalovania (6).mid, remove 10,11,12
            final var rootDirPath = "D:\\Sync\\Ongakucraft\\midi\\Hey Ya - OutKast"; // megalovania (6).mid, remove 10,11,12
            final var reportIndex = 5; // 5,6,8,13,16,17
            final var reportList = findCandidates(rootDirPath, true, true);
            final var report = reportList.get(reportIndex);
            final var music = Music16.of(report, 1);//, 8, 6);
            log.info("-------------------------------------------------");
            log.info(formatFileReportInfo(report));
            for (final var noteSequence : music.getSequenceList()) {
                log.info(String.format("id/min-max/count : %2d/%3d-%3d/%3d", noteSequence.getId(),
                         noteSequence.getMinKey(), noteSequence.getMaxKey(), noteSequence.getCount()));
            }
            final var structure = CircuitUtils.buildDemoCircuits(VERSION, music, -50, 3, 3, 3);
//            NbtWriter.of(VERSION).write(structure, rootDirPath + "/demo.nbt");
            NbtWriter.of(VERSION).write(structure, "C:\\Users\\User\\AppData\\Roaming\\.minecraft\\saves\\Test World 189\\datapacks\\ongakucraft\\data\\ongakucraft\\structures\\demo.nbt");
            log.info("demo size : {} {} {}", structure.getRange3().getX().length(), structure.getRange3().getY().length(), structure.getRange3().getZ().length());
        } catch (Exception e) {
            log.error("MidiLoadingApp", e);
        }
    }

    private MidiLoadingApp() {}
}
