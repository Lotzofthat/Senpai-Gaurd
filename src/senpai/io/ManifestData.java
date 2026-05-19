package senpai.io;

import java.util.jar.Manifest;

public final class ManifestData {

    public final Manifest manifest;
    public final boolean present;

    public ManifestData(Manifest manifest, boolean present) {
        this.manifest = manifest;
        this.present = present;
    }

    public static ManifestData empty() {
        return new ManifestData(new Manifest(), false);
    }
}
