package com.ongakucraft.app.midi;

import com.ongakucraft.core.OcException;
import com.ongakucraft.core.midi.MidiFile;
import com.ongakucraft.core.midi.MidiTempo;
import com.ongakucraft.core.midi.MidiTrack;
import com.ongakucraft.core.midi.MidiTrackBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class MidiReader {
    private static final int SIMULTANEOUS_TRACKS = 1;

    public static MidiFile read(String filePath) {
        return read(filePath, 1);
    }

    public static MidiFile read(String filePath, int timeScale) {
        final var sourceUrl = getSourceUrl(filePath);
        final var sequence = getSequence(filePath);
        checkFileType(sequence, filePath);
        checkDivisionType(sequence, filePath);
        final var msDuration = (int)(sequence.getMicrosecondLength() / 1000);
        final var wholeNoteTicks = getWholeNoteTicks(sequence);
        final List<MidiTempo> tempoList = new ArrayList<>();
        final var tracks = getMidiTracks(sequence.getTracks(), tempoList, timeScale);
        return MidiFile.of(filePath, sourceUrl, msDuration, wholeNoteTicks, tracks, tempoList);
    }

    private static String getSourceUrl(String path) {
        try {
            return Files.readString(Path.of(path + ".txt"));
        } catch (Exception e) {
            return "null";
        }
    }

    private static Sequence getSequence(String path) {
        try {
            return MidiSystem.getSequence(new File(path));
        } catch (Exception e) {
            throw new OcException("getSequence fail : %s, %s", path, e);
        }
    }

    private static void checkFileType(Sequence sequence, String path) {
        if (!MidiSystem.isFileTypeSupported(SIMULTANEOUS_TRACKS, sequence)) {
            throw new OcException("checkFileType fail : %s", path);
        }
    }

    private static void checkDivisionType(Sequence sequence, String path) {
        if (Sequence.PPQ != sequence.getDivisionType()) {
            throw new OcException("checkDivisionType fail : %s", path);
        }
    }

    private static int getWholeNoteTicks(Sequence sequence) {
        return sequence.getResolution() * 4;
    }

    private static List<MidiTrack> getMidiTracks(Track[] tracks, List<MidiTempo> tempoList, int timeScale) {
        final var instruments = getInstruments();
        final var trackIdToBuilder = new HashMap<Integer, MidiTrackBuilder>();
        iterateNote(tracks, (id, event) -> {
            final var trackBuilder = trackIdToBuilder.computeIfAbsent(id, info -> new MidiTrackBuilder());
            final var sm = (ShortMessage) event.getMessage();
            final var key = sm.getData1();
            final var tick = ((int) event.getTick()) * timeScale;
            switch (sm.getCommand()) {
                case ShortMessage.NOTE_ON:
                    final var velocity = sm.getData2();
                    if (0 < velocity) {
                        trackBuilder.on(key, tick, velocity);
                    } else {
                        trackBuilder.off(key, tick);
                    }
                    break;
                case ShortMessage.NOTE_OFF:
                    trackBuilder.off(key, tick);
                    break;
                default:
            }
        }, (midiStaffId, event) -> {
            final var trackBuilder = trackIdToBuilder.computeIfAbsent(midiStaffId, info -> new MidiTrackBuilder());
            final var sm = (ShortMessage) event.getMessage();
            trackBuilder.addInstrument(instruments[sm.getData1()].getName().trim().toLowerCase());
        }, event -> {
            final var mm = (MetaMessage) event.getMessage();
            final var data = mm.getData();
            final var mspq = ((data[0] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[2] & 0xff);
            final var tick = (int) event.getTick();
            if (!tempoList.isEmpty()) {
                final var lastTempo = tempoList.get(tempoList.size() - 1);
                if (mspq == lastTempo.getMspq()) {
                    return;
                }
                if (tick == lastTempo.getTick()) {
                    tempoList.remove(tempoList.size() - 1);
                }
            }
            tempoList.add(MidiTempo.of(mspq, tick));
        });
        final var idList = trackIdToBuilder.keySet().stream().sorted().toList();
        final List<MidiTrack> staffs = new ArrayList<>();
        for (var id : idList) {
            staffs.add(trackIdToBuilder.get(id).build(id));
        }
        return staffs;
    }

    private static Instrument[] getInstruments() {
        try {
            return MidiSystem.getSynthesizer().getDefaultSoundbank().getInstruments();
        } catch (MidiUnavailableException e) {
            throw new OcException("getInstruments fail : %s", e);
        }
    }

    private static void iterateNote(Track[] tracks,
                                    BiConsumer<Integer, MidiEvent> noteHandler,
                                    BiConsumer<Integer, MidiEvent> instrumentHandler,
                                    Consumer<MidiEvent> tempoHandler) {
        for (var t = 0; t < tracks.length; ++t) {
            final var track = tracks[t];
            var dummy = 0;
            for (var i = 0; i < track.size(); ++i) {
                final var event = track.get(i);
                final var message = event.getMessage();
                if (message instanceof SysexMessage) {
                    continue;
                }
                if (message instanceof final MetaMessage mm) {
                    if (0x51 == mm.getType() && 3 == mm.getData().length) {
                        tempoHandler.accept(event);
                    }
                    continue;
                }
                final var sm = (ShortMessage) message;
                switch (sm.getCommand()) {
                    case ShortMessage.NOTE_OFF, ShortMessage.NOTE_ON -> noteHandler.accept(t, event);
                    case ShortMessage.PROGRAM_CHANGE -> instrumentHandler.accept(t, event);
                    case ShortMessage.CONTROL_CHANGE -> ++dummy;
                    case ShortMessage.PITCH_BEND -> ++dummy;
                    default -> throw new OcException("unknown message type : %s", sm.getCommand());
                }
            }
        }
    }
}
