package com.ongakucraft.core.midi;

import lombok.Getter;

import java.util.*;

@Getter
public final class MidiTrackBuilder {
    private final Map<Integer, Queue<Integer>> keyToOnTickList = new HashMap<>();
    private final List<MidiNote> notes = new ArrayList<>();
    private final List<MidiNote> unmatchedNoteOffList = new ArrayList<>();
    private final List<String> instruments = new ArrayList<>();

    public void on(int key, int tick) {
        getOnTicks(key).add(tick);
    }

    public void off(int key, int tick) {
        final var onTicks = getOnTicks(key);
        if (onTicks.isEmpty() || tick < onTicks.peek()) {
            unmatchedNoteOffList.add(MidiNote.of(key, tick, tick));
        } else {
            final var onTick = onTicks.poll();
            notes.add(MidiNote.of(key, onTick, tick));
        }
    }

    public void addInstrument(String instrument) {
        if (!instruments.contains(instrument)) {
            instruments.add(instrument);
        }
    }

    public MidiTrack build(int id) {
        final var unmatchedNoteOnList = keyToOnTickList.entrySet().stream()
                                                    .map(entry -> entry.getValue().stream().map(tick -> MidiNote.of(entry.getKey(), tick, tick)).toList())
                                                    .flatMap(List::stream).toList();
        return MidiTrack.of(id, notes, unmatchedNoteOnList, unmatchedNoteOffList, instruments);
    }

    private Queue<Integer> getOnTicks(int key) {
        return keyToOnTickList.computeIfAbsent(key, k -> new LinkedList<>());
    }
}
