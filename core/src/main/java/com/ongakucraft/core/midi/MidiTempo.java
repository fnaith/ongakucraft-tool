package com.ongakucraft.core.midi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor(staticName = "of")
@Getter
@ToString
public final class MidiTempo {
    private final int mspq;
    private final int tick;
}
