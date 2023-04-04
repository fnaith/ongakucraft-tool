package com.ongakucraft.core.circuit;

import java.util.List;

import com.ongakucraft.core.block.BlockDataset;
import com.ongakucraft.core.structure.Cursor;
import com.ongakucraft.core.structure.Structure;

public abstract class CircuitBuilder {
    public abstract void generate(Cursor cursor, List<List<Note>> sequenceList);

    protected final BlockDataset blockDataset;

    protected CircuitBuilder(BlockDataset blockDataset) {
        this.blockDataset = blockDataset;
    }

    public Structure generate(List<List<Note>> sequenceList) {
        final Structure structure = new Structure();
        final Cursor cursor = new Cursor(blockDataset, structure);
        generate(cursor, sequenceList);
        return structure;
    }
}
