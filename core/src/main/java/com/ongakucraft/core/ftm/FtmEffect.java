package com.ongakucraft.core.ftm;

import com.ongakucraft.core.OcException;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public final class FtmEffect {
    private final int param0;
    private final boolean forChannel;
    private final int param1;
    private final int param2;

    public static FtmEffect phaseReset() {
        return new FtmEffect('=', false, 0, 0);
    }

    public static FtmEffect arpeggio(int x, int y) {
        if (x < 0 || 16 <= x) {
            throw new OcException("[FtmEffect] x should in [0, 16) : %d", x);
        }
        if (y < 0 || 16 <= y) {
            throw new OcException("[FtmEffect] y should in [0, 16) : %d", y);
        }
        return new FtmEffect('0', true, x, y);
    }

    public static FtmEffect slideUp(int xx) {
        return new FtmEffect('1', true, xx);
    }

    public static FtmEffect slideDown(int xx) {
        return new FtmEffect('2', true, xx);
    }

    public static FtmEffect portamento(int xx) {
        return new FtmEffect('3', true, xx);
    }

    public static FtmEffect vibrato(int x, int y) {
        return new FtmEffect('4', true, x, y);
    }

    public static FtmEffect tremolo(int x, int y) {
        return new FtmEffect('7', true, x, y);
    }

    public static FtmEffect volumeSlide(int x, int y) {
        return new FtmEffect('A', true, x, y);
    }

    public static FtmEffect jump(int xx) {
        return new FtmEffect('B', false, xx);
    }

    public static FtmEffect halt() {
        return new FtmEffect('C', false, 0);
    }

    public static FtmEffect skip(int xx) {
        return new FtmEffect('D', false, xx);
    }

    public static FtmEffect speedTempo(int xx) {
        return new FtmEffect('F', false, xx);
    }

    public static FtmEffect noteDelay(int xx) {
        return new FtmEffect('G', false, xx);
    }

    // TODO complete general effect

    private FtmEffect(int param0, boolean forChannel, int param12) {
        this(param0, forChannel, param12 / 16, param12 % 16);
    }

    private FtmEffect(int param0, boolean forChannel, int param1, int param2) {
        if (param1 < 0 || 16 <= param1) {
            throw new OcException("[FtmEffect] param1 should in [0, 16) : %d", param1);
        }
        if (param2 < 0 || 16 <= param2) {
            throw new OcException("[FtmEffect] param2 should in [0, 16) : %d", param2);
        }
        this.param0 = param0;
        this.forChannel = forChannel;
        this.param1 = param1;
        this.param2 = param2;
    }

    public FtmEffect disable() {
        if (forChannel) {
            return new FtmEffect(param0, true, 0, 0);
        } else {
            return null;
        }
    }

    public boolean isDisabled() {
        return forChannel && 0 == param1 && 0 == param2;
    }
}
