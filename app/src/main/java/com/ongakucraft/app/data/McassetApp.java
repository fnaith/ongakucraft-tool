package com.ongakucraft.app.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ongakucraft.core.block.BlockDefine;
import com.ongakucraft.core.block.BlockId;
import com.ongakucraft.core.block.BlockModelDefine;
import com.ongakucraft.core.block.BlockPropertyDefine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class McassetApp {
    private static final String ROOT_DIR_PATH = "./data/mcasset";
    private static final String VERSION = "1.18.2";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<BlockModelDefine> generateBlockModelDefineList(String version) throws Exception {
        final var inputFilePath = String.format("%s/InventivetalentDev-minecraft-assets-%s/assets/minecraft/models/block/_all.json", ROOT_DIR_PATH, version);
        final var bytes = FileUtils.readFileToByteArray(new File(inputFilePath));
        final var jsonNode = mapper.readTree(bytes);
        final Set<String> idSet = new HashSet<>();
        final List<BlockModelDefine> blockModelDefineList = new ArrayList<>();
        for (var it = jsonNode.fields(); it.hasNext(); ) {
            final var entry = it.next();
            final var id = entry.getKey();
            if (idSet.contains(id)) {
                log.error("duplicated id : {}", id);
                continue;
            }
            idSet.add(id);
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
                        log.error("north field is missing : {}", id);
                        break;
                    }
                    final var south = texturesNode.get("south").asText(null);
                    if (null == south) {
                        log.error("south field is missing : {}", id);
                        break;
                    }
                    final var east = texturesNode.get("east").asText(null);
                    if (null == east) {
                        log.error("east field is missing : {}", id);
                        break;
                    }
                    final var west = texturesNode.get("west").asText(null);
                    if (null == west) {
                        log.error("west field is missing : {}", id);
                        break;
                    }
                    final var up = texturesNode.get("up").asText(null);
                    if (null == up) {
                        log.error("up field is missing : {}", id);
                        break;
                    }
                    final var down = texturesNode.get("down").asText(null);
                    if (null == down) {
                        log.error("down field is missing : {}", id);
                        break;
                    }
                    blockModelDefineList.add(new BlockModelDefine(id, down, up, north, south, west, east));
                }
                case "minecraft:block/cube_all" -> {
                    final var all = texturesNode.get("all").asText(null);
                    if (null == all) {
                        log.error("all field is missing : {}", id);
                        break;
                    }
                    blockModelDefineList.add(new BlockModelDefine(id, all, all, all, all, all, all));
                }
                case "minecraft:block/cube_bottom_top" -> {
                    final var top = texturesNode.get("top").asText(null);
                    if (null == top) {
                        log.error("top field is missing : {}", id);
                        break;
                    }
                    final var bottom = texturesNode.get("bottom").asText(null);
                    if (null == bottom) {
                        log.error("bottom field is missing : {}", id);
                        break;
                    }
                    final var side = texturesNode.get("side").asText(null);
                    if (null == side) {
                        log.error("side field is missing : {}", id);
                        break;
                    }
                    blockModelDefineList.add(new BlockModelDefine(id, bottom, top, side, side, side, side));
                }
                case "minecraft:block/cube_column" -> {
                    final var end = texturesNode.get("end").asText(null);
                    if (null == end) {
                        log.error("end field is missing : {}", id);
                        break;
                    }
                    final var side = texturesNode.get("side").asText(null);
                    if (null == side) {
                        log.error("side field is missing : {}", id);
                        break;
                    }
                    blockModelDefineList.add(new BlockModelDefine(id, end, end, side, side, side, side));
                }
                case "block/cube_column" -> {
                    final var end = texturesNode.get("end").asText(null);
                    if (null == end) {
                        log.error("end field is missing : {}", id);
                        break;
                    }
                    final var side = texturesNode.get("side").asText(null);
                    if (null == side) {
                        log.error("side field is missing : {}", id);
                        break;
                    }
                    blockModelDefineList.add(new BlockModelDefine(id, end, end, side, side, side, side));
                }
                case "minecraft:block/cube_top" -> {
                    final var top = texturesNode.get("top").asText(null);
                    if (null == top) {
                        log.error("top field is missing : {}", id);
                        break;
                    }
                    final var side = texturesNode.get("side").asText(null);
                    if (null == side) {
                        log.error("side field is missing : {}", id);
                        break;
                    }
                    blockModelDefineList.add(new BlockModelDefine(id, side, top, side, side, side, side));
                }
                case "minecraft:block/leaves" -> {
                    final var all = texturesNode.get("all").asText(null);
                    if (null == all) {
                        log.error("all field is missing : {}", id);
                        break;
                    }
                    blockModelDefineList.add(new BlockModelDefine(id, all, all, all, all, all, all));
                }
                case "minecraft:block/orientable" -> {
                    final var top = texturesNode.get("top").asText(null);
                    if (null == top) {
                        log.error("top field is missing : {}", id);
                        break;
                    }
                    final var front = texturesNode.get("front").asText(null);
                    if (null == front) {
                        log.error("front field is missing : {}", id);
                        break;
                    }
                    final var side = texturesNode.get("side").asText(null);
                    if (null == side) {
                        log.error("side field is missing : {}", id);
                        break;
                    }
                    blockModelDefineList.add(new BlockModelDefine(id, top, top, front, side, side, side));
                }
                case "minecraft:block/orientable_with_bottom" -> {
                    final var top = texturesNode.get("top").asText(null);
                    if (null == top) {
                        log.error("top field is missing : {}", id);
                        break;
                    }
                    final var bottom = texturesNode.get("bottom").asText(null);
                    if (null == bottom) {
                        log.error("bottom field is missing : {}", id);
                        break;
                    }
                    final var side = texturesNode.get("side").asText(null);
                    if (null == side) {
                        log.error("side field is missing : {}", id);
                        break;
                    }
                    final var front = texturesNode.get("front").asText(null);
                    if (null == front) {
                        log.error("front field is missing : {}", id);
                        break;
                    }
                    blockModelDefineList.add(new BlockModelDefine(id, bottom, top, front, side, side, side));
                }
                case "minecraft:block/template_command_block" -> {
                    final var front = texturesNode.get("front").asText(null);
                    if (null == front) {
                        log.error("front field is missing : {}", id);
                        break;
                    }
                    final var back = texturesNode.get("back").asText(null);
                    if (null == back) {
                        log.error("back field is missing : {}", id);
                        break;
                    }
                    final var side = texturesNode.get("side").asText(null);
                    if (null == side) {
                        log.error("side field is missing : {}", id);
                        break;
                    }
                    blockModelDefineList.add(new BlockModelDefine(id, side, side, front, back, side, side));
                }
                case "minecraft:block/template_glazed_terracotta" -> {
                    final var pattern = texturesNode.get("pattern").asText(null);
                    if (null == pattern) {
                        log.error("pattern field is missing : {}", id);
                        break;
                    }
                    blockModelDefineList.add(new BlockModelDefine(id, pattern, pattern, pattern, pattern, pattern, pattern));
                }
                case "minecraft:block/template_single_face" -> {
                    final var texture = texturesNode.get("texture").asText(null);
                    if (null == texture) {
                        log.error("texture field is missing : {}", id);
                        break;
                    }
                    blockModelDefineList.add(new BlockModelDefine(id, texture, texture, texture, texture, texture, texture));
                }
                case "minecraft:block/cube_column_horizontal" -> {}
                case "minecraft:block/cross" -> {}
            }
        }
        return blockModelDefineList;
    }

    public static void main(String[] args) {
        try {
            final var blockModelDefineList = generateBlockModelDefineList(VERSION);
//            log.info("blockModelDefineList : {}", blockModelDefineList);
            log.info("blockModelDefineList : {}", blockModelDefineList.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
