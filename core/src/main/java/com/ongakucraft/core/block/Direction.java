package com.ongakucraft.core.block;

import com.ongakucraft.core.OcException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public enum Direction {
    E(1, 0, 0, "east"),
    S(0, 0, 1, "south"),
    W(-1, 0, 0, "west"),
    N(0, 0, -1, "north"),
    U(0, 1, 0, "up"),
    D(0, -1, 0, "down");

    public static Direction of(@NonNull String text) {
        return switch (text) {
            case "east" -> E;
            case "south" -> S;
            case "west" -> W;
            case "north" -> N;
            case "up" -> U;
            case "down" -> D;
            default -> throw new OcException("invalid direction text : {}", text);
        };
    }

    private final int x;
    private final int y;
    private final int z;
    private final String text;

    public Direction back() {
        return switch (this) {
            case E -> W;
            case S -> N;
            case W -> E;
            case N -> S;
            case U -> U;
            case D -> D;
        };
    }

    public Direction left() {
        return switch (this) {
            case E -> N;
            case S -> E;
            case W -> S;
            case N -> W;
            case U -> U;
            case D -> D;
        };
    }

    public Direction right() {
        return switch (this) {
            case E -> S;
            case S -> W;
            case W -> N;
            case N -> E;
            case U -> U;
            case D -> D;
        };
    }

    public Direction rotate(int times) {
        return switch (((times % 4 + 4) % 4)) {
            case 0 -> this;
            case 1 -> left();
            case 2 -> back();
            case 3 -> right();
            default -> throw new OcException("");
        };
    }

    @Override
    public String toString() {
        return text;
    }
}
