package com.ongakucraft.app.nbt;

import com.ongakucraft.app.data.DataLoadingApp;
import com.ongakucraft.app.midi.MidiReader;
import com.ongakucraft.core.block.BlockDataset;
import com.ongakucraft.core.block.BlockDatasetVersion;
import com.ongakucraft.core.circuit.*;
import com.ongakucraft.core.circuit.builder.*;
import com.ongakucraft.core.midi.MidiFileReport;
import com.ongakucraft.core.music.Music16;
import com.ongakucraft.core.music.Sequence;
import com.ongakucraft.core.structure.Position;
import com.ongakucraft.core.structure.Range;
import com.ongakucraft.core.structure.Range3;
import com.ongakucraft.core.structure.Structure;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class CircuitUtils {
    private static final String ROOT_DIR_PATH = "./data/generated";
    private static final BlockDatasetVersion VERSION = BlockDatasetVersion.of("1.18.2", 2975);

    public static Structure buildDemoCircuits(BlockDatasetVersion version, Music16 music,
                                              int length, int rows, int xOffset, int yOffset) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var circuitBuilder = CheckPatternBuilder.of(blockDataset, true, 0, "white_wool", "redstone_lamp");
        final List<Structure> circuits = new ArrayList<>();
        for (final var staff : music.getStaffList()) {
            final var trackReport = staff.getTrackReport();
            final var lowerCount = trackReport.getLowerThanKeyNotes(KeyRange.LOWEST_KEY).size();
            final var higherCount = trackReport.getHigherThanKeyNotes(KeyRange.HIGHEST_KEY).size();
            final NoteConvertor convertor;
            if (0 < lowerCount || 0 < higherCount) {
                final var octaveModifier = trackReport.calculateAdjustableOctaves(KeyRange.LOWEST_KEY, KeyRange.HIGHEST_KEY).get(0);
                convertor = FindFirstInstrumentNoteConvertor.DEFAULT.withOctaveModifier(octaveModifier);
            } else {
                convertor = FindFirstInstrumentNoteConvertor.DEFAULT;
            }
            for (final var sequence : staff.getSequenceList()) {
                circuits.add(circuitBuilder.generate(List.of(convertor.convert(sequence))));
            }
        }
        return buildDemoCircuits(circuits, length, rows, xOffset, yOffset);
    }

    private static Structure buildDemoCircuits(List<Structure> circuits, int length, int rows,
                                              int xOffset, int yOffset) {
        final var structure = new Structure();
        final var columns = (circuits.size() + rows - 1) / rows;
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var y = (i / columns) * yOffset;
            final var x = (i % columns) * xOffset;
            circuit.translate(x, y, 0);
            structure.paste(circuit);
        }
        if (0 < length) {
            final var range3 = structure.getRange3();
            structure.cut(range3.translate(0, 0, length));
        }
        return structure;
    }

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
        final CircuitBuilder circuitBuilder = FishBoneOneSide4Builder.of(blockDataset, true, "white_wool", "redstone_lamp");

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
        final CircuitBuilder rightBuilder = SquareWaveBuilder.of(blockDataset, true, 0, "white_wool", "redstone_lamp");
        final CircuitBuilder leftBuilder = SquareWaveBuilder.of(blockDataset, false, 0, "white_wool", "redstone_lamp");
        final CircuitBuilder extendedBuilder = SquareWaveExtendedBuilder.of(blockDataset, true, 0, "white_wool", "redstone_lamp");

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

    private static Structure megalovania(BlockDatasetVersion version, String inputFilePath) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1);
        final var sequenceList = music.getSequenceList();

        final List<Structure> circuits = new ArrayList<>();
        final var convertor = FindFirstInstrumentNoteConvertor.DEFAULT;
        final CircuitBuilder circuitBuilder = TwisterOneSideBuilder.of(blockDataset, true, "white_wool", "redstone_lamp");

        final int[][] groups = {
                {0, 1, 2}
        };
        final NoteConvertor[] convertors = {
                convertor, convertor, convertor, convertor
        };
        final CircuitBuilder[] builders = {
                circuitBuilder
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
                Position.of(0, 0, 0)
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
        final CircuitBuilder rightBuilder = CheckPatternBuilder.of(blockDataset, true, 0, "white_wool", "redstone_lamp");
        final CircuitBuilder leftBuilder = CheckPatternBuilder.of(blockDataset, false, 0, "white_wool", "redstone_lamp");
        final CircuitBuilder extendedBuilder = CheckPatternExtendedBuilder.of(blockDataset, true, 0, "white_wool", "redstone_lamp");

        final int[][] groups = {
                {2}, {0, 1}, {3}
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

    private static Structure requiem(BlockDatasetVersion version, String inputFilePath) throws Exception {
/*
division / staffs / min. : 16 / 2 / 02:35
	track : 1 [piano 1]
	ticks/start/end : 1253 / 0 / 188
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |   73 |      |  488 |      |  459 |      |  233 |      |      |      |
	track : 2 []
!!!	adjustable octaves [1, 2, 3]
!!!	min/max/lower/higher : 31 / 57 / 52 / 0
	ticks/start/end : 1245 / 0 / 191
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |  486 |      |  666 |      |   41 |      |      |      |      |      |      |      |

/function ongakucraft:set_circuit
/tp @a 16 -46 -6 0 -10
/scoreboard players set @a ticks 1
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:redstone_block
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:air

/execute if entity @e[scores={ticks=1..3190}] run scoreboard players add @a ticks 1
/execute if entity @e[scores={ticks=21..3190}] as @a at @s run tp @s ~ ~ ~0.5 0 -10
/execute if entity @e[scores={ticks=3190..3200}] as @a at @s run scoreboard players set @a ticks 0
*/
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1);
        final var sequenceList = music.getSequenceList();

        final List<Structure> circuits = new ArrayList<>();
        final CircuitBuilder rightBuilderS = SquareWaveBuilder.of(blockDataset, true, 1, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderS = SquareWaveBuilder.of(blockDataset, false, 1, "barrier", "redstone_lamp");
        final CircuitBuilder rightBuilderC = CheckPatternBuilder.of(blockDataset, true, 0, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderC = CheckPatternBuilder.of(blockDataset, false, 0, "barrier", "redstone_lamp");
        final CircuitBuilder rightBuilderCD = CheckPatternBuilder.of(blockDataset, true, 1, "barrier", "redstone_lamp");

        final int[][] groups = {
                {2}, {0}, {1}, {3},
                {5}, {4}, {6},
                {2}, {0}, {1}, {3},
                {5}, {4}, {6},
                {2}, {0}, {1}, {3},
                {5}, {4}, {6},
        };
        final var convertor0 = FindFirstInstrumentNoteConvertor.of(0, Instrument.IRON_XYLOPHONE, Instrument.BELL, Instrument.GUITAR);
        final var convertor1 = FindFirstInstrumentNoteConvertor.of(1, Instrument.BASS, Instrument.HARP);
        final var convertor2 = FindFirstInstrumentNoteConvertor.of(0, Instrument.PLING, Instrument.FLUTE, Instrument.DIDGERIDOO);
        final var convertor3 = FindFirstInstrumentNoteConvertor.of(1, Instrument.BASS, Instrument.BANJO);
        final NoteConvertor[][] convertors = {
                {convertor0}, {convertor0}, {convertor0}, {convertor0},
                {convertor1}, {convertor1}, {convertor1},
                {convertor2}, {convertor2}, {convertor2}, {convertor2},
                {convertor3}, {convertor3}, {convertor3},
                {convertor2}, {convertor2}, {convertor2}, {convertor2},
                {convertor3}, {convertor3}, {convertor3},
        };
        final CircuitBuilder[] builders = {
                leftBuilderC, leftBuilderC, rightBuilderC, rightBuilderC,
                leftBuilderS, rightBuilderS, rightBuilderS,
                leftBuilderC, leftBuilderC, leftBuilderC, leftBuilderC,
                leftBuilderC, leftBuilderC, leftBuilderC,
                rightBuilderCD, rightBuilderCD, rightBuilderCD, rightBuilderCD,
                rightBuilderCD, rightBuilderCD, rightBuilderCD,
        };
        for (var i = 0; i < groups.length; ++i) {
            final List<List<Note>> subSequenceList = new ArrayList<>();
            final var group = groups[i];
            for (var j = 0; j < group.length; ++j) {
                final var noteConvertor = convertors[i][j];
                subSequenceList.add(noteConvertor.convert(sequenceList.get(group[j])));
            }
            final var struct = builders[i].generate(subSequenceList);
            struct.regulate();

            circuits.add(struct);
        }

        final var sideOffset = 15;
        final var frontOffset = 8;
        final var mid = 4;
        final var heads = List.of(
                Position.of(9 + 4, 3, 0),
                Position.of(6 + 4, 3, 0),
                Position.of(3 - 4, 3, 0),
                Position.of(0 - 4, 3, 0),
                Position.of(8, 0, 0),
                Position.of(4, 0, 0),
                Position.of(0, 0, 0),
                Position.of(mid + sideOffset + 1 - 1, 9, frontOffset),
                Position.of(mid + sideOffset + 1, 6, frontOffset),
                Position.of(mid + sideOffset + 1, 3, frontOffset),
                Position.of(mid + sideOffset + 1, 0, frontOffset),
                Position.of(mid + sideOffset + 1, -3, frontOffset),
                Position.of(mid + sideOffset + 1, -6, frontOffset),
                Position.of(mid + sideOffset + 1 - 1, -9, frontOffset),
                Position.of(mid - sideOffset + 1, 9, frontOffset),
                Position.of(mid - sideOffset, 6, frontOffset),
                Position.of(mid - sideOffset, 3, frontOffset),
                Position.of(mid - sideOffset, 0, frontOffset),
                Position.of(mid - sideOffset, -3, frontOffset),
                Position.of(mid - sideOffset, -6, frontOffset),
                Position.of(mid - sideOffset + 1, -9, frontOffset)
        );

        final var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }
        structure.regulate();

//        final var range3 = structure.getRange3();
//        return structure.cut(Range3.of(range3.getX(), range3.getY(), Range.of(50)));

        log.info("range3 : {}", structure.getRange3());

        return structure;
    }

    private static Structure hopesAndDreamsOrchestra(BlockDatasetVersion version, String inputFilePath) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1);
        final var sequenceList = music.getSequenceList();

/*
division / staffs / min. : 16 / 6 / 02:49
	track : 0 [trumpet, mutedtrumpet]
	ticks/start/end : 209 / 0 / 226
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   86 |    5 |  112 |      |    6 |      |      |      |
	track : 1 [clarinet]
	ticks/start/end : 401 / 0 / 230
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |   12 |      |  144 |   24 |  209 |    8 |    4 |      |      |      |
	track : 2 [alto sax]
	ticks/start/end : 268 / 0 / 226
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |   11 |    1 |  199 |   13 |   44 |      |      |      |      |      |
	track : 3 [trombone]
	ticks/start/end : 347 / 0 / 230
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |    9 |    2 |  205 |    3 |  128 |      |      |      |      |      |      |      |
	track : 4 [marimba]
	ticks/start/end : 170 / 30 / 61
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |   72 |      |   90 |      |    8 |      |      |      |      |      |
	track : 5 []
	ticks/start/end : 482 / 92 / 226
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |  251 |   17 |  184 |    8 |   22 |      |      |      |      |      |

/function ongakucraft:set_circuit
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:redstone_block
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:air
*/
        final List<Structure> circuits = new ArrayList<>();
        final var convertor0 = FindFirstInstrumentNoteConvertor.of(0, Instrument.PLING, Instrument.BELL);
        final var convertor1 = FindFirstInstrumentNoteConvertor.of(0, Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.SQUARE_WAVE);
        final var convertor2 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.GUITAR);
        final var convertor3 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.IRON_XYLOPHONE);
        final var convertor4 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.HARP);
        final var convertor5 = FindFirstInstrumentNoteConvertor.of(0, Instrument.IRON_XYLOPHONE, Instrument.BASS);
        final CircuitBuilder rightBuilder = FishBoneOneSide4Builder.of(blockDataset, true, "white_wool", "redstone_lamp");
        final CircuitBuilder leftBuilder = FishBoneOneSide4Builder.of(blockDataset, false, "white_wool", "redstone_lamp");

        final int[][] groups = {
                {6, 7}, {3, 1}, {0, 2}, {4, 5}
        };
        final NoteConvertor[] convertors = {
                convertor0, convertor1, convertor2, convertor3,
                convertor4, convertor4, convertor5, convertor5
        };
        final CircuitBuilder[] builders = {
                leftBuilder, leftBuilder, rightBuilder, rightBuilder
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
                Position.of(22, 0, 0),
                Position.of(12, 0, 0),
                Position.of(10, 0, 0),
                Position.of(0, 0, 0)
        );

        var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }

