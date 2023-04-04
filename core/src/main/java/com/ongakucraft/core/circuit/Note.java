package com.ongakucraft.core.circuit;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(staticName = "of")
@Getter
public final class Note {
    private final String path;
    private final int note;
}
