package com.ongakucraft.app.nbt;

import com.ongakucraft.app.data.DataLoadingApp;
import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.Block;
import com.ongakucraft.core.block.BlockDatasetVersion;
import com.ongakucraft.core.block.color.BlockMapColor;
import com.ongakucraft.core.structure.Position;
import com.ongakucraft.core.structure.Structure;
import dev.dewy.nbt.Nbt;
import dev.dewy.nbt.api.Tag;
import dev.dewy.nbt.io.CompressionType;
import dev.dewy.nbt.tags.array.ByteArrayTag;
import dev.dewy.nbt.tags.collection.CompoundTag;
import dev.dewy.nbt.tags.collection.ListTag;
import dev.dewy.nbt.tags.primitive.ByteTag;
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

    public void write(BlockMapColor[][] colors, String outputFilePath) throws Exception {
        final var compoundTag = tag(colors, version.getDataVersion());
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
            final var entityData = block.getEntityData();
            blocks.add(block(pos, state, entityData));
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

    private static CompoundTag tag(BlockMapColor[][] colors, int dataVersion) {
        final var tag = new CompoundTag("null");
        tag.put(tag("DataVersion", dataVersion));
        tag.put(tag(colors));
        return tag;
    }

    private static CompoundTag tag(BlockMapColor[][] colorMap) {
        final var h = colorMap.length;
        final var w = colorMap[0].length;
        final var colors = new byte[w * h];
        for (var z = 0; z < h; ++z) {
            final var row = colorMap[z];
            for (var x = 0; x < w; ++x) {
                final var mapColor = row[x];
                colors[x + z * w] = (byte) mapColor.getColorId();
            }
        }
        final var tag = new CompoundTag("data");
        tag.put(tag("banners", List.of()));
        tag.put(tag("colors", colors));
        tag.put(tag("dimension", "minecraft:overworld"));
        tag.put(tag("frames", List.of()));
        tag.put(tag("locked", true));
        tag.put(tag("scale", (byte) 0));
        tag.put(tag("trackingPosition", false));
        tag.put(tag("unlimitedTracking", false));
        tag.put(tag("xCenter", 0));
        tag.put(tag("zCenter", 0));
        return tag;
    }

    public static int comparePosition(Position a, Position b) {
        return Comparator.comparingInt(Position::getY)
                         .thenComparingInt(Position::getZ)
                         .thenComparingInt(Position::getX)
                         .compare(a, b);
    }

    private static CompoundTag block(Position pos, int state, Map<String, Object> entityData) {
        final var block = new CompoundTag((String) null);
        block.put(pos(pos));
        block.put(tag("state", state));
        if (!entityData.isEmpty()) {
            block.put(nbt(entityData));
        }
        return block;
    }

    private static ListTag<IntTag> pos(Position pos) {
        return tag("pos", pos.getX(), pos.getY(), pos.getZ());
    }

    private static CompoundTag nbt(Map<String, Object> entityData) {
        final var nbt = new CompoundTag("nbt");
        for (var entry : entityData.entrySet()) {
            final var value = entry.getValue();
            if (value instanceof Boolean) {
                nbt.put(tag(entry.getKey(), (Boolean) value));
            } else if (value instanceof Integer) {
                nbt.put(tag(entry.getKey(), (Integer) value));
            } else if (value instanceof String) {
                nbt.put(tag(entry.getKey(), (String) value));
            } else {
                throw new OcException(value.getClass().toString());
            }
        }
        return nbt;
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

    private static ByteArrayTag tag(String name, byte[] value) {
        return new ByteArrayTag(name, value);
    }

    private static ByteTag tag(String name, boolean b) {
        return new ByteTag(name, (byte) (b ? 1 : 0));
    }

    private static ByteTag tag(String name, byte b) {
        return new ByteTag(name, b);
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
            final var nbtWriter = of(VERSION);
            final var snbt = nbtWriter.dump(structure);
            log.info("snbt : {}", snbt);
        } catch (Exception e) {
            log.error("NbtUtils", e);
        }
    }
}
