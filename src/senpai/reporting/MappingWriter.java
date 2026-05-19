package senpai.reporting;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class MappingWriter {

    public void write(Map<String, String> classMap, Map<String, String> memberMap, Path target) {
        try (BufferedWriter w = Files.newBufferedWriter(target)) {
            w.write("# senpai mapping");
            w.newLine();
            w.write("# format: <kind> <original> => <renamed>");
            w.newLine();
            for (Map.Entry<String, String> e : classMap.entrySet()) {
                w.write("class ");
                w.write(e.getKey());
                w.write(" => ");
                w.write(e.getValue());
                w.newLine();
            }
            for (Map.Entry<String, String> e : memberMap.entrySet()) {
                w.write("member ");
                w.write(e.getKey());
                w.write(" => ");
                w.write(e.getValue());
                w.newLine();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("failed to write mapping to " + target, ex);
        }
    }
}
