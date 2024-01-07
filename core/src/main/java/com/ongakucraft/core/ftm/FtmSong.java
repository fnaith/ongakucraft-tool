package com.ongakucraft.core.ftm;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class FtmSong {
    private static final int[] SPEED_LIST = {6, 5, 7, 4, 8, 3, 9};
    private static final int[] TEMPO_LIST = {150, 120, 180, 90, 210, 60, 240};
    public static FtmSong of(int bpm, List<FtmChannel> channelList) {
        var min_diff = Double.MAX_VALUE;
        var min_speed = Integer.MAX_VALUE;
        var min_tempo = Integer.MAX_VALUE;
        for (final var speed : SPEED_LIST) {
            for (final var tempo : SPEED_LIST) {
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
        return new FtmSong(min_speed, min_tempo, Collections.unmodifiableList(newChannelList), rows);
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
        final var sb = new StringBuilder();
        sb.append('\n');
        sb.append("# track HEADER block");
        sb.append('\n');
        sb.append(String.format("TRACK  64   %d %d \"SONG_NAME\"", song.getSpeed(), song.getTempo()));
        sb.append('\n');
        sb.append('\n');
        sb.append("COLUMNS :");
        for (final var channel : song.getChannelList()) {
            sb.append(' ');
            sb.append(channel.getFxCount());
        }
        sb.append('\n');
        sb.append("# track FRAMES block");
        sb.append('\n');
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
        return sb.toString();
    }

    private final int speed;
    private final int tempo;
    private final List<FtmChannel> channelList;
    private final int rows;

    @Override
    public String toString() {
        return format(this);
    }
}
