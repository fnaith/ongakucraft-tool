package com.ongakucraft.app.data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.BlockId;
import com.ongakucraft.core.block.define.BlockModelDefine;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class McassetApp {
    private static final String ROOT_DIR_PATH = "./data/mcasset";
    private static final String VERSION = "1.18.2";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<BlockModelDefine> generateBlockModelDefineList(String version) throws Exception {
        final var inputFilePath = String.format("%s/InventivetalentDev-minecraft-assets-%s/assets/minecraft/models/block/_all.json", ROOT_DIR_PATH, version);
        final var bytes = FileUtils.readFileToByteArray(new File(inputFilePath));
        final var jsonNode = mapper.readTree(bytes);
        final Set<String> idSet = new HashSet<>();
        final List<BlockModelDefine> blockModelDefineList = new ArrayList<>();
        for (final var it = jsonNode.fields(); it.hasNext(); ) {
            final var entry = it.next();
            final var id = entry.getKey();
            if (idSet.contains(id)) {
                throw new OcException("duplicated id : %s", id);
            }
            idSet.add(id);
            final var blockId = new BlockId(id);
            final var node = entry.getValue();
            final var parentNode = node.get("parent");
            if (null == parentNode || !parentNode.isTextual()) {
                continue;
            }
            final var texturesNode = node.get("textures");
            if (null == texturesNode || !texturesNode.isObject()) {
                continue;
            }
            final var parent = parentNode.asText();
            switch (parent) {
                case "minecraft:block/cube" -> {
                    final var north = texturesNode.get("north").asText(null);
                    if (null == north) {
                        throw new OcException("north field is missing : %s", id);
                    }
                    final var south = texturesNode.get("south").asText(null);
                    if (null == south) {
                        throw new OcException("south field is missing : %s", id);
                    }
                    final var east = texturesNode.get("east").asText(null);
                    if (null == east) {
                        throw new OcException("east field is missing : %s", id);
                    }
                    final var west = texturesNode.get("west").asText(null);
                    if (null == west) {
                        throw new OcException("west field is missing : %s", id);
                    }
                    final var up = texturesNode.get("up").asText(null);
                    if (null == up) {
                        throw new OcException("up field is missing : %s", id);
                    }
                    final var down = texturesNode.get("down").asText(null);
                    if (null == down) {
                        throw new OcException("down field is missing : %s", id);
                    }
                    blockModelDefineList.add(BlockModelDefine.of(blockId, down, up, north, south, west, east));
                }
                case "minecraft:block/cube_all", "minecraft:block/leaves" -> {
                    final var all = texturesNode.get("all").asText(null);
                    if (null == all) {
                        throw new OcException("all field is missing : %s", id);
                    }
                    blockModelDefineList.add(BlockModelDefine.of(blockId, all, all, all, all, all, all));
                }
                case "minecraft:block/cube_bottom_top" -> {
                    final var top = texturesNode.get("top").asText(null);
                    if (null == top) {
                        throw new OcException("top field is missing : %s", id);
                    }
                    final var bottom = texturesNode.get("bottom").asText(null);
                    if (null == bottom) {
                        throw new OcException("bottom field is missing : %s", id);
                    }
                    final var side = texturesNode.get("side").asText(null);
                    if (null == side) {
                        throw new OcException("side field is missing : %s", id);
                    }
                    blockModelDefineList.add(BlockModelDefine.of(blockId, bottom, top, side, side, side, side));
                }
                case "minecraft:block/cube_column", "block/cube_column" -> {
                    final var end = texturesNode.get("end").asText(null);
                    if (null == end) {
                        throw new OcException("end field is missing : %s", id);
                    }
                    final var side = texturesNode.get("side").asText(null);
                    if (null == side) {
                        throw new OcException("side field is missing : %s", id);
                    }
                    blockModelDefineList.add(BlockModelDefine.of(blockId, end, end, side, side, side, side));
                }
                case "minecraft:block/cube_top" -> {
                    final var top = texturesNode.get("top").asText(null);
                    if (null == top) {
                        throw new OcException("top field is missing : %s", id);
                    }
                    final var side = texturesNode.get("side").asText(null);
                    if (null == side) {
                        throw new OcException("side field is missing : %s", id);
                    }
                    blockModelDefineList.add(BlockModelDefine.of(blockId, side, top, side, side, side, side));
                }
                case "minecraft:block/orientable" -> {
                    final var top = texturesNode.get("top").asText(null);
                    if (null == top) {
                        throw new OcException("top field is missing : %s", id);
                    }
                    final var front = texturesNode.get("front").asText(null);
                    if (null == front) {
                        throw new OcException("front field is missing : %s", id);
                    }
                    final var side = texturesNode.get("side").asText(null);
                    if (null == side) {
                        throw new OcException("side field is missing : %s", id);
                    }
                    blockModelDefineList.add(BlockModelDefine.of(blockId, top, top, front, side, side, side));
                }
                case "minecraft:block/orientable_with_bottom" -> {
                    final var top = texturesNode.get("top").asText(null);
                    if (null == top) {
                        throw new OcException("top field is missing : %s", id);
                    }
                    final var bottom = texturesNode.get("bottom").asText(null);
                    if (null == bottom) {
                        throw new OcException("bottom field is missing : %s", id);
                    }
                    final var side = texturesNode.get("side").asText(null);
                    if (null == side) {
                        throw new OcException("side field is missing : %s", id);
                    }
                    final var front = texturesNode.get("front").asText(null);
                    if (null == front) {
                        throw new OcException("front field is missing : %s", id);
                    }
                    blockModelDefineList.add(BlockModelDefine.of(blockId, bottom, top, front, side, side, side));
                }
                case "minecraft:block/template_glazed_terracotta" -> {
                    final var pattern = texturesNode.get("pattern").asText(null);
                    if (null == pattern) {
                        throw new OcException("pattern field is missing : %s", id);
                    }
                    blockModelDefineList.add(BlockModelDefine.of(blockId, pattern, pattern, pattern, pattern, pattern, pattern));
                }
                case "minecraft:block/template_single_face" -> {
                    final var texture = texturesNode.get("texture").asText(null);
                    if (null == texture) {
                        throw new OcException("texture field is missing : %s", id);
                    }
                    blockModelDefineList.add(BlockModelDefine.of(blockId, texture, texture, texture, texture, texture, texture));
                }
            }
        }
        return blockModelDefineList;
    }

//    public static void generateGrassTopImage(String version) throws Exception {
//        final var dirtFilePath = String.format("%s/InventivetalentDev-minecraft-assets-%s/assets/minecraft/textures/block/dirt.png", ROOT_DIR_PATH, version);
//        final var grassBlockTopOverlayFilePath = String.format("%s/InventivetalentDev-minecraft-assets-%s/assets/minecraft/textures/block/grass_block_top.png", ROOT_DIR_PATH, version);
//        final var grassBlockTopFilePath = String.format("%s/InventivetalentDev-minecraft-assets-%s/assets/minecraft/textures/generated/grass_block_top_blend.png", ROOT_DIR_PATH, version);
//        final var dirtImage = argb(ImageIO.read(new File(dirtFilePath)));
//        final var grassBlockTopImage = new BufferedImage(dirtImage.getWidth(), dirtImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
//        final var grassBlockTopOverlayImage = argb(ImageIO.read(new File(grassBlockTopOverlayFilePath)));
//        final var g2  = grassBlockTopImage.createGraphics();
//        g2.setComposite(AlphaComposite.Src);
//        g2.drawImage(dirtImage, 0, 0, null);
//        g2.setComposite(BlendComposite.Overlay);
//        g2.drawImage(grassBlockTopOverlayImage, 0, 0, null);
//        g2.dispose();
//        ImageIO.write(grassBlockTopImage, "PNG", new File(grassBlockTopFilePath));
//    }
//
//    private static BufferedImage argb(BufferedImage im) {
//        final var w = im.getWidth();
//        final var h = im.getHeight();
//        final var out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
//        out.getGraphics().drawImage(im.getScaledInstance(w, h, Image.SCALE_DEFAULT), 0, 0, null);
//        return out;
//    }

    public static void main(String[] args) {
        try {
            final var blockModelDefineList = generateBlockModelDefineList(VERSION);
//            log.info("blockModelDefineList : {}", blockModelDefineList);
            log.info("blockModelDefineList : {}", blockModelDefineList.size());
        } catch (Exception e) {
            log.error("McassetApp", e);
        }
    }

    private McassetApp() {}
}
