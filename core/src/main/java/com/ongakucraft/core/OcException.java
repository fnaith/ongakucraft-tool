package com.ongakucraft.core;

import java.io.Serial;

public class OcException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 3501001193599409174L;

    public OcException(String format, Object... args) {
        super(String.format(format, args));
    }
}