//        final var range3 = structure.getRange3();
//        structure = structure.cut(Range3.of(range3.getX(), range3.getY(), Range.of(50)));

        final var outputFilePath = String.format("%s/%s/structure/hopes-and-dreams-orchestra.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
        final var nbtWriter = NbtWriter.of(VERSION);
        nbtWriter.write(structure, outputFilePath);

        return structure;
    }

    private static Structure yakimochi(BlockDatasetVersion version, String inputFilePath) throws Exception {
/*
division / staffs / min. : 16 / 2 / 03:20
	track : 0 [piano 1]
	ticks/start/end : 580 / 0 / 128
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   38 |   64 |  274 |   51 |  153 |      |      |      |
	track : 1 []
	ticks/start/end : 486 / 0 / 128
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |  161 |   67 |  242 |   14 |    2 |      |      |      |      |      |
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 61- 87/513
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 67- 85/ 64
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 77- 78/  3
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  4/ 44- 68/438
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  4/ 58- 64/ 32
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  4/ 63- 63/ 16
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - demo size : 5 8 1073

/function ongakucraft:set_circuit
/tp @a 17 7 -6 0 10
/scoreboard players set @a ticks 1
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:redstone_block
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:air

/execute if entity @e[scores={ticks=1..2148}] run scoreboard players add @a ticks 1
/execute if entity @e[scores={ticks=21..2148}] as @a at @s run tp @s ~ ~ ~0.5 0 10
/execute if entity @e[scores={ticks=2148..2158}] as @a at @s run scoreboard players set @a ticks 0
*/
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1);
        final var sequenceList = music.getSequenceList();

        final List<Structure> circuits = new ArrayList<>();
        final CircuitBuilder rightBuilderS = SquareWaveBuilder.of(blockDataset, true, 1, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderS = SquareWaveBuilder.of(blockDataset, false, 1, "barrier", "redstone_lamp");
        final CircuitBuilder rightBuilderC = CheckPatternBuilder.of(blockDataset, true, 0, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderC = CheckPatternBuilder.of(blockDataset, false, 0, "barrier", "redstone_lamp");

        final int[][] groups = {
                {0}, {1}, {2},
                {3}, {4}, {5},
                {0}, {1}, {2},
                {3}, {4}, {5}
        };
        final var convertor0 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.BELL);
        final var convertor1 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.HARP);
        final var convertor2 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.FLUTE);
        final var convertor3 = FindFirstInstrumentNoteConvertor.of(0, Instrument.GUITAR, Instrument.HARP);
        final NoteConvertor[][] convertors = {
                {convertor0}, {convertor0}, {convertor0},
                {convertor1}, {convertor1}, {convertor1},
                {convertor2}, {convertor2}, {convertor2},
                {convertor3}, {convertor3}, {convertor3}
        };
        final CircuitBuilder[] builders = {
                rightBuilderS, rightBuilderS, rightBuilderS,
                leftBuilderS, leftBuilderS, leftBuilderS,
                rightBuilderC, rightBuilderC, rightBuilderC,
                leftBuilderC, leftBuilderC, leftBuilderC,
        };
        for (var i = 0; i < groups.length; ++i) {
            final List<List<Note>> subSequenceList = new ArrayList<>();
            final var group = groups[i];
            for (var j = 0; j < group.length; ++j) {
                final var noteConvertor = convertors[i][j];
                subSequenceList.add(noteConvertor.convert(sequenceList.get(group[j])));
            }
            final var struct = builders[i].generate(subSequenceList);
            struct.regulate();

            circuits.add(struct);
        }

        final var heads = List.of(
                Position.of(-2, 3, 0),
                Position.of(-2, 0, 0),
                Position.of(-6, 0, 0),
                Position.of(2 + 2, 3, 0),
                Position.of(2 + 2, 0, 0),
                Position.of(6 + 2, 0, 0),

                Position.of(-15, 6, 6),
                Position.of(-12, 3, 4),
                Position.of(-9, 0, 2),
                Position.of(15 + 3, 6, 6),
                Position.of(12 + 3, 3, 4),
                Position.of(9 + 3, 0, 2)
        );

        final var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }
        structure.regulate();

//        final var range3 = structure.getRange3();
//        return structure.cut(Range3.of(range3.getX(), range3.getY(), Range.of(50)));

        log.info("range3 : {}", structure.getRange3());
        return structure;
    }

    private static void yakimochiDesign(BlockDatasetVersion version, String inputFilePath) throws Exception {
/*
division / staffs / min. : 16 / 2 / 03:20
	track : 0 [piano 1]
	ticks/start/end : 580 / 0 / 128
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   38 |   64 |  274 |   51 |  153 |      |      |      |
	track : 1 []
	ticks/start/end : 486 / 0 / 128
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |  161 |   67 |  242 |   14 |    2 |      |      |      |      |      |
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 61- 87/513
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 67- 85/ 64
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 77- 78/  3
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  4/ 44- 68/438
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  4/ 58- 64/ 32
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  4/ 63- 63/ 16
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - demo size : 5 8 1073

/function ongakucraft:set_circuit
/tp @a 17 7 -6 0 10
/scoreboard players set @a ticks 1
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:redstone_block
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:air

/execute if entity @e[scores={ticks=1..2148}] run scoreboard players add @a ticks 1
/execute if entity @e[scores={ticks=21..2148}] as @a at @s run tp @s ~ ~ ~0.5 0 10
/execute if entity @e[scores={ticks=2148..2158}] as @a at @s run scoreboard players set @a ticks 0
*/
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1);
        final var sequenceList = music.getSequenceList();

        final List<Structure> circuits = new ArrayList<>();
        final CircuitBuilder rightBuilderS = CheckPatternBuilder.of(blockDataset, false, 0, "light_blue_wool", "redstone_lamp");
        final CircuitBuilder leftBuilderS = CheckPatternBuilder.of(blockDataset, false, 0, "light_blue_wool", "redstone_lamp");

        final int[][] groups = {
                {0}, {1}, {2},
                {3}, {4}, {5}
        };
        final var convertor0 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.BELL);
        final var convertor1 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.HARP);
        final NoteConvertor[][] convertors = {
                {convertor0}, {convertor0}, {convertor0},
                {convertor1}, {convertor1}, {convertor1}
        };
        final CircuitBuilder[] builders = {
                rightBuilderS, rightBuilderS, rightBuilderS,
                leftBuilderS, leftBuilderS, leftBuilderS
        };
        for (var i = 0; i < groups.length; ++i) {
            final List<List<Note>> subSequenceList = new ArrayList<>();
            final var group = groups[i];
            for (var j = 0; j < group.length; ++j) {
                final var noteConvertor = convertors[i][j];
                subSequenceList.add(noteConvertor.convert(sequenceList.get(group[j])));
            }
            final var struct = builders[i].generate(subSequenceList);
            struct.regulate();

            circuits.add(struct);
        }

        final var heads = List.of(
                Position.of(0, 0, 0),
                Position.of(3, 0, 0),
                Position.of(6, 0, 0),
                Position.of(9, 0, 0),
                Position.of(12, 0, 0),
                Position.of(15, 0, 0)
        );

        final var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }
        structure.regulate();

        final var outputDirPath = String.format("%s/%s/design/yakimochi", ROOT_DIR_PATH, VERSION.getMcVersion());
        DiagramUtils.drawDesignDiagram(structure, 64, outputDirPath, "1-6", "tw");
    }

    private static Structure nightDancer(BlockDatasetVersion version, String inputFilePath) throws Exception {
/*
division / staffs / min. : 16 / 2 / 03:30
	track : 0 [piano 1]
	ticks/start/end : 738 / 0 / 196
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |  141 |      |  503 |   18 |   76 |      |      |      |
	track : 1 []
	ticks/start/end : 809 / 0 / 192
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |  129 |      |  350 |    3 |  324 |      |    3 |      |      |      |      |      |
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 57- 86/597
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 62- 86/ 71
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 69- 89/ 62
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 72- 75/  8
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  5/ 31- 77/541
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  5/ 43- 68/115
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  5/ 46- 63/ 94
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  5/ 50- 63/ 59
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - demo size : 8 8 1641

/function ongakucraft:set_circuit
/tp @a 12 -52 -6 0 -6
/scoreboard players set @a ticks 1
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:redstone_block
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:air

/execute if entity @e[scores={ticks=1..3272}] run scoreboard players add @a ticks 1
/execute if entity @e[scores={ticks=21..3272}] as @a at @s run tp @s ~ ~ ~0.5 0 -6
/execute if entity @e[scores={ticks=3272..3282}] as @a at @s run scoreboard players set @a ticks 0
*/
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1);
        final var sequenceList = music.getSequenceList();

        final List<Structure> circuits = new ArrayList<>();
        final CircuitBuilder rightBuilderS = SquareWaveBuilder.of(blockDataset, true, 1, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderS = SquareWaveBuilder.of(blockDataset, false, 1, "barrier", "redstone_lamp");
        final CircuitBuilder rightBuilderC = CheckPatternBuilder.of(blockDataset, true, 0, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderC = CheckPatternBuilder.of(blockDataset, false, 0, "barrier", "redstone_lamp");

        final int[][] groups = {
                {2}, {0}, {1}, {3},
                {7}, {5}, {4}, {6}
        };
        final var convertor0 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.BELL);
        final var convertor1 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.BELL);
        final var convertor2 = FindFirstInstrumentNoteConvertor.of(0, Instrument.IRON_XYLOPHONE, Instrument.BELL);
        final var convertor3 = FindFirstInstrumentNoteConvertor.of(0, Instrument.IRON_XYLOPHONE, Instrument.BELL);
        final var convertor4 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.HARP);
        final var convertor5 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.HARP);
        final var convertor6 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.GUITAR);
        final var convertor7 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.BANJO);
        final NoteConvertor[][] convertors = {
                {convertor2}, {convertor0}, {convertor1}, {convertor3},
                {convertor7}, {convertor5}, {convertor4}, {convertor6}
        };
        final CircuitBuilder[] builders = {
                rightBuilderS, rightBuilderC, leftBuilderC, leftBuilderS,
                rightBuilderS, rightBuilderC, leftBuilderC, leftBuilderS
        };
        for (var i = 0; i < groups.length; ++i) {
            final List<List<Note>> subSequenceList = new ArrayList<>();
            final var group = groups[i];
            for (var j = 0; j < group.length; ++j) {
                final var noteConvertor = convertors[i][j];
                subSequenceList.add(noteConvertor.convert(sequenceList.get(group[j])));
            }
            final var struct = builders[i].generate(subSequenceList);
            struct.regulate();

            circuits.add(struct);
        }

        final var heads = List.of(
                Position.of(-9, 5, 6),
                Position.of(-1, 3, 0),
                Position.of(2 + 2, 3, 0),
                Position.of(9 + 2, 5, 6),

                Position.of(-10, -2, 6),
                Position.of(-1, 0, 0),
                Position.of(2 + 2, 0, 0),
                Position.of(10 + 2, -2, 6)
        );

        final var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }
        structure.regulate();

//        final var range3 = structure.getRange3();
//        return structure.cut(Range3.of(range3.getX(), range3.getY(), Range.of(50)));

        log.info("range3 : {}", structure.getRange3());
        return structure;
    }

    private static Structure megalovaniaDesign(BlockDatasetVersion version, String inputFilePath) throws Exception {
/*
sequences : 12
url : null
division / staffs / min. : 16 / 12 / 02:31
	track : 1 [flute]
	ticks/start/end : 234 / 15 / 138
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   64 |      |  166 |      |    4 |      |      |      |
	track : 2 [clarinet]
	ticks/start/end : 364 / 0 / 145
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   96 |      |  268 |      |      |      |      |      |
	track : 3 [clarinet]
	ticks/start/end : 264 / 15 / 136
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |  120 |      |  144 |      |      |      |      |      |      |      |
	track : 4 [oboe]
	ticks/start/end : 308 / 15 / 145
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   80 |      |  228 |      |      |      |      |      |
	track : 5 [bassoon]
	ticks/start/end : 336 / 7 / 136
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |  336 |      |      |      |      |      |      |      |      |      |
	track : 6 [alto sax]
	ticks/start/end : 328 / 15 / 145
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |  116 |      |  212 |      |      |      |      |      |
	track : 7 [tenor sax]
	ticks/start/end : 239 / 7 / 138
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |  205 |      |   34 |      |      |      |      |      |
	track : 8 [trumpet, mutedtrumpet]
	ticks/start/end : 302 / 0 / 145
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |   40 |      |  134 |      |  116 |    2 |   10 |      |      |      |
	track : 9 [trombone, mutedtrumpet]
	ticks/start/end : 522 / 0 / 145
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |  402 |      |  120 |      |      |      |      |      |      |      |
	track : 10 [tuba]
	ticks/start/end : 668 / 0 / 145
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |  668 |      |      |      |      |      |      |      |      |      |
	track : 11 [marimba]
	ticks/start/end : 280 / 0 / 145
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   72 |      |  208 |      |      |      |      |      |
	track : 12 [trumpet, mutedtrumpet]
	ticks/start/end : 188 / 7 / 46
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |    8 |      |  180 |      |      |      |      |      |      |      |
division / staffs / min. : 16 / 12 / 02:31

[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 65- 81/234
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  2/ 65- 77/364
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  3/ 46- 57/264
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  4/ 65- 77/308
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  5/ 46- 50/336
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  6/ 55- 77/328
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  7/ 58- 74/239
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  8/ 53- 81/302
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  9/ 46- 57/522
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 10/ 46- 51/668
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 11/ 65- 77/280
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 12/ 53- 62/188
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - demo size : 11 8 1217

/function ongakucraft:set_circuit
/tp @a 12 -52 -6 0 -6
/scoreboard players set @a ticks 1
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:redstone_block
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:air

/execute if entity @e[scores={ticks=1..3272}] run scoreboard players add @a ticks 1
/execute if entity @e[scores={ticks=21..3272}] as @a at @s run tp @s ~ ~ ~0.5 0 -6
/execute if entity @e[scores={ticks=3272..3282}] as @a at @s run scoreboard players set @a ticks 0
*/
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1);
        final var sequenceList = music.getSequenceList();

        final List<Structure> circuits = new ArrayList<>();
        final var convertor0 = FindFirstInstrumentNoteConvertor.of(0, Instrument.SQUARE_WAVE, Instrument.FLUTE);
        final var convertor1 = FindFirstInstrumentNoteConvertor.of(2, Instrument.CHIME);
        final var convertor2 = FindFirstInstrumentNoteConvertor.of(0, Instrument.GUITAR); // 321-383,449-511
        final var convertor3 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BANJO);
        final var convertor4 = FindFirstInstrumentNoteConvertor.of(0, Instrument.DIDGERIDOO);
        final var convertor5 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP);
        final var convertor6 = FindFirstInstrumentNoteConvertor.of(0, Instrument.PLING);
        final var convertor7 = FindFirstInstrumentNoteConvertor.of(0, Instrument.COW_BELL, Instrument.IRON_XYLOPHONE, Instrument.HIHAT);
        final var convertor8 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HIHAT);
        final var convertor9 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS);
        final var convertor10 = FindFirstInstrumentNoteConvertor.of(2, Instrument.XYLOPHONE);
        final var convertor11 = FindFirstInstrumentNoteConvertor.of(0, Instrument.GUITAR);
