package com.ongakucraft.core.ftm;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ongakucraft.core.OcException;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class FtmSong {
    private static final int[] SPEED_LIST = {6, 5, 7, 4, 8, 3, 9};
    private static final int[] TEMPO_LIST = {150, 120, 180, 90, 210, 60, 240};
    public static FtmSong of(String title, String author, String copyright, int bpm,
                             List<FtmInstrument> instrumentList, List<FtmChannel> channelList) {
        var min_diff = Double.MAX_VALUE;
        var min_speed = Integer.MAX_VALUE;
        var min_tempo = Integer.MAX_VALUE;
        for (final var speed : SPEED_LIST) {
            for (final var tempo : TEMPO_LIST) {
                final var diff = Math.abs(bpm - 6.0 * tempo / speed);
                if (diff < min_diff) {
                    min_diff = diff;
                    min_speed = speed;
                    min_tempo = tempo;
                }
            }
        }
        final var rows = channelList.stream().filter(Objects::nonNull).mapToInt(FtmChannel::size).max().orElse(0);
        final List<FtmChannel> newChannelList = new ArrayList<>();
        for (final var channel : channelList) {
            if (null == channel) {
                newChannelList.add(newChannel(rows));
            } else {
                newChannelList.add(expandChannel(channel, rows));
            }
        }
        return new FtmSong(title, author, copyright, min_speed, min_tempo,
                           Collections.unmodifiableList(instrumentList),
                           Collections.unmodifiableList(newChannelList),
                           rows);
    }

    private static FtmChannel newChannel(int rows) {
        final List<FtmNote> noteList = new ArrayList<>();
        for (var i = 0; i < rows; ++i) {
            noteList.add(null);
        }
        return FtmChannel.of(noteList);
    }

    private static FtmChannel expandChannel(FtmChannel channel, int rows) {
        final List<FtmNote> noteList = new ArrayList<>();
        for (var i = 0; i < rows; ++i) {
            if (i < channel.size()) {
                noteList.add(channel.getNoteList().get(i));
            } else {
                noteList.add(null);
            }
        }
        return FtmChannel.of(noteList);
    }

    private static String format(FtmEffect fx) {
        if (null == fx) {
            return "...";
        }
        return String.format("%s%X%X", (char) fx.getParam0(), fx.getParam1(), fx.getParam2());
    }

    private static String format(FtmNote note, int fxCount) {
        final var sb = new StringBuilder();
        if (null == note) {
            sb.append("... .. .");
        } else if (FtmNote.NOTE_CUT == note.getKey()) {
            sb.append("--- .. .");
        } else {
            final var volume = 0 < note.getVolume() ? String.valueOf(note.getVolume()) : ".";
            sb.append(String.format("%s %02d %s", FtmNote.keyToName(note.getKey()), note.getInstrument(), volume));
        }
        for (var idx = 0; idx < fxCount; ++idx) {
            sb.append(' ');
            sb.append(format(null == note ? null : note.getEffect(idx)));
        }
        return sb.toString();
    }

    private static String format(FtmSong song) {
        final var rowPerPattern = 64;
        final var patterns = (song.getRows() + rowPerPattern - 1) / rowPerPattern;

        final List<String> sequences = new ArrayList<>();
        final List<String> samples = new ArrayList<>();
        final List<String> instruments = new ArrayList<>();
        final Map<String, Integer> sequenceTypeIdCounts = new HashMap<>();
        int dcpmdefCount = 0;

        for (int i = 0; i < song.getInstrumentList().size(); ++i) {
            final var instrument = song.getInstrumentList().get(i);
            if ("INST2A03".equals(instrument.getType()) || "INSTVRC6".equals(instrument.getType())) {
                final List<String> tokens = new ArrayList<>();
                tokens.add(instrument.getType());
                tokens.add(String.valueOf(i));
                for (final var sequence : instrument.getSequences()) {
                    if (null == sequence) {
                        tokens.add("-1");
                    } else {
                        final var sequenceNo = sequenceTypeIdCounts.getOrDefault(sequence.getTypeId(), 0);
                        sequences.add(String.format("%s %s %d %s", sequence.getType(), sequence.getTypeId(), sequenceNo, sequence.getParams()));
                        tokens.add(String.valueOf(sequenceNo));
                        sequenceTypeIdCounts.merge(sequence.getTypeId(), 1, Integer::sum);
                    }
                }
                tokens.add('"' + instrument.getName() + '"');
                for (int j = 0; j < instrument.getSamples().size(); ++j) {
                    final var sample = instrument.getSamples().get(j);
                    samples.add(String.format("DPCMDEF %d %s \"%s\"", dcpmdefCount, sample.getSize(), sample.getName()));
                    final var keyDcpm = instrument.getKeyDpcm().get(j).split("\s+");
                    keyDcpm[1] = String.valueOf(i);
                    keyDcpm[4] = String.valueOf(dcpmdefCount);
                    ++dcpmdefCount;
                    samples.addAll(sample.getDpcm());
                }
                instruments.add(String.join(" ", tokens));
            } else if ("INSTFDS".equals(instrument.getType())) {
                final var tokens = instrument.getContent().split("\s+");
                tokens[1] = String.valueOf(i);
                instruments.add(String.join(" ", tokens));
                for (final var fds : instrument.getFds()) {
                    final var fdsTokens = fds.split("\s+");
                    fdsTokens[1] = String.valueOf(i);
                    instruments.add(String.join(" ", fdsTokens));
                }
            } else {
                throw new OcException("format");
            }
        }
        Collections.sort(sequences);

        final var sb = new StringBuilder();
        sb.append("""
# Dn-FamiTracker text export 0.5.0.1
# Module version 0450

""");
        sb.append("# INFO block\n");
        sb.append(String.format("TITLE           \"%s\"%n", song.getTitle()));
        sb.append(String.format("AUTHOR          \"%s\"%n", song.getAuthor()));
        sb.append(String.format("COPYRIGHT       \"%s\"%n", song.getCopyright()));
        sb.append('\n');
        sb.append("# COMMENTS block\n");
        sb.append('\n');
        sb.append("""
# PARAMS block
MACHINE         0
FRAMERATE       0
EXPANSION       1
VIBRATO         1
SPLIT           32
PLAYBACKRATE    0 16639
""");
        sb.append('\n');
        sb.append("# SEQUENCES block\n");
        for (final var sequence : sequences) {
            sb.append(sequence);
            sb.append('\n');
        }
        sb.append('\n');
        sb.append("# DPCM SAMPLES block\n");
        for (final var sample : samples) {
            sb.append(sample);
            sb.append('\n');
        }
        sb.append('\n');
        sb.append("""
# DETUNETABLES block

# GROOVES block

# Tracks using default groove:
""");
        sb.append('\n');
        sb.append("# INSTRUMENTS block\n");
        for (final var instrument : instruments) {
            sb.append(instrument);
            sb.append('\n');
        }
        sb.append('\n');
        sb.append("# track HEADER block\n");
        sb.append(String.format("TRACK  64   %d %d \"SONG_NAME\"%n", song.getSpeed(), song.getTempo()));
        sb.append("COLUMNS :");
        for (final var channel : song.getChannelList()) {
            sb.append(' ');
            sb.append(channel.getFxCount());
        }
        sb.append('\n');
        sb.append('\n');
        sb.append("# track FRAMES block\n");
        for (var pattern = 0; pattern < patterns; ++pattern) {
            final var patternNo = String.format("%02x", pattern);
            sb.append("ORDER ");
            sb.append(patternNo);
            sb.append(" :");
            for (final var channel : song.getChannelList()) {
                sb.append(' ');
                sb.append(patternNo);
            }
            sb.append('\n');
        }
        sb.append('\n');
        sb.append("# track PATTERNS block\n");
        for (var pattern = 0; pattern < patterns; ++pattern) {
            sb.append(String.format("PATTERN %02x%n", pattern));
            final var start = pattern * rowPerPattern;
            final var stop = Math.min(start + rowPerPattern, song.getRows());
            for (var row = start; row < stop; ++row) {
                sb.append(String.format("ROW %02x", row % rowPerPattern));
                for (final var channel : song.getChannelList()) {
                    sb.append(" : ");
                    sb.append(format(channel.getNoteList().get(row), channel.getFxCount()));
                }
                sb.append('\n');
            }
        }
        sb.append('\n');
        sb.append("""
# track BOOKMARKS block

# JSON block
JSON "null"

# PARAMS_EMU block
USEEXTERNALOPLL 0

# End of export
""");
        return sb.toString();
    }

    private final String title;
    private final String author;
    private final String copyright;
    private final int speed;
    private final int tempo;
    private final List<FtmInstrument> instrumentList;
    private final List<FtmChannel> channelList;
    private final int rows;

    @Override
    public String toString() {
        return format(this);
    }
}
