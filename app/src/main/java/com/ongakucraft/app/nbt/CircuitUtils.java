package com.ongakucraft.app.nbt;

import com.ongakucraft.app.data.DataLoadingApp;
import com.ongakucraft.app.midi.MidiReader;
import com.ongakucraft.core.block.BlockDatasetVersion;
import com.ongakucraft.core.circuit.CircuitBuilder;
import com.ongakucraft.core.circuit.FindFirstInstrumentNoteConvertor;
import com.ongakucraft.core.circuit.Note;
import com.ongakucraft.core.circuit.NoteConvertor;
import com.ongakucraft.core.circuit.builder.*;
import com.ongakucraft.core.midi.MidiFileReport;
import com.ongakucraft.core.music.Music16;
import com.ongakucraft.core.music.Sequence;
import com.ongakucraft.core.structure.Position;
import com.ongakucraft.core.structure.Structure;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class CircuitUtils {
    private static final String ROOT_DIR_PATH = "./data/generated";
    private static final BlockDatasetVersion VERSION = BlockDatasetVersion.of("1.18.2", 2975);

    private static Structure happyBirthday(BlockDatasetVersion version, String inputFilePath) throws Exception {
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        log.info("tracks : {}", midiFileReport.getTrackReportList().size());
        final var trackReport = midiFileReport.getTrackReportList().get(0);
        final var track = trackReport.getTrack();
        final var noteList = track.getNoteList();
        log.info("noteList : {}", noteList.size());
        final var sixteenthNoteTicks = midiFile.getWholeNoteTicks() / 16;
        for (final var note : noteList) {
            log.info("note : key={}, on={}, duration={}", note.getKey(), note.getOn(), note.getDuration());
        }
//        for (final var note : noteList) {
//            log.info("note : key={}, on={}, duration={}", note.getKey() - 66,
//                    note.getOn() / sixteenthNoteTicks, note.getDuration() / sixteenthNoteTicks);
//        }
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var structure = new Structure();
        final var grassBlock = blockDataset.getBlock("grass_block");
        final var noteBlock = blockDataset.getBlock("note_block");
        final var repeater = blockDataset.getBlock("repeater");
        var z = 0;
        for (var i = 0; i < noteList.size(); ++i) {
            final var note = noteList.get(i);
            final var key = note.getKey() - 66;
            structure.put(Position.of(0, 0, z), grassBlock);
            structure.put(Position.of(0, 1, z), noteBlock.putProperty("note", key));
            if (i < noteList.size() - 1) {
                final var duration = note.getDuration() / sixteenthNoteTicks;
                for (var j = 0; j < duration / 4; ++j) {
                    ++z;
                    structure.put(Position.of(0, 0, z), grassBlock);
                    structure.put(Position.of(0, 1, z), repeater.putProperty("delay", 4));
                }
                if (0 != duration % 4) {
                    ++z;
                    structure.put(Position.of(0, 0, z), grassBlock);
                    structure.put(Position.of(0, 1, z), repeater.putProperty("delay", duration % 4));
                }
                ++z;
            }
        }
        return structure;
    }

    private static Structure laLion(BlockDatasetVersion version, String inputFilePath) throws Exception {
        /*
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:redstone_block
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:air
        */
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1);
        final var sequenceList = music.getSequenceList();

        final List<Structure> circuits = new ArrayList<>();
        final var convertor = FindFirstInstrumentNoteConvertor.DEFAULT;
        final CircuitBuilder circuitBuilder = FishBoneOneSideBuilder.of(blockDataset, true, "white_wool", "redstone_lamp");

        final int[][] groups = {
                {0}, {1}, {2, 3}, {4, 5}, {6}, {7}, {8}
        };
        final NoteConvertor[] convertors = {
                convertor.withOctaveModifier(-1), convertor, convertor, convertor, convertor, convertor, convertor
        };
        for (var i = 0; i < groups.length; ++i) {
            final var noteConvertor = convertors[i];
            final List<List<Note>> subSequenceList = new ArrayList<>();
            for (final var index : groups[i]) {
                subSequenceList.add(noteConvertor.convert(sequenceList.get(index)));
            }
            circuits.add(circuitBuilder.generate(subSequenceList));
        }

        final var heads = List.of(
                Position.of(20, 3, 0),
                Position.of(30, 3, 0),
                Position.of(10, 3, 0),
                Position.of(40, 3, 0),

                Position.of(25, 0, 0),
                Position.of(15, 0, 0),
                Position.of(35, 0, 0)
        );

        final var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }

        log.info("count : {}", sequenceList.stream().mapToInt(Sequence::getCount).sum());
        log.info("stat : {}", structure.stat().get("minecraft:note_block"));

        return structure;
    }

    private static Structure dreamSheep(BlockDatasetVersion version, String inputFilePath) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1);
        final var sequenceList = music.getSequenceList();

        final List<Structure> circuits = new ArrayList<>();
        final var convertor = FindFirstInstrumentNoteConvertor.DEFAULT;
        final CircuitBuilder rightBuilder = SquareWaveBuilder.of(blockDataset, true, "white_wool", "redstone_lamp");
        final CircuitBuilder leftBuilder = SquareWaveBuilder.of(blockDataset, false, "white_wool", "redstone_lamp");
        final CircuitBuilder extendedBuilder = SquareWaveExtendedBuilder.of(blockDataset, true, "white_wool", "redstone_lamp");

        final int[][] groups = {
                {0}, {1, 2}, {3}, {4}, {5}, {6}
        };
        final NoteConvertor[] convertors = {
                convertor.withOctaveModifier(1), convertor.withOctaveModifier(1), convertor.withOctaveModifier(1), convertor.withOctaveModifier(1),
                convertor.withOctaveModifier(1), convertor.withOctaveModifier(1), convertor.withOctaveModifier(1)
        };
        final CircuitBuilder[] builders = {
                leftBuilder, extendedBuilder, rightBuilder,
                leftBuilder, rightBuilder, rightBuilder
        };
        for (var i = 0; i < groups.length; ++i) {
            final List<List<Note>> subSequenceList = new ArrayList<>();
            for (final var index : groups[i]) {
                final var noteConvertor = convertors[index];
                subSequenceList.add(noteConvertor.convert(sequenceList.get(index)));
            }
            circuits.add(builders[i].generate(subSequenceList));
        }

        final var heads = List.of(
                Position.of(10, 3, 0),
                Position.of(7, 3, 0),
                Position.of(2, 3, 0),

                Position.of(9, 0, 0),
                Position.of(7, 0, 0),
                Position.of(3, 0, 0)
        );

        final var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }

        return structure;
    }

    private static Structure hopesAndDreams(BlockDatasetVersion version, String inputFilePath) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1);
        final var sequenceList = music.getSequenceList();

        final List<Structure> circuits = new ArrayList<>();
        final var convertor = FindFirstInstrumentNoteConvertor.DEFAULT;
        final CircuitBuilder rightBuilder = CheckPatternBuilder.of(blockDataset, true, "white_wool", "redstone_lamp");
        final CircuitBuilder leftBuilder = CheckPatternBuilder.of(blockDataset, false, "white_wool", "redstone_lamp");
        final CircuitBuilder extendedBuilder = CheckPatternExtendedBuilder.of(blockDataset, true, "white_wool", "redstone_lamp");

        final int[][] groups = {
                {2}, {0, 1}, {2}
        };
        final NoteConvertor[] convertors = {
                convertor, convertor, convertor, convertor
        };
        final CircuitBuilder[] builders = {
                leftBuilder, extendedBuilder, rightBuilder
        };
        for (var i = 0; i < groups.length; ++i) {
            final List<List<Note>> subSequenceList = new ArrayList<>();
            for (final var index : groups[i]) {
                final var noteConvertor = convertors[index];
                subSequenceList.add(noteConvertor.convert(sequenceList.get(index)));
            }
            circuits.add(builders[i].generate(subSequenceList));
        }

        final var heads = List.of(
                Position.of(18, 0, 0),
                Position.of(10, 0, 0),
                Position.of(1, 0, 0)
        );

        final var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }

        return structure;
    }

    public static void main(String[] args) {
        try {
            final var nbtWriter = NbtWriter.of(VERSION);

//            final var inputFilePath = String.format("%s/input/Happy_Birthday/Happy_Birthday_for_Violin.mid", ROOT_DIR_PATH);
//            final var structure = happyBirthday(VERSION, inputFilePath);
//            final var outputFilePath = String.format("%s/%s/structure/happy-birthday.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, outputFilePath);

//            final var inputFilePath = String.format("%s/input/La-Lion - Momosuzu Nene/La-Lion_A_song_for_Nene_made_for_Shishiro_Botan-cut.mid", ROOT_DIR_PATH);
//            final var structure = laLion(VERSION, inputFilePath);
//            final var outputFilePath = String.format("%s/%s/structure/la-lion.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, outputFilePath);

//            final var inputFilePath = String.format("%s/input/Dream Sheep - Tsunomaki Watame/dreamy-sheep-cut.mid", ROOT_DIR_PATH);
//            final var structure = dreamSheep(VERSION, inputFilePath);
//            final var outputFilePath = String.format("%s/%s/structure/dream-sheep.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, outputFilePath);

            final var inputFilePath = String.format("%s/input/Hopes and Dreams - Undertale/Hopes_and_Dreams-cut.mid", ROOT_DIR_PATH);
            final var structure = hopesAndDreams(VERSION, inputFilePath);
            final var outputFilePath = String.format("%s/%s/structure/hopes-and-dreams.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
            nbtWriter.write(structure, outputFilePath);
        } catch (Exception e) {
            log.error("PrefabUtils", e);
        }
    }

    private CircuitUtils() {}
}
