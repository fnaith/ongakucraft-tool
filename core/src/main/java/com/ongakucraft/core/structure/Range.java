package com.ongakucraft.core.structure;

import com.ongakucraft.core.OcException;

import lombok.Getter;

@Getter
public final class Range {
    public static final Range EMPTY = of(0, 0);

    public static Range of(int start, int stop) {
        if (stop < start) {
            throw new OcException("start should <= stop : %d %d", start, stop);
        }
        return new Range(start, stop);
    }

    private final int start;
    private final int stop;

    private Range(int start, int stop) {
        this.start = start;
        this.stop = stop;
    }

    public int length() {
        return stop - start;
    }

    public int getMin() {
        return start;
    }

    public int getMax() {
        return stop - 1;
    }

    public boolean contains(int value) {
        return start <= value && value < stop;
    }

    public Range translate(int value) {
        return of(start + value, stop + value);
    }

    @Override
    public String toString() {
        return String.format("[%d:%d]", start, stop);
    }
}
