package com.ongakucraft.app.nbt;

import com.ongakucraft.app.data.DataLoadingApp;
import com.ongakucraft.core.block.Block;
import com.ongakucraft.core.block.BlockDatasetVersion;
import com.ongakucraft.core.structure.Position;
import com.ongakucraft.core.structure.Structure;
import dev.dewy.nbt.Nbt;
import dev.dewy.nbt.api.Tag;
import dev.dewy.nbt.io.CompressionType;
import dev.dewy.nbt.tags.collection.CompoundTag;
import dev.dewy.nbt.tags.collection.ListTag;
import dev.dewy.nbt.tags.primitive.IntTag;
import dev.dewy.nbt.tags.primitive.StringTag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor(staticName = "of")
@Getter
public final class NbtWriter {
    private static final BlockDatasetVersion VERSION = BlockDatasetVersion.of("1.18.2", 2975);

    @NonNull private final BlockDatasetVersion version;

    public void write(Structure structure, String outputFilePath) throws Exception {
        structure = structure.clone();
        structure.regulate();
        final var compoundTag = tag(structure, version.getDataVersion());
        new Nbt().toFile(compoundTag, new File(outputFilePath), CompressionType.GZIP);
    }

    public String dump(Structure structure) {
        final var compoundTag = tag(structure, version.getDataVersion());
        return new Nbt().toSnbt(compoundTag);
    }

    private static CompoundTag tag(Structure structure, int dataVersion) {
        final var range3 = structure.getRange3();
        final var xLength = range3.getX().length();
        final var yLength = range3.getY().length();
        final var zLength = range3.getZ().length();
        final var blocks = new ArrayList<Tag>();
        final var palettes = new ArrayList<Tag>();
        final var blockToState = new HashMap<Block, Integer>();
        for (var pos : structure.getPositionList().stream().sorted(NbtWriter::comparePosition).toList()) {
            final var block = structure.get(pos);
            final var state = blockToState.computeIfAbsent(block, k -> blockToState.size());
            blocks.add(block(pos, state));
        }
        for (var i = 0; i < blockToState.size(); ++i) {
            palettes.add(null);
        }
        for (var entry : blockToState.entrySet()) {
            final var block = entry.getKey();
            final var palette = palette(block.getProperties(), block.getId().getId());
            palettes.set(entry.getValue(), palette);
        }
        final var tag = new CompoundTag("null");
        tag.put(tag("size", xLength, yLength, zLength));
        tag.put(tag("entities", List.of()));
        tag.put(tag("blocks", blocks));
        tag.put(tag("palette", palettes));
        tag.put(tag("DataVersion", dataVersion));
        return tag;
    }

    public static int comparePosition(Position a, Position b) {
        return Comparator.comparingInt(Position::getY)
                         .thenComparingInt(Position::getZ)
                         .thenComparingInt(Position::getX)
                         .compare(a, b);
    }

    private static CompoundTag block(Position pos, int state) {
        final var block = new CompoundTag((String) null);
        block.put(pos(pos));
        block.put(tag("state", state));
        return block;
    }

    private static ListTag<IntTag> pos(Position pos) {
        return tag("pos", pos.getX(), pos.getY(), pos.getZ());
    }

    private static CompoundTag palette(Map<String, String> properties, String name) {
        final var palette = new CompoundTag("null");
        final var props = new CompoundTag("Properties");
        for (var entry : properties.entrySet()) {
            props.put(tag(entry.getKey(), entry.getValue()));
        }
        palette.put(props);
        palette.put(tag("Name", name));
        return palette;
    }

    private static ListTag<IntTag> tag(String name, int... l) {
        return new ListTag<>(name, Arrays.stream(l).mapToObj(NbtWriter::tag).collect(Collectors.toList()));
    }

    private static ListTag<Tag> tag(String name, List<Tag> l) {
        return new ListTag<>(name, l);
    }

    private static IntTag tag(int i) {
        return tag((String) null, i);
    }

    private static IntTag tag(String name, int i) {
        return new IntTag(name, i);
    }

    private static StringTag tag(String name, String value) {
        return new StringTag(name, value);
    }

    public static void main(String[] args) {
        try {
            final var blockDataset = DataLoadingApp.loadBlockDataset(VERSION);
            final var block = blockDataset.getBlock("note_block");
            final var structure = new Structure();
            structure.put(Position.ZERO, block);
            final var nbtWriter = NbtWriter.of(VERSION);
            final var snbt = nbtWriter.dump(structure);
            log.info("snbt : {}", snbt);
        } catch (Exception e) {
            log.error("NbtUtils", e);
        }
    }
}
