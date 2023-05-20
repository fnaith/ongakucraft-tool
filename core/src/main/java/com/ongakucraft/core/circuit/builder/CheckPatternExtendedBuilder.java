package com.ongakucraft.core.circuit.builder;

import com.ongakucraft.core.block.BlockDataset;
import com.ongakucraft.core.circuit.CircuitBuilder;
import com.ongakucraft.core.circuit.Note;
import com.ongakucraft.core.structure.Cursor;

import java.util.List;

public final class CheckPatternExtendedBuilder extends CircuitBuilder {
    public static final int BEATS_PER_SECTION = 2;

    public static CheckPatternExtendedBuilder of(BlockDataset blockDataset, boolean turnRight, int delay, String floorPath, String stubPath) {
        return new CheckPatternExtendedBuilder(blockDataset, turnRight, delay, floorPath, stubPath);
    }

    private final boolean turnRight;
    private final int delay;
    private final String floor;
    private final String stub;

    private CheckPatternExtendedBuilder(BlockDataset blockDataset, boolean turnRight, int delay,
                                        String floorPath, String stubPath) {
        super(blockDataset);
        this.turnRight = turnRight;
        this.delay = delay;
        floor = floorPath;
        stub = stubPath;
    }

    @Override
    public void generate(Cursor cursor, List<List<Note>> sequenceList) {
        assertSequenceSize(sequenceList, 2);
        fillSequenceList(sequenceList, 2);
        assertDelayCount(delay);
        final var facing = cursor.getFacing();
        final var rotateTimes = turnRight ? -1 : 1;
        final var trunkCursorLow = cursor.clone();
        final var trunkCursorHigh = cursor.clone().jump(1);
        final var sequence0 = sequenceList.get(0);
        final var sequence1 = sequenceList.get(1);
        final var maxBeat = sequenceList.stream().map(List::size).max(Integer::compareTo).orElse(0);
        final var beats = maxBeat + (BEATS_PER_SECTION - maxBeat % BEATS_PER_SECTION) % BEATS_PER_SECTION;
        if (0 < delay) {
            trunkCursorLow.clone().rotate(rotateTimes).step().face(facing).place(floor);
            trunkCursorHigh.clone().rotate(rotateTimes).step().face(facing).placeRepeater(delay);
            trunkCursorLow.place(floor).step();
            trunkCursorHigh.placeRepeater(delay).step();
        }
        trunkCursorLow.clone().rotate(rotateTimes).step().face(facing).place(floor);
        trunkCursorHigh.clone().rotate(rotateTimes).step().face(facing).placeRepeater(1);
        for (var beat = 0; beat < beats; ++beat) {
            final var beatIndex = beat % BEATS_PER_SECTION;
            final var note0 = sequence0.size() <= beat ? null : sequence0.get(beat);
            final var note1 = sequence1.size() <= beat ? null : sequence1.get(beat);
            trunkCursorLow.place(floor).step();
            trunkCursorHigh.placeRepeater(2).step();
            if (null == note0) {
                trunkCursorLow.place(floor);
                trunkCursorHigh.place(stub);
            } else {
                trunkCursorLow.place(note0.getPath());
                trunkCursorHigh.placeNoteBlock(note0.getNote());
            }
            final var isInFrontOfStartingPoint = 0 == beatIndex;
            if (null != note1) {
                trunkCursorLow.clone().rotate(rotateTimes * (isInFrontOfStartingPoint ? -1 : 1)).step().place(note1.getPath());
                trunkCursorHigh.clone().rotate(rotateTimes * (isInFrontOfStartingPoint ? -1 : 1)).step().placeNoteBlock(note1.getNote());
            }
            trunkCursorLow.rotate(rotateTimes * (isInFrontOfStartingPoint ? 1 : -1)).step().face(facing);
            trunkCursorHigh.rotate(rotateTimes * (isInFrontOfStartingPoint ? 1 : -1)).step().face(facing);
        }
    }
}
