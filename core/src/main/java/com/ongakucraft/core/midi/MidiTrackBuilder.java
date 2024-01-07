package com.ongakucraft.core.midi;

import lombok.Getter;

import java.util.*;

@Getter
public final class MidiTrackBuilder {
    private final Map<Integer, Queue<int[]>> keyToOnInfoList = new HashMap<>();
    private final List<MidiNote> notes = new ArrayList<>();
    private final List<MidiNote> unmatchedNoteOffList = new ArrayList<>();
    private final List<String> instruments = new ArrayList<>();

    public void on(int key, int onTick, int onVelocity) {
        getOnInfoList(key).add(new int[]{onTick, onVelocity});
    }

    public void off(int key, int offTick) {
        final var onInfoList = getOnInfoList(key);
        if (onInfoList.isEmpty() || offTick < onInfoList.peek()[0]) {
            unmatchedNoteOffList.add(MidiNote.of(key, offTick, offTick));
        } else {
            final var onInfo = onInfoList.poll();
            notes.add(MidiNote.of(key, onInfo[0], offTick, onInfo[1]));
        }
    }

    public void addInstrument(String instrument) {
        if (!instruments.contains(instrument)) {
            instruments.add(instrument);
        }
    }

    public MidiTrack build(int id) {
        final var unmatchedNoteOnList = keyToOnInfoList.entrySet().stream()
                                                    .map(entry -> entry.getValue().stream().map(onInfo -> MidiNote.of(entry.getKey(), onInfo[0], onInfo[0])).toList())
                                                    .flatMap(List::stream).toList();
        return MidiTrack.of(id, notes, unmatchedNoteOnList, unmatchedNoteOffList, instruments);
    }

    private Queue<int[]> getOnInfoList(int key) {
        return keyToOnInfoList.computeIfAbsent(key, k -> new LinkedList<>());
    }
}
