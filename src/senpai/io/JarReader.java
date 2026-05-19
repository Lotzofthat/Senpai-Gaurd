package senpai.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import senpai.reporting.IntegrityReport;

public final class JarReader {

    public LoadedJar read(Path source) {
        try (InputStream in = Files.newInputStream(source); JarInputStream jar = new JarInputStream(in)) {
            ManifestData manifest = jar.getManifest() != null
                ? new ManifestData(jar.getManifest(), true)
                : ManifestData.empty();
            LoadedJar loaded = new LoadedJar(manifest);
            JarEntry entry;
            while ((entry = jar.getNextJarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                byte[] bytes = drain(jar);
                if (entry.getName().endsWith(".class")) {
                    ClassReader cr = new ClassReader(bytes);
                    ClassNode node = new ClassNode();
                    cr.accept(node, 0);
                    loaded.classes().put(node.name, node);
                } else {
                    loaded.resources().put(entry.getName(), bytes);
                }
            }
            return loaded;
        } catch (IOException ex) {
            throw new IllegalStateException("failed to read jar " + source, ex);
        }
    }

    public IntegrityReport scanForIntegrity(Path source) {
        IntegrityReport report = new IntegrityReport();
        try (InputStream in = Files.newInputStream(source); JarInputStream jar = new JarInputStream(in)) {
            JarEntry entry;
            int classes = 0;
            while ((entry = jar.getNextJarEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }
                byte[] bytes = drain(jar);
                try {
                    new ClassReader(bytes).accept(new ClassNode(), 0);
                    classes++;
                } catch (RuntimeException ex) {
                    report.fail(entry.getName(), ex.getMessage());
                }
            }
            report.note("classes scanned: " + classes);
        } catch (IOException ex) {
            report.fail(source.toString(), ex.getMessage());
        }
        return report;
    }

    private static byte[] drain(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(4096);
        byte[] chunk = new byte[4096];
        int read;
        while ((read = in.read(chunk)) > 0) {
            buf.write(chunk, 0, read);
        }
        return buf.toByteArray();
    }
}
