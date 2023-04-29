package com.ongakucraft.core.circuit.builder;

import java.util.List;

import com.ongakucraft.core.block.BlockDataset;
import com.ongakucraft.core.circuit.CircuitBuilder;
import com.ongakucraft.core.circuit.Note;
import com.ongakucraft.core.structure.Cursor;

public final class TwisterOneSideBuilder extends CircuitBuilder {
    public static final int BEATS_PER_SECTION = 4;

    public static TwisterOneSideBuilder of(BlockDataset blockDataset, boolean turnRight, String floorPath, String stubPath) {
        return new TwisterOneSideBuilder(blockDataset, turnRight, floorPath, stubPath);
    }

    private final boolean turnRight;
    private final String floor;
    private final String stub;

    private TwisterOneSideBuilder(BlockDataset blockDataset, boolean turnRight,
                                  String floorPath, String stubPath) {
        super(blockDataset);
        this.turnRight = turnRight;
        floor = floorPath;
        stub = stubPath;
    }

    @Override
    public void generate(Cursor cursor, List<List<Note>> sequenceList) {
        assertSequenceSize(sequenceList, 3);
        fillSequenceList(sequenceList, 3);
        final var rotateTimes = turnRight ? -1 : 1;
        final var facing = turnRight ? cursor.getFacing().right() : cursor.getFacing().left();
        final var sidesList0 = List.of(List.of(cursor.getFacing(), facing.back()), List.of(cursor.getFacing(), cursor.getFacing().back()), List.of(cursor.getFacing().back(), facing.back()));
        final var sidesList1 = List.of(List.of(cursor.getFacing(), facing), List.of(cursor.getFacing(), cursor.getFacing().back()), List.of(cursor.getFacing().back(), facing));
        final var trunkCursorLow = cursor.clone().rotate(rotateTimes);
        final var trunkCursorHigh = cursor.clone().rotate(rotateTimes).jump(1);
        var turnCount = 0;
        final var sequence0 = sequenceList.get(0);
        final var sequence1 = sequenceList.get(1);
        final var sequence2 = sequenceList.get(2);
        final var maxBeat = sequenceList.stream().map(List::size).max(Integer::compareTo).orElse(0);
        final var beats = maxBeat + (BEATS_PER_SECTION - maxBeat % BEATS_PER_SECTION) % BEATS_PER_SECTION;
        for (var beat = 0; beat < beats; ++beat) {
            final var beatIndex = beat % BEATS_PER_SECTION;
            if (0 < beat && 0 == beatIndex) {
                final var inEvenSegment = 0 == turnCount % 2;
                final var sidesList = inEvenSegment ? sidesList0 : sidesList1;
                ++turnCount;

                trunkCursorLow.rotate(rotateTimes * (inEvenSegment ? -1 : 1));
                trunkCursorHigh.rotate(rotateTimes * (inEvenSegment ? -1 : 1));

                trunkCursorLow.place(floor).step().place(floor).step().place(floor);
                trunkCursorHigh.placeRedstoneWire(sidesList.get(0)).step()
                               .placeRedstoneWire(sidesList.get(1)).step()
                               .placeRedstoneWire(sidesList.get(2));

                trunkCursorLow.rotate(rotateTimes * (inEvenSegment ? -1 : 1)).step();
                trunkCursorHigh.rotate(rotateTimes * (inEvenSegment ? -1 : 1)).step();
            }
            trunkCursorLow.place(floor).step();
            trunkCursorHigh.placeRepeater(1).step();
            final var note0 = sequence0.size() <= beat ? null : sequence0.get(beat);
            final var note1 = sequence1.size() <= beat ? null : sequence1.get(beat);
            final var note2 = sequence2.size() <= beat ? null : sequence2.get(beat);
            if (null == note0) {
                trunkCursorLow.clone().rotate(rotateTimes).step().place(floor);
            } else {
                trunkCursorLow.clone().rotate(rotateTimes).step().place(note0.getPath());
                trunkCursorHigh.clone().rotate(rotateTimes).step().placeNoteBlock(note0.getNote());
            }
            if (null == note2) {
                trunkCursorLow.clone().rotate(-rotateTimes).step().place(floor);
            } else {
                trunkCursorLow.clone().rotate(-rotateTimes).step().place(note2.getPath());
                trunkCursorHigh.clone().rotate(-rotateTimes).step().placeNoteBlock(note2.getNote());
            }
            if (null == note1) {
                trunkCursorLow.place(floor);
                trunkCursorHigh.place(stub);
            } else {
                trunkCursorLow.place(note1.getPath());
                trunkCursorHigh.placeNoteBlock(note1.getNote());
            }
            trunkCursorLow.step();
            trunkCursorHigh.step();
        }
    }
}
