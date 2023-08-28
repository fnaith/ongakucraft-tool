package com.ongakucraft.app.nbt;

import com.ongakucraft.app.data.DataLoadingApp;
import com.ongakucraft.app.midi.MidiReader;
import com.ongakucraft.core.block.BlockDatasetVersion;
import com.ongakucraft.core.circuit.*;
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
        final CircuitBuilder rightBuilder = FishBoneOneSideBuilder.of(blockDataset, true, "white_wool", "redstone_lamp");
        final CircuitBuilder leftBuilder = FishBoneOneSideBuilder.of(blockDataset, false, "white_wool", "redstone_lamp");

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
        final CircuitBuilder rightBuilder = FishBoneOneSideBuilder.of(blockDataset, true, "magenta_glazed_terracotta", "redstone_lamp");
        final CircuitBuilder leftBuilder = FishBoneOneSideBuilder.of(blockDataset, false, "magenta_glazed_terracotta", "redstone_lamp");

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

            final var inputFilePath = String.format("%s/input/Hey Ya - OutKast/Hey_Ya (16).mid", ROOT_DIR_PATH);
            final var structure = heyYa(VERSION, inputFilePath);
            final var outputFilePath = String.format("%s/%s/structure/hey-ya.nbt", ROOT_DIR_PATH, VERSION.getMcVersion());
            nbtWriter.write(structure, "C:\\Users\\User\\AppData\\Roaming\\.minecraft\\saves\\case62c1\\datapacks\\ongakucraft\\data\\ongakucraft\\structures\\demo.nbt");
            nbtWriter.write(structure, outputFilePath);
        } catch (Exception e) {
            log.error("CircuitUtils", e);
        }
    }

    private CircuitUtils() {}
}
