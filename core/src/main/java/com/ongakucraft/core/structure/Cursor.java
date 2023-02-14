package com.ongakucraft.core.structure;

import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.Block;
import com.ongakucraft.core.block.Direction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public final class Cursor implements Cloneable {
    @NonNull private Structure structure;
    @NonNull private Position position;
    @NonNull private Direction facing;

    public Cursor(Structure structure) {
        this(structure, Position.ZERO, Direction.S);
    }

    @Override
    public Cursor clone() {
        return new Cursor(structure, position, facing);
    }

    public void step() {
        step(1);
    }

    public void step(int times) {
        position = position.step(facing, times);
    }

    public void jump(int y) {
        position = position.jump(y);
    }

    public void move(int x, int z) {
        translate(x, 0, z);
    }

    public void translate(int x, int y, int z) {
        final var relativePosition = Position.of(x, y, z).rotate(rotateTimes());
        position = position.translate(relativePosition.getX(), relativePosition.getY(), relativePosition.getZ());
    }

    public void back() {
        facing = facing.back();
    }

    public void left() {
        facing = facing.left();
    }

    public void right() {
        facing = facing.right();
    }

    public void rotate(int times) {
        facing = facing.rotate(times);
    }

    public void put(@NonNull Block t) {
        structure.put(position, t.rotate(rotateTimes()));
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
