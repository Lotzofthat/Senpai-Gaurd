package senpai.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public final class JarWriter {

    public void write(LoadedJar jar, Path target) {
        try (OutputStream out = Files.newOutputStream(target); JarOutputStream zip = openZip(out, jar)) {
            for (Map.Entry<String, ClassNode> e : jar.classes().entrySet()) {
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                e.getValue().accept(cw);
                writeEntry(zip, e.getKey() + ".class", cw.toByteArray());
            }
            for (Map.Entry<String, byte[]> e : jar.resources().entrySet()) {
                writeEntry(zip, e.getKey(), e.getValue());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("failed to write jar " + target, ex);
        }
    }

    private static JarOutputStream openZip(OutputStream out, LoadedJar jar) throws IOException {
        if (jar.manifest().present) {
            return new JarOutputStream(out, jar.manifest().manifest);
        }
        return new JarOutputStream(out);
    }

    private static void writeEntry(JarOutputStream zip, String name, byte[] bytes) throws IOException {
        JarEntry entry = new JarEntry(name);
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }
}
