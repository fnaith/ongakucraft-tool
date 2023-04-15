package com.ongakucraft.core.circuit.builder;

import java.util.List;

import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.BlockDataset;
import com.ongakucraft.core.circuit.CircuitBuilder;
import com.ongakucraft.core.circuit.Note;
import com.ongakucraft.core.structure.Cursor;

public final class CheckPatternBuilder extends CircuitBuilder {
    public static final int BEATS_PER_SECTION = 2;

    public static CheckPatternBuilder of(BlockDataset blockDataset, boolean turnRight, String floorPath, String stubPath) {
        return new CheckPatternBuilder(blockDataset, turnRight, floorPath, stubPath);
    }

    private final boolean turnRight;
    private final String floor;
    private final String stub;

    private CheckPatternBuilder(BlockDataset blockDataset, boolean turnRight,
                                String floorPath, String stubPath) {
        super(blockDataset);
        this.turnRight = turnRight;
        floor = floorPath;
        stub = stubPath;
    }

    @Override
    public void generate(Cursor cursor, List<List<Note>> sequenceList) {
        if (1 != sequenceList.size()) {
            throw new OcException("[CheckPatternBuilder][generate] size : %d", sequenceList.size());
        }
        final var facing = cursor.getFacing();
        final var rotateTimes = turnRight ? -1 : 1;
        final var trunkCursorLow = cursor.clone();
        final var trunkCursorHigh = cursor.clone().jump(1);
        final var sequence0 = sequenceList.get(0);
        final var maxBeat = sequenceList.stream().map(List::size).max(Integer::compareTo).orElse(0);
        final var beats = maxBeat + (BEATS_PER_SECTION - maxBeat % BEATS_PER_SECTION) % BEATS_PER_SECTION;
        trunkCursorLow.clone().rotate(rotateTimes).step().face(facing).place(floor);
        trunkCursorHigh.clone().rotate(rotateTimes).step().face(facing).placeRepeater(1);
        for (var beat = 0; beat < beats; ++beat) {
            final var beatIndex = beat % BEATS_PER_SECTION;
            final var note0 = sequence0.size() <= beat ? null : sequence0.get(beat);
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
            trunkCursorLow.rotate(rotateTimes * (isInFrontOfStartingPoint ? 1 : -1)).step().face(facing);
            trunkCursorHigh.rotate(rotateTimes * (isInFrontOfStartingPoint ? 1 : -1)).step().face(facing);
        }
    }
}
