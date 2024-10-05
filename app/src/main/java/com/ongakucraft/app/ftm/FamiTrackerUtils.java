package com.ongakucraft.app.ftm;

import com.ongakucraft.core.ftm.FtmChannel;
import com.ongakucraft.core.ftm.FtmInstrument;
import com.ongakucraft.core.ftm.FtmNote;
import com.ongakucraft.core.ftm.FtmSong;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public final class FamiTrackerUtils {
    private static void setChannel(List<FtmNote> mxlChannel, int instrument, int volume) {
        for (final var ftmNote : mxlChannel) {
            if (null != ftmNote) {
                ftmNote.setInstrument(instrument);
                ftmNote.setVolume(volume);
            }
        }
    }

    private static List<List<FtmNote>> getChannels(Map<Integer, Map<String, List<FtmNote>>> mxlPartToVoiceToChannel) {
        final List<List<FtmNote>> channels = new ArrayList<>();
        for (final var part : mxlPartToVoiceToChannel.keySet().stream().sorted().toList()) {
            final var mxlVoiceToChannel = mxlPartToVoiceToChannel.get(part);
            for (final var voice : mxlVoiceToChannel.keySet().stream().sorted().toList()) {
                log.info("channel : {} {}", part, voice);
                channels.add(mxlVoiceToChannel.get(voice));
            }
        }
        return channels;
    }

    private static void write(FtmSong ftmSong, String filePath) throws Exception {
        IOUtils.write(ftmSong.toString(), new FileOutputStream(filePath), StandardCharsets.UTF_8);
    }

    private static FtmSong blueClapperFromMxl(String filePath, Map<String, FtmInstrument> nameToInstrument) {
        final var mxlFile = new MxlFile(filePath);
        mxlFile.processMxl();
        final var mxlPartToVoiceToChannel = mxlFile.getMxlPartToVoiceToChannel();
        final List<FtmInstrument> instrumentList = new ArrayList<>();
        instrumentList.add(nameToInstrument.get("cookie-snow-2a03-piano"));
        instrumentList.add(nameToInstrument.get("trojan-mage-2a03-Tri Bass 1"));
        instrumentList.add(nameToInstrument.get("cookie-snow-2a03-kick bass"));
        instrumentList.add(nameToInstrument.get("cookie-smile-vrc6-chimes"));
        instrumentList.add(nameToInstrument.get("isabelle-trouble-fds-Bass"));
        final var channels = getChannels(mxlPartToVoiceToChannel);
        final var channel0 = channels.get(0);
        final var channel1 = channels.get(1);
        final var channel2 = channels.get(2);
        final var channel3 = channels.get(3);
        final var channel4 = channels.get(4);
        setChannel(channel0, 0, 8);
        setChannel(channel1, 0, 8);
        setChannel(channel2, 1, 7);
        setChannel(channel3, 3, 6);
        setChannel(channel4, 3, 6);
        final List<FtmChannel> channelList = new ArrayList<>();
        channelList.add(FtmChannel.of(channel0));
        channelList.add(FtmChannel.of(channel1));
        channelList.add(FtmChannel.of(channel2));
        channelList.add(null);
        channelList.add(null);
        channelList.add(FtmChannel.of(channel3));
        channelList.add(FtmChannel.of(channel4));
        channelList.add(null);
        final var ftmSong = FtmSong.of("BLUE CLAPPER", "Ongakucraft", "COVER Corp.",
                                       146, instrumentList, channelList);
//            log.info("channelList : {}", channelList.size());
//            log.info("{}", ftmSong);
        return ftmSong;
    }

    private static FtmSong laLionFromMxl(String filePath, Map<String, FtmInstrument> nameToInstrument) {
        final var mxlFile = new MxlFile(filePath);
        mxlFile.processMxl();
        final var mxlPartToVoiceToChannel = mxlFile.getMxlPartToVoiceToChannel();
        final List<FtmInstrument> instrumentList = new ArrayList<>();
        instrumentList.add(nameToInstrument.get("cookie-snow-2a03-piano"));
        instrumentList.add(nameToInstrument.get("trojan-mage-2a03-Tri Bass 1"));
        instrumentList.add(nameToInstrument.get("isabelle-salmon-2a03-saw synth"));
        instrumentList.add(nameToInstrument.get("cookie-smile-vrc6-chimes"));
        instrumentList.add(nameToInstrument.get("isabelle-trouble-fds-Bass"));
        final var channels = getChannels(mxlPartToVoiceToChannel);
        final var channel0 = channels.get(0);
        final var channel1 = channels.get(1);
        final var channel2 = channels.get(2);
        final var channel3 = channels.get(3);
        final var channel4 = channels.get(4); // TODO
        final var channel5 = channels.get(5); // TODO
        final var channel6 = channels.get(6); // TODO
        final var channel7 = channels.get(7); // TODO
        setChannel(channel0, 0, 8);
        setChannel(channel1, 0, 8);
        setChannel(channel2, 1, 7);
        setChannel(channel3, 1, 7);
//        setChannel(channel4, 2, 7);
        final List<FtmChannel> channelList = new ArrayList<>();
        channelList.add(FtmChannel.of(channel0));
        channelList.add(FtmChannel.of(channel1));
        channelList.add(null);
        channelList.add(null);
        channelList.add(null);
        channelList.add(FtmChannel.of(channel2));
        channelList.add(FtmChannel.of(channel3));
        channelList.add(null);
        final var ftmSong = FtmSong.of("La Lion", "Ongakucraft", "COVER Corp.",
                120, instrumentList, channelList);
//            log.info("channelList : {}", channelList.size());
//        log.info("{}", ftmSong);
        return ftmSong;
    }
/*
    private static void shinySmilyStoryFromMxl(String filePath, Map<String, FtmInstrument> nameToInstrument) {
        processMxl(filePath);
        final List<FtmInstrument> instrumentList = new ArrayList<>();
        instrumentList.add(nameToInstrument.get("cookie-snow-2a03-piano"));
        instrumentList.add(nameToInstrument.get("trojan-mage-2a03-Tri Bass 1"));
        instrumentList.add(nameToInstrument.get("cookie-snow-2a03-kick bass"));
        instrumentList.add(nameToInstrument.get("cookie-smile-vrc6-chimes"));
        instrumentList.add(nameToInstrument.get("isabelle-trouble-fds-Bass"));
        for (final var mxlChannelList : mxlPartToVoiceToChannel.values()) {
            final var channel1 = mxlChannelList.get("1");
            final var channel5 = mxlChannelList.get("5");
            setChannel(channel1, 0, 8);
            setChannel(channel5, 3, 6);
            final List<FtmChannel> channelList = new ArrayList<>();
            channelList.add(FtmChannel.of(channel1));
            channelList.add(null);
            channelList.add(null);
            channelList.add(null);
            channelList.add(null);
            channelList.add(FtmChannel.of(channel5));
            channelList.add(null);
            channelList.add(null);
            final var ftmSong = FtmSong.of("Shiny Smily Story", "Ongakucraft", "COVER Corp.",
                    168, instrumentList, channelList);
//            log.info("channelList : {}", channelList.size());
            log.info("{}", ftmSong);
        }
    }

    private static void captureTheMomentFromMxl(String filePath, Map<String, FtmInstrument> nameToInstrument) {
        processMxl(filePath);
        final List<FtmInstrument> instrumentList = new ArrayList<>();
        instrumentList.add(nameToInstrument.get("cookie-snow-2a03-piano"));
        instrumentList.add(nameToInstrument.get("trojan-mage-2a03-Tri Bass 1"));
        instrumentList.add(nameToInstrument.get("cookie-snow-2a03-kick bass"));
        instrumentList.add(nameToInstrument.get("cookie-smile-vrc6-chimes"));
        instrumentList.add(nameToInstrument.get("isabelle-trouble-fds-Bass"));
        for (final var mxlChannelList : mxlPartToVoiceToChannel.values()) {
            final var channel1 = mxlChannelList.get("1");
            final var channel5 = mxlChannelList.get("5");
            setChannel(channel1, 0, 8);
            setChannel(channel5, 3, 6);
            final List<FtmChannel> channelList = new ArrayList<>();
            channelList.add(FtmChannel.of(channel1));
            channelList.add(null);
            channelList.add(null);
            channelList.add(null);
            channelList.add(null);
            channelList.add(FtmChannel.of(channel5));
            channelList.add(null);
            channelList.add(null);
            final var ftmSong = FtmSong.of("Shiny Smily Story", "Ongakucraft", "COVER Corp.",
                    168, instrumentList, channelList);
//            log.info("channelList : {}", channelList.size());
            log.info("{}", ftmSong);
        }
    }
*/

    public static void main(String[] args) {
        try {
            final var rootDirPath = "D:/Share/LoopHero/mxl/";
            final var outputFilePath = "./data/generated/ftm/";

            // load instrument
            final var instrumentRootDirPath = "D:/Share/LoopHero/8bits/_instrument_txt";
            final var nameToInstrument = FtmInstrumentUtils.loadFtmInstruments(instrumentRootDirPath);

//            final var ftmSong = blueClapperFromMxl(rootDirPath + "5th/BLUE_CLAPPER/BLUE_CLAPPER__Hololive_IDOL_PROJECT.mxl", nameToInstrument);
//            write(ftmSong, outputFilePath + "BLUE CLAPPER.txt");

            final var ftmSong = laLionFromMxl(rootDirPath + "5th/La-Lion/La-Lion_A_song_for_Nene_made_for_Shishiro_Botan.mxl", nameToInstrument);
            write(ftmSong, outputFilePath + "La-Lion.txt");

//            checkMxl(rootDirPath + "idol/Shiny_Smily_Story/Shiny_Smily_Story_-_Hololive_IDOL_PROJECT.mxl");
//            shinySmilyStoryFromMxl(rootDirPath + "idol/Shiny_Smily_Story/Shiny_Smily_Story_-_Hololive_IDOL_PROJECT.mxl", nameToInstrument);
//            checkMxl(rootDirPath + "idol/Capture the Moment/Capture_the_Moment__Hololive_IDOL_PROJECT_Hololive_5th_Fes..mxl");// TODO fix grace
//            captureTheMomentFromMxl(rootDirPath + "idol/Capture the Moment/Capture_the_Moment__Hololive_IDOL_PROJECT_Hololive_5th_Fes..mxl", nameToInstrument);
//            checkMxl(rootDirPath + "idol/Capture the Moment/Capture_the_Moments.mxl");
        } catch (Exception e) {
            log.error("FamiTrackerApp", e);
        }
    }

    private FamiTrackerUtils() {}
}
