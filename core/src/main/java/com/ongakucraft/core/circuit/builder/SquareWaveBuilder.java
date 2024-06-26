package com.ongakucraft.core.circuit.builder;

import com.ongakucraft.core.block.BlockDataset;
import com.ongakucraft.core.circuit.CircuitBuilder;
import com.ongakucraft.core.circuit.Note;
import com.ongakucraft.core.structure.Cursor;

import java.util.List;

public final class SquareWaveBuilder extends CircuitBuilder {
    public static final int BEATS_PER_SECTION = 4;

    public static SquareWaveBuilder of(BlockDataset blockDataset, boolean turnRight, int delay, String floorPath, String stubPath) {
        return new SquareWaveBuilder(blockDataset, turnRight, delay, floorPath, stubPath);
    }

    private final boolean turnRight;
    private final int delay;
    private final String floor;
    private final String stub;

    private SquareWaveBuilder(BlockDataset blockDataset, boolean turnRight, int delay,
                              String floorPath, String stubPath) {
        super(blockDataset);
        this.turnRight = turnRight;
        this.delay = delay;
        floor = floorPath;
        stub = stubPath;
    }

    @Override
    public void generate(Cursor cursor, List<List<Note>> sequenceList) {
        assertSequenceSize(sequenceList, 1);
        fillSequenceList(sequenceList, 1);
        assertDelayCount(delay);
        final var rotateTimes = turnRight ? -1 : 1;
        final var trunkCursorLow = cursor.clone();
        final var trunkCursorHigh = cursor.clone().jump(1);
        final var sequence0 = sequenceList.get(0);
        final var maxBeat = sequenceList.stream().map(List::size).max(Integer::compareTo).orElse(0);
        final var beats = maxBeat + (BEATS_PER_SECTION - maxBeat % BEATS_PER_SECTION) % BEATS_PER_SECTION;
        if (0 < delay) {
            trunkCursorLow.place(floor).step();
            trunkCursorHigh.placeRepeater(delay).step();
        }
        for (var beat = 0; beat < beats; ++beat) {
            final var beatIndex = beat % BEATS_PER_SECTION;
            final var note0 = sequence0.size() <= beat ? null : sequence0.get(beat);
            trunkCursorLow.place(floor).step();
            trunkCursorHigh.placeRepeater(1).step();
            if (null == note0) {
                trunkCursorLow.place(floor);
                trunkCursorHigh.place(stub);
            } else {
                trunkCursorLow.place(note0.getPath());
                trunkCursorHigh.placeNoteBlock(note0.getNote());
            }
            final var isBeatInMiddle = 0 != beatIndex % (BEATS_PER_SECTION - 1);
            trunkCursorLow.rotate(rotateTimes * (isBeatInMiddle ? -1 : 1)).step();
            trunkCursorHigh.rotate(rotateTimes * (isBeatInMiddle ? -1 : 1)).step();
        }
    }
}
