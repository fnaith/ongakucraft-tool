package com.ongakucraft.core.circuit.builder;

import com.ongakucraft.core.block.BlockDataset;
import com.ongakucraft.core.circuit.CircuitBuilder;
import com.ongakucraft.core.circuit.Note;
import com.ongakucraft.core.structure.Cursor;

import java.util.List;

public final class FishBoneOneSideBuilder extends CircuitBuilder {
    private final int beatsPerSection;
    private final boolean turnRight;
    private final String floor;
    private final String stub;

    public FishBoneOneSideBuilder(BlockDataset blockDataset,
                                  int beatsPerSection, boolean turnRight,
                                  String floorPath, String stubPath) {
        super(blockDataset);
        this.beatsPerSection = beatsPerSection;
        this.turnRight = turnRight;
        floor = floorPath;
        stub = stubPath;
    }

    @Override
    public void generate(Cursor cursor, List<List<Note>> sequenceList) {
        assertSequenceSize(sequenceList, 2);
        fillSequenceList(sequenceList, 2);
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
        final var beats = maxBeat + (beatsPerSection - maxBeat % beatsPerSection) % beatsPerSection;
        for (var beat = 0; beat < beats; ++beat) {
            final var beatIndex = beat % beatsPerSection;
            if (0 == beatIndex) {
                branchLength = 0;
                for (var i = 0; i < beatsPerSection; ++i) {
                    for (final var sequence : sequenceList) {
                        if (beat + i < sequence.size() && null != sequence.get(beat + i)) {
                            branchLength = i + 1;
                        }
                    }
                }

                trunkCursorLow.place(floor).step();
                trunkCursorHigh.placeRepeater(beatsPerSection).step();

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