//        final var circuitBuilder = CheckPatternBuilder.of(blockDataset, true, 0, "white_wool", "redstone_lamp");
        final CircuitBuilder rightBuilder = FishBoneOneSide4Builder.of(blockDataset, true, "magenta_glazed_terracotta", "redstone_lamp");
        final CircuitBuilder leftBuilder = FishBoneOneSide4Builder.of(blockDataset, false, "magenta_glazed_terracotta", "redstone_lamp");

        final int[][] groups = {
                {0, 11}, {1, 2}, {3, 4}, {5, 6}, {7, 8}, {9, 10}
        };
        final NoteConvertor[][] convertors = {
                { convertor0, convertor11 },
                { convertor1, convertor2 },
                { convertor3, convertor4 },
                { convertor5, convertor6 },
                { convertor7, convertor8 },
                { convertor9, convertor10 }
        };
        final CircuitBuilder[] builders = {
                rightBuilder, rightBuilder, rightBuilder,
                leftBuilder, leftBuilder, leftBuilder
        };
        final var heads = List.of(
                Position.of(-11, 0, 0),
                Position.of(-1, 0, 0),
                Position.of(-21, 0, 0),
                Position.of(11, 0, 0),
                Position.of(21, 0, 0),
                Position.of(1, 0, 0)
        );
        final int[] orders = { 4, 3, 5, 1, 0, 2 };
        /*final int[][] groups = {
                {0}, {1}, {2}, {3},
                {4}, {5}, {6}, {7},
                {8}, {9}, {10}, {11}
        };
        final NoteConvertor[][] convertors = {
                { convertor0 }, { convertor1 }, { convertor2 }, { convertor3 },
                { convertor4 }, { convertor5 }, { convertor6 }, { convertor7 },
                { convertor8 }, { convertor9 }, { convertor10 }, { convertor11 }
        };
        final CircuitBuilder[] builders = {
                circuitBuilder, circuitBuilder, circuitBuilder, circuitBuilder,
                circuitBuilder, circuitBuilder, circuitBuilder, circuitBuilder,
                circuitBuilder, circuitBuilder, circuitBuilder, circuitBuilder
        };
        final var heads = List.of(
                Position.of(0, 0, 0),
                Position.of(3, 0, 0),
                Position.of(6, 0, 0),
                Position.of(9, 0, 0),

                Position.of(0, 3, 0),
                Position.of(3, 3, 0),
                Position.of(6, 3, 0),
                Position.of(9, 3, 0),

                Position.of(0, 6, 0),
                Position.of(3, 6, 0),
                Position.of(6, 6, 0),
                Position.of(9, 6, 0)
        );*/

        for (var i = 0; i < groups.length; ++i) {
            final List<List<Note>> subSequenceList = new ArrayList<>();
            final var group = groups[i];
            final var noteConvertorList = convertors[i];
            for (var j = 0; j < group.length; ++j) {
                final var id = group[j];
                final var noteConvertor = noteConvertorList[j];
                subSequenceList.add(noteConvertor.convert(sequenceList.get(id)));
            }
            final var circuit = builders[i].generate(subSequenceList);
            circuits.add(circuit);
            final var outputDirPath = String.format("%s/%s/design/Megalovania - Undertale", ROOT_DIR_PATH, VERSION.getMcVersion());
            DiagramUtils.drawDesignDiagram(circuit, 64, outputDirPath, String.valueOf(orders[i] + 1), "tw");
        }

        final var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }

//        final var range3 = structure.getRange3();
//        return structure.cut(Range3.of(range3.getX(), range3.getY(), Range.of(50)));

        final var empty = new Structure();
        empty.fill(structure.getRange3(), blockDataset.getBlock("air"));
        empty.paste(structure);
        structure.paste(empty);

        return structure;
    }

    private static Structure heyYa(BlockDatasetVersion version, String inputFilePath) throws Exception {
/*
division / staffs / min. : 8 / 10 / 01:41
	track : 0 [trumpet, muted trumpet]
	ticks/start/end : 228 / 0 / 129
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |    1 |      |  203 |    5 |   19 |      |      |      |
	track : 1 [trumpet, muted trumpet]
	ticks/start/end : 239 / 0 / 129
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   16 |   19 |  198 |    2 |    4 |      |      |      |
	track : 2 [french horn]
	ticks/start/end : 261 / 0 / 129
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   59 |   11 |  191 |      |      |      |      |      |
	track : 3 [alto sax]
	ticks/start/end : 266 / 0 / 129
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   12 |    3 |  251 |      |      |      |      |      |
	track : 4 [tenor sax]
	ticks/start/end : 155 / 0 / 129
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |  155 |      |      |      |      |      |      |      |
	track : 5 [baritone sax]
	ticks/start/end : 256 / 0 / 129
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |  256 |      |      |      |      |      |      |      |      |      |
	track : 6 [trombone]
	ticks/start/end : 256 / 0 / 129
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |    3 |      |  253 |      |      |      |      |      |      |      |
	track : 7 [trombone]
	ticks/start/end : 255 / 0 / 129
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |  255 |      |      |      |      |      |      |      |      |      |
	track : 8 [tuba]
	ticks/start/end : 199 / 0 / 129
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |  199 |      |      |      |      |      |      |      |
	track : 9 [tuba]
	ticks/start/end : 256 / 0 / 129
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |  256 |      |      |      |      |      |      |      |      |      |      |      |

division / staffs / min. : 8 / 10 / 01:41
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 64- 83/226
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 83- 83/  1
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 88- 88/  1
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  4/ 59- 76/226
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  4/ 71- 79/ 13
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  6/ 62- 76/261
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  7/ 62- 76/266
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  8/ 57- 64/155
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  9/ 43- 52/256
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 10/ 52- 64/256
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 11/ 43- 52/255
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 12/ 55- 64/199
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 13/ 31- 40/256
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - demo size : 14 8 1081

   9 8 7 6   2 3 4 5
       1 0 0 0 1

/function ongakucraft:set_circuit
/tp @a 16 -54 -3 0 20
/scoreboard players set @a ticks 1
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:redstone_block
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:air

/execute if entity @e[scores={ticks=1..2174}] run scoreboard players add @a ticks 1
/execute if entity @e[scores={ticks=21..2174}] as @a at @s run tp @s ~ ~ ~0.5 0 20
/execute if entity @e[scores={ticks=2174..2184}] as @a at @s run scoreboard players set @a ticks 0
*/
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1);
        final var sequenceList = music.getSequenceList();

        final List<Structure> circuits = new ArrayList<>();
        final CircuitBuilder rightBuilderS = SquareWaveBuilder.of(blockDataset, true, 1, "barrier", "redstone_lamp");
        final CircuitBuilder rightBuilderC = CheckPatternBuilder.of(blockDataset, true, 0, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderC = CheckPatternBuilder.of(blockDataset, false, 0, "barrier", "redstone_lamp");

        final int[][] groups = {
                {0}, {1}, {2}, {3}, {4},
                {5}, {6}, {7}, {8}, {9},
                {10}, {11}, {12}
        };
        final var convertor0 = FindFirstInstrumentNoteConvertor.of(1, Instrument.XYLOPHONE, Instrument.HARP);
        final var convertor1 = FindFirstInstrumentNoteConvertor.of(1, Instrument.XYLOPHONE);
        final var convertor2 = FindFirstInstrumentNoteConvertor.of(1, Instrument.XYLOPHONE);
        final var convertor3 = FindFirstInstrumentNoteConvertor.of(-1, Instrument.IRON_XYLOPHONE, Instrument.HIHAT);
        final var convertor4 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BELL, Instrument.COW_BELL);
        final var convertor5 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BANJO); // 2
        final var convertor6 = FindFirstInstrumentNoteConvertor.of(0, Instrument.PLING);
        final var convertor7 = FindFirstInstrumentNoteConvertor.of(0, Instrument.GUITAR);
        final var convertor8 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS);
        final var convertor9 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HIHAT);
        final var convertor10 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS_DRUM);
        final var convertor11 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP);
        final var convertor12 = FindFirstInstrumentNoteConvertor.of(0, Instrument.DIDGERIDOO);
        final NoteConvertor[][] convertors = {
                {convertor0}, {convertor1}, {convertor2}, {convertor3}, {convertor4},
                {convertor5}, {convertor6}, {convertor7}, {convertor8}, {convertor9},
                {convertor10}, {convertor11}, {convertor12}
        };
        final CircuitBuilder[] builders = {
                rightBuilderS, rightBuilderC, leftBuilderC, leftBuilderC, rightBuilderC,
                rightBuilderC, rightBuilderC, rightBuilderC, rightBuilderC, leftBuilderC,
                leftBuilderC, leftBuilderC, leftBuilderC
        };
        for (var i = 0; i < groups.length; ++i) {
            final List<List<Note>> subSequenceList = new ArrayList<>();
            final var group = groups[i];
            for (var j = 0; j < group.length; ++j) {
                final var noteConvertor = convertors[i][j];
                subSequenceList.add(noteConvertor.convert(sequenceList.get(group[j])));
            }
            final var struct = builders[i].generate(subSequenceList);
            struct.regulate();

            circuits.add(struct);
        }
        final var offset = 2;
        final var heads = List.of(
                Position.of(0, 0, 0),
                Position.of(-3, 0, 2),
                Position.of(4, 0, 2),

                Position.of(8, 0, 4),
                Position.of(-7, 0, 4),

                Position.of(-3 - offset, 2, 7),
                Position.of(-6 - offset, 3, 7),
                Position.of(-10 - offset, 4, 9),
                Position.of(-13 - offset, 6, 11),

                Position.of(14 + offset, 6, 11),
                Position.of(11 + offset, 4, 9),
                Position.of(7 + offset, 3, 7),
                Position.of(4 + offset, 2, 7)
        );

        final var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }
        structure.regulate();

//        final var range3 = structure.getRange3();
//        return structure.cut(Range3.of(range3.getX(), range3.getY(), Range.of(50)));

        log.info("range3 : {}", structure.getRange3());
        return structure;
    }

    private static Structure highHopes(BlockDatasetVersion version, String inputFilePath) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1);
        final var sequenceList = music.getSequenceList();
