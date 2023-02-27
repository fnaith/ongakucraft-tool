package com.ongakucraft.app.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.BlockId;
import com.ongakucraft.core.block.Direction;
import com.ongakucraft.core.block.define.BlockLabColorDefine;
import com.ongakucraft.core.block.define.BlockModelDefine;
import com.ongakucraft.core.block.define.BlockRgbColorDefine;
import com.ongakucraft.core.color.ColorConverter;
import com.ongakucraft.core.color.LabColor;
import com.ongakucraft.core.color.RgbColor;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
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
            final var bufferedImage = ImageIO.read(new File(texture));
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

    private static List<BlockRgbColorDefine> generateRgbColorDefineList(List<BlockModelDefine> texturedBlockModelDefineList) throws Exception {
        final List<BlockRgbColorDefine> blockRgbColorDefineList = new ArrayList<>();
        final Map<String, RgbColor> textureToColor = new HashMap<>();
        for (final var blockModelDefine : texturedBlockModelDefineList) {
            final Map<Direction, RgbColor> colors = new EnumMap<>(Direction.class);
            for (final var entry : blockModelDefine.getTextures().entrySet()) {
                final var texture = entry.getValue();
                final var color = textureToColor.computeIfAbsent(texture, DataGenerationApp::generateRgbColor);
                colors.put(entry.getKey(), color);
            }
            blockRgbColorDefineList.add(BlockRgbColorDefine.of(blockModelDefine.getId(), colors));
        }
        return blockRgbColorDefineList;
    }

    private static void saveBlockRgbColorDefineList(String version, List<BlockRgbColorDefine> blockRgbColorDefineList) throws Exception {
        final var outputFilePath = String.format("%s/%s/block/rgb.json", ROOT_DIR_PATH, version);
        mapper.writeValue(new File(outputFilePath), blockRgbColorDefineList);
    }

    private static LabColor generateLabColor(String texture) {
        try {
            final var bufferedImage = ImageIO.read(new File(texture));
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

    private static List<BlockLabColorDefine> generateLabColorDefineList(List<BlockModelDefine> texturedBlockModelDefineList) throws Exception {
        final List<BlockLabColorDefine> blockLabColorDefineList = new ArrayList<>();
        final Map<String, LabColor> textureToColor = new HashMap<>();
        for (final var blockModelDefine : texturedBlockModelDefineList) {
            final Map<Direction, LabColor> colors = new EnumMap<>(Direction.class);
            for (final var entry : blockModelDefine.getTextures().entrySet()) {
                final var texture = entry.getValue();
                final var color = textureToColor.computeIfAbsent(texture, DataGenerationApp::generateLabColor);
                colors.put(entry.getKey(), color);
            }
            blockLabColorDefineList.add(BlockLabColorDefine.of(blockModelDefine.getId(), colors));
        }
        return blockLabColorDefineList;
    }

    private static void saveBlockLabColorDefineList(String version, List<BlockLabColorDefine> blockRgbColorDefineList) throws Exception {
        final var outputFilePath = String.format("%s/%s/block/lab.json", ROOT_DIR_PATH, version);
        mapper.writeValue(new File(outputFilePath), blockRgbColorDefineList);
    }

    public static List<BlockRgbColorDefine> loadBlockRgbColorDefineList(String version) throws Exception {
        final var inputFilePath = String.format("%s/%s/block/rgb.json", ROOT_DIR_PATH, version);
        return mapper.readValue(new File(inputFilePath), new TypeReference<>(){});
    }

    public static List<BlockLabColorDefine> loadBlockLabColorDefineList(String version) throws Exception {
        final var inputFilePath = String.format("%s/%s/block/lab.json", ROOT_DIR_PATH, version);
        return mapper.readValue(new File(inputFilePath), new TypeReference<>(){});
    }

    public static void main(String[] args) {
        try {
            final var texturedBlockModelDefineList = generateTexturedBlockModelDefineList(VERSION);
            log.info("texturedBlockDefineList : {}", texturedBlockModelDefineList.size());
            final var blockRgbColorDefineList = generateRgbColorDefineList(texturedBlockModelDefineList);
            saveBlockRgbColorDefineList(VERSION, blockRgbColorDefineList);
            final var blockLabColorDefineList = generateLabColorDefineList(texturedBlockModelDefineList);
            saveBlockLabColorDefineList(VERSION, blockLabColorDefineList);
        } catch (Exception e) {
            log.error("DataGenerationApp", e);
        }
    }

    private DataGenerationApp() {}
}
