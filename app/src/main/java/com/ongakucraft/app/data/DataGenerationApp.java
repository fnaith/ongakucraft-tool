package com.ongakucraft.app.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ongakucraft.app.graphics.GraphicUtils;
import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.BlockId;
import com.ongakucraft.core.block.Direction;
import com.ongakucraft.core.block.color.BlockLabColor;
import com.ongakucraft.core.block.color.BlockMapColor;
import com.ongakucraft.core.block.color.BlockRgbColor;
import com.ongakucraft.core.block.define.BlockDefine;
import com.ongakucraft.core.block.define.BlockModelDefine;
import com.ongakucraft.core.color.ColorConverter;
import com.ongakucraft.core.color.LabColor;
import com.ongakucraft.core.color.RgbColor;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public final class DataGenerationApp {
    private static final String ROOT_DIR_PATH = "./data/generated";
    private static final String VERSION = "1.18.2";
    private static final ObjectMapper mapper = new ObjectMapper();

    private static List<BlockModelDefine> generateTexturedBlockModelDefineList(String version) throws Exception {
        final var blockPropertyDefineList = ArticDataApp.generateBlockPropertyDefineList(version);
        final var blockDefineList = ArticDataApp.generateBlockDefineList(version, blockPropertyDefineList);
        log.info("blockDefineList : {}", blockDefineList.size());
        final var blockModelDefineList = McassetApp.generateBlockModelDefineList(version);
        log.info("blockModelDefineList : {}", blockModelDefineList.size());
        final Map<BlockId, BlockModelDefine> blockModelMap = blockModelDefineList.stream().collect(Collectors.toMap(BlockModelDefine::getId, Function.identity()));
        final List<BlockModelDefine> texturedBlockModelDefineList = new ArrayList<>();
        for (var blockDefine : blockDefineList) {
            final var path = blockDefine.getId().getPath();
            if (blockDefine.isCollisionShapeFullBlock() &&
                !"air".equals(path) &&
                !"barrier".equals(path) &&
                !"beacon".equals(path) &&
                !"chorus_flower".equals(path) &&
                !"chain_command_block".equals(path) &&
                !"command_block".equals(path) &&
                !"dried_kelp_block".equals(path) && // TODO
                !"frosted_ice".equals(path) && // TODO
                !"grass_block".equals(path) && // https://www.quora.com/Why-cant-I-change-the-colors-of-the-grass-and-leaves-in-Minecraft
                !"jigsaw".equals(path) &&
                !"observer".equals(path) && // TODO
                !"repeating_command_block".equals(path) &&
                !"respawn_anchor".equals(path) && // TODO
                !"slime_block".equals(path) && // TODO
                !path.startsWith("infested_") &&
                !path.startsWith("waxed_") && // TODO
                !path.endsWith("_leaves") &&
                !path.endsWith("shulker_box") && // TODO
                !blockModelMap.containsKey(blockDefine.getId())) {
                throw new OcException("blockModelId : %s", path);
            }
            if (blockModelMap.containsKey(blockDefine.getId())) {
                texturedBlockModelDefineList.add(blockModelMap.get(blockDefine.getId()));
            }
        }
        return texturedBlockModelDefineList;
    }

    private static RgbColor generateRgbColor(String texture) {
        try {
            final var bufferedImage = GraphicUtils.readImage(texture);
            final var w = bufferedImage.getWidth();
            final var h = bufferedImage.getHeight();
            final var size = w * h;
            var rSum = 0;
            var gSum = 0;
            var bSum = 0;
            for (var y = 0; y < h; ++y) {
                for (var x = 0; x < w; ++x) {
                    final var pixel = new Color(bufferedImage.getRGB(x, y), true);
                    rSum += pixel.getRed();
                    gSum += pixel.getGreen();
                    bSum += pixel.getBlue();
                }
            }
            return RgbColor.of(rSum / size, gSum / size, bSum / size);
        } catch (Exception e) {
            throw new OcException("%s : %s", e.getMessage(), texture);
        }
    }

    private static List<BlockRgbColor> generateBlockRgbColorList(List<BlockModelDefine> texturedBlockModelDefineList) throws Exception {
        final List<BlockRgbColor> blockRgbColorList = new ArrayList<>();
        final Map<String, RgbColor> textureToColor = new HashMap<>();
        for (final var blockModelDefine : texturedBlockModelDefineList) {
            final Map<Direction, RgbColor> colors = new EnumMap<>(Direction.class);
            for (final var entry : blockModelDefine.getTextures().entrySet()) {
                final var texture = entry.getValue();
                final var color = textureToColor.computeIfAbsent(texture, DataGenerationApp::generateRgbColor);
                colors.put(entry.getKey(), color);
            }
            blockRgbColorList.add(BlockRgbColor.of(blockModelDefine.getId(), colors));
        }
        return blockRgbColorList;
    }

    private static void saveBlockRgbColorList(String version, List<BlockRgbColor> blockRgbColorList) throws Exception {
        final var outputFilePath = String.format("%s/%s/block/rgb_color.json", ROOT_DIR_PATH, version);
        mapper.writeValue(new File(outputFilePath), blockRgbColorList);
    }

    private static LabColor generateLabColor(String texture) {
        try {
            final var bufferedImage = GraphicUtils.readImage(texture);
            final var w = bufferedImage.getWidth();
            final var h = bufferedImage.getHeight();
            final var size = w * h;
            var lSum = 0.0;
            var aSum = 0.0;
            var bSum = 0.0;
            for (var y = 0; y < h; ++y) {
                for (var x = 0; x < w; ++x) {
                    final var pixel = new Color(bufferedImage.getRGB(x, y), true);
                    final var lab = ColorConverter.RGBtoLAB(pixel.getRed(), pixel.getGreen(), pixel.getBlue());
                    lSum += lab[0];
                    aSum += lab[1];
                    bSum += lab[2];
                }
            }
            return LabColor.of(lSum / size, aSum / size, bSum / size);
        } catch (Exception e) {
            throw new OcException("%s : %s", e.getMessage(), texture);
        }
    }

    private static List<BlockLabColor> generateBlockLabColorList(List<BlockModelDefine> texturedBlockModelDefineList) throws Exception {
        final List<BlockLabColor> blockLabColorList = new ArrayList<>();
        final Map<String, LabColor> textureToColor = new HashMap<>();
        for (final var blockModelDefine : texturedBlockModelDefineList) {
            final Map<Direction, LabColor> colors = new EnumMap<>(Direction.class);
            for (final var entry : blockModelDefine.getTextures().entrySet()) {
                final var texture = entry.getValue();
                final var color = textureToColor.computeIfAbsent(texture, DataGenerationApp::generateLabColor);
                colors.put(entry.getKey(), color);
            }
            blockLabColorList.add(BlockLabColor.of(blockModelDefine.getId(), colors));
        }
        return blockLabColorList;
    }

    private static void saveBlockLabColorList(String version, List<BlockLabColor> blockLabColorList) throws Exception {
        final var outputFilePath = String.format("%s/%s/block/lab_color.json", ROOT_DIR_PATH, version);
        mapper.writeValue(new File(outputFilePath), blockLabColorList);
    }

    private static List<BlockMapColor> generateBaseBlockMapColorList(String version) throws Exception {
        final var blockPropertyDefineList = ArticDataApp.generateBlockPropertyDefineList(version);
        final var blockDefineList = ArticDataApp.generateBlockDefineList(version, blockPropertyDefineList);
        log.info("blockDefineList : {}", blockDefineList.size());
        final var blockMapColorDefineList = ArticDataApp.generateBlockMapColorDefineList(version);
        log.info("blockMapColorDefineList : {}", blockMapColorDefineList.size());
        final List<BlockDefine> mapBlockDefineList = blockDefineList.stream()
                .filter(blockDefine -> {
                    final var path = blockDefine.getId().getPath();
                    switch (path) {
                        case "water":
                        case "crimson_nylium":
                        case "warped_nylium":
                        case "glow_lichen":
                            return true;
                    }
                    if (path.endsWith("_ore") || path.startsWith("stripped_") || path.contains("copper")) {
                        return false;
                    }
                    if (blockDefine.isGravity() || blockDefine.isLiquid() || blockDefine.isRandomlyTicks()) {
                        return false;
                    }
                    if (!blockDefine.isCollisionShapeFullBlock()) {
                        return false;
                    }
                    return true;
                }).toList();
        log.info("mapBlockDefineList : {}", mapBlockDefineList.size());
        final List<BlockMapColor> baseBlockMapColorList = new ArrayList<>();
        for (final var blockMapColorDefine : blockMapColorDefineList) {
            if (0 == blockMapColorDefine.getId()) {
                continue;
            }
            final var blockIdList = mapBlockDefineList.stream()
                    .filter(blockDefine -> blockMapColorDefine.getId() == blockDefine.getMapColorId())
                    .map(BlockDefine::getId).toList();
            final BlockId mapBlockId;
            final var woolBlockIdOptional = blockIdList.stream().filter(blockId -> blockId.getPath().endsWith("_wool")).findAny();
            final var terracottaBlockIdOptional = blockIdList.stream().filter(blockId -> blockId.getPath().endsWith("_terracotta")).findAny();
            final var planksBlockIdOptional = blockIdList.stream().filter(blockId -> blockId.getPath().endsWith("_planks")).findAny();
            final var blockBlockIdOptional = blockIdList.stream().filter(blockId -> blockId.getPath().endsWith("_block")).findAny();
            final var mushroomStemBlockIdOptional = blockIdList.stream().filter(blockId -> "mushroom_stem".equals(blockId.getPath())).findAny();
            if (woolBlockIdOptional.isPresent()) {
                mapBlockId = woolBlockIdOptional.get();
            } else if (terracottaBlockIdOptional.isPresent()) {
                mapBlockId = terracottaBlockIdOptional.get();
            } else if (planksBlockIdOptional.isPresent()) {
                mapBlockId = planksBlockIdOptional.get();
            } else if (blockBlockIdOptional.isPresent()) {
                mapBlockId = blockBlockIdOptional.get();
            } else if (mushroomStemBlockIdOptional.isPresent()) {
                mapBlockId = mushroomStemBlockIdOptional.get();
            } else {
                mapBlockId = blockIdList.get(0);
            }
            log.info("blockIdList : {} {} {}", blockMapColorDefine.getId(), blockIdList.size(), mapBlockId.getPath());
            baseBlockMapColorList.add(BlockMapColor.of(blockMapColorDefine.getId(), mapBlockId, blockMapColorDefine.getColor(), 1));
        }
        return baseBlockMapColorList;
    }

    private static List<BlockMapColor> generateBlockMapColorList(List<BlockMapColor> baseBlockMapColorList) {
        final List<BlockMapColor> blockMapColorList = new ArrayList<>();
        final int[][] parameters = { { 180, -1 }, { 220, 0 }, { 255, 1 } };
        for (final var baseBlockMapColor : baseBlockMapColorList) {
            final var rgbColor = baseBlockMapColor.getRgbColor();
            for (final var parameter : parameters) {
                final var multiplier = parameter[0];
                final var gradient = parameter[1];
                final var variantColor = RgbColor.of((int)(rgbColor.getR() * multiplier / 255.0 + 0.5),
                                                     (int)(rgbColor.getG() * multiplier / 255.0 + 0.5),
                                                     (int)(rgbColor.getB() * multiplier / 255.0 + 0.5));
                blockMapColorList.add(BlockMapColor.of(baseBlockMapColor.getMapColorId(), baseBlockMapColor.getBlockId(), variantColor, gradient));
            }
        }
        return blockMapColorList;
    }

    private static void saveBlockMapColorList(String version, List<BlockMapColor> blockMapColorList) throws Exception {
        final var outputFilePath = String.format("%s/%s/block/map_color.json", ROOT_DIR_PATH, version);
        mapper.writeValue(new File(outputFilePath), blockMapColorList);
    }

    public static List<BlockRgbColor> loadBlockRgbColorList(String version) throws Exception {
        final var inputFilePath = String.format("%s/%s/block/rgb_color.json", ROOT_DIR_PATH, version);
        return mapper.readValue(new File(inputFilePath), new TypeReference<>(){});
    }

    public static List<BlockLabColor> loadBlockLabColorList(String version) throws Exception {
        final var inputFilePath = String.format("%s/%s/block/lab_color.json", ROOT_DIR_PATH, version);
        return mapper.readValue(new File(inputFilePath), new TypeReference<>(){});
    }

    public static List<BlockMapColor> loadBlockMapColorList(String version) throws Exception {
        final var inputFilePath = String.format("%s/%s/block/map_color.json", ROOT_DIR_PATH, version);
        return mapper.readValue(new File(inputFilePath), new TypeReference<>(){});
    }

    public static void main(String[] args) {
        try {
//            final var texturedBlockModelDefineList = generateTexturedBlockModelDefineList(VERSION);
////            log.info("texturedBlockDefineList : {}", texturedBlockModelDefineList);
//            log.info("texturedBlockDefineList : {}", texturedBlockModelDefineList.size());
//            final var blockRgbColorList = generateBlockRgbColorList(texturedBlockModelDefineList);
//            saveBlockRgbColorList(VERSION, rgbColorList);
//            final var blockLabColorList = generateBlockLabColorList(texturedBlockModelDefineList);
//            saveBlockLabColorList(VERSION, blockLabColorList);
            final var baseBlockMapColorList = generateBaseBlockMapColorList(VERSION);
            final var blockMapColorList = generateBlockMapColorList(baseBlockMapColorList);
//            log.info("blockMapColorList : {}", blockMapColorList);
            log.info("blockMapColorList : {}", blockMapColorList.size());
            saveBlockMapColorList(VERSION, blockMapColorList);
        } catch (Exception e) {
            log.error("DataGenerationApp", e);
        }
    }

    private DataGenerationApp() {}
}