/*
sequences : 11
url : https://musescore.com/user/30069197/scores/5504406
division / staffs / min. : 16 / 11 / 01:32
	track : 0 [flute]
	ticks/start/end : 95 / 47 / 121
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   17 |      |   39 |      |   39 |      |      |      |
	track : 1 [clarinet]
	ticks/start/end : 86 / 46 / 88
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |    6 |      |   80 |      |      |      |      |      |
	track : 2 [alto sax]
	ticks/start/end : 96 / 11 / 107
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |    7 |      |   85 |      |    4 |      |      |      |
	track : 3 [tenor sax]
	ticks/start/end : 241 / 0 / 120
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |   78 |      |  163 |      |      |      |      |      |      |      |
	track : 4 [trumpet, mutedtrumpet]
	ticks/start/end : 230 / 14 / 121
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   78 |      |  152 |      |      |      |      |      |
	track : 5 [trumpet, mutedtrumpet]
	ticks/start/end : 229 / 0 / 120
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |  153 |      |   76 |      |      |      |      |      |
	track : 6 [french horns]
	ticks/start/end : 89 / 11 / 107
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |    6 |      |   77 |      |    6 |      |      |      |      |      |
	track : 7 [trombone]
	ticks/start/end : 13 / 11 / 107
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |    3 |      |   10 |      |      |      |      |      |      |      |
	track : 8 [french horns]
	ticks/start/end : 156 / 0 / 121
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |  100 |      |   56 |      |      |      |      |      |      |      |
	track : 9 [baritone sax]
	ticks/start/end : 163 / 0 / 121
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |   84 |      |   79 |      |      |      |      |      |      |      |      |      |
	track : 10 [tuba]
	ticks/start/end : 245 / 0 / 121
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |  128 |      |  117 |      |      |      |      |      |      |      |      |      |

[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 62- 84/ 95
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  2/ 65- 77/ 86
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  3/ 62- 81/ 96
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  4/ 52- 65/241
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  5/ 60- 76/230
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  6/ 59- 70/229
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  7/ 53- 69/ 89
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  8/ 50- 63/ 13
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  9/ 48- 58/156
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 10/ 38- 48/163
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 11/ 38- 48/245

/function ongakucraft:set_circuit
/tp @a 17 -55 -4 0 15
/scoreboard players set @a ticks 1
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:redstone_block
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:air

/execute if entity @e[scores={ticks=1..2048}] run scoreboard players add @a ticks 1
/execute if entity @e[scores={ticks=21..2048}] as @a at @s run tp @s ~ ~ ~0.5 0 15
/execute if entity @e[scores={ticks=2048..2058}] as @a at @s run scoreboard players set @a ticks 0
*/
        final List<Structure> circuits = new ArrayList<>();
        final var convertor0 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.FLUTE);
        final var convertor1 = FindFirstInstrumentNoteConvertor.of(0, Instrument.IRON_XYLOPHONE);
        final var convertor2 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.COW_BELL);
        final var convertor3 = FindFirstInstrumentNoteConvertor.of(0, Instrument.GUITAR);
        final var convertor4 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP);
        final var convertor5 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BANJO);
        final var convertor6 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.PLING);
        final var convertor7 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HIHAT);
        final var convertor8 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.HARP);
        final var convertor9 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS);
        final var convertor10 = FindFirstInstrumentNoteConvertor.of(0, Instrument.DIDGERIDOO);
        final CircuitBuilder rightBuilderS = SquareWaveBuilder.of(blockDataset, true, 1, "barrier", "redstone_lamp");
        final CircuitBuilder rightBuilderC = CheckPatternBuilder.of(blockDataset, true, 0, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderC = CheckPatternBuilder.of(blockDataset, false, 0, "barrier", "redstone_lamp");

        final int[][] groups = {
                {0}, {1}, {2},
                {5}, {3}, {4}, {6},
                {8}, {7}, {9}, {10}
        };
        final NoteConvertor[] convertors = {
                convertor0, convertor1, convertor2, convertor3,
                convertor4, convertor5, convertor6, convertor7,
                convertor8, convertor9, convertor10
        };
        final CircuitBuilder[] builders = {
                leftBuilderC, rightBuilderS, rightBuilderC,
                leftBuilderC, leftBuilderC, leftBuilderC, leftBuilderC,
                rightBuilderC, rightBuilderC, rightBuilderC, rightBuilderC
        };
        for (var i = 0; i < groups.length; ++i) {
            final List<List<Note>> subSequenceList = new ArrayList<>();
            for (final var index : groups[i]) {
                final var noteConvertor = convertors[index];
                subSequenceList.add(noteConvertor.convert(sequenceList.get(index)));
            }
            circuits.add(builders[i].generate(subSequenceList));
        }

        final var gap = 5;
        final var heads = List.of(
                Position.of(-6, 0, 1),
                Position.of(1, 0, 0),
                Position.of(6, 0, 1),

                Position.of(-gap - 8, 0, 6),
                Position.of(-gap - 12, 2, 12),
                Position.of(-gap - 8, 3, 6),
                Position.of(-gap - 12, 5, 12),

                Position.of(gap + 8, 0, 6),
                Position.of(gap + 12, 2, 12),
                Position.of(gap + 8, 3, 6),
                Position.of(gap + 12, 5, 12)
        );

        var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }

//        final var range3 = structure.getRange3();
//        structure = structure.cut(Range3.of(range3.getX(), range3.getY(), Range.of(50)));

        final var outputFilePath = String.format("%s/%s/structure/high-hopes.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
        final var nbtWriter = NbtWriter.of(VERSION);
        nbtWriter.write(structure, outputFilePath);

        log.info("range3 : {}", structure.getRange3());
        return structure;
    }

    private static Structure livingLillennium(BlockDatasetVersion version, String inputFilePath) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var timeScale = 3;
        final var midiFile = MidiReader.read(inputFilePath, timeScale);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1);
        final var sequenceList = music.getSequenceList();
/*
sequences : 29
(0) path : D:\Sync\Ongakucraft\midi\living millennium - iyowa\1000_-_-clean.mid
url : null
division / staffs / min. : 16 / 12 / 03:09
	track : 1 [piano 1]
	ticks/start/end : 556 / 4 / 462
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |    5 |      |  116 |    3 |  226 |   17 |  189 |      |      |      |
	track : 2 []
	ticks/start/end : 533 / 4 / 455
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |   11 |      |  148 |   16 |  108 |   15 |  234 |    1 |      |      |
	track : 3 [piano 1]
	ticks/start/end : 620 / 28 / 454
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   26 |    2 |  252 |   38 |  296 |      |    6 |      |
	track : 4 []
	ticks/start/end : 435 / 50 / 455
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |   10 |    2 |  117 |    9 |  142 |   11 |  123 |    3 |   18 |      |
	track : 5 [piano 1]
	ticks/start/end : 1835 / 5 / 500
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |   26 |   19 |  980 |   96 |  638 |    1 |   75 |      |      |      |
	track : 6 []
!!!	adjustable octaves [1, 2, 3]
!!!	min/max/lower/higher : 30 / 62 / 1 / 0
	ticks/start/end : 682 / 5 / 506
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|    8 |  199 |   23 |  330 |   15 |  106 |      |      |      |      |      |      |      |
	track : 7 [piano 1]
	ticks/start/end : 252 / 120 / 449
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   86 |   16 |  150 |      |      |      |      |      |
	track : 8 []
!!!	adjustable octaves [1, 2, 3]
!!!	min/max/lower/higher : 30 / 65 / 14 / 0
	ticks/start/end : 642 / 28 / 495
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|   14 |  362 |   25 |  194 |    9 |   24 |      |      |      |      |      |      |      |
	track : 9 [piano 1]
	ticks/start/end : 76 / 408 / 500
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |      |      |   24 |   10 |   42 |      |      |      |
	track : 10 []
	ticks/start/end : 38 / 408 / 454
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   12 |    5 |   21 |      |      |      |      |      |
	track : 11 [piano 1]
	ticks/start/end : 654 / 5 / 501
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |  622 |   12 |   19 |      |    1 |      |      |      |      |      |      |      |
	track : 12 [piano 1]
	ticks/start/end : 633 / 5 / 501
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |    3 |  492 |  138 |      |      |      |      |      |      |      |      |      |
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 49- 87/548
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 61- 72/  8
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  3/ 48- 90/503
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  3/ 52- 72/ 29
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  5/ 56- 93/551
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  5/ 62- 87/ 47
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  5/ 68- 79/ 18
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  8/ 48- 92/221
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  8/ 53- 94/110
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  8/ 62- 94/ 89
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  8/ 67- 87/ 13
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 12/ 51- 88/642
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 12/ 55- 84/525
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 12/ 60- 86/492
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 12/ 63- 84/156
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 12/ 70- 85/ 11
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 12/ 71- 87/  6
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 12/ 88- 88/  2
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 12/ 89- 89/  1
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 20/ 27- 62/384
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 20/ 39- 58/271
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 20/ 53- 58/ 25
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 23/ 55- 75/252
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 24/ 29- 65/641
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 25/ 73- 85/ 76
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 26/ 61- 73/ 38
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 27/ 36- 49/479
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 27/ 38- 55/173
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 29/ 42- 51/630

/function ongakucraft:set_circuit
/tp @a 36 -35 0 0 90
/scoreboard players set @a ticks 1
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:redstone_block
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:air

/execute if entity @e[scores={ticks=1..8431}] run scoreboard players add @a ticks 1
/execute if entity @e[scores={ticks=21..8431}] as @a at @s run tp @s ~ ~ ~0.3333 0 90
/execute if entity @e[scores={ticks=8431..8441}] as @a at @s run scoreboard players set @a ticks 0

// 1 bpm 100 / tick rate 30
// 46 bpm 200 / 45*16*3/3*2 = *1,440* / tick rate 60
// 64 bpm 100 / 63*16*3/3*2 = *2,016* / tick rate 30
*/
        final List<Structure> circuits = new ArrayList<>();
        final var convertor0 = FindFirstInstrumentNoteConvertor.of(0, Instrument.GUITAR, Instrument.FLUTE);
        final var convertor1 = FindFirstInstrumentNoteConvertor.of(0, Instrument.GUITAR, Instrument.FLUTE);
        final var convertor2 = FindFirstInstrumentNoteConvertor.of(0, Instrument.SQUARE_WAVE, Instrument.CHIME);
        final var convertor22 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.CHIME);
        final var convertor3 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.HARP, Instrument.FLUTE, Instrument.BELL);
        final var convertor4 = FindFirstInstrumentNoteConvertor.of(1, Instrument.HARP, Instrument.XYLOPHONE);
        final var convertor42 = FindFirstInstrumentNoteConvertor.of(1, Instrument.HARP, Instrument.BELL);
        final var convertor5 = FindFirstInstrumentNoteConvertor.of(1, Instrument.BASS, Instrument.SQUARE_WAVE);
        final var convertor52 = FindFirstInstrumentNoteConvertor.of(1, Instrument.BASS, Instrument.HARP);
        final var convertor6 = FindFirstInstrumentNoteConvertor.of(1, Instrument.FLUTE);
        final var convertor7 = FindFirstInstrumentNoteConvertor.of(1, Instrument.BANJO, Instrument.DIDGERIDOO);
        final var convertor8 = FindFirstInstrumentNoteConvertor.of(0, Instrument.COW_BELL);
        final var convertor9 = FindFirstInstrumentNoteConvertor.of(0, Instrument.IRON_XYLOPHONE);
        final var convertor10 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.GUITAR);
        final var convertor11 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS_DRUM);
        final CircuitBuilder rightBuilderF = FishBoneOneSide3Builder.of(blockDataset, true, "barrier", "redstone_lamp");

        final int[][] groups = {
                {0}, {1},
                {2}, {3},
                {4}, {5}, {6},
                {7}, {8}, {9}, {10},
                {11}, {12}, {13}, {14}, {15}, {16}, {17}, {18},
                {19}, {20}, {21},
                {22},
                {23},
                {24},
                {25},
                {26}, {27},
                {28}
        };
        final NoteConvertor[] convertors = {
                convertor0, convertor0,
                convertor1, convertor1,
                convertor2, convertor22, convertor22,
                convertor3, convertor3, convertor3, convertor3,
                convertor4, convertor4, convertor42, convertor42, convertor42, convertor42, convertor42, convertor42,
                convertor5, convertor52, convertor52,
                convertor6,
                convertor7,
                convertor8,
                convertor9,
                convertor10, convertor10,
                convertor11
        };
        final CircuitBuilder[] builders = {
                rightBuilderF, rightBuilderF,
                rightBuilderF, rightBuilderF,
                rightBuilderF, rightBuilderF, rightBuilderF,
                rightBuilderF, rightBuilderF, rightBuilderF, rightBuilderF,
                rightBuilderF, rightBuilderF, rightBuilderF, rightBuilderF, rightBuilderF, rightBuilderF, rightBuilderF, rightBuilderF,
                rightBuilderF, rightBuilderF, rightBuilderF,
                rightBuilderF,
                rightBuilderF,
                rightBuilderF,
                rightBuilderF,
                rightBuilderF, rightBuilderF,
                rightBuilderF
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
                Position.of(0, 0, 0), Position.of(0, 3, 0),
                Position.of(7, 0, 0), Position.of(7, 3, 0),
                Position.of(14, 0, 0), Position.of(14, 3, 0), Position.of(14, 6, 0),
                Position.of(21, 0, 0), Position.of(21, 3, 0), Position.of(21, 6, 0), Position.ZERO,
                Position.of(28, 0, 0), Position.of(28, 3, 0), Position.of(28, 6, 0), Position.ZERO,
                Position.ZERO, Position.ZERO, Position.ZERO, Position.ZERO,
                Position.of(35, 0, 0), Position.of(35, 3, 0), Position.of(35, 6, 0),
                Position.of(42, 0, 0), Position.of(42, 3, 0),
                Position.of(49, 0, 0), Position.of(49, 3, 0),
                Position.of(56, 0, 0), Position.of(56, 3, 0), Position.of(56, 6, 0)
        );

        var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            if (10 == i || (14 <= i && i <= 18)) {
                continue;
            }
            circuit.translate(head);
            structure.paste(circuit);
        }

