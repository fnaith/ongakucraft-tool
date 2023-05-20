package com.ongakucraft.core.structure;

import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.Block;
import com.ongakucraft.core.block.BlockDataset;
import com.ongakucraft.core.block.Direction;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public final class Cursor implements Cloneable {
    @NonNull private BlockDataset blockDataset;
    @NonNull private Structure structure;
    @NonNull private Position position;
    @NonNull private Direction facing;
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private Structure tmpStructure;

    public Cursor(BlockDataset blockDataset, Structure structure) {
        this(blockDataset, structure, Position.ZERO, Direction.S, null);
    }

    public void setPreventModify(boolean flag) {
        if (getPreventModify()) {
            structure = tmpStructure;
            tmpStructure = null;
        }
        if (flag) {
            tmpStructure = structure;
            structure = structure.clone();
        }
    }

    public boolean getPreventModify() {
        return null != tmpStructure;
    }

    @Override
    public Cursor clone() {
        return new Cursor(blockDataset, structure, position, facing, tmpStructure);
    }

    public Cursor step() {
        return step(1);
    }

    public Cursor step(int times) {
        position = position.step(facing, times);
        return this;
    }

    public Cursor jump(int y) {
        position = position.jump(y);
        return this;
    }

    public Cursor move(int x, int z) {
        return translate(x, 0, z);
    }

    public void translate(Position position) {
        translate(position.getX(), position.getY(), position.getZ());
    }

    public Cursor translate(int x, int y, int z) {
        final var relativePosition = Position.of(x, y, z).rotate(rotateTimes());
        position = position.translate(relativePosition.getX(), relativePosition.getY(), relativePosition.getZ());
        return this;
    }

    public Cursor back() {
        facing = facing.back();
        return this;
    }

    public Cursor left() {
        facing = facing.left();
        return this;
    }

    public Cursor right() {
        facing = facing.right();
        return this;
    }

    public Cursor rotate(int times) {
        facing = facing.rotate(times);
        return this;
    }

    public Cursor face(Direction facing) {
        this.facing = facing;
        return this;
    }

    public Block getBlock(@NonNull String path) {
        return blockDataset.getBlock(path).withFacing(getPlaceFacing(path));
    }

    public Cursor place(@NonNull String path) {
        final var block = getBlock(path);
        structure.put(position, block);
        return this;
    }

    public Cursor placeRepeater(int delay) {
        final var block = getBlock("repeater").putProperty("delay", delay);
        structure.put(position, block);
        return this;
    }

    public Cursor placeRedstoneWire(List<Direction> sides) {
        var block = getBlock("redstone_wire");
        for (var side : sides) {
            block = block.putProperty(side.getText(), "side");
        }
        structure.put(position, block);
        return this;
    }

    public Cursor placeNoteBlock(int note) {
        final var block = getBlock("note_block").putProperty("note", note);
        structure.put(position, block);
        return this;
    }

    public Cursor put(@NonNull Block t) {
        structure.put(position, t.rotate(rotateTimes()));
        return this;
    }

    public Block get() {
        return structure.get(position);
    }

    public Block remove() {
        return structure.remove(position);
    }

    public Structure cut(@NonNull Range3 range3) {
        return structure.cut(transform(range3));
    }

    public Structure copy(@NonNull Range3 range3) {
        return structure.copy(transform(range3));
    }

    public void fill(@NonNull Range3 range3, @NonNull Block block) {
        structure.fill(transform(range3), block);
    }

    public void paste(@NonNull Structure src) {
        structure.paste(transform(src));
    }

    private int rotateTimes() {
        return switch (facing) {
            case S -> 0;
            case E -> 1;
            case N -> 2;
            case W -> 3;
            default -> throw new OcException("");
        };
    }

    private Direction getPlaceFacing(String path) {
        if ("repeater".equals(path) ||
            "lectern".equals(path) ||
            path.endsWith("_trapdoor")) {
            return facing.back();
        }
        return facing;
    }

    private Range3 transform(Range3 range3) {
        return range3.rotate(rotateTimes()).translate(position.getX(), position.getY(), position.getZ());
    }

    private Structure transform(Structure structure) {
        final var newStructure = structure.clone();
        newStructure.rotate(rotateTimes());
        newStructure.translate(position.getX(), position.getY(), position.getZ());
        return newStructure;
    }
}
