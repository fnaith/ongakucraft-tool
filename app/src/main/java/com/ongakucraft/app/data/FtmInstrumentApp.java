package com.ongakucraft.app.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.ongakucraft.core.OcException;
import com.ongakucraft.core.ftm.FtmInstrument;
import com.ongakucraft.core.ftm.FtmSample;
import com.ongakucraft.core.ftm.FtmSequence;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FtmInstrumentApp {
    private static final Pattern SPACES = Pattern.compile("\s+");

    private static List<String> readFti(String filePath) throws IOException {
        final var path = Path.of(filePath);
        return Files.readAllLines(path);
    }

    private static Map<String, List<String>> parseFtiSection(List<String> lines) {
        final Map<String, List<String>> sections = new LinkedHashMap<>();
        final var n = lines.size();
        String section = null;
        for (int i = 0; i < n; ++i) {
            final var line = lines.get(i);
            if (line.startsWith("# ")) {
                section = line;
                sections.put(section, new ArrayList<>());
            } else {
                if (!line.isBlank()) {
                    sections.get(section).add(line);
                }
            }
        }
        return sections;
    }

    private static Map<String, FtmSequence> parseFtiSequence(Map<String, List<String>> sections) {
        final Map<String, FtmSequence> idToSequence = new LinkedHashMap<>();
        final var lines = sections.get("# SEQUENCES block");
        final var n = lines.size();
        for (int i = 0; i < n; ++i) {
            final var line = lines.get(i);
            final var type = line.split(" ")[0];
            if ("MACRO".equals(type) || "MACROVRC6".equals(type)) {
                final var pattern = Pattern.compile(type + "\\s+(\\d+)\\s+(\\d+)\\s+(.+)");
                final var matcher = pattern.matcher(line);
                if (matcher.find()) {
                    final var typeId = matcher.group(1);
                    final var sequenceNo = matcher.group(2);
                    final var params = matcher.group(3);
                    final var id = typeId + '\t' + sequenceNo;
                    final var sequence = FtmSequence.of(id, type, typeId, sequenceNo, params);
                    idToSequence.put(id, sequence);
                    continue;
                }
            }
            throw new OcException("parseFtiSequence : %s", line);
        }
        return idToSequence;
    }

    private static Map<String, FtmSample> parseFtiSample(Map<String, List<String>> sections) {
        final Map<String, FtmSample> idToSample = new LinkedHashMap<>();
        final var lines = sections.get("# DPCM SAMPLES block");
        final var n = lines.size();
        FtmSample sample = null;
        final var pattern = Pattern.compile("DPCMDEF\\s+(\\d+)\\s+(\\d+)\\s+\"(.+)\"");
        for (int i = 0; i < n; ++i) {
            final var line = lines.get(i);
            if (line.startsWith("DPCMDEF ")) {
                final var matcher = pattern.matcher(line);
                if (matcher.find()) {
                    final var id = matcher.group(1);
                    final var size = matcher.group(2);
                    final var name = matcher.group(3);
                    sample = FtmSample.of(id, size, name);
                    idToSample.put(id, sample);
                } else {
                    throw new OcException("parseFtiSample : %s", line);
                }
            } else {
                if (line.startsWith("DPCM ")) {
                    sample.getDpcm().add(line);
                    continue;
                }
                throw new OcException("parseFtiSample : %s", line);
            }
        }
        return idToSample;
    }

    private static Map<String, FtmInstrument> parseFtiInstrument(Map<String, List<String>> sections) {
        final Map<String, FtmInstrument> idToInstrument = new LinkedHashMap<>();
        final var lines = sections.get("# INSTRUMENTS block");
        final var n = lines.size();
        FtmInstrument instrument = null;
        for (int i = 0; i < n; ++i) {
            final var line = lines.get(i);
            if (line.endsWith("\"")) {
                final var tokens = line.split("\"");
                final var params = SPACES.split(tokens[0]);
                final var type = params[0];
                final var id = params[1];
                final var name = tokens[1];
                instrument = FtmInstrument.of(name, type, id, line);
                idToInstrument.put(id, instrument);
            } else {
                if ("INST2A03".equals(instrument.getType())) {
                    if (line.startsWith("KEYDPCM ")) {
                        instrument.getKeyDpcm().add(line);
                        continue;
                    }
                }
                if ("INSTFDS".equals(instrument.getType())) {
                    if (line.startsWith("FDSWAVE ") || line.startsWith("FDSMOD ") || line.startsWith("FDSMACRO ")) {
                        instrument.getFds().add(line);
                        continue;
                    }
                }
                throw new OcException("parseFtiInstrument : %s", line);
            }
        }
        return idToInstrument;
    }

    private static Map<String, FtmInstrument> parseFti(String filePath) throws IOException {
        final var lines = readFti(filePath);
        final var sections = parseFtiSection(lines);
        final var idToSequence = parseFtiSequence(sections);
        final var idToSample = parseFtiSample(sections);
        final var idToInstrument = parseFtiInstrument(sections);
        for (final var entry : idToInstrument.entrySet()) {
            final var instrument = entry.getValue();
            final var tokens = SPACES.split(instrument.getContent());
            if (!"INSTFDS".equals(instrument.getType())) {
                for (int i = 0; i <= 4; ++i) {
                    final var token = tokens[i + 2];
                    if ("-1".equals(token)) {
                        instrument.getSequences().add(null);
                    } else {
                        final var sequenceId = String.valueOf(i) + '\t' + token;
                        final var sequence = idToSequence.get(sequenceId);
                        if (null == sequence) {
                            instrument.getSequences().add(null);
                            throw new OcException("parseFti : %s, %s", filePath, sequenceId);
                        } else {
                            instrument.getSequences().add(sequence);
                        }
                    }
                }
            }
            for (final var dcpm : instrument.getKeyDpcm()) {
                final var sampleId = SPACES.split(dcpm)[4];
                final var sample = idToSample.get(sampleId);
                if (null == sample) {
                    throw new OcException("parseFti : %s, %s", filePath, dcpm);
                }
                instrument.getSamples().add(sample);
            }
        }
//        log.info("{}", idToInstrument);
        return idToInstrument;
    }

    public static Map<String, FtmInstrument> loadFtmInstruments(String rootDirPath) throws IOException {
        final List<String> filePathList = new ArrayList<>();
        try (final var walk = Files.walk(Paths.get(rootDirPath))) {
            walk.forEach(path -> {
                if (Files.isDirectory(path)) {
                    return;
                }
                final var filePath = path.toString();
                final var fileTokens = filePath.split("\\\\");
                final var fileName = fileTokens[fileTokens.length - 1];
                if (fileName.endsWith(".txt")) {
                    filePathList.add(filePath);
                }
            });
        }
        final Map<String, FtmInstrument> nameToInstrument = new LinkedHashMap<>();
        for (final var filePath : filePathList) {
            final var idToInstrument = parseFti(filePath);
            for (final var instrument : idToInstrument.values()) {
                final var name = instrument.getName();
                if (nameToInstrument.containsKey(name)) {
                    log.warn("loadFtmInstruments : {}", name);
                } else {
                    nameToInstrument.put(name, instrument);
                }
            }
        }
        return nameToInstrument;
    }

    public static void main(String[] args) {
        final var rootDirPath = "/Users/wilson/Downloads/_instrument_txt/";
        try {
////            final var filePath = rootDirPath + "cello/2a03/cello-2a03.txt";
//            final var filePath = rootDirPath + "piano/2a03/piano-2a03.txt";
//            parseFti(filePath);

            final var nameToInstrument = loadFtmInstruments(rootDirPath);
            log.info("{}", nameToInstrument.keySet());
        } catch (Exception e) {
            log.error("FtmInstrumentApp", e);
        }
    }

    private FtmInstrumentApp() {}
}