//        final var range3 = structure.getRange3();
//        structure = structure.cut(Range3.of(range3.getX(), range3.getY(), Range.of(1000)));

        final var outputFilePath = String.format("%s/%s/structure/living-millennium.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
        final var nbtWriter = NbtWriter.of(VERSION);
        nbtWriter.write(structure, outputFilePath);

        log.info("range3 : {}", structure.getRange3());
        return structure;
    }

    private static List<Structure> holotoriDance(BlockDataset blockDataset, Music16 music, NoteConvertor[] convertors) {
        final var sequenceList = music.getSequenceList();
        final List<Structure> circuits = new ArrayList<>();
        final CircuitBuilder rightBuilderF = FishBoneOneSide4Builder.of(blockDataset, true, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderF = FishBoneOneSide4Builder.of(blockDataset, false, "barrier", "redstone_lamp");
        final int[][] groups = {
                {0}, {1},
                {2}, {3},
                {4}, {5},
                {6}, {7},
                {8}, {9},
                {10}
        };
        final CircuitBuilder[] builders = {
                leftBuilderF, leftBuilderF,
                leftBuilderF, leftBuilderF,
                leftBuilderF, leftBuilderF,
                rightBuilderF, rightBuilderF,
                rightBuilderF, rightBuilderF,
                rightBuilderF
        };
        for (var i = 0; i < groups.length; ++i) {
            final List<List<Note>> subSequenceList = new ArrayList<>();
            for (final var index : groups[i]) {
                final var noteConvertor = convertors[index];
                subSequenceList.add(noteConvertor.convert(sequenceList.get(index)));
            }
            circuits.add(builders[i].generate(subSequenceList));
        }
        return circuits;
    }

    private static Structure holotoriDance(BlockDatasetVersion version, String inputFilePath) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var timeScale = 2;
        final var midiFile = MidiReader.read(inputFilePath, timeScale);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1);
        final var sequenceList = music.getSequenceList();
/*
sequences : 11
(0) path : D:\Sync\Ongakucraft\midi\HOLOTORI Dance\HOLOTORI_Dance-clean.mid
url : null
division / staffs / min. : 16 / 6 / 02:11
	track : 1 [trumpet, mutedtrumpet]
	ticks/start/end : 599 / 0 / 281
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |  289 |      |  310 |      |      |      |      |      |
	track : 2 [picked bs., slap bass 1, slap bass 2]
	ticks/start/end : 422 / 3 / 281
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |  421 |      |    1 |      |      |      |      |      |      |      |      |      |
	track : 3 [piano 1]
	ticks/start/end : 602 / 0 / 280
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |  311 |  188 |  100 |      |    3 |      |      |      |      |      |      |      |
	track : 4 [violin, pizzicatostr, tremolo str]
	ticks/start/end : 334 / 33 / 265
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |  256 |      |   78 |      |      |      |      |      |
	track : 5 [piano 1]
	ticks/start/end : 599 / 0 / 281
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |  289 |      |  310 |      |      |      |      |      |
	track : 6 []
	ticks/start/end : 364 / 3 / 276
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |  364 |      |      |      |      |      |      |      |      |      |
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 56- 73/563
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 65- 72/ 36
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  3/ 36- 48/392
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  3/ 41- 41/ 30
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  5/ 36- 57/531
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  5/ 38- 57/ 71
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  7/ 56- 72/310
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  7/ 67- 72/ 24
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  9/ 56- 73/563
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  9/ 65- 72/ 36
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 11/ 48- 53/364

/function ongakucraft:set_circuit
/tp @a 25 -38 12 0 90
/scoreboard players set @a ticks 1
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:redstone_block
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:air

/execute if entity @e[scores={ticks=1..4682}] run scoreboard players add @a ticks 1
/execute if entity @e[scores={ticks=21..4682}] as @a at @s run tp @s ~ ~ ~0.25 0 90
/execute if entity @e[scores={ticks=4682..4692}] as @a at @s run scoreboard players set @a ticks 0
*/
        final var convertor0 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BANJO);
        final var convertor1 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BANJO);
        final var convertor2 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS); // 196
        final var convertor3 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS); // 196
        final var convertor2_ = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS_DRUM); // 195
        final var convertor3_ = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS_DRUM); // 195
        final var convertor4 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS_DRUM, Instrument.HARP); // 196
        final var convertor5 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS_DRUM, Instrument.HARP); // 196
        final var convertor4_ = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.HARP); // 195
        final var convertor5_ = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.HARP); // 195
        final var convertor6 = FindFirstInstrumentNoteConvertor.of(0, Instrument.SQUARE_WAVE); // 196
        final var convertor7 = FindFirstInstrumentNoteConvertor.of(0, Instrument.SQUARE_WAVE); // 196
        final var convertor6_ = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP); // 195
        final var convertor7_ = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP); // 195
        final var convertor8 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP); // 196
        final var convertor9 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP); // 196
        final var convertor8_ = FindFirstInstrumentNoteConvertor.of(0, Instrument.PLING); // 195
        final var convertor9_ = FindFirstInstrumentNoteConvertor.of(0, Instrument.PLING); // 195
        final var convertor10 = FindFirstInstrumentNoteConvertor.of(0, Instrument.GUITAR); // 196
        final var convertor10_ = FindFirstInstrumentNoteConvertor.of(0, Instrument.DIDGERIDOO); // 196

        final NoteConvertor[] convertors = {
                convertor0, convertor1,
                convertor2, convertor3,
                convertor4, convertor5,
                convertor6, convertor7,
                convertor8, convertor9,
                convertor10
        };
        final NoteConvertor[] convertors_ = {
                convertor0, convertor1,
                convertor2_, convertor3_,
                convertor4_, convertor5_,
                convertor6_, convertor7_,
                convertor8_, convertor9_,
                convertor10_
        };

        final List<Structure> circuits = holotoriDance(blockDataset, music, convertors);
        final List<Structure> circuits_ = holotoriDance(blockDataset, music, convertors_);

        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i);
            final var circuit_ = circuits_.get(i);
            final var range3 = circuit.getRange3();
            circuit.paste(circuit_.cut(Range3.of(range3.getX(), range3.getY(), Range.of(650, range3.getZ().getStop()))));
        }

        final var heads = List.of(
                Position.of(0, 0, 0), Position.of(0, 3, 0),
                Position.of(9, 0, 0), Position.of(9, 3, 0),
                Position.of(18, 0, 0), Position.of(18, 3, 0),
                Position.of(36, 0, 0), Position.of(36, 3, 0),
                Position.of(45, 0, 0), Position.of(45, 3, 0),
                Position.of(54, 0, 0)
        );

        var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }

//        final var range3 = structure.getRange3();
//        structure = structure.cut(Range3.of(range3.getX(), range3.getY(), Range.of(500)));

        final var outputFilePath = String.format("%s/%s/structure/holotori-dance.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
        final var nbtWriter = NbtWriter.of(VERSION);
        nbtWriter.write(structure, outputFilePath);

        log.info("range3 : {}", structure.getRange3());
        return structure;
    }

    private static Structure blingBangBangBorn(BlockDatasetVersion version, String inputFilePath) throws Exception {
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1);
        final var sequenceList = music.getSequenceList();
/*
sequences : 9
(0) path : D:\Sync\Ongakucraft\midi\bling bang bang born - Mashle OP 2\Bling-Bang-Bang-Born (1).mid
url : https://musescore.com/user/38788197/scores/14167417
division / staffs / min. : 16 / 5 / 01:14
	track : 0 [vibraphone]
	ticks/start/end : 342 / 0 / 93
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   80 |    3 |  224 |    1 |   34 |      |      |      |
	track : 1 [marimba]
	ticks/start/end : 367 / 3 / 93
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   54 |    1 |  311 |    1 |      |      |      |      |
	track : 2 []
	ticks/start/end : 19 / 0 / 94
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |   14 |    2 |    3 |      |      |      |      |      |      |      |
	track : 3 [xylophone]
	ticks/start/end : 411 / 1 / 92
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |      |      |  139 |    1 |  263 |      |    8 |      |
	track : 4 [piano 1]
	ticks/start/end : 225 / 1 / 94
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |  225 |      |      |      |      |      |      |      |      |      |      |      |

[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 56- 84/272
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 66- 84/ 70
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  3/ 56- 77/250
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  3/ 68- 78/117
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  5/ 45- 55/ 18
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  5/ 57- 57/  1
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  7/ 69- 98/383
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  7/ 72- 88/ 28
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  9/ 36- 36/225

/function ongakucraft:set_circuit
/tp @a 33 -38 -6 0 40
/scoreboard players set @a ticks 1
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:redstone_block
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:air

/execute if entity @e[scores={ticks=1..1570}] run scoreboard players add @a ticks 1
/execute if entity @e[scores={ticks=21..1570}] as @a at @s run tp @s ~ ~ ~0.5 0 40
/execute if entity @e[scores={ticks=1570..1590}] as @a at @s run scoreboard players set @a ticks 0
*/
        final List<Structure> circuits = new ArrayList<>();
        final var convertor0 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.XYLOPHONE);
        final var convertor1 = FindFirstInstrumentNoteConvertor.of(0, Instrument.PLING, Instrument.XYLOPHONE);
        final var convertor2 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP);
        final var convertor3 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP);
        final var convertor4 = FindFirstInstrumentNoteConvertor.of(0, Instrument.GUITAR);
        final var convertor5 = FindFirstInstrumentNoteConvertor.of(0, Instrument.GUITAR);
        final var convertor6 = FindFirstInstrumentNoteConvertor.of(0, Instrument.XYLOPHONE, Instrument.IRON_XYLOPHONE);
        final var convertor7 = FindFirstInstrumentNoteConvertor.of(0, Instrument.XYLOPHONE, Instrument.IRON_XYLOPHONE);
        final var convertor8 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS_DRUM);
        final CircuitBuilder leftBuilderF = FishBoneOneSide2Builder.of(blockDataset, false, "barrier", "redstone_lamp");
        final CircuitBuilder rightBuilderF = FishBoneOneSide2Builder.of(blockDataset, true, "barrier", "redstone_lamp");

        final int[][] groups = {
                {0, 1},
                {2, 3},
                {4, 5},
                {6, 7},
                {8}
        };
        final NoteConvertor[] convertors = {
                convertor0, convertor1,
                convertor2, convertor3,
                convertor4, convertor5,
                convertor6, convertor7,
                convertor8
        };
        for (var i = 0; i < groups.length; ++i) {
            final List<List<Note>> subSequenceList = new ArrayList<>();
            for (final var index : groups[i]) {
                final var noteConvertor = convertors[index];
                subSequenceList.add(noteConvertor.convert(sequenceList.get(index)));
            }
            circuits.add(leftBuilderF.generate(subSequenceList));
            circuits.add(rightBuilderF.generate(subSequenceList));
        }

        final var gap = 9;
        final var heads = List.of(
                Position.of((gap + 0), 0, 0),
                Position.of(-(gap + 0), 0, 0),

                Position.of((gap + 5), -3, 1),
                Position.of(-(gap + 5), -3, 1),

                Position.of((gap + 10), 0, 3),
                Position.of(-(gap + 10), 0, 3),

                Position.of((gap + 15), -3, 6),
                Position.of(-(gap + 15), -3, 6),

                Position.of((gap + 20), 0, 10),
                Position.of(-(gap + 20), 0, 10)
        );

        var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }

