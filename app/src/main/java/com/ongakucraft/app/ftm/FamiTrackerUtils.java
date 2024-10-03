package com.ongakucraft.app.ftm;

import com.ongakucraft.app.mxl.MxlFile;
import com.ongakucraft.app.mxl.MxlNote;
import com.ongakucraft.core.ftm.FtmChannel;
import com.ongakucraft.core.ftm.FtmInstrument;
import com.ongakucraft.core.ftm.FtmNote;
import com.ongakucraft.core.ftm.FtmSong;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public final class FamiTrackerUtils {
    private static void write(FtmSong ftmSong, String filePath) throws Exception {
        IOUtils.write(ftmSong.toString(), new FileOutputStream(filePath), StandardCharsets.UTF_8);
    }

    private static FtmChannel buildChannel(MxlFile mxlFile, int partId, String voice, Function<MxlNote, FtmNote> noteConverter) {
        final Map<MxlNote, FtmNote> map = new HashMap<>();
        final var part = mxlFile.getPart(partId);
        final List<FtmNote> ftmNotes = new ArrayList<>();
        for (var beat = 0; beat < part.getBeatSize(); ++beat) {
            ftmNotes.add(null);
        }
        var row = 0;
        for (var m = 0; m < part.getMeasures().size(); ++m) {
            final var measure = part.getMeasures().get(m);
            if (measure.hasVoice(voice)) {
                final var mxlNotes = measure.getVoiceToMxlNotes().get(voice);
                for (var beat = 0; beat < measure.getBeatSize(); ++ beat) {
                    final var mxlNote = mxlNotes.get(beat);
                    if (null != mxlNote && !mxlNote.isRest()) {
                        if (mxlNote.isTieStop()) {
                            final var ftmNote = ftmNotes.get(row + beat - 1);
                            ftmNotes.set(row + beat, ftmNote);
                        } else {
                            final var ftmNote = map.computeIfAbsent(mxlNote, noteConverter);
                            ftmNotes.set(row + beat, ftmNote);
                        }
                    }
                }
            }
            row += measure.getBeatSize();
        }
        // add note cut
        row = 1;
        for (; row < ftmNotes.size(); ++row) {
            final var ftmNote = ftmNotes.get(row);
            final var prevNote = ftmNotes.get(row - 1);
            if (null == ftmNote && null != prevNote && !prevNote.isNoteCut()) {
                ftmNotes.set(row, FtmNote.NOTE_CUT);
            }
        }
        // remove note duration
        row = ftmNotes.size() - 1;
        for (; 1 <= row; --row) {
            final var ftmNote = ftmNotes.get(row);
            final var prevNote = ftmNotes.get(row - 1);
            if (prevNote == ftmNote) {
                ftmNotes.set(row, null);
            }
        }
        return FtmChannel.of(ftmNotes);
    }

    private static FtmSong asuENoKyoukaisen(String filePath, Map<String, FtmInstrument> nameToInstrument) {
        final var mxlFile = new MxlFile(filePath);
        final var partIdToVoices = mxlFile.getPartIdToVoices();
        log.info("{}", partIdToVoices);
//        {0=[1, 2, 5]}
        return null;
    }

    private static FtmSong laLion(String filePath, Map<String, FtmInstrument> nameToInstrument) {
        final var mxlFile = new MxlFile(filePath);
        final var partIdToVoices = mxlFile.getPartIdToVoices();
        log.info("{}", partIdToVoices);
//        {0=[1], 1=[1], 2=[1, 5], 3=[1, 2], 4=[1], 5=[1]}
        return null;
    }

    private static FtmSong hologramCircus(String filePath, Map<String, FtmInstrument> nameToInstrument) {
        final var mxlFile = new MxlFile(filePath);
        final var partIdToVoices = mxlFile.getPartIdToVoices();
        log.info("{}", partIdToVoices);
//        {0=[1, 2, 5]}
        return null;
    }

    private static FtmSong blueClapper(String filePath, Map<String, FtmInstrument> nameToInstrument) {
        final var mxlFile = new MxlFile(filePath);
        final var partIdToVoices = mxlFile.getPartIdToVoices();
        log.info("{}", partIdToVoices);

        final List<String> instrumentNames = new ArrayList<>();
        instrumentNames.add("cookie-snow-2a03-piano");
        instrumentNames.add("trojan-mage-2a03-Tri Bass 1");
        instrumentNames.add("cookie-snow-2a03-kick bass");
        instrumentNames.add("cookie-smile-vrc6-chimes");
        instrumentNames.add("isabelle-trouble-fds-Bass");
        final List<FtmInstrument> instruments = instrumentNames.stream().map(name -> nameToInstrument.get(name)).toList();

        // TODO https://battleofthebits.com/lyceum/View/FamiTracker+Effects+Commands
        final var channel_0_1 = buildChannel(mxlFile, 0, "1", mxlNote -> {
            var ftmNote = FtmNote.builder().key(mxlNote.getKey()).effects(List.of()).build();
            ftmNote = ftmNote.addChord(mxlNote.getChord());
            ftmNote = ftmNote.withInstrument(instrumentNames.indexOf("cookie-snow-2a03-piano"));
            ftmNote = ftmNote.withVolume(8);
            return ftmNote;
        });
        final var channel_0_2 = buildChannel(mxlFile, 0, "2", mxlNote -> {
            var ftmNote = FtmNote.builder().key(mxlNote.getKey()).effects(List.of()).build();
            ftmNote = ftmNote.addChord(mxlNote.getChord());
            ftmNote = ftmNote.withInstrument(instrumentNames.indexOf("cookie-snow-2a03-piano"));
            ftmNote = ftmNote.withVolume(8);
            return ftmNote;
        });
        final var channel_0_3 = buildChannel(mxlFile, 0, "3", mxlNote -> {
            var ftmNote = FtmNote.builder().key(mxlNote.getKey()).effects(List.of()).build();
            ftmNote = ftmNote.addChord(mxlNote.getChord());
            ftmNote = ftmNote.withInstrument(instrumentNames.indexOf("trojan-mage-2a03-Tri Bass 1"));
            ftmNote = ftmNote.withVolume(7);
            return ftmNote;
        });
        final var channel_0_5 = buildChannel(mxlFile, 0, "5", mxlNote -> {
            var ftmNote = FtmNote.builder().key(mxlNote.getKey()).effects(List.of()).build();
            ftmNote = ftmNote.addChord(mxlNote.getChord());
            ftmNote = ftmNote.withInstrument(instrumentNames.indexOf("cookie-smile-vrc6-chimes"));
            ftmNote = ftmNote.withVolume(6);
            return ftmNote;
        });
        final var channel_0_6 = buildChannel(mxlFile, 0, "6", mxlNote -> {
            var ftmNote = FtmNote.builder().key(mxlNote.getKey()).effects(List.of()).build();
            ftmNote = ftmNote.addChord(mxlNote.getChord());
            ftmNote = ftmNote.withInstrument(instrumentNames.indexOf("cookie-smile-vrc6-chimes"));
            ftmNote = ftmNote.withVolume(6);
            return ftmNote;
        });

        final List<FtmChannel> channels = new ArrayList<>();
        channels.add(channel_0_1);
        channels.add(channel_0_2);
        channels.add(channel_0_3);
        channels.add(null);
        channels.add(null);
        channels.add(channel_0_5);
        channels.add(channel_0_6);
        channels.add(null);
        final var ftmSong = FtmSong.of("BLUE CLAPPER", "Ongakucraft", "COVER Corp.",
                                       146, instruments, channels);
            log.info("channelList : {}", channels.size());
            log.info("{}", ftmSong);
        return ftmSong;
    }

    public static void main(String[] args) {
        try {
            final var rootDirPath = "D:/Share/LoopHero/mxl/";
            final var outputFilePath = "./data/generated/ftm/";

            // load instrument
            final var instrumentRootDirPath = "D:/Share/LoopHero/8bits/_instrument_txt";
            final var nameToInstrument = FtmInstrumentUtils.loadFtmInstruments(instrumentRootDirPath);

//            final var ftmSong = asuENoKyoukaisen(rootDirPath + "5th/Asu e no Kyoukaisen - Yukihana Lamy/Asu_e_no_Taisen_-_Yukihana_Lamy.mxl", nameToInstrument);
//            final var ftmSong = laLion(rootDirPath + "5th/La-Lion/La-Lion_A_song_for_Nene_made_for_Shishiro_Botan.mxl", nameToInstrument);
            final var ftmSong = hologramCircus(rootDirPath + "5th/HOLOGRAM CIRCUS - Omaru Polka/HOLOGRAM_CIRCUS.mxl", nameToInstrument);

//            final var ftmSong = blueClapper(rootDirPath + "5th/BLUE_CLAPPER/BLUE_CLAPPER__Hololive_IDOL_PROJECT.mxl", nameToInstrument);
//            write(ftmSong, outputFilePath + "BLUE CLAPPER.txt");
        } catch (Exception e) {
            log.error("FamiTrackerUtils2", e);
        }
    }

    private FamiTrackerUtils() {}
}
