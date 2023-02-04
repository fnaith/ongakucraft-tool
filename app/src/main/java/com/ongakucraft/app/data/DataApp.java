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
public class DataApp {
    private static final String VERSION = "1.18.2";

    public static void main(String[] args) {
        try {
            final var blockPropertyDefineList = ArticDataApp.generateBlockPropertyDefineList(VERSION);
            final var blockDefineList = ArticDataApp.generateBlockDefineList(VERSION, blockPropertyDefineList);
            log.info("blockDefineList : {}", blockDefineList.size());
            final var blockModelDefineList = McassetApp.generateBlockModelDefineList(VERSION);
            log.info("blockModelDefineList : {}", blockModelDefineList.size());
            final var blockModelIdSet = blockModelDefineList.stream().map(BlockModelDefine::getId).collect(Collectors.toSet());
            for (var blockDefine : blockDefineList) {
                final var path = blockDefine.getId().getPath();
                if (blockDefine.isCollisionShapeFullBlock() &&
                        !"air".equals(path) &&
                        !"barrier".equals(path) &&
                        !"beacon".equals(path) &&
                        !"chorus_flower".equals(path) &&
                        !"dried_kelp_block".equals(path) &&
                        !"frosted_ice".equals(path) &&
                        !"grass_block".equals(path) &&
                        !"jigsaw".equals(path) &&
                        !"observer".equals(path) &&
                        !"respawn_anchor".equals(path) &&
                        !"slime_block".equals(path) &&
                        !path.startsWith("infested_") &&
                        !path.startsWith("waxed_") &&
                        !path.endsWith("shulker_box") &&
                        !blockModelIdSet.contains(path)) {
                    log.info("blockModelId : {}", path);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
