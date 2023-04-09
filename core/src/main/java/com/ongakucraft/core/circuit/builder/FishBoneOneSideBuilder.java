package com.ongakucraft.core.circuit.builder;

import java.util.List;

import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.BlockDataset;
import com.ongakucraft.core.circuit.CircuitBuilder;
import com.ongakucraft.core.circuit.Note;
import com.ongakucraft.core.structure.Cursor;

public final class FishBoneOneSideBuilder extends CircuitBuilder {
    public static final int BEATS_PER_SECTION = 4;

    public static FishBoneOneSideBuilder of(BlockDataset blockDataset, boolean turnRight, String floorPath, String stubPath) {
        return new FishBoneOneSideBuilder(blockDataset, turnRight, floorPath, stubPath);
    }

    private final boolean turnRight;
    private final String floor;
    private final String stub;

    private FishBoneOneSideBuilder(BlockDataset blockDataset, boolean turnRight,
                                   String floorPath, String stubPath) {
        super(blockDataset);
        this.turnRight = turnRight;
        floor = floorPath;
        stub = stubPath;
    }

    @Override
    public void generate(Cursor cursor, List<List<Note>> sequenceList) {
        if (sequenceList.isEmpty() || 2 < sequenceList.size()) {
            throw new OcException("[FishBoneOneSideBuilder][generate] size : %d", sequenceList.size());
        }
        while (sequenceList.size() < 2) {
            sequenceList.add(List.of());
        }
        final var facing = cursor.getFacing();
        final var turn = turnRight ? facing.right() : facing.left();
        final var sides = List.of(turn, turn.back());
        final var trunkCursorLow = cursor.clone();
        final var trunkCursorHigh = cursor.clone().jump(1);
        Cursor branchCursorLow = null;
        Cursor branchCursorHigh = null;
        var branchLength = 0;
        final var sequence0 = sequenceList.get(0);
        final var sequence1 = sequenceList.get(1);
        final var maxBeat = sequenceList.stream().map(List::size).max(Integer::compareTo).orElse(0);
        final var beats = maxBeat + (BEATS_PER_SECTION - maxBeat % BEATS_PER_SECTION) % BEATS_PER_SECTION;
        for (var beat = 0; beat < beats; ++beat) {
            final var beatIndex = beat % BEATS_PER_SECTION;
            if (0 == beatIndex) {
                branchLength = 0;
                for (var i = 0; i < BEATS_PER_SECTION; ++i) {
                    for (final var sequence : sequenceList) {
                        if (beat + i < sequence.size() && null != sequence.get(beat + i)) {
                            branchLength = i + 1;
                        }
                    }
                }

                trunkCursorLow.place(floor).step();
                trunkCursorHigh.placeRepeater(BEATS_PER_SECTION).step();

                branchCursorLow = trunkCursorLow.clone().face(turn).step();
                branchCursorHigh = trunkCursorHigh.clone().face(turn).step();

                trunkCursorLow.place(floor).step();
                trunkCursorHigh.place(stub).step();
            }
            branchCursorLow.place(floor);
            branchCursorLow.clone().face(facing).step().place(floor);
            if (branchLength <= beatIndex) {
                branchCursorHigh.setPreventModify(true);
            }
            if (0 == beatIndex) {
                branchCursorHigh.placeRedstoneWire(sides);
            } else {
                branchCursorHigh.placeRepeater(1);
            }
            branchCursorHigh.setPreventModify(false);
            branchCursorLow.step();
            branchCursorHigh.step();
            if (branchLength <= beatIndex) {
                branchCursorHigh.setPreventModify(true);
            }
            final var note0 = sequence0.size() <= beat ? null : sequence0.get(beat);
            final var note1 = sequence1.size() <= beat ? null : sequence1.get(beat);
            if (null == note0) {
                if (null == note1) {
                    branchCursorLow.place(floor);
                    branchCursorLow.clone().face(facing).step().place(floor);
                    branchCursorHigh.place(stub);
                } else {
                    branchCursorLow.place(floor);
                    branchCursorLow.clone().face(facing).step().place(note1.getPath());
                    branchCursorHigh.place(stub);
                    branchCursorHigh.clone().face(facing).step().placeNoteBlock(note1.getNote());
                }
            } else {
                if (null == note1) {
                    branchCursorLow.place(note0.getPath());
                    branchCursorLow.clone().face(facing).step().place(floor);
                    branchCursorHigh.placeNoteBlock(note0.getNote());
                } else {
                    branchCursorLow.place(note0.getPath());
                    branchCursorLow.clone().face(facing).step().place(note1.getPath());
                    branchCursorHigh.placeNoteBlock(note0.getNote());
                    branchCursorHigh.clone().face(facing).step().placeNoteBlock(note1.getNote());
                }
            }
            branchCursorHigh.setPreventModify(false);
            branchCursorLow.step();
            branchCursorHigh.step();
        }
    }
}
