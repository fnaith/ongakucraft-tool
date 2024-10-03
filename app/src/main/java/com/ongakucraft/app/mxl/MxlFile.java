package com.ongakucraft.app.mxl;

import com.ongakucraft.core.OcException;
import lombok.extern.slf4j.Slf4j;
import org.audiveris.proxymusic.ScorePartwise;
import org.audiveris.proxymusic.mxl.Mxl;
import org.audiveris.proxymusic.util.Marshalling;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class MxlFile {
    // https://www.w3.org/2021/06/musicxml40/musicxml-reference/elements/note/
    private static ScorePartwise loadMxl(String filePath) {
        try (final var mif = new Mxl.Input(new File(filePath))) {
            final List<ScorePartwise> scorePartwiseList = new ArrayList<>();
            for (final var rootFile : mif.getRootFiles()) {
                final var zipEntry = mif.getEntry(rootFile.fullPath);
                final var is = mif.getInputStream(zipEntry);
                scorePartwiseList.add((ScorePartwise) Marshalling.unmarshal(is));
            }
            if (1 != scorePartwiseList.size()) {
                throw new OcException("[FamiTrackerApp][loadMxl] : %d", scorePartwiseList.size());
            }
            return scorePartwiseList.get(0);
        } catch (Exception e) {
            throw new OcException("[FamiTrackerApp][loadMxl] : %s", e.getMessage());
        }
    }

    private final MxlScore score;

    public MxlFile(String filePath) {
        this(filePath, loadMxl(filePath));
    }

    public MxlFile(String filePath, ScorePartwise scorePartwise) {
        score = new MxlScore(filePath, scorePartwise);
    }

    public Map<Integer, List<String>> getPartIdToVoices() {
        final Map<Integer, List<String>> partIdToVoices = new HashMap<>();
        for (final var part : score.getParts()) {
            partIdToVoices.put(part.getId(), part.getVoices());
        }
        return partIdToVoices;
    }

    public MxlPart getPart(int partId) {
        for (final var part : score.getParts()) {
            if (partId == part.getId()) {
                return part;
            }
        }
        return null;
    }

    private static void checkMxl(String filePath) {
        final var mxlFile = new MxlFile(filePath);
    }

    private static void check0th(String rootDirPath) {
        checkMxl(rootDirPath + "0th/Step and Go - Tokino Sora/Step_and_Go____Tokino_Sora.mxl");
//        checkMxl(rootDirPath + "0th/Kotonoha - Roboco-san/____Kotonoha_-_Roboco-san.mxl"); 32nd
        checkMxl(rootDirPath + "0th/afterglow - AZKi/afterglow.mxl");
        checkMxl(rootDirPath + "0th/Inochi - AZkI/Inochi_-_AZkI_WHiTE.mxl");
//        checkMxl(rootDirPath + "0th/Sakura Kaz - Sakura Miko/mxl (1).mxl"); 32nd
        checkMxl(rootDirPath + "0th/Sakura Kaz - Sakura Miko/Sakura_Kaze.mxl");
        checkMxl(rootDirPath + "0th/Sakura Kaz - Sakura Miko/Sakura_Kaze_Piano_Ver._-_Sakura_Miko.mxl");
//        checkMxl(rootDirPath + "0th/wii-wii-woo - Hoshimachi Suisei/_wii-wii-woo.mxl"); 32nd
//        checkMxl(rootDirPath + "0th/bibidiba - Hoshimachi Suisei/_-_.mxl"); 32nd
        checkMxl(rootDirPath + "0th/bibidiba - Hoshimachi Suisei/BIBIDIBA___Saxophone_ver..mxl");
//        checkMxl(rootDirPath + "0th/bibidiba - Hoshimachi Suisei/Bibbidiba.mxl"); 32nd
//        checkMxl(rootDirPath + "0th/Kakero - Hoshimachi Suisei/__.mxl"); 32nd
//        checkMxl(rootDirPath + "0th/SUICHAN-NO-MAINTENANCE - Hoshimachi Suisei/.mxl"); 32nd
//        checkMxl(rootDirPath + "0th/soiree - Hoshimachi Suisei/.mxl"); 32nd
        checkMxl(rootDirPath + "0th/soiree - Hoshimachi Suisei/_-_.mxl");
//        checkMxl(rootDirPath + "0th/Michizure - Hoshimachi Suisei/.mxl"); 32nd
        checkMxl(rootDirPath + "0th/Michizure - Hoshimachi Suisei/_-_.mxl");
        checkMxl(rootDirPath + "0th/Michizure - Hoshimachi Suisei/_-_Ayase.mxl");
//        checkMxl(rootDirPath + "0th/Michizure - Hoshimachi Suisei/_Michizure.mxl"); note beats should be int : 3/16
//        checkMxl(rootDirPath + "0th/Bye Bye Rainy - Hoshimachi Suisei/.mxl"); 32nd
        checkMxl(rootDirPath + "0th/GHOST - Hoshimachi Suisei/GHOST.mxl");
//        checkMxl(rootDirPath + "0th/GHOST - Hoshimachi Suisei/GHOST__.mxl"); 32nd
//        checkMxl(rootDirPath + "0th/GHOST - Hoshimachi Suisei/GHOST (1).mxl"); 32nd
        checkMxl(rootDirPath + "0th/GHOST - Hoshimachi Suisei/GHOST__Hoshimachi_Suisei.mxl");
        checkMxl(rootDirPath + "0th/GHOST - Hoshimachi Suisei/GHOST_-_HOSHIMACHI_SUISEI.mxl");
//        checkMxl(rootDirPath + "0th/GHOST - Hoshimachi Suisei/GHOST__Hoshimachi_Suisei_by_Cassi.mxl"); 32nd
//        checkMxl(rootDirPath + "0th/GHOST - Hoshimachi Suisei/GHOST_-_____Shiroha.mxl"); beat 4 should < 16
//        checkMxl(rootDirPath + "0th/Stellar Stellar - Hoshimachi Suisei/Stellar_Stellar.mxl"); 32nd
//        checkMxl(rootDirPath + "0th/Stellar Stellar - Hoshimachi Suisei/Stellar_Stellar (1).mxl"); 32nd
//        checkMxl(rootDirPath + "0th/Stellar Stellar - Hoshimachi Suisei/Stellar_Stellar (2).mxl"); 32nd
//        checkMxl(rootDirPath + "0th/Stellar Stellar - Hoshimachi Suisei/Stellar_Stellar_-_____Shiroha.mxl"); tuplet 5
//        checkMxl(rootDirPath + "0th/Stellar Stellar - Hoshimachi Suisei/Stellar_Stellar_-_Hoshimachi_Suisei.mxl"); tuplet 5
//        checkMxl(rootDirPath + "0th/Stellar Stellar - Hoshimachi Suisei/Stellar_Stellar__Hoshimachi_Suisei__THE_FIRST_TAKE_Version.mxl"); invalid tuplet 3
//        checkMxl(rootDirPath + "0th/Tenkyuu Suisei wa Yoru wo Mataide - Hoshimachi Suisei/.mxl"); 32nd
//        checkMxl(rootDirPath + "0th/Tenkyuu Suisei wa Yoru wo Mataide - Hoshimachi Suisei/__.mxl"); 64th
//        checkMxl(rootDirPath + "0th/Tenkyuu Suisei wa Yoru wo Mataide - Hoshimachi Suisei/Tenkyuu.mxl"); 32nd
//        checkMxl(rootDirPath + "0th/Next Color Planet - Hoshimachi Suisei/New_Colour_Planet.mxl"); 32nd
//        checkMxl(rootDirPath + "0th/Next Color Planet - Hoshimachi Suisei/NEXT_COLOR_PLANET.mxl"); 32nd
//        checkMxl(rootDirPath + "0th/Next Color Planet - Hoshimachi Suisei/NEXT_COLOR_PLANET_-_____Shiro.mxl"); 32nd
        checkMxl(rootDirPath + "0th/Next Color Planet - Hoshimachi Suisei/Next_Color_Planet_-_Hoshimachi_Suisei (1).mxl");
        checkMxl(rootDirPath + "0th/Next Color Planet - Hoshimachi Suisei/Next_Color_Planet_-_Hoshimachi_Suisei.mxl");
//        checkMxl(rootDirPath + "0th/Next Color Planet - Hoshimachi Suisei/Next_Color_Planet__Hoshimachi_Suisei_for_String_Orchestra.mxl"); 32nd
//        checkMxl(rootDirPath + "0th/Next Color Planet - Hoshimachi Suisei/Next_Colour_Planet.mxl"); 32nd
    }

    private static void check1th(String rootDirPath) {
//        checkMxl(rootDirPath + "1th/Shallys - Aki Rosenthal/Shallys.mxl"); 32nd
//        checkMxl(rootDirPath + "1th/Shallys - Aki Rosenthal/Shallys_-_Aki_Rosenthal.mxl"); 32nd
        checkMxl(rootDirPath + "1th/Heroine Audition - Aki Rosenthal/Heroine_Audition__Aki_Rosenthal.mxl");
        checkMxl(rootDirPath + "1th/Your Destiny Situation - Aki Rosenthal/___Aki_Rosenthal.mxl");
        checkMxl(rootDirPath + "1th/REDHEART - Akai Haato/REDHEART.mxl");
//        checkMxl(rootDirPath + "1th/REDHEART - Akai Haato/REDHEART_-_Akai_HaatoHaachama.mxl"); 32nd
//        checkMxl(rootDirPath + "1th/REDHEART - Akai Haato/REDHEART_Jazz_ver..mxl"); note type is dotted 16th
        checkMxl(rootDirPath + "1th/Infinity - Akai Haato/Infinity_-_Akai_Haato_x_Haachama.mxl");
        // TODO 夏色まつり
        // TODO 夜空メル
    }

    private static void check2th(String rootDirPath) {
        checkMxl(rootDirPath + "2th/Mage of Violet - Murasaki Shion/Mage_of_Violet_-_Murasaki_Shion.mxl");
        checkMxl(rootDirPath + "2th/Docchi Docchi Song - Nakiri Ayame/Extended.mxl");
        // TODO 癒月ちょこ
//        checkMxl(rootDirPath + "2th/Pleiades - Oozora Subaru/mxl (1).mxl"); tuplet 16t
//        checkMxl(rootDirPath + "2th/Aqua iro palette - Aqua Minato/_Aqua_Colored_Palette_-_Minato_Aqua.mxl"); 32nd
        checkMxl(rootDirPath + "2th/Aqua iro palette - Aqua Minato/_Aqua-Coloured_Palette_-__Minato_Aqua.mxl");
        checkMxl(rootDirPath + "2th/Aqua iro palette - Aqua Minato/Aqua_iro_palette_-_Aqua_Minato.mxl");
        checkMxl(rootDirPath + "2th/Aqua iro palette - Aqua Minato/arr-.mxl");
        checkMxl(rootDirPath + "2th/Kira Kira Minato Aqua/_-_.mxl");
        checkMxl(rootDirPath + "2th/Kira Kira Minato Aqua/mxl (2).mxl");
//        checkMxl(rootDirPath + "2th/I Wanna - Minato Aqua/I_Wanna_-_Minato_Aqua.mxl"); 32nd
//        checkMxl(rootDirPath + "2th/I Wanna - Minato Aqua/mxl (1).mxl"); 32nd
//        checkMxl(rootDirPath + "2th/Imada Aoi - Minato Aqua/__.mxl"); 32nd
        checkMxl(rootDirPath + "2th/Imada Aoi - Minato Aqua/imada_aoi.mxl");
//        checkMxl(rootDirPath + "2th/Imada Aoi - Minato Aqua/Imada_Aoi___-_Minato_Aqua_.mxl"); tuplet 7
//        checkMxl(rootDirPath + "2th/uni-birth - Minato Aqua/uni-birth.mxl"); tuplet 16th
    }

    private static void check3th(String rootDirPath) {
//        checkMxl(rootDirPath + "3th/Iiwake bunny - pekora/.mxl"); tuplet 7
        checkMxl(rootDirPath + "3th/Pekorandom Brain - Usada Pekora/Pekorandom_Brain.mxl");
        checkMxl(rootDirPath + "3th/Atelier - Shiranui Flare/_Atelier_-_Shiranui_Flare.mxl");
        checkMxl(rootDirPath + "3th/Homenobi - Shirogane Noel/__.mxl");
//        checkMxl(rootDirPath + "3th/ririkaru monster - Shirogane Noel/Monster.mxl"); invalid tuplet 3
//        checkMxl(rootDirPath + "3th/Im Your Treasure Box - Houshou Marine/Im_Your_Treasure_Box.mxl"); 32nd
//        checkMxl(rootDirPath + "3th/Im Your Treasure Box - Houshou Marine/Im_Your_Treasure_Box___ (1).mxl"); 32nd
//        checkMxl(rootDirPath + "3th/Im Your Treasure Box - Houshou Marine/Im_Your_Treasure_Box___.mxl"); 32nd
//        checkMxl(rootDirPath + "3th/Ahoy We are the Houshou Pirates - Houshou Marine/Ahoy_.mxl"); beat 0 should < 16
//        checkMxl(rootDirPath + "3th/Ahoy We are the Houshou Pirates - Houshou Marine/Ahoy___.mxl"); 32nd
//        checkMxl(rootDirPath + "3th/Ahoy We are the Houshou Pirates - Houshou Marine/Ahoy___-_Marine_Hosho_Hololive.mxl"); 32nd
        checkMxl(rootDirPath + "3th/Ahoy We are the Houshou Pirates - Houshou Marine/c28d004cf9c4162124a77d4434b62a0d20cc589d.mxl");
//        checkMxl(rootDirPath + "3th/Unison - Houshou Marine/UNISON.mxl"); invalid tuplet 3
//        checkMxl(rootDirPath + "3th/Marine Set Sail - Houshou Marine/.mxl"); tuplet 16th
        checkMxl(rootDirPath + "3th/omoikou - Uruha Rushia/.mxl");
//        checkMxl(rootDirPath + "3th/omoikou - Uruha Rushia/omoikou_-_Uruha_Rushia.mxl"); tuplet 16th
    }

    private static void check4th(String rootDirPath) {
//        checkMxl(rootDirPath + "4th/Tokusya-Seizon Wonder-la-der - Amane Kanata/.mxl"); invalid tuplet 3
//        checkMxl(rootDirPath + "4th/Oracle - Amane Kanata/Oracle___Amane_Kanata.mxl"); 32nd
//        checkMxl(rootDirPath + "4th/Dreamy Sheep - Tsunomaki watame/.mxl"); 32nd
        checkMxl(rootDirPath + "4th/Everlasting Soul - Tsunomaki Watame/Everlasting_Soul.mxl");
//        checkMxl(rootDirPath + "4th/mayday mayday - Tsunomaki Watame/mayday_mayday.mxl"); tuplet 5
//        checkMxl(rootDirPath + "4th/My Song - Tsunomaki Watame/My_song_-_Horie_Shota.mxl");
        checkMxl(rootDirPath + "4th/My Song - Tsunomaki Watame/My_Song_-_Tsunomaki_Watame.mxl");
        checkMxl(rootDirPath + "4th/My Song - Tsunomaki Watame/My_Song_WIP_-_Watame.mxl");
//        checkMxl(rootDirPath + "4th/Ai-mai Chocolate - Tsunomaki Watame/Ai-mai_Chocolate_-_Tsunomaki_Watame.mxl"); tuplet 16th
//        checkMxl(rootDirPath + "4th/Cloudy Sheep - Tsunomaki Watame/_Cloudy_Sheep_-_Tsunomaki_Watame.mxl"); tuplet 16th
//        checkMxl(rootDirPath + "4th/FACT - Tokoyami Towa/FACT.mxl"); note rows should be int : 3/8
        // TODO 姫森ルーナ
        checkMxl(rootDirPath + "4th/Weather Hackers - Kiryu Coco/Weather_Hackers.mxl");
        checkMxl(rootDirPath + "4th/Weather Hackers - Kiryu Coco/Weather_Hackers__Kiryu_Coco.mxl");
        checkMxl(rootDirPath + "4th/Weather Hackers - Kiryu Coco/Weather_Hackers_-_Kiryu_Coco.mxl");
//        checkMxl(rootDirPath + "4th/Weather Hackers - Kiryu Coco/Weather_Hackers__Kiryu_Coco__Instrumental.mxl"); 32nd
//        checkMxl(rootDirPath + "4th/Weather Hackers - Kiryu Coco/Weather_Hackers_Full_Band.mxl"); 32nd
//        checkMxl(rootDirPath + "4th/Kiseki Knot - hololive 4th Generation/_Kiseki_Knot__Hololive_IDOL_PROJECT.mxl"); 32nd
        checkMxl(rootDirPath + "4th/Kiseki Knot - hololive 4th Generation/Kiseki_Knot.mxl");
        checkMxl(rootDirPath + "4th/Kiseki Knot - hololive 4th Generation/Kiseki_Knot_-_hololive_4th_Generation.mxl");
//        checkMxl(rootDirPath + "4th/Kiseki Knot - hololive 4th Generation/.mxl"); tuplet 16th
    }

    private static void check5th(String rootDirPath) {
        checkMxl(rootDirPath + "5th/Asu e no Kyoukaisen - Yukihana Lamy/Asu_e_no_Taisen_-_Yukihana_Lamy-clean.mxl");
//        checkMxl(rootDirPath + "5th/Lunch with me - Momosuzu Nene/Lunch_with_Me.mxl"); 32nd
//        checkMxl(rootDirPath + "5th/Lunch with me - Momosuzu Nene/Lunch_with_me (1).mxl"); tuplet 16th
//        checkMxl(rootDirPath + "5th/Nenenenenenenene Daibakusou - Momosuzu Nene/.mxl"); 32nd
//        checkMxl(rootDirPath + "5th/Congrachumarch - Momosuzu Nene/CHU__Congrachu_March.mxl"); 32nd
        checkMxl(rootDirPath + "5th/La-Lion/La-Lion_A_song_for_Nene_made_for_Shishiro_Botan.mxl");
        checkMxl(rootDirPath + "5th/HOLOGRAM CIRCUS - Omaru Polka/HOLOGRAM_CIRCUS.mxl");
        checkMxl(rootDirPath + "5th/HOLOGRAM CIRCUS - Omaru Polka/HOLOGRAM_CIRCUS_-_Omaru_Polka.mxl");
//        checkMxl(rootDirPath + "5th/Saikyoutic Polka/.mxl"); 32nd
        checkMxl(rootDirPath + "5th/BLUE_CLAPPER/BLUE_CLAPPER__Hololive_IDOL_PROJECT.mxl");
//        checkMxl(rootDirPath + "5th/Twinkle 4 You/Twinkle_4_You_-_NePoLaBo.mxl"); tuplet 7
    }

    private static void check6th(String rootDirPath) {
//        checkMxl(rootDirPath + "6th/drop candy - La+ Darknesss/drop_candy.mxl"); 32nd
        // TODO 鷹嶺ルイ
//        checkMxl(rootDirPath + "6th/WAO - Hakui Koyori/WAO.mxl"); tuplet 16th
//        checkMxl(rootDirPath + "6th/WAO - Hakui Koyori/WAO_-_.mxl"); tuplet 16th
//        checkMxl(rootDirPath + "6th/Paralyze - Sakamata Chloe/Paralyze_-_Sakamata_Chloe.mxl"); invalid tuplet 4
//        checkMxl(rootDirPath + "6th/IrohaStep - kazama iroha/.mxl"); 32nd
    }

    private static void check7th(String rootDirPath) {
        // TODO 火威青
        // TODO 音乃瀬奏
        // TODO 一条莉々華
        // TODO 儒烏風亭らでん
        // TODO 轟はじめ
    }

    private static void checkEn1(String rootDirPath) {
//        checkMxl(rootDirPath + "en1/Ijimekko Bully - Mori Calliope/_Bully__Ijimekko_Bully_-_Mori_Calliope_.mxl"); 32nd
        checkMxl(rootDirPath + "en1/Red - Calliope Mori/Red.mxl");
//        checkMxl(rootDirPath + "en1/Red - Calliope Mori/Red (1).mxl"); 32nd
        checkMxl(rootDirPath + "en1/Excuse My Rudeness But Could You Please RIP - Calliope Mori/Excuse_My_Rudeness_But_Could_You_Please_RIP__WIP.mxl");
//        checkMxl(rootDirPath + "en1/Guh - Calliope Mori/guh.mxl"); 32nd
        checkMxl(rootDirPath + "en1/Dead Beats - Mori Calliope/Dead_Beats_-_Mori_Calliope.mxl");
//        checkMxl(rootDirPath + "en1/DO U - Takanashi Kiara/DO_U.mxl"); tuplet 16th
//        checkMxl(rootDirPath + "en1/DO U - Takanashi Kiara/DO_U_-_KIRA__Takanashi_Kiara_Orchestral_Arrangement_by_Deemo_Harlos.mxl"); tuplet 16th
        checkMxl(rootDirPath + "en1/SPARKS - Takanashi Kiara/SPARKS__Takanashi_Kiara.mxl");
        checkMxl(rootDirPath + "en1/HINOTORI - Takanashi Kiara/Hinotori.mxl");
        checkMxl(rootDirPath + "en1/HINOTORI - Takanashi Kiara/Hinotori_by_Takanashi_Kiara.mxl");
//        checkMxl(rootDirPath + "en1/HINOTORI - Takanashi Kiara/Hinotori_O.mxl"); 64th
        checkMxl(rootDirPath + "en1/Fever Night - Takanashi Kiara/Fever_Night_-_Takanashi_Kiara.mxl");
        checkMxl(rootDirPath + "en1/Violet - Ninomae Ina nis/Violet (1).mxl");
//        checkMxl(rootDirPath + "en1/Violet - Ninomae Ina nis/VIOLET (2).mxl"); 32nd
//        checkMxl(rootDirPath + "en1/Violet - Ninomae Ina nis/Violet (3).mxl"); tuplet 16th
//        checkMxl(rootDirPath + "en1/Violet - Ninomae Ina nis/Violet.mxl"); 32nd
//        checkMxl(rootDirPath + "en1/Violet - Ninomae Ina nis/Violet__seibin.mxl"); 32nd
//        checkMxl(rootDirPath + "en1/Violet - Ninomae Ina nis/HL__I.mxl"); tuplet 16th
        checkMxl(rootDirPath + "en1/MECONOPSIS - Ninomae Ina nis/MECONOPSIS.mxl");
//        checkMxl(rootDirPath + "en1/REFLECT - Gawr Gura/REFLECT_Preview_Size_-_Gawr_Gura.mxl"); tuplet 5
        checkMxl(rootDirPath + "en1/REFLECT - Gawr Gura/REFLECT_-_Gura__Test.mxl");
//        checkMxl(rootDirPath + "en1/REFLECT - Gawr Gura/REFLECT__Gawr_Gura_Reflect_-_Gawr_Gura.mxl"); 32nd
//        checkMxl(rootDirPath + "en1/REFLECT - Gawr Gura/REFLECT_-_Gawr_Gura.mxl"); 32nd
//        checkMxl(rootDirPath + "en1/REFLECT - Gawr Gura/REFLECT.mxl"); 32nd
//        checkMxl(rootDirPath + "en1/REFLECT - Gawr Gura/Reflect (1).mxl"); tuplet 5
//        checkMxl(rootDirPath + "en1/REFLECT - Gawr Gura/ORIGINAL_REFLECT_-_Gawr_Gura_-_Farhan_Sarasin.mxl"); tuplet 16th
//        checkMxl(rootDirPath + "en1/Tokyo Wabi Sabi Lullaby - Gawr Gura/Tokyo_Wabi_Sabi_Lullaby.mxl"); 32nd
        checkMxl(rootDirPath + "en1/ChikuTaku - Watson Amelia/ChikuTaku.mxl");
//        checkMxl(rootDirPath + "en1/Non-Fiction/Non-Fiction.mxl"); 32nd
//        checkMxl(rootDirPath + "en1/Non-Fiction/Non-Fiction__hololive_English_-Myth-.mxl"); 32nd
    }

    private static void checkEn2(String rootDirPath) {
//        checkMxl(rootDirPath + "en2/Let Me Stay Here - Ceres Fauna/Let_Me_Say_Here.mxl"); dotted 16th
//        checkMxl(rootDirPath + "en2/Daydream - Ouro Kronii/Daydream (1).mxl"); 32nd
//        checkMxl(rootDirPath + "en2/Daydream - Ouro Kronii/Daydream.mxl"); 32nd
//        checkMxl(rootDirPath + "en2/Daydream - Ouro Kronii/Daydream_-_Ouro_Kronii (1).mxl"); 32nd
//        checkMxl(rootDirPath + "en2/Daydream - Ouro Kronii/Daydream_-_Ouro_Kronii.mxl"); tuplet 16th
//        checkMxl(rootDirPath + "en2/A New Start - Nanashi Mumei/a_new_start (1).mxl"); invalid tuplet 4
//        checkMxl(rootDirPath + "en2/A New Start - Nanashi Mumei/A_New_Start (2).mxl"); invalid tuplet 3
//        checkMxl(rootDirPath + "en2/A New Start - Nanashi Mumei/A_New_Start.mxl"); invlaid duration
        checkMxl(rootDirPath + "en2/A New Start - Nanashi Mumei/A_New_Start_-_Nanashi_Mumei.mxl");
//        checkMxl(rootDirPath + "en2/A New Start - Nanashi Mumei/A_New_Start__Nanashi_Mumei_Ch..mxl"); 32nd
//        checkMxl(rootDirPath + "en2/mumei - Nanashi Mumei/mumei_-_TKN__Nanashi_Mumei_Piano_Solo_Arr._Harlos.mxl"); 32nd
        checkMxl(rootDirPath + "en2/mumei - Nanashi Mumei/Mumei.mxl");
//        checkMxl(rootDirPath + "en2/PLAY DICE - Hakos Baelz/PLAY_DICE__Hakos_Baelz.mxl"); 32nd
//        checkMxl(rootDirPath + "en2/PLAY DICE - Hakos Baelz/PLAY_DICE_-_Hakos_Baelz.mxl"); 32nd
        checkMxl(rootDirPath + "en2/MESS - Hakos Baelz/MESS.mxl");
//        checkMxl(rootDirPath + "en2/MESS - Hakos Baelz/MESS_-_Hakos_Baelz.mxl"); 32nd
        checkMxl(rootDirPath + "en2/Astrogirl - Tsukumo Sana/Astrogirl (1).mxl");
//        checkMxl(rootDirPath + "en2/Astrogirl - Tsukumo Sana/Astrogirl.mxl"); 32nd
//        checkMxl(rootDirPath + "en2/Astrogirl - Tsukumo Sana/Astrogirl__Tsukumo_Sana.mxl"); invalid tuplet 3
    }

    private static void checkEn3(String rootDirPath) {
        // TODO シオリ・ノヴェラ
        // TODO 古石ビジュー
        // TODO ネリッサ・レイヴンクロフト
        // TODO フワワ・アビスガード
        // TODO モココ・アビスガード
    }

    private static void checkEn4(String rootDirPath) {
        // TODO エリザベス・ローズ・ブラッドフレイム
        // TODO ジジ・ムリン
        // TODO セシリア・イマーグリーン
        // TODO ラオーラ・パンテーラ
    }

    private static void checkGamers(String rootDirPath) {
//        checkMxl(rootDirPath + "gamer/Say Fanfare - Shirakami Fubuki/Playable_Solo_Piano_Say_Say_Fanfare_-_Shirakami_Fubuki_.mxl"); invalid tuplet 3
        checkMxl(rootDirPath + "gamer/Say Fanfare - Shirakami Fubuki/Say_Fanfare.mxl");
//        checkMxl(rootDirPath + "gamer/Say Fanfare - Shirakami Fubuki/Say_Fanfare__Shirakami_Fubuki.mxl"); tuplet 5
        checkMxl(rootDirPath + "gamer/Say Fanfare - Shirakami Fubuki/Shirakami_Fubuki_-_Say.mxl");
//        checkMxl(rootDirPath + "gamer/LETTER - Shirakami Fubuki/LETTER.mxl"); 32nd
        checkMxl(rootDirPath + "gamer/LETTER - Shirakami Fubuki/LETTER_-_Shirakami_Fubuki.mxl");
//        checkMxl(rootDirPath + "gamer/Hi Fine FOX - Shirakami Fubuki/Hi_Fine_FOX__Shirakami_Fubuki.mxl"); invalid tuplet 3
//        checkMxl(rootDirPath + "gamer/KINGWORLD - Shirakami Fubuki/KINGWORLD_-_sasakure.UK___Fubuki_Piano_Solo_Arr._Harlos.mxl"); note rows should be int : 9/24
        checkMxl(rootDirPath + "gamer/KONKON Beats - Shirakami Fubuki/KONKON_Beats.mxl");
        // TODO 大神ミオ
        checkMxl(rootDirPath + "gamer/Mogu Mogu Yummy - Nekomata Okayu/YUMMY.mxl");
        checkMxl(rootDirPath + "gamer/Mogu Mogu Yummy - Nekomata Okayu/YUMMY (1).mxl");
//        checkMxl(rootDirPath + "gamer/Mogu Mogu Yummy - Nekomata Okayu/Mogu_Mogu_Yummy__Nekomata_Okayu_MOGU_MOGU_YUMMY__YUMMY.mxl"); 32nd
//        checkMxl(rootDirPath + "gamer/Saikyou Tensai Wonderful World of Korone - Inugami Korone/korones_saikyou_tensai_wonderful_world.mxl"); tuplet 16th
//        checkMxl(rootDirPath + "gamer/Saikyou Tensai Wonderful World of Korone - Inugami Korone/Saikyou_Tensai_Wonderful_World_of_Korone.mxl"); 32nd
//        checkMxl(rootDirPath + "gamer/Doggy Gods Street - Inugami Korone/Doggy_Gods_Street_-_for_Sax_Quartet.mxl"); rows should be int : 8/12
        checkMxl(rootDirPath + "gamer/Haro Haro Nariyansu - Inugami Korone/HALO_HALO_NARIYANSU_ONDO.mxl");
    }

    private static void checkHope(String rootDirPath) {
        // TODO IRyS
    }

    private static void checkId1(String rootDirPath) {
        checkMxl(rootDirPath + "id1/ALiCE&u - Ayunda Risu/Aliceu.mxl");
        checkMxl(rootDirPath + "id1/ALiCE&u - Ayunda Risu/ALiCEu_-_Ayunda_Risu.mxl");
//        checkMxl(rootDirPath + "id1/High Tide - Moona Hoshinova/High_Tide (1).mxl"); 32nd
        checkMxl(rootDirPath + "id1/High Tide - Moona Hoshinova/High_Tide.mxl");
//        checkMxl(rootDirPath + "id1/High Tide - Moona Hoshinova/High_Tide__Moona_Hoshinova.mxl"); 32nd
        checkMxl(rootDirPath + "id1/High Tide - Moona Hoshinova/High_Tide_String_Quartet_Arrangement.mxl");
        checkMxl(rootDirPath + "id1/Taut Hati – Moona Hoshinova/Taut_Hati.mxl");
        checkMxl(rootDirPath + "id1/Bersama Ioforia - Airani Iofifteen/Bersama Ioforia.mxl");
//        checkMxl(rootDirPath + "id1/Dramatic XViltration - AREA 15/Dramatic_XViltration___XViltrasi_Dramatis.mxl"); 32nd
    }

    private static void checkId2(String rootDirPath) {
        // TODO Kureiji Ollie
        // TODO Anya Melfissa
        // TODO Pavolia Reine
    }

    private static void checkId3(String rootDirPath) {
//        checkMxl(rootDirPath + "id3/You're Mine - Vestia Zeta/Youre_Mine_-_Vestia_Zeta.mxl"); 32nd
//        checkMxl(rootDirPath + "id3/BACKSEAT - Kaela Kovalskia/BACKSEAT__Kaela_Kovalskia.mxl"); 32nd
        checkMxl(rootDirPath + "id3/Mantra Hujan - Kobo Kaneru/Mantra_Hujan.mxl");
        checkMxl(rootDirPath + "id3/Mantra Hujan - Kobo Kaneru/Mantra_Hujan__Kobo_Kanaeru.mxl");
        checkMxl(rootDirPath + "id3/Mantra Hujan - Kobo Kaneru/Mantra_Hujan_-_Kobo_Kanaeru.mxl");
        checkMxl(rootDirPath + "id3/Mantra Hujan - Kobo Kaneru/Mantra_Hujan_-_Kobo_Kaneru.mxl");
        checkMxl(rootDirPath + "id3/Mantra Hujan - Kobo Kaneru/-Mantra_Hujan_-_Kobo_Kanaeru.mxl");
//        checkMxl(rootDirPath + "id3/HELP - Kobo Kanaeru/HELP.mxl"); 32nd
        checkMxl(rootDirPath + "id3/HELP - Kobo Kanaeru/HELP__Kobo_Kanaeru (1).mxl");
//        checkMxl(rootDirPath + "id3/HELP - Kobo Kanaeru/HELP__Kobo_Kanaeru.mxl"); 32nd
//        checkMxl(rootDirPath + "id3/HELP - Kobo Kanaeru/HELP__Kobo_Kanaeru__Full_Ensemble_Transcription_almost.mxl"); 32nd
    }

    private static void checkIdol(String rootDirPath) {
        checkMxl(rootDirPath + "idol/Asuiro ClearSky/ClearSky_-_hololive_IDOL_PROJECT.mxl");
//        checkMxl(rootDirPath + "idol/Candy-Go-Round/Candy-Go-Round.mxl"); 32nd
//        checkMxl(rootDirPath + "idol/Capture the Moment/Capture_the_Moments.mxl"); 32nd
        checkMxl(rootDirPath + "idol/Capture the Moment/Capture_the_Moment__Hololive_IDOL_PROJECT_Hololive_5th_Fes..mxl");
        checkMxl(rootDirPath + "idol/DAILY DIARY/DAILY_DIARY_-_hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "idol/DAILY DIARY/DAILY_DIARY_short_ver._-_Matsuri_Subaru_Miko_Noel_Marine.mxl");
        checkMxl(rootDirPath + "idol/Dreaming Days/Dreaming_Days (1).mxl");
        checkMxl(rootDirPath + "idol/Dreaming Days/DREAMING_DAYS.mxl");
        checkMxl(rootDirPath + "idol/Dreaming Days/Dreaming_Days__Hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "idol/Hyakkaryoran Hanafubuki/__Hyakka_Ryouran_Hanafubuki__hololive_IDOL_PROJECT.mxl");
//        checkMxl(rootDirPath + "idol/Hyakkaryoran Hanafubuki/__Hyakkaryoran_Hanafubuki_-_hololive_IDOL_PROJECT.mxl"); tuplet 16th
        checkMxl(rootDirPath + "idol/Hyakkaryoran Hanafubuki/_Hyakkaryouran_Hanafubuki__hololive_IDOL_PROJECT.mxl");
        checkMxl(rootDirPath + "idol/Hyakkaryoran Hanafubuki/hanafubuki.mxl");
        checkMxl(rootDirPath + "idol/Kirameki Rider/Kirameki_Rider.mxl");
        checkMxl(rootDirPath + "idol/Kirameki Rider/Kirameki_Rider_.mxl");
        checkMxl(rootDirPath + "idol/Koyoi wa Halloween Night/Halloween_Night.mxl");
//        checkMxl(rootDirPath + "idol/Koyoi wa Halloween Night/Halloween_Night_-_hololive_IDOL_PROJECT.mxl"); 32nd
//        checkMxl(rootDirPath + "idol/Non-Fiction/Non-Fiction.mxl"); 32nd
//        checkMxl(rootDirPath + "idol/Non-Fiction/Non-Fiction__hololive_English_-Myth-.mxl"); 32nd
//        checkMxl(rootDirPath + "idol/Plasmagic Seasons/Plasmagic_Seasons.mxl"); note rows should be int : 9/24
//        checkMxl(rootDirPath + "idol/Plasmagic Seasons/Plasmagic_Seasons_-_hololive_IDOL_PROJECT.mxl"); note rows should be int : 9/24
//        checkMxl(rootDirPath + "idol/Prism Melody/Prism_Melody_-_hololive_IDOL_PROJECT.mxl"); tuplet 5
        checkMxl(rootDirPath + "idol/Shijoshugi Adtruck/_Shijoshugi_Adtruck__Hololive_IDOL_PROJECT.mxl");
//        checkMxl(rootDirPath + "idol/Shijoshugi Adtruck/_Shijoshugi_Adtruck_-_hololive_IDOL_PROJECT.mxl"); 32nd
        checkMxl(rootDirPath + "idol/Shiny_Smily_Story/Shiny_Smily_Story (2).mxl");
//        checkMxl(rootDirPath + "idol/Shiny_Smily_Story/Shiny_Smily_Story.mxl"); 32nd
        checkMxl(rootDirPath + "idol/Shiny_Smily_Story/Shiny_Smily_Story_.mxl");
        checkMxl(rootDirPath + "idol/Shiny_Smily_Story/Shiny_Smily_Story_-_Hololive_IDOL_PROJECT.mxl");
//        checkMxl(rootDirPath + "idol/Shiny_Smily_Story/shiny_smily_story-hololive.mxl"); 32nd
//        checkMxl(rootDirPath + "idol/STARDUST SONG/STARDUST_SONG.mxl"); tuplet 16th
        checkMxl(rootDirPath + "idol/STARDUST SONG/STARDUST_SONG__hololive_IDOL_PROJECT.mxl");
//        checkMxl(rootDirPath + "idol/STARDUST SONG/STARDUST_SONG_-_hololive_IDOL_PROJECT.mxl"); 32nd
        checkMxl(rootDirPath + "idol/Suspect/Suspect_-_hololive_IDOL_PROJECT.mxl");
    }

    private static void checkUnit(String rootDirPath) {
        checkMxl(rootDirPath + "unit/Happiness World - BABACORN/Happiness_World.mxl");
        checkMxl(rootDirPath + "unit/PekoMiko Great War/PekoMiko_Great_War.mxl");
//        checkMxl(rootDirPath + "unit/story time - Star Flower/story_time.mxl"); 32nd
//        checkMxl(rootDirPath + "unit/story time - Star Flower/story_time__full_ver..mxl"); 32nd
        checkMxl(rootDirPath + "unit/story time - Star Flower/story_time_-_Star_Flower_Hoshimachi_Suisei_AZKi_Moona_Hoshinova_IRyS.mxl");
    }

    public static void main(String[] args) {
        try {
            final var rootDirPath = "D:/Share/LoopHero/mxl/";

            checkMxl(rootDirPath + "5th/La-Lion/La-Lion_A_song_for_Nene_made_for_Shishiro_Botan.mxl");

            check0th(rootDirPath);
            check1th(rootDirPath);
            check2th(rootDirPath);
            check3th(rootDirPath);
            check4th(rootDirPath);
            check5th(rootDirPath);
            check6th(rootDirPath);
            check7th(rootDirPath);
            checkEn1(rootDirPath);
            checkEn2(rootDirPath);
            checkEn3(rootDirPath);
            checkEn4(rootDirPath);
            checkGamers(rootDirPath);
            checkHope(rootDirPath);
            checkId1(rootDirPath);
            checkId2(rootDirPath);
            checkId3(rootDirPath);
            checkIdol(rootDirPath);
            checkUnit(rootDirPath);
        } catch (Exception e) {
            log.error("FamiTrackerApp", e);
        }
    }
}
