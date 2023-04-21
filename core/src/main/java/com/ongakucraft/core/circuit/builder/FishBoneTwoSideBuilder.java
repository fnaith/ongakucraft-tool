package com.ongakucraft.core.circuit.builder;

import java.util.List;

import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.BlockDataset;
import com.ongakucraft.core.circuit.CircuitBuilder;
import com.ongakucraft.core.circuit.Note;
import com.ongakucraft.core.structure.Cursor;
import com.ongakucraft.core.structure.Structure;

public final class FishBoneTwoSideBuilder extends CircuitBuilder {
    public static FishBoneTwoSideBuilder of(BlockDataset blockDataset, String floorPath, String stubPath) {
        return new FishBoneTwoSideBuilder(blockDataset,
                                          FishBoneOneSideBuilder.of(blockDataset, true, floorPath, stubPath),
                                          FishBoneOneSideBuilder.of(blockDataset, false, floorPath, stubPath));
    }

    private final FishBoneOneSideBuilder fishBoneRightSideBuilder;
    private final FishBoneOneSideBuilder fishBoneLeftSideBuilder;

    private FishBoneTwoSideBuilder(BlockDataset blockDataset,
                                   FishBoneOneSideBuilder fishBoneRightSideBuilder,
                                   FishBoneOneSideBuilder fishBoneLeftSideBuilder) {
        super(blockDataset);
        this.fishBoneRightSideBuilder = fishBoneRightSideBuilder;
        this.fishBoneLeftSideBuilder = fishBoneLeftSideBuilder;
    }

    @Override
    public void generate(Cursor cursor, List<List<Note>> sequenceList) {
        assertSequenceSize(sequenceList, 4);
        fillSequenceList(sequenceList, 4);
        final var rightStructure = new Structure();
        final var rightCursor = cursor.clone();
        rightCursor.setStructure(rightStructure);
        fishBoneRightSideBuilder.generate(rightCursor, List.of(sequenceList.get(0), sequenceList.get(1)));
        final var leftStructure = new Structure();
        final var leftCursor = cursor.clone();
        leftCursor.setStructure(leftStructure);
        fishBoneLeftSideBuilder.generate(leftCursor, List.of(sequenceList.get(2), sequenceList.get(3)));
        rightStructure.regulate();
        leftStructure.regulate();
        leftStructure.translate(rightStructure.getRange3().getX().length() - 1, 0, 0);
        cursor.paste(rightStructure);
        cursor.paste(leftStructure);
    }
}
