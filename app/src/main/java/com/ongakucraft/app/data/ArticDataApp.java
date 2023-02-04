package com.ongakucraft.app.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ongakucraft.core.block.BlockDefine;
import com.ongakucraft.core.block.BlockId;
import com.ongakucraft.core.block.BlockPropertyDefine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class ArticDataApp {
    private static final String ROOT_DIR_PATH = "./data/ArticData";
    private static final String VERSION = "1.18.2";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<BlockPropertyDefine> generateBlockPropertyDefineList(String version) throws Exception {
        final var inputFilePath = String.format("%s/ArticData-%s/%s_block_properties.json", ROOT_DIR_PATH, version, version.replaceAll("\\.", "_"));
        final var bytes = FileUtils.readFileToByteArray(new File(inputFilePath));
        final var jsonNode = mapper.readTree(bytes);
        final Set<String> idSet = new HashSet<>();
        final List<BlockPropertyDefine> blockPropertyDefineList = new ArrayList<>();
        for (var it = jsonNode.fields(); it.hasNext(); ) {
            final var entry = it.next();
            final var id = entry.getKey();
            if (idSet.contains(id)) {
                log.error("duplicated id : {}", id);
                continue;
            }
            idSet.add(id);
            final var node = entry.getValue();
            final var key = node.get("key").asText(null);
            if (null == key) {
                log.error("key field is missing : {}", id);
                continue;
            }
            final var valuesNode = node.get("values");
            if (null == valuesNode) {
                log.error("values field is missing: {}", id);
                continue;
            }
            if (!valuesNode.isArray()) {
                log.error("values field should be array : {}", id);
                continue;
            }
            var nullCount = 0;
            final List<String> values = new ArrayList<>();
            for (var valueNode : valuesNode) {
                final var value = valueNode.asText(null);
                if (null == value) {
                    ++nullCount;
                    continue;
                }
                values.add(value.toLowerCase());
            }
            if (0 < nullCount) {
                log.error("values field should not have null : {} {}", id, nullCount);
                continue;
            }
            blockPropertyDefineList.add(new BlockPropertyDefine(id, key, values));
        }
        return blockPropertyDefineList;
    }

    public static List<BlockDefine> generateBlockDefineList(String version, List<BlockPropertyDefine> blockPropertyDefineList) throws Exception {
        final var inputFilePath = String.format("%s/ArticData-%s/%s_blocks.json", ROOT_DIR_PATH, version, version.replaceAll("\\.", "_"));
        final var bytes = FileUtils.readFileToByteArray(new File(inputFilePath));
        final var jsonNode = mapper.readTree(bytes);
        final Set<String> idSet = new HashSet<>();
        final List<BlockDefine> blockDefineList = new ArrayList<>();
        final Map<String, BlockPropertyDefine> blockPropertyDefineMap = blockPropertyDefineList.stream()
                .collect(Collectors.toMap(BlockPropertyDefine::getId, Function.identity()));
        for (var it = jsonNode.fields(); it.hasNext(); ) {
            final var entry = it.next();
            final var id = entry.getKey();
            if (idSet.contains(id)) {
                log.error("duplicated id : {}", id);
                continue;
            }
            idSet.add(id);
            if (!id.startsWith("minecraft:")) {
                log.error("id is invalid : {}", id);
                continue;
            }
            final var blockId = new BlockId("minecraft", id.substring(10));
            final var node = entry.getValue();
            final var states = node.get("states");
            if (null == states) {
                log.error("states field is missing: {}", id);
                continue;
            }
            if (!states.isArray()) {
                log.error("states field should be array : {}", id);
                continue;
            }
            final var state = states.get(0);
            if (null == state) {
                log.error("states[0] is missing : {}", id);
                continue;
            }
            if (!state.isObject()) {
                log.error("states[0] should be boolean : {}", id);
                continue;
            }
            final var collisionShapeFullBlockNode = state.get("collisionShapeFullBlock");
            if (null == collisionShapeFullBlockNode) {
                log.error("collisionShapeFullBlock field is missing : {}", id);
                continue;
            }
            if (!collisionShapeFullBlockNode.isBoolean()) {
                log.error("collisionShapeFullBlock field should be boolean : {}", id);
                continue;
            }
            final var collisionShapeFullBlock = collisionShapeFullBlockNode.asBoolean();
            final var propertiesNode = node.get("properties");
            if (null == propertiesNode) {
                log.error("properties field is missing: {}", id);
                continue;
            }
            if (!propertiesNode.isArray()) {
                log.error("properties field should be array : {}", id);
                continue;
            }
            var nullCount = 0;
            final List<BlockPropertyDefine> properties = new ArrayList<>();
            for (var propertyNode : propertiesNode) {
                final var property = propertyNode.asText(null);
                if (null == property) {
                    ++nullCount;
                    continue;
                }
                final var blockPropertyDefine = blockPropertyDefineMap.get(property);
                if (null == blockPropertyDefine) {
                    log.error("properties field should not have invalid element : {} {}", id, property);
                    ++nullCount;
                    continue;
                }
                properties.add(blockPropertyDefine);
            }
            if (0 < nullCount) {
                log.error("properties field should not have null : {} {}", id, nullCount);
                continue;
            }
            blockDefineList.add(new BlockDefine(blockId, collisionShapeFullBlock, properties));
        }
        return blockDefineList;
    }

    public static void main(String[] args) {
        try {
            final var blockPropertyDefineList = generateBlockPropertyDefineList(VERSION);
//            log.info("blockPropertyDefineList : {}", blockPropertyDefineList);
//            log.info("blockPropertyDefineList : {}", blockPropertyDefineList.size());
            final var blockDefineList = generateBlockDefineList(VERSION, blockPropertyDefineList);
//            log.info("blockDefineList : {}", blockDefineList);
//            log.info("blockDefineList : {}", blockDefineList.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