//        final var range3 = structure.getRange3();
//        structure = structure.cut(Range3.of(range3.getX(), range3.getY(), Range.of(50)));

        final var outputFilePath = String.format("%s/%s/structure/bling-bang-bang-born.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
        final var nbtWriter = NbtWriter.of(VERSION);
        nbtWriter.write(structure, outputFilePath);

        log.info("range3 : {}", structure.getRange3());
        return structure;
    }

    private static Structure alive(BlockDatasetVersion version, String inputFilePath) throws Exception {
/*
sequences : 6
url : https://musescore.com/user/28845685/scores/8948702
division / staffs / min. : 16 / 2 / 01:29
	track : 0 [piano 1]
	ticks/start/end : 260 / 0 / 53
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   60 |      |  151 |      |   49 |      |      |      |
	track : 1 []
!!!	adjustable octaves [1, 2]
!!!	min/max/lower/higher : 31 / 69 / 7 / 0
	ticks/start/end : 341 / 0 / 53
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |   78 |      |  187 |      |   68 |      |    1 |      |      |      |      |      |

[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 55- 79/173
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 62- 84/ 78
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 81- 86/  9
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  4/ 24- 69/287
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  4/ 31- 62/ 52
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  4/ 36- 55/  2

/function ongakucraft:set_circuit
/tp @a 23 -44 -8 0 30
/scoreboard players set @a ticks 1
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:redstone_block
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:air

/execute if entity @e[scores={ticks=1..904}] run scoreboard players add @a ticks 1
/execute if entity @e[scores={ticks=21..904}] as @a at @s run tp @s ~ ~ ~0.5 0 30
/execute if entity @e[scores={ticks=904..914}] as @a at @s run scoreboard players set @a ticks 0
*/
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1, 6, 32, 1);
        final var sequenceList = music.getSequenceList();

        final List<Structure> circuits = new ArrayList<>();
        final CircuitBuilder rightBuilderS = SquareWaveBuilder.of(blockDataset, true, 1, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderS = SquareWaveBuilder.of(blockDataset, false, 1, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderC = CheckPatternBuilder.of(blockDataset, false, 0, "barrier", "redstone_lamp");
        final CircuitBuilder rightBuilderCD = CheckPatternBuilder.of(blockDataset, true, 1, "barrier", "redstone_lamp");

        final int[][] groups = {
                {2}, {0}, {1},
                {4}, {3}, {5},
                {2}, {0}, {1},
                {4}, {3}, {5},
                {2}, {0}, {1},
                {4}, {3}, {5},
        };
        final var convertor0 = FindFirstInstrumentNoteConvertor.of(1, Instrument.BASS, Instrument.HARP, Instrument.BELL);
        final var convertor1 = FindFirstInstrumentNoteConvertor.of(1, Instrument.BASS, Instrument.HARP, Instrument.BELL);
        final NoteConvertor[][] convertors = {
                {convertor0}, {convertor0}, {convertor0},
                {convertor1}, {convertor1}, {convertor1},
                {convertor0}, {convertor0}, {convertor0},
                {convertor1}, {convertor1}, {convertor1},
                {convertor0}, {convertor0}, {convertor0},
                {convertor1}, {convertor1}, {convertor1}
        };
        final CircuitBuilder[] builders = {
                rightBuilderS, rightBuilderS, rightBuilderS,
                leftBuilderS, leftBuilderS, leftBuilderS,
                leftBuilderC, leftBuilderC, leftBuilderC,
                leftBuilderC, leftBuilderC, leftBuilderC,
                rightBuilderCD, rightBuilderCD, rightBuilderCD,
                rightBuilderCD, rightBuilderCD, rightBuilderCD
        };
        for (var i = 0; i < groups.length; ++i) {
            final List<List<Note>> subSequenceList = new ArrayList<>();
            final var group = groups[i];
            for (var j = 0; j < group.length; ++j) {
                final var noteConvertor = convertors[i][j];
                subSequenceList.add(noteConvertor.convert(sequenceList.get(group[j])));
            }
            final var struct = builders[i].generate(subSequenceList);
            struct.regulate();

            circuits.add(struct);
        }

        final var sideOffset = 18;
        final var frontOffset = 8;
        final var mid = 4;
        final var heads = List.of(
                Position.of(8, -1, 1),
                Position.of(4, -1, 0),
                Position.of(0, -1, 1),
                Position.of(8, 2, 1),
                Position.of(4, 2, 0),
                Position.of(0, 2, 1),
                Position.of(mid + sideOffset + 1, 8, frontOffset),
                Position.of(mid + sideOffset + 1 + 2, 5, frontOffset + 2),
                Position.of(mid + sideOffset + 1 + 4, 2, frontOffset + 4),
                Position.of(mid + sideOffset + 1 + 4, -1, frontOffset + 4),
                Position.of(mid + sideOffset + 1 + 2, -4, frontOffset + 2),
                Position.of(mid + sideOffset + 1, -7, frontOffset),
                Position.of(mid - sideOffset, 8, frontOffset),
                Position.of(mid - sideOffset - 2, 5, frontOffset + 2),
                Position.of(mid - sideOffset - 4, 2, frontOffset + 4),
                Position.of(mid - sideOffset - 4, -1, frontOffset + 4),
                Position.of(mid - sideOffset - 2, -4, frontOffset + 2),
                Position.of(mid - sideOffset, -7, frontOffset)
        );

        var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }
        structure.regulate();

//        final var range3 = structure.getRange3();
//        structure = structure.cut(Range3.of(range3.getX(), range3.getY(), Range.of(50)));

        final var outputFilePath = String.format("%s/%s/structure/alive.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
        final var nbtWriter = NbtWriter.of(VERSION);
        nbtWriter.write(structure, outputFilePath);

        log.info("range3 : {}", structure.getRange3());
        return structure;
    }

    private static Structure unwelcomeSchool(BlockDatasetVersion version, String inputFilePath) throws Exception {
/*
sequences : 4
url : https://musescore.com/user/79422448/scores/13415836
division / staffs / min. : 16 / 4 / 02:08
	track : 0 [violin, pizzicatostr, tremolo str]
	ticks/start/end : 522 / 0 / 142
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   42 |      |  367 |   11 |  102 |      |      |      |
	track : 1 [violin, pizzicatostr, tremolo str]
	ticks/start/end : 443 / 0 / 142
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |  203 |    7 |  213 |      |   18 |      |    2 |      |
	track : 2 [strings, pizzicatostr, tremolo str]
	ticks/start/end : 488 / 0 / 142
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |   78 |    3 |  385 |    1 |   21 |      |      |      |      |      |
	track : 3 [strings, pizzicatostr, tremolo str]
	ticks/start/end : 384 / 0 / 142
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |  101 |    2 |  245 |    3 |   33 |      |      |      |      |      |      |      |

[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 57- 88/522
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  2/ 55- 93/443
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  3/ 50- 77/488
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  4/ 38- 64/384

/function ongakucraft:set_circuit
/tp @a 11 -52 -8 0 12
/scoreboard players set @a ticks 1
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:redstone_block
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:air

/execute if entity @e[scores={ticks=1..2354}] run scoreboard players add @a ticks 1
/execute if entity @e[scores={ticks=21..2354}] as @a at @s run tp @s ~ ~ ~0.5 0 12
/execute if entity @e[scores={ticks=2354..2364}] as @a at @s run scoreboard players set @a ticks 0
*/
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1, 6, 32, 1);
        final var sequenceList = music.getSequenceList();

        final List<Structure> circuits = new ArrayList<>();
        final CircuitBuilder rightBuilderS = SquareWaveBuilder.of(blockDataset, true, 1, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderS = SquareWaveBuilder.of(blockDataset, false, 1, "barrier", "redstone_lamp");

        final int[][] groups = {
                {0}, {0}, {1}, {1}, // TODO 1 become background
                {2}, {2}, {3}, {3}
        };
        final var convertor01 = FindFirstInstrumentNoteConvertor.of(0, Instrument.IRON_XYLOPHONE, Instrument.BELL);
        final var convertor00 = FindFirstInstrumentNoteConvertor.of(0, Instrument.SQUARE_WAVE, Instrument.BELL);
        final var convertor10 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.CHIME);
        final var convertor11 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.XYLOPHONE);
        final var convertor20 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.PLING);
        final var convertor21 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.HARP);
        final var convertor30 = FindFirstInstrumentNoteConvertor.of(0, Instrument.DIDGERIDOO, Instrument.BANJO);
        final var convertor31 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.BANJO);
        final NoteConvertor[][] convertors = {
                {convertor00}, {convertor01}, {convertor10}, {convertor11},
                {convertor20}, {convertor21}, {convertor30}, {convertor31}
        };
        final CircuitBuilder[] builders = {
                leftBuilderS, leftBuilderS, rightBuilderS, rightBuilderS,
                leftBuilderS, leftBuilderS, rightBuilderS, rightBuilderS
        };
        for (var i = 0; i < groups.length; ++i) {
            final List<List<Note>> subSequenceList = new ArrayList<>();
            final var group = groups[i];
            for (var j = 0; j < group.length; ++j) {
                final var noteConvertor = convertors[i][j];
                subSequenceList.add(noteConvertor.convert(sequenceList.get(group[j])));
            }
            final var struct = builders[i].generate(subSequenceList);
            struct.regulate();

            circuits.add(struct);
        }

        final var sideOffset = 18;
        final var frontOffset = 8;
        final var mid = 4;
        final var heads = List.of(
                Position.of(6 + mid, 3, 1),
                Position.of(2 + mid, 3, 1),
                Position.of(-2 - mid, 3, 1),
                Position.of(-6 - mid, 3, 1),
                Position.of(6 + mid, 0, 1),
                Position.of(2 + mid, 0, 1),
                Position.of(-2 - mid, 0, 1),
                Position.of(-6 - mid, 0, 1)
        );

        var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }
        structure.regulate();

//        final var range3 = structure.getRange3();
//        structure = structure.cut(Range3.of(range3.getX(), range3.getY(), Range.of(50)));

        final var outputFilePath = String.format("%s/%s/structure/unwelcome-school.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
        final var nbtWriter = NbtWriter.of(VERSION);
        nbtWriter.write(structure, outputFilePath);

        log.info("range3 : {}", structure.getRange3());
        return structure;
    }

    private static Structure shunkanHeartbeat(BlockDatasetVersion version, String inputFilePath) throws Exception {
/*
sequences : 12
(1) path : D:\Sync\Ongakucraft\midi\ReGLOSS - Shunkan Heartbeat\����??????-clean2.mid
url : null
division / staffs / min. : 16 / 4 / 03:26
	track : 1 [piano 1]
	ticks/start/end : 836 / 0 / 215
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   43 |    1 |  368 |    7 |  296 |      |  121 |      |
	track : 2 []
	ticks/start/end : 561 / 15 / 211
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |  188 |    7 |  237 |      |  129 |      |      |      |
	track : 3 [piano 1]
	ticks/start/end : 278 / 0 / 199
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |  139 |      |  122 |      |   17 |      |      |      |      |      |
	track : 4 []
!!!	adjustable octaves [1, 2]
!!!	min/max/lower/higher : 31 / 70 / 33 / 0
	ticks/start/end : 662 / 0 / 215
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |  156 |      |  221 |      |  210 |      |   42 |      |      |      |      |      |

[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 55- 94/550
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 62- 94/269
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 75- 87/ 17
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  4/ 56- 82/397
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  4/ 65- 87/135
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  4/ 72- 84/ 29
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  7/ 47- 73/132
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  7/ 53- 67/ 89
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  7/ 55- 70/ 57
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 10/ 24- 70/488
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 10/ 36- 70/139
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count : 10/ 56- 70/ 35

/function ongakucraft:set_circuit
/tp @a 12 -49 -6 0 15
/scoreboard players set @a ticks 1
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:redstone_block
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:air

/execute if entity @e[scores={ticks=1..3589}] run scoreboard players add @a ticks 1
/execute if entity @e[scores={ticks=21..3589}] as @a at @s run tp @s ~ ~ ~0.5 0 15
/execute if entity @e[scores={ticks=3589..3599}] as @a at @s run scoreboard players set @a ticks 0
*/
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1, 6, 0, 1);
        final var sequenceList = music.getSequenceList();

        final List<Structure> circuits = new ArrayList<>();
        final CircuitBuilder rightBuilderS = SquareWaveBuilder.of(blockDataset, true, 1, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderS = SquareWaveBuilder.of(blockDataset, false, 1, "barrier", "redstone_lamp");

        final int[][] groups = {
                {0}, {1}, {2},
                {3}, {4}, {5},
                {6}, {7}, {8},
                {9}, {10}, {11}
        };
        final var convertor00 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.BELL);
        final var convertor01 = FindFirstInstrumentNoteConvertor.of(0, Instrument.IRON_XYLOPHONE, Instrument.XYLOPHONE);
        final var convertor02 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BANJO, Instrument.FLUTE);
        final var convertor10 = FindFirstInstrumentNoteConvertor.of(0, Instrument.SQUARE_WAVE, Instrument.CHIME);
        final var convertor11 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.CHIME);
        final var convertor12 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.CHIME);
        final var convertor20 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.GUITAR);
        final var convertor21 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.GUITAR);
        final var convertor22 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.GUITAR);
        final var convertor30 = FindFirstInstrumentNoteConvertor.of(1, Instrument.BASS, Instrument.HARP, Instrument.FLUTE);
        final var convertor31 = FindFirstInstrumentNoteConvertor.of(1, Instrument.BASS, Instrument.HARP, Instrument.FLUTE);
        final var convertor32 = FindFirstInstrumentNoteConvertor.of(1, Instrument.BASS, Instrument.HARP, Instrument.FLUTE);
        final NoteConvertor[][] convertors = {
                {convertor00}, {convertor01}, {convertor02},
                {convertor10}, {convertor11}, {convertor12},
                {convertor20}, {convertor21}, {convertor22},
                {convertor30}, {convertor31}, {convertor32}
        };
        final CircuitBuilder[] builders = {
                rightBuilderS, rightBuilderS, rightBuilderS,
                leftBuilderS, leftBuilderS, leftBuilderS,
                rightBuilderS, rightBuilderS, rightBuilderS,
                leftBuilderS, leftBuilderS, leftBuilderS
        };
        for (var i = 0; i < groups.length; ++i) {
            final List<List<Note>> subSequenceList = new ArrayList<>();
            final var group = groups[i];
            for (var j = 0; j < group.length; ++j) {
                final var noteConvertor = convertors[i][j];
                subSequenceList.add(noteConvertor.convert(sequenceList.get(group[j])));
            }
            final var struct = builders[i].generate(subSequenceList);
            struct.regulate();

            circuits.add(struct);
        }

        final var sideOffset = 18;
        final var frontOffset = 8;
        final var mid = 2;
        final var heads = List.of(
                Position.of(-2 - mid, 6, 3), Position.of(-1 - mid, 3, 2), Position.of(-0 - mid, 0, 1),
                Position.of(2 + mid, 6, 3), Position.of(1 + mid, 3, 2), Position.of(0 + mid, 0, 1),
                Position.of(-7 - mid * 2, 6, 5), Position.of(-5 - mid * 2, 3, 3), Position.of(-3 - mid * 2, 0, 1),
                Position.of(7 + mid * 2, 6, 5), Position.of(5 + mid * 2, 3, 3), Position.of(3 + mid * 2, 0, 1)
        );

        var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }
        structure.regulate();

