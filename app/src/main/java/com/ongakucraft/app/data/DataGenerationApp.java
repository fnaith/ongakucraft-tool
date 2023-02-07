package com.ongakucraft.app.data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.BlockId;
import com.ongakucraft.core.block.define.BlockModelDefine;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class DataGenerationApp {
    private static final String VERSION = "1.18.2";

    public static void main(String[] args) {
        try {
            final var blockPropertyDefineList = ArticDataApp.generateBlockPropertyDefineList(VERSION);
            final var blockDefineList = ArticDataApp.generateBlockDefineList(VERSION, blockPropertyDefineList);
            log.info("blockDefineList : {}", blockDefineList.size());
            final var blockModelDefineList = McassetApp.generateBlockModelDefineList(VERSION);
            log.info("blockModelDefineList : {}", blockModelDefineList.size());
            final var blockModelIdSet = blockModelDefineList.stream().map(BlockModelDefine::getId).collect(Collectors.toSet());
            final List<BlockId> texturedBlockIdList = new ArrayList<>();
            for (var blockDefine : blockDefineList) {
                final var path = blockDefine.getId().getPath();
                if (blockDefine.isCollisionShapeFullBlock() &&
                        !"air".equals(path) &&
                        !"barrier".equals(path) &&
                        !"beacon".equals(path) &&
                        !"chorus_flower".equals(path) &&
                        !"chain_command_block".equals(path) &&
                        !"command_block".equals(path) &&
                        !"dried_kelp_block".equals(path) &&
                        !"frosted_ice".equals(path) &&
                        !"grass_block".equals(path) && // https://www.quora.com/Why-cant-I-change-the-colors-of-the-grass-and-leaves-in-Minecraft
                        !"jigsaw".equals(path) &&
                        !"observer".equals(path) &&
                        !"repeating_command_block".equals(path) &&
                        !"respawn_anchor".equals(path) &&
                        !"slime_block".equals(path) &&
                        !path.startsWith("infested_") &&
                        !path.startsWith("waxed_") &&
                        !path.endsWith("shulker_box") &&
                        !blockModelIdSet.contains(blockDefine.getId())) {
                    throw new OcException("blockModelId : %s", path);
                }
                texturedBlockIdList.add(blockDefine.getId());
            }
            log.info("texturedBlockIdList : {}", texturedBlockIdList);
        } catch (Exception e) {
            log.error("DataGenerationApp", e);
        }
    }

    private DataGenerationApp() {}
}
