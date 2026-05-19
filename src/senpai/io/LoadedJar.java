package senpai.io;

import java.util.LinkedHashMap;
import java.util.Map;
import org.objectweb.asm.tree.ClassNode;

public final class LoadedJar {

    private final Map<String, ClassNode> classes = new LinkedHashMap<>();
    private final Map<String, byte[]> resources = new LinkedHashMap<>();
    private final ManifestData manifest;

    public LoadedJar(ManifestData manifest) {
        this.manifest = manifest;
    }

    public Map<String, ClassNode> classes() {
        return classes;
    }

    public Map<String, byte[]> resources() {
        return resources;
    }

    public ManifestData manifest() {
        return manifest;
    }
}
