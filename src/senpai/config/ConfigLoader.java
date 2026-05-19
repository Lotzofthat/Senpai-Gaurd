package senpai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigLoader {

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    public SenpaiConfig loadOrDefault(Path path) {
        if (path == null) {
            return SenpaiConfig.defaults();
        }
        if (!Files.exists(path)) {
            System.err.println("config not found, using defaults: " + path);
            return SenpaiConfig.defaults();
        }
        try {
            byte[] raw = Files.readAllBytes(path);
            ConfigDocument doc = yaml.readValue(raw, ConfigDocument.class);
            SenpaiConfig cfg = doc.toConfig();
            new ConfigValidator().validate(cfg);
            return cfg;
        } catch (IOException ex) {
            throw new IllegalArgumentException("failed to read config at " + path, ex);
        }
    }
}
