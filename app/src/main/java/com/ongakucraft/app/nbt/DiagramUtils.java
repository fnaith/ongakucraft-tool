package com.ongakucraft.app.nbt;

import com.ongakucraft.app.graphics.GraphicUtils;
import com.ongakucraft.core.OcException;
import com.ongakucraft.core.block.Direction;
import com.ongakucraft.core.structure.Position;
import com.ongakucraft.core.structure.Structure;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public final class DiagramUtils {
    private static final Map<String, String> en = new HashMap<>();
    private static final Map<String, String> tw = new HashMap<>();
    static {
        en.put("circuit-prefix", "circuit-");
        en.put("step-1-file-name", "step-1-place-layer1-wool-and-instrument-block.png");
        en.put("step-2-1-file-name", "step-2-1-place-layer2-redstone-and-repeater.png");
        en.put("step-2-2-file-name", "step-2-2-layer2-repeater-click-times.png");
        en.put("step-3-1-file-name", "step-3-1-place-layer2-noteblock.png");
        en.put("step-3-2-file-name", "step-3-2-layer2-noteblock-click-times.png");
        en.put("step-4-file-name", "step4-place-layer2-lamp.png");
        tw.put("circuit-prefix", "音軌-");
        tw.put("step-1-file-name", "第1步-放置第一層羊毛與樂器方塊.png");
        tw.put("step-2-1-file-name", "第2-1步-放置第二層紅石粉與中繼器.png");
        tw.put("step-2-2-file-name", "第2-2步-第二層中繼器右鍵次數.png");
        tw.put("step-3-1-file-name", "第3-1步-放置第二層音階盒.png");
        tw.put("step-3-2-file-name", "第3-2步-第二層音階盒右鍵次數.png");
        tw.put("step-4-file-name", "第4步-第二層放置羊毛.png");
    }

    public static void drawDesignDiagram(Structure structure, int pixelSize, String dirPath, String label, String lang) {
        try {
            final var xlt = "tw".equals(lang) ? tw : en;
            final var textures = loadBlockTextures(pixelSize);
            final var monoColorDigits = loadMonoColorDigits(pixelSize);
            final var gradientDigits = loadGradientDigits(pixelSize);
            final var xLength = structure.getRange3().getX().length();
            final var zLength = structure.getRange3().getZ().length();
            log.info("structure size : {} {} {}", xLength, structure.getRange3().getY().length(), zLength);
            final var xStart = structure.getRange3().getX().getStart();
            final var zStart = structure.getRange3().getZ().getStart();

            final var img1 = new BufferedImage(pixelSize * xLength, pixelSize * zLength, BufferedImage.TYPE_INT_ARGB);
            final var g2d1 = img1.createGraphics();
            for (var z = structure.getRange3().getZ().getStart(); z < structure.getRange3().getZ().getStop(); ++z) {
                for (var x = 0; x < xLength; ++x) {
                    final var block = structure.get(Position.of(x + xStart, 0, z + zStart));
                    if (null == block) {
                        continue;
                    }
                    final var texture = textures.get(block.getId().getPath());
                    if (null == texture) {
                        continue;
                    }
                    g2d1.drawImage(texture, pixelSize * (xLength - 1 - x), pixelSize * (zLength - 1 - z), null);
                }
            }
            g2d1.dispose();
            GraphicUtils.writeImage(img1, dirPath + '/' + xlt.get("circuit-prefix") + label + '/' + tw.get("step-1-file-name"));

            final var img2 = GraphicUtils.copyWithAlpha(img1);
            final var g2d2 = img2.createGraphics();
            for (var z = structure.getRange3().getZ().getStart(); z < structure.getRange3().getZ().getStop(); ++z) {
                for (var x = 0; x < xLength; ++x) {
                    final var block = structure.get(Position.of(x + xStart, 1, z + zStart));
                    if (null == block) {
                        continue;
                    }
                    if (!"redstone_wire".equals(block.getId().getPath()) && !"repeater".equals(block.getId().getPath())) {
                        continue;
                    }
                    var texture = textures.get(block.getId().getPath());
                    if (null == texture) {
                        continue;
                    }
                    if ("repeater".equals(block.getId().getPath())) {
                        var dir = block.getFacing();
                        while (Direction.N != dir) {
                            dir = dir.rotate(1);
                            texture = GraphicUtils.rotate(texture, true);
                        }
                    }
                    g2d2.drawImage(texture, pixelSize * (xLength - 1 - x), pixelSize * (zLength - 1 - z), null);
                }
            }
            g2d2.dispose();
            GraphicUtils.writeImage(img2, dirPath + '/' + xlt.get("circuit-prefix") + label + '/' + tw.get("step-2-1-file-name"));

            final var img22 = GraphicUtils.copyWithAlpha(img2);
            final var g2d22 = img22.createGraphics();
            for (var z = structure.getRange3().getZ().getStart(); z < structure.getRange3().getZ().getStop(); ++z) {
                for (var x = 0; x < xLength; ++x) {
                    final var block = structure.get(Position.of(x + xStart, 1, z + zStart));
                    if (null == block) {
                        continue;
                    }
                    if (!"repeater".equals(block.getId().getPath())) {
                        continue;
                    }
                    final var click = Integer.parseInt(block.getProperty("delay"));
                    final var texture = monoColorDigits.get(click);
                    g2d22.drawImage(texture, pixelSize * (xLength - 1 - x), pixelSize * (zLength - 1 - z), null);
                }
            }
            g2d22.dispose();

            final var img23 = GraphicUtils.copyWithAlpha(img2);
            final var g2d23 = img23.createGraphics();
            for (var z = structure.getRange3().getZ().getStart(); z < structure.getRange3().getZ().getStop(); ++z) {
                for (var x = 0; x < xLength; ++x) {
                    final var block = structure.get(Position.of(x + xStart, 1, z + zStart));
                    if (null == block) {
                        continue;
                    }
                    if (!"repeater".equals(block.getId().getPath())) {
                        continue;
                    }
                    final var click = Integer.parseInt(block.getProperty("delay")) - 1;
                    final var texture = monoColorDigits.get(click);
                    g2d23.drawImage(texture, pixelSize * (xLength - 1 - x), pixelSize * (zLength - 1 - z), null);
                }
            }
            g2d23.dispose();
            GraphicUtils.writeImage(img23, dirPath + '/' + xlt.get("circuit-prefix") + label + '/' + tw.get("step-2-2-file-name"));

            final var img3 = GraphicUtils.copyWithAlpha(img2);
            final var g2d3 = img3.createGraphics();
            for (var z = structure.getRange3().getZ().getStart(); z < structure.getRange3().getZ().getStop(); ++z) {
                for (var x = 0; x < xLength; ++x) {
                    final var block = structure.get(Position.of(x + xStart, 1, z + zStart));
                    if (null == block) {
                        continue;
                    }
                    if (!"note_block".equals(block.getId().getPath())) {
                        continue;
                    }
                    final var texture = textures.get(block.getId().getPath());
                    if (null == texture) {
                        continue;
                    }
                    g2d3.drawImage(texture, pixelSize * (xLength - 1 - x), pixelSize * (zLength - 1 - z), null);
                }
            }
            g2d3.dispose();
            GraphicUtils.writeImage(img3, dirPath + '/' + xlt.get("circuit-prefix") + label + '/' + tw.get("step-3-1-file-name"));

            final var img32 = GraphicUtils.copyWithAlpha(img3);
            final var g2d32 = img32.createGraphics();
            for (var z = structure.getRange3().getZ().getStart(); z < structure.getRange3().getZ().getStop(); ++z) {
                for (var x = 0; x < xLength; ++x) {
                    final var block = structure.get(Position.of(x + xStart, 1, z + zStart));
                    if (null == block) {
                        continue;
                    }
                    if (!"note_block".equals(block.getId().getPath())) {
                        continue;
                    }
                    final var click = Integer.parseInt(block.getProperty("note"));
                    final var texture = gradientDigits.get(click);
                    g2d32.drawImage(texture, pixelSize * (xLength - 1 - x), pixelSize * (zLength - 1 - z), null);
                }
            }
            g2d32.dispose();
            GraphicUtils.writeImage(img32, dirPath + '/' + xlt.get("circuit-prefix") + label + '/' + tw.get("step-3-2-file-name"));

            final var img4 = GraphicUtils.copyWithAlpha(img3);
            final var g2d4 = img4.createGraphics();
            for (var z = structure.getRange3().getZ().getStart(); z < structure.getRange3().getZ().getStop(); ++z) {
                for (var x = 0; x < xLength; ++x) {
                    final var block = structure.get(Position.of(x + xStart, 1, z + zStart));
                    if (null == block) {
                        continue;
                    }
                    if (!"redstone_lamp".equals(block.getId().getPath()) && !block.getId().getPath().endsWith("_wool")) {
                        continue;
                    }
                    final var texture = textures.get(block.getId().getPath());
                    if (null == texture) {
                        continue;
                    }
                    g2d4.drawImage(texture, pixelSize * (xLength - 1 - x), pixelSize * (zLength - 1 - z), null);
                }
            }
            g2d4.dispose();
            GraphicUtils.writeImage(img4, dirPath + '/' + xlt.get("circuit-prefix") + label + '/' + tw.get("step-4-file-name"));
        } catch (Exception e) {
            throw new OcException("[DiagramUtils][drawDesignDiagram] %s", e);
        }
    }

    private static Map<String, BufferedImage> loadBlockTextures(int pixelSize) {
        final Map<String, BufferedImage> textures = new HashMap<>();
        try {
            final List<String> wool_path_list = Stream.of(
                    "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                    "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
            ).map(wool_color -> wool_color + "_wool").toList();
            for (var wool_path : wool_path_list) {
                textures.put(wool_path, GraphicUtils.readImage("./design/texture/" + wool_path + ".png"));
            }
            textures.put("oak_log", GraphicUtils.readImage("./design/texture/oak_log.png"));
            textures.put("sand", GraphicUtils.readImage("./design/texture/sand.png"));
            textures.put("glass", GraphicUtils.readImage("./design/texture/glass.png"));
            textures.put("stone", GraphicUtils.readImage("./design/texture/stone.png"));
            textures.put("gold_block", GraphicUtils.readImage("./design/texture/gold_block.png"));
            textures.put("clay", GraphicUtils.readImage("./design/texture/clay.png"));
            textures.put("packed_ice", GraphicUtils.readImage("./design/texture/packed_ice.png"));
            textures.put("bone_block", GraphicUtils.readImage("./design/texture/bone_block.png"));
            textures.put("iron_block", GraphicUtils.readImage("./design/texture/iron_block.png"));
            textures.put("soul_sand", GraphicUtils.readImage("./design/texture/soul_sand.png"));
            textures.put("pumpkin", GraphicUtils.readImage("./design/texture/pumpkin.jpg"));
            textures.put("emerald_block", GraphicUtils.readImage("./design/texture/emerald_block.png"));
            textures.put("hay_block", GraphicUtils.readImage("./design/texture/hay_block_side.png"));
            textures.put("glowstone", GraphicUtils.readImage("./design/texture/glowstone.png"));
            textures.put("grass_block", GraphicUtils.readImage("./design/texture/grass_block_top.png"));

            textures.put("redstone_wire", GraphicUtils.readImage("./design/texture/redstone.png"));
            textures.put("repeater", GraphicUtils.readImage("./design/texture/repeater.png"));
            textures.put("note_block", GraphicUtils.readImage("./design/texture/note_block.png"));
            textures.put("redstone_lamp", GraphicUtils.readImage("./design/texture/redstone_lamp.png"));
            textures.put("magenta_glazed_terracotta", GraphicUtils.readImage("./design/texture/magenta_glazed_terracotta.png"));
            textures.replaceAll((k, v) -> GraphicUtils.scaleByHeight(textures.get(k), pixelSize));
        } catch (Exception e) {
            throw new OcException("[DiagramUtils][loadBlockTextures] %s", e);
        }
        return textures;
    }

    private static Map<Integer, BufferedImage> loadMonoColorDigits(int pixelSize) {
        final Map<Integer, BufferedImage> textures = new HashMap<>();
        try {
            for (var i = 0; i < 5; ++i) {
                textures.put(i, GraphicUtils.readImage(String.format("./design/monocolor/%d.png", i)));
            }
            textures.replaceAll((k, v) -> GraphicUtils.scaleByHeight(textures.get(k), pixelSize));
        } catch (Exception e) {
            throw new OcException("[DiagramUtils][loadMonoColorDigits] %s", e);
        }
        return textures;
    }

    private static Map<Integer, BufferedImage> loadGradientDigits(int pixelSize) {
        final Map<Integer, BufferedImage> textures = new HashMap<>();
        try {
            for (var i = 0; i < 25; ++i) {
                textures.put(i, GraphicUtils.readImage(String.format("./design/gradient/%d.png", i)));
            }
            textures.replaceAll((k, v) -> GraphicUtils.scaleByHeight(textures.get(k), pixelSize));
        } catch (Exception e) {
            throw new OcException("[DiagramUtils][loadGradientDigits] %s", e);
        }
        return textures;
    }

    private DiagramUtils() {}
}
