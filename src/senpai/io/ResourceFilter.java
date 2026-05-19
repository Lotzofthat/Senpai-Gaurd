package senpai.io;

public final class ResourceFilter {

    public boolean shouldPassthrough(String name) {
        if (name.equals("META-INF/MANIFEST.MF")) {
            return false;
        }
        if (name.startsWith("META-INF/") && (name.endsWith(".SF") || name.endsWith(".DSA") || name.endsWith(".RSA") || name.endsWith(".EC"))) {
            return false;
        }
        return true;
    }
}