//        final var range3 = structure.getRange3();
//        structure = structure.cut(Range3.of(range3.getX(), range3.getY(), Range.of(50)));

        final var outputFilePath = String.format("%s/%s/structure/shunkan-heartbeat.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
        final var nbtWriter = NbtWriter.of(VERSION);
        nbtWriter.write(structure, outputFilePath);

        log.info("range3 : {}", structure.getRange3());
        return structure;
    }

    private static Structure ghostsAndGlass(BlockDatasetVersion version, String inputFilePath) throws Exception {
/*
sequences : 4
(0) path : D:\Sync\Ongakucraft\midi\Halo Reach - Ghosts and Glass\Ghosts_and_Glass.mid
url : https://musescore.com/user/43634933/scores/8178984
division / staffs / min. : 8 / 4 / 02:32
	track : 0 [trombone, mutedtrumpet]
	ticks/start/end : 116 / 0 / 87
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |   14 |    4 |   94 |    3 |    1 |      |      |      |      |      |
	track : 1 [trombone, mutedtrumpet]
	ticks/start/end : 144 / 0 / 87
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |   19 |   25 |   94 |    3 |    3 |      |      |      |      |      |
	track : 2 [trombone, mutedtrumpet]
	ticks/start/end : 74 / 10 / 87
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |   43 |    6 |   25 |      |      |      |      |      |      |      |
	track : 3 [trombone, mutedtrumpet]
	ticks/start/end : 80 / 0 / 87
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |   49 |    3 |   27 |      |    1 |      |      |      |      |      |      |      |

[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 51- 68/116
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  2/ 46- 68/144
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  3/ 43- 65/ 74
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  4/ 32- 58/ 80

/function ongakucraft:set_circuit
/tp @a 33 -49 -6 0 21
/scoreboard players set @a ticks 1
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:redstone_block
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:air

/execute if entity @e[scores={ticks=1..1470}] run scoreboard players add @a ticks 1
/execute if entity @e[scores={ticks=21..1470}] as @a at @s run tp @s ~ ~ ~0.5 0 21
/execute if entity @e[scores={ticks=1470..1480}] as @a at @s run scoreboard players set @a ticks 0
*/
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1, 6, 0, 1);
        final var sequenceList = music.getSequenceList();
        final var music2 = Music16.of(midiFileReport, 1, 3, 100, 1);
        final var sequenceList2 = music2.getSequenceList();

        final List<Structure> circuits = new ArrayList<>();
        final CircuitBuilder rightBuilderC0 = CheckPatternBuilder.of(blockDataset, true, 0, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderC0 = CheckPatternBuilder.of(blockDataset, false, 0, "barrier", "redstone_lamp");
        final CircuitBuilder rightBuilderC1 = CheckPatternBuilder.of(blockDataset, true, 1, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderC1 = CheckPatternBuilder.of(blockDataset, false, 1, "barrier", "redstone_lamp");

        final int[][] groups = {
                {0}, {1},
                {2}, {3},
                {10}, {11},
                {12}, {13},
                {10}, {11},
                {12}, {13}
        };
        final var convertor00 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.HARP);
        final var convertor01 = FindFirstInstrumentNoteConvertor.of(1, Instrument.FLUTE, Instrument.HARP);
        final var convertor02 = FindFirstInstrumentNoteConvertor.of(0, Instrument.GUITAR);
        final var convertor03 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.HARP);
        final var convertor10 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.HARP);
        final var convertor11 = FindFirstInstrumentNoteConvertor.of(1, Instrument.FLUTE, Instrument.SQUARE_WAVE);
        final var convertor12 = FindFirstInstrumentNoteConvertor.of(0, Instrument.GUITAR);
        final var convertor13 = FindFirstInstrumentNoteConvertor.of(0, Instrument.DIDGERIDOO, Instrument.HARP);
        final NoteConvertor[][] convertors = {
                {convertor00}, {convertor01},
                {convertor02}, {convertor03},
                {convertor10}, {convertor11},
                {convertor12}, {convertor13},
                {convertor10}, {convertor11},
                {convertor12}, {convertor13}
        };
        final CircuitBuilder[] builders = {
                leftBuilderC0, rightBuilderC0,
                leftBuilderC0, rightBuilderC0,
                leftBuilderC1, rightBuilderC1,
                leftBuilderC1, rightBuilderC1,
                leftBuilderC0, rightBuilderC0,
                leftBuilderC0, rightBuilderC0
        };
        for (var i = 0; i < groups.length; ++i) {
            final List<List<Note>> subSequenceList = new ArrayList<>();
            final var group = groups[i];
            for (var j = 0; j < group.length; ++j) {
                final var noteConvertor = convertors[i][j];
                if (group[j] < 10) {
                    subSequenceList.add(noteConvertor.convert(sequenceList.get(group[j])));
                } else {
                    subSequenceList.add(noteConvertor.convert(sequenceList2.get(group[j] % 10)));
                }
            }
            final var struct = builders[i].generate(subSequenceList);
            struct.regulate();

            circuits.add(struct);
        }

        final var sideOffset = 30;
        final var frontOffset = 16;
        final var mid = 1;
        final var heads = List.of(
                Position.of(1 + mid, 3, 0), Position.of(-2 - mid, 3, 0),
                Position.of(1 + mid, 0, 0), Position.of(-2 - mid, 0, 0),
                Position.of(1 + mid + sideOffset, 3, frontOffset), Position.of(-2 - mid + sideOffset, 3, frontOffset),
                Position.of(1 + mid + sideOffset, 0, frontOffset), Position.of(-2 - mid + sideOffset, 0, frontOffset),
                Position.of(1 + mid - sideOffset, 3, frontOffset), Position.of(-2 - mid - sideOffset, 3, frontOffset),
                Position.of(1 + mid - sideOffset, 0, frontOffset), Position.of(-2 - mid - sideOffset, 0, frontOffset)
        );

        var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }
        structure.regulate();

//        final var range3 = structure.getRange3();
//        structure = structure.cut(Range3.of(range3.getX(), range3.getY(), Range.of(50)));

        final var outputFilePath = String.format("%s/%s/structure/ghosts-and-glass.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
        final var nbtWriter = NbtWriter.of(VERSION);
        nbtWriter.write(structure, outputFilePath);

        log.info("range3 : {}", structure.getRange3());
        return structure;
    }

    private static Structure ifTheMoonCouldSpeak(BlockDatasetVersion version, String inputFilePath) throws Exception {
/*
sequences : 5
(0) path : D:\Sync\Ongakucraft\midi\如果月亮會說話 - 王心凌\_.mid
url : https://musescore.com/user/11147046/scores/21752872
division / staffs / min. : 16 / 2 / 03:04
	track : 0 [piano 1]
	ticks/start/end : 539 / 0 / 170
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |      |      |  379 |      |  160 |      |      |      |
	track : 1 []
	ticks/start/end : 621 / 1 / 170
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |   70 |      |  191 |      |  326 |      |   34 |      |      |      |      |      |
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - path : D:\Sync\Ongakucraft\midi\如果月亮會說話 - 王心凌\_.mid
url : https://musescore.com/user/11147046/scores/21752872
division / staffs / min. : 16 / 2 / 03:04

[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 67- 87/512
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 76- 79/ 27
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  3/ 39- 68/498
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  3/ 58- 68/122
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  3/ 62- 62/  1

/function ongakucraft:set_circuit
/tp @a 29 -47 -6 0 24
/scoreboard players set @a ticks 1
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:redstone_block
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:air

/execute if entity @e[scores={ticks=1..2870}] run scoreboard players add @a ticks 1
/execute if entity @e[scores={ticks=21..2870}] as @a at @s run tp @s ~ ~ ~0.5 0 24
/execute if entity @e[scores={ticks=2860..2870}] as @a at @s run scoreboard players set @a ticks 0
*/
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1, 8, 100, 4);
        final var sequenceList = music.getSequenceList();

        final List<Structure> circuits = new ArrayList<>();
        final CircuitBuilder rightBuilderC0 = CheckPatternBuilder.of(blockDataset, true, 0, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderC1 = CheckPatternBuilder.of(blockDataset, false, 1, "barrier", "redstone_lamp");
        final CircuitBuilder rightBuilderS0 = SquareWaveBuilder.of(blockDataset, true, 0, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderS0 = SquareWaveBuilder.of(blockDataset, false, 0, "barrier", "redstone_lamp");

        final int[][] groups = {
                {0}, {1},
                {2}, {3}, {4},
                {0}, {1},
                {2}, {3}, {4},
                {0}, {1},
                {2}, {3}, {4}
        };
        final var convertor0 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.BELL);
        final var convertor1 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.HARP);
        final NoteConvertor[][] convertors = {
                {convertor0}, {convertor0},
                {convertor1}, {convertor1}, {convertor1},
                {convertor0}, {convertor0},
                {convertor1}, {convertor1}, {convertor1},
                {convertor0}, {convertor0},
                {convertor1}, {convertor1}, {convertor1}
        };
        final CircuitBuilder[] builders = {
                leftBuilderS0, rightBuilderS0,
                rightBuilderC0, rightBuilderS0, rightBuilderC0,
                rightBuilderC0, rightBuilderC0,
                rightBuilderC0, rightBuilderC0, rightBuilderC0,
                leftBuilderC1, leftBuilderC1,
                leftBuilderC1, leftBuilderC1, leftBuilderC1
        };
        for (var i = 0; i < groups.length; ++i) {
            final List<List<Note>> subSequenceList = new ArrayList<>();
            final var group = groups[i];
            for (var j = 0; j < group.length; ++j) {
                final var noteConvertor = convertors[i][j];
                subSequenceList.add(noteConvertor.convert(sequenceList.get(group[j])));
            }
            final var struct = builders[i].generate(subSequenceList);
            struct.regulate();
            circuits.add(struct);
        }

        final var sideOffset = 20;
        final var frontOffset = 10;
        final var heads = List.of(
                Position.of(-2, 3, 0), Position.of(2, 3, 0),
                Position.of(-3, 0, 0), Position.of(0, 0, 0), Position.of(4, 0, 0),
                Position.of(-sideOffset - 5, -1, frontOffset), Position.of(-sideOffset - 5, 4, frontOffset),
                Position.of(-sideOffset - 8, -3, frontOffset + 4), Position.of(-sideOffset - 8, 2, frontOffset + 4), Position.of(-sideOffset - 8, 7, frontOffset + 4),
                Position.of(sideOffset + 6, -1, frontOffset), Position.of(sideOffset + 6, 4, frontOffset),
                Position.of(sideOffset + 9, -3, frontOffset + 4), Position.of(sideOffset + 9, 2, frontOffset + 4), Position.of(sideOffset + 9, 7, frontOffset + 4)
        );

        var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }
        structure.regulate();

