package com.ongakucraft.core.circuit;

import java.util.List;

import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.BlockDataset;
import com.ongakucraft.core.structure.Cursor;
import com.ongakucraft.core.structure.Structure;

public abstract class CircuitBuilder {
    public abstract void generate(Cursor cursor, List<List<Note>> sequenceList);

    protected final BlockDataset blockDataset;

    protected CircuitBuilder(BlockDataset blockDataset) {
        this.blockDataset = blockDataset;
    }

    protected void assertSequenceSize(List<List<Note>> sequenceList, int limit) {
        final var size = sequenceList.isEmpty() ? 0 : sequenceList.size();
        if (size < 1 || limit < size) {
            throw new OcException("[CircuitBuilder][assertSequenceSize] size must in [1,%d] : %d", limit, size);
        }
    }

    protected void fillSequenceList(List<List<Note>> sequenceList, int limit) {
        while (sequenceList.size() < limit) {
            sequenceList.add(List.of());
        }
    }

    protected void assertDelayCount(int delay) {
        if (delay < 0 || 4 < delay) {
            throw new OcException("[CircuitBuilder][assertDelayCount] delay must in [1,4] : %d", delay);
        }
    }

    public Structure generate(List<List<Note>> sequenceList) {
        final Structure structure = new Structure();
        final Cursor cursor = new Cursor(blockDataset, structure);
        generate(cursor, sequenceList);
        return structure;
    }
}
