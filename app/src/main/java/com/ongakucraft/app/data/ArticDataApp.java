package com.ongakucraft.app.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.BlockId;
import com.ongakucraft.core.block.define.BlockDefine;
import com.ongakucraft.core.block.define.BlockMapColorDefine;
import com.ongakucraft.core.block.define.BlockPropertyDefine;
import com.ongakucraft.core.color.RgbColor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public final class ArticDataApp {
    private static final String ROOT_DIR_PATH = "./data/ArticData";
    private static final String VERSION = "1.18.2";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Pattern PATTERN_DOT = Pattern.compile("\\.");

    public static List<BlockPropertyDefine> generateBlockPropertyDefineList(String version) throws Exception {
        final var inputFilePath = String.format("%s/ArticData-%s/%s_block_properties.json", ROOT_DIR_PATH, version, PATTERN_DOT.matcher(version).replaceAll("_"));
        final var jsonNode = readTree(inputFilePath);
        final Set<String> idSet = new HashSet<>();
        final List<BlockPropertyDefine> blockPropertyDefineList = new ArrayList<>();
        for (final var it = jsonNode.fields(); it.hasNext(); ) {
            final var entry = it.next();
            final var id = entry.getKey();
            if (idSet.contains(id)) {
                throw new OcException("duplicated id : %s", id);
            }
            idSet.add(id);
            final var node = entry.getValue();
            final var key = node.get("key").asText(null);
            if (null == key) {
                throw new OcException("key field is missing : %s", id);
            }
            final var valuesNode = node.get("values");
            if (!valuesNode.isArray()) {
                throw new OcException("values field should be array : %s", id);
            }
            final List<String> values = new ArrayList<>();
            for (var valueNode : valuesNode) {
                final var value = valueNode.asText(null);
                values.add(value.toLowerCase());
            }
            blockPropertyDefineList.add(BlockPropertyDefine.of(id, key, values));
        }
        return blockPropertyDefineList;
    }

    public static List<BlockDefine> generateBlockDefineList(String version, List<BlockPropertyDefine> blockPropertyDefineList) throws Exception {
        final var inputFilePath = String.format("%s/ArticData-%s/%s_blocks.json", ROOT_DIR_PATH, version, PATTERN_DOT.matcher(version).replaceAll("_"));
        final var jsonNode = readTree(inputFilePath);
        final Set<String> idSet = new HashSet<>();
        final List<BlockDefine> blockDefineList = new ArrayList<>();
        final Map<String, BlockPropertyDefine> blockPropertyDefineMap = blockPropertyDefineList.stream()
                .collect(Collectors.toMap(BlockPropertyDefine::getId, Function.identity()));
        for (final var it = jsonNode.fields(); it.hasNext(); ) {
            final var entry = it.next();
            final var id = entry.getKey();
            if (idSet.contains(id)) {
                throw new OcException("duplicated id : %s", id);
            }
            idSet.add(id);
            final var blockIdOptional = BlockId.parse(id);
            if (blockIdOptional.isEmpty()) {
                throw new OcException("id is invalid : %s", id);
            }
            final var blockId = blockIdOptional.get();
            if (!BlockId.DEFAULT_NAMESPACE.equals(blockId.getNamespace())) {
                throw new OcException("namespace is invalid : %s", id);
            }
            final var node = entry.getValue();
            final var gravityNode = node.get("gravity");
            if (!gravityNode.isBoolean()) {
                throw new OcException("gravity field should be boolean : %s", id);
            }
            final var gravity = gravityNode.asBoolean();
            final var propertiesNode = node.get("properties");
            if (!propertiesNode.isArray()) {
                throw new OcException("properties field should be array : %s", id);
            }
            final List<BlockPropertyDefine> properties = new ArrayList<>();
            final Set<String> propertyKeySet = new HashSet<>();
            for (var propertyNode : propertiesNode) {
                final var property = propertyNode.asText(null);
                final var blockPropertyDefine = blockPropertyDefineMap.get(property);
                if (null == blockPropertyDefine) {
                    throw new OcException("properties field should not have null : %s", id);
                }
                if (propertyKeySet.contains(blockPropertyDefine.getKey())) {
                    throw new OcException("properties field should not have duplicated key : %s %d", id, blockPropertyDefine.getKey());
                }
                propertyKeySet.add(blockPropertyDefine.getKey());
                properties.add(blockPropertyDefine);
            }
            final var states = node.get("states");
            if (!states.isArray()) {
                throw new OcException("states field should be array : %s", id);
            }
            final var state = states.get(0);
            if (!state.isObject()) {
                throw new OcException("states[0] should be boolean : %s", id);
            }
            final var mapColorIdNode = state.get("mapColorId");
            if (!mapColorIdNode.isInt()) {
                throw new OcException("mapColorId field should be int : %s", id);
            }
            final var mapColorId = mapColorIdNode.asInt();
            final var liquidNode = state.get("liquid");
            if (!liquidNode.isBoolean()) {
                throw new OcException("liquid field should be boolean : %s", id);
            }
            final var liquid = liquidNode.asBoolean();
            final var randomlyTicksNode = state.get("randomlyTicks");
            if (!randomlyTicksNode.isBoolean()) {
                throw new OcException("randomlyTicks field should be boolean : %s", id);
            }
            final var randomlyTicks = randomlyTicksNode.asBoolean();
            final var collisionShapeFullBlockNode = state.get("collisionShapeFullBlock");
            if (!collisionShapeFullBlockNode.isBoolean()) {
                throw new OcException("collisionShapeFullBlock field should be boolean : %s", id);
            }
            final var collisionShapeFullBlock = collisionShapeFullBlockNode.asBoolean();
            blockDefineList.add(BlockDefine.of(blockId, properties,
                                               gravity, mapColorId,
                                               liquid, randomlyTicks, collisionShapeFullBlock));
        }
        return blockDefineList;
    }

    public static List<BlockMapColorDefine> generateBlockMapColorDefineList(String version) throws Exception {
        final var inputFilePath = String.format("%s/ArticData-%s/%s_map_colors.json", ROOT_DIR_PATH, version, PATTERN_DOT.matcher(version).replaceAll("_"));
        final var jsonNode = readTree(inputFilePath);
        final Set<Integer> idSet = new HashSet<>();
        final List<BlockMapColorDefine> blockMapColorDefineList = new ArrayList<>();
        for (final var node : jsonNode) {
            final var idNode = node.get("id");
            if (!idNode.isInt()) {
                throw new OcException("id field should be int : %s", idNode);
            }
            final var id = idNode.asInt();
            if (idSet.contains(id)) {
                throw new OcException("duplicated id : %s", id);
            }
            idSet.add(id);
            final var colorNode = node.get("color");
            if (!colorNode.isInt()) {
                throw new OcException("color field should be int : %s", id);
            }
            final var colorValue = colorNode.asInt();
            final var b = colorValue & 0xff;
            final var g = (colorValue >> 8) & 0xff;
            final var r = (colorValue >> 16) & 0xff;
            final var rgbColor = RgbColor.of(r, g, b);
            blockMapColorDefineList.add(BlockMapColorDefine.of(id, rgbColor));
        }
        return blockMapColorDefineList;
    }

    private static JsonNode readTree(String inputFilePath) throws IOException {
        final var bytes = FileUtils.readFileToByteArray(new File(inputFilePath));
        return mapper.readTree(bytes);
    }

    public static void main(String[] args) {
        try {
            final var blockPropertyDefineList = generateBlockPropertyDefineList(VERSION);
//            log.info("blockPropertyDefineList : {}", blockPropertyDefineList);
            log.info("blockPropertyDefineList : {}", blockPropertyDefineList.size());
            final var blockDefineList = generateBlockDefineList(VERSION, blockPropertyDefineList);
//            log.info("blockDefineList : {}", blockDefineList);
            log.info("blockDefineList : {}", blockDefineList.size());
            final var blockMapColorDefineList = generateBlockMapColorDefineList(VERSION);
//            log.info("blockMapColorDefineList : {}", blockMapColorDefineList);
            log.info("blockMapColorDefineList : {}", blockMapColorDefineList.size());
        } catch (Exception e) {
            log.error("ArticDataApp", e);
        }
    }

    private ArticDataApp() {}
}