//        final var range3 = structure.getRange3();
//        structure = structure.cut(Range3.of(range3.getX(), range3.getY(), Range.of(50)));

        final var outputFilePath = String.format("%s/%s/structure/if-the-moon-could-speak.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
        final var nbtWriter = NbtWriter.of(VERSION);
        nbtWriter.write(structure, outputFilePath);

        log.info("range3 : {}", structure.getRange3());
        return structure;
    }

    private static Structure babel(BlockDatasetVersion version, String inputFilePath) throws Exception {
/*
sequences : 7
(0) path : D:\Sync\Ongakucraft\midi\BABEL - Arknights\Arknights__BABEL_OST_for_piano_and_orchestrating-clean.mid
url : null
division / staffs / min. : 16 / 2 / 01:46
	track : 1 [piano 1]
	ticks/start/end : 419 / 0 / 95
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |      |      |      |      |   86 |      |  253 |      |   80 |      |      |      |
	track : 2 []
	ticks/start/end : 449 / 0 / 97
		|  F#1 |      |  F#2 |      |  F#3 |      |  F#4 |      |  F#5 |      |  F#6 |      |  F#7 |
		|      |   38 |      |  171 |    1 |  235 |    2 |    2 |      |      |      |      |      |
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - path : D:\Sync\Ongakucraft\midi\BABEL - Arknights\Arknights__BABEL_OST_for_piano_and_orchestrating-clean.mid
url : null
division / staffs / min. : 16 / 2 / 01:46

[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 55- 84/249
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 59- 89/115
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 62- 88/ 43
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 67- 83/ 10
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  1/ 79- 81/  2
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  6/ 35- 66/356
[main] INFO com.ongakucraft.app.data.MidiLoadingApp - id/min-max/count :  6/ 44- 69/ 93

/function ongakucraft:set_circuit
/tp @a 29 -47 -6 0 24
/scoreboard players set @a ticks 1
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:redstone_block
/execute as @e[type=minecraft:item_frame] at @s run setblock ~ ~1 ~ minecraft:air

/execute if entity @e[scores={ticks=1..1644}] run scoreboard players add @a ticks 1
/execute if entity @e[scores={ticks=21..1644}] as @a at @s run tp @s ~ ~ ~0.5 0 24
/execute if entity @e[scores={ticks=1644..1654}] as @a at @s run scoreboard players set @a ticks 0
*/
        final var blockDataset = DataLoadingApp.loadBlockDataset(version);
        final var midiFile = MidiReader.read(inputFilePath);
        final var midiFileReport = MidiFileReport.of(midiFile);
        final var music = Music16.of(midiFileReport, 1, 8, 6, 4);
        final var sequenceList = music.getSequenceList();

        final List<Structure> circuits = new ArrayList<>();
        final CircuitBuilder rightBuilderC0 = CheckPatternBuilder.of(blockDataset, true, 0, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderC1 = CheckPatternBuilder.of(blockDataset, false, 0, "barrier", "redstone_lamp");
        final CircuitBuilder rightBuilderS0 = SquareWaveBuilder.of(blockDataset, true, 0, "barrier", "redstone_lamp");
        final CircuitBuilder leftBuilderS0 = SquareWaveBuilder.of(blockDataset, false, 0, "barrier", "redstone_lamp");

        final int[][] groups = {
                {0}, {1},
                {2}, {3}, {4},
                {5}, {6},
                {0}, {1},
                {2}, {3}, {4},
                {5}, {6}
        };
        final var convertor00 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.BELL);
        final var convertor01 = FindFirstInstrumentNoteConvertor.of(0, Instrument.HARP, Instrument.BELL);
        final var convertor02 = FindFirstInstrumentNoteConvertor.of(0, Instrument.IRON_XYLOPHONE, Instrument.BELL);
        final var convertor03 = FindFirstInstrumentNoteConvertor.of(0, Instrument.FLUTE);
        final var convertor04 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BELL);
        final var convertor10 = FindFirstInstrumentNoteConvertor.of(0, Instrument.BASS, Instrument.GUITAR);
        final var convertor11 = FindFirstInstrumentNoteConvertor.of(0, Instrument.GUITAR, Instrument.HARP);
        final NoteConvertor[][] convertors = {
                {convertor00}, {convertor01}, {convertor02},
                {convertor03}, {convertor04},
                {convertor10}, {convertor11},
                {convertor00}, {convertor01}, {convertor02},
                {convertor03}, {convertor04},
                {convertor10}, {convertor11}
        };
        final CircuitBuilder[] builders = {
                leftBuilderC1, leftBuilderC1, leftBuilderC1,
                leftBuilderC1, leftBuilderC1,
                leftBuilderC1, leftBuilderC1,
                rightBuilderC0, rightBuilderC0, rightBuilderC0,
                rightBuilderC0, rightBuilderC0,
                rightBuilderC0, rightBuilderC0
        };
        for (var i = 0; i < groups.length; ++i) {
            final List<List<Note>> subSequenceList = new ArrayList<>();
            final var group = groups[i];
            for (var j = 0; j < group.length; ++j) {
                final var noteConvertor = convertors[i][j];
                subSequenceList.add(noteConvertor.convert(sequenceList.get(group[j])));
            }
            final var struct = builders[i].generate(subSequenceList);
            struct.regulate();
            circuits.add(struct);
        }

        final var heads = List.of(
                Position.of(1, 0, 0), Position.of(4, 0, 6), Position.of(7, 0, 14),
                Position.of(5, -3, 6), Position.of(8, -6, 14),
                Position.of(5, 3, 6), Position.of(8, 6, 14),
                Position.of(-1-1, 0, 0), Position.of(-1-4, 0, 6), Position.of(-1-7, 0, 14),
                Position.of(-1-5, -3, 6), Position.of(-1-8, -6, 14),
                Position.of(-1-5, 3, 6), Position.of(-1-8, 6, 14)
        );

        var structure = new Structure();
        for (var i = 0; i < circuits.size(); ++i) {
            final var circuit = circuits.get(i).clone();
            final var head = heads.get(i);
            circuit.translate(head);
            structure.paste(circuit);
        }
        structure.regulate();

//        final var range3 = structure.getRange3();
//        structure = structure.cut(Range3.of(range3.getX(), range3.getY(), Range.of(50)));

        final var outputFilePath = String.format("%s/%s/structure/babel.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
        final var nbtWriter = NbtWriter.of(VERSION);
        nbtWriter.write(structure, outputFilePath);

        log.info("range3 : {}", structure.getRange3());
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

//            final var inputFilePath = String.format("%s/input/Hopes and Dreams - Undertale/Hopes_and_Dreams-cut.mid", ROOT_DIR_PATH);
//            final var structure = hopesAndDreams(VERSION, inputFilePath);
//            final var outputFilePath = String.format("%s/%s/structure/hopes-and-dreams.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, outputFilePath);

//            final var inputFilePath = String.format("%s/input/レクイエム - Kanaria and 星街すいせい/Hopes_and_Dreams-cut.mid", ROOT_DIR_PATH);
//            final var structure = requiem(VERSION, inputFilePath);
//            final var outputFilePath = String.format("%s/%s/structure/requiem.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, outputFilePath);

//            final var inputFilePath = String.format("%s/input/Megalovania - Undertale/Megalovania__Toby_Fox_Megalovania-cut.mid", ROOT_DIR_PATH);
//            final var structure = megalovania(VERSION, inputFilePath);
//            final var outputFilePath = String.format("%s/%s/structure/megalovania.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, outputFilePath);

//            final var inputFilePath = String.format("%s/input/Hopes and Dreams - Undertale/Hopes_and_Dreams_FINALLY_FINISHED.mid", ROOT_DIR_PATH);
//            hopesAndDreamsOrchestra(VERSION, inputFilePath);

//            final var inputFilePath = String.format("%s/input/ヤキモチ - 高橋優/yakimochi.mid", ROOT_DIR_PATH);
//            final var structure = yakimochi(VERSION, inputFilePath);
//            final var outputFilePath = String.format("%s/%s/structure/yakimochi.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, outputFilePath);

//            final var inputFilePath = String.format("%s/input/ヤキモチ - 高橋優/yakimochi.mid", ROOT_DIR_PATH);
//            yakimochiDesign(VERSION, inputFilePath);

//            final var inputFilePath = String.format("%s/input/NIGHT DANCER - imase/NIGHT_DANCER__imase.mid", ROOT_DIR_PATH);
//            final var structure = nightDancer(VERSION, inputFilePath);
//            final var outputFilePath = String.format("%s/%s/structure/night-dancer.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, outputFilePath);

//            final var inputFilePath = String.format("%s/input/Megalovania - Undertale - Design/megalovania (6)-remove.mid", ROOT_DIR_PATH);
//            final var structure = megalovaniaDesign(VERSION, inputFilePath);
//            final var outputFilePath = String.format("%s/%s/structure/megalovania-design.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, outputFilePath);

//            final var inputFilePath = String.format("%s/input/Hey Ya - OutKast/Hey_Ya (16).mid", ROOT_DIR_PATH);
//            final var structure = heyYa(VERSION, inputFilePath);
//            final var outputFilePath = String.format("%s/%s/structure/hey-ya.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
//            nbtWriter.write(structure, "C:\\Users\\User\\AppData\\Roaming\\.minecraft\\saves\\case62c1\\datapacks\\ongakucraft\\data\\ongakucraft\\structures\\demo.nbt");
//            nbtWriter.write(structure, outputFilePath);

//            final var inputFilePath = String.format("%s/input/high-hopes/High_Hopes (3).mid", ROOT_DIR_PATH);
//            final var structure = highHopes(VERSION, inputFilePath);
//            nbtWriter.write(structure, "C:\\Users\\fnaith\\AppData\\Roaming\\.minecraft\\saves\\Test World 193\\datapacks\\ongakucraft\\data\\ongakucraft\\structures\\demo.nbt");

//            final var inputFilePath = String.format("%s/input/living millennium - iyowa/1000_-_-clean.mid", ROOT_DIR_PATH);
//            final var structure = livingLillennium(VERSION, inputFilePath);
//            nbtWriter.write(structure, "C:\\Users\\fnaith\\AppData\\Roaming\\.minecraft\\saves\\Test World 193\\datapacks\\ongakucraft\\data\\ongakucraft\\structures\\demo.nbt");

//            final var inputFilePath = String.format("%s/input/HOLOTORI Dance/HOLOTORI_Dance-clean.mid", ROOT_DIR_PATH);
//            final var structure = holotoriDance(VERSION, inputFilePath);
//            nbtWriter.write(structure, "C:\\Users\\fnaith\\AppData\\Roaming\\.minecraft\\saves\\case65c1\\datapacks\\ongakucraft\\data\\ongakucraft\\structures\\demo.nbt");

//            final var inputFilePath = String.format("%s/input/bling bang bang born - Mashle OP 2/Bling-Bang-Bang-Born (1).mid", ROOT_DIR_PATH);
//            final var structure = blingBangBangBorn(VERSION, inputFilePath);
//            nbtWriter.write(structure, "C:\\Users\\fnaith\\AppData\\Roaming\\.minecraft\\saves\\case66c1\\datapacks\\ongakucraft\\data\\ongakucraft\\structures\\demo.nbt");

//            final var inputFilePath = String.format("%s/input/Alive - Arknights/Alive.mid", ROOT_DIR_PATH);
//            final var structure = alive(VERSION, inputFilePath);
//            nbtWriter.write(structure, "C:\\Users\\fnaith\\AppData\\Roaming\\.minecraft\\saves\\case67c1\\datapacks\\ongakucraft\\data\\ongakucraft\\structures\\demo.nbt");

//            final var inputFilePath = String.format("%s/input/Blue Archive - Unwelcome School/Unwelcome_School_-_Mitsukiyo (1).mid", ROOT_DIR_PATH);
//            final var structure = unwelcomeSchool(VERSION, inputFilePath);
//            nbtWriter.write(structure, "C:\\Users\\fnaith\\AppData\\Roaming\\.minecraft\\saves\\case68c1\\datapacks\\ongakucraft\\data\\ongakucraft\\structures\\demo.nbt");

//            final var inputFilePath = String.format("%s/input/ReGLOSS - Shunkan Heartbeat/Shunkan Heartbeat.mid", ROOT_DIR_PATH);
//            final var structure = shunkanHeartbeat(VERSION, inputFilePath);
//            nbtWriter.write(structure, "C:\\Users\\fnaith\\AppData\\Roaming\\.minecraft\\saves\\case69c1\\datapacks\\ongakucraft\\data\\ongakucraft\\structures\\demo.nbt");

//            final var inputFilePath = String.format("%s/input/Halo Reach - Ghosts and Glass/Ghosts_and_Glass.mid", ROOT_DIR_PATH);
//            final var structure = ghostsAndGlass(VERSION, inputFilePath);
//            nbtWriter.write(structure, "C:\\Users\\fnaith\\AppData\\Roaming\\.minecraft\\saves\\case70c1\\datapacks\\ongakucraft\\data\\ongakucraft\\structures\\demo.nbt");

//            final var inputFilePath = String.format("%s/input/如果月亮會說話 - 王心凌/_.mid", ROOT_DIR_PATH);
//            final var structure = ifTheMoonCouldSpeak(VERSION, inputFilePath);
//            nbtWriter.write(structure, "C:\\Users\\fnaith\\AppData\\Roaming\\.minecraft\\saves\\Test World 202\\datapacks\\ongakucraft\\data\\ongakucraft\\structures\\demo.nbt");

            final var inputFilePath = String.format("%s/input/BABEL - Arknights/Arknights__BABEL_OST_for_piano_and_orchestrating-clean.mid", ROOT_DIR_PATH);
            final var structure = babel(VERSION, inputFilePath);
            nbtWriter.write(structure, "C:\\Users\\fnaith\\AppData\\Roaming\\.minecraft\\saves\\case72c1\\datapacks\\ongakucraft\\data\\ongakucraft\\structures\\demo.nbt");
        } catch (Exception e) {
            log.error("CircuitUtils", e);
        }
    }

    private CircuitUtils() {}
}
