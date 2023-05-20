package com.ongakucraft.core.music;

import com.ongakucraft.core.OcException;
import com.ongakucraft.core.midi.MidiFileReport;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class Music16 {
    private static final int DIVISION = 16;

    public static Music16 of(MidiFileReport fileReport, int maxDuration) {
        if (DIVISION < fileReport.getMinValidDivision()) {
            throw new OcException("getMinValidDivision fail : %d", fileReport.getMinValidDivision());
        }
        final var midiFile = fileReport.getFile();
        if (0 != midiFile.getWholeNoteTicks() % DIVISION) {
            throw new OcException("getWholeNoteTicks fail : %d", fileReport.getFile().getWholeNoteTicks());
        }
        final var divisionTicks = midiFile.getWholeNoteTicks() / DIVISION;
        final var maxTickOff = midiFile.getMaxTickOff();
        final var beats = calculateBeats(maxTickOff, divisionTicks);
        final List<Staff> staffList = new ArrayList<>();
        var startSequenceId = 0;
        for (final var trackReport : fileReport.getTrackReportList()) {
            final var staff = Staff.of(trackReport, beats, divisionTicks, startSequenceId, maxDuration);
            startSequenceId += staff.getSequenceList().size();
            staffList.add(staff);
        }
        return new Music16(fileReport, Collections.unmodifiableList(staffList));
    }

    private final MidiFileReport fileReport;
    private final List<Staff> staffList;

    public List<Sequence> getSequenceList() {
        final List<Sequence> sequenceList = new ArrayList<>();
        for (final var staff : staffList) {
            sequenceList.addAll(staff.getSequenceList());
        }
        return Collections.unmodifiableList(sequenceList);
    }

    private static int calculateBeats(int maxTick, int divisionTicks) {
        return (maxTick / divisionTicks) + 1;
    }
}
