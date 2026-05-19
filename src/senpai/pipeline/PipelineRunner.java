package senpai.pipeline;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import senpai.config.SenpaiConfig;
import senpai.io.JarReader;
import senpai.io.JarWriter;
import senpai.io.LoadedJar;
import senpai.reporting.MappingWriter;
import senpai.reporting.RunSummary;
import senpai.reporting.SummaryWriter;

public final class PipelineRunner {

    private final SenpaiConfig cfg;

    public PipelineRunner(SenpaiConfig cfg) {
        this.cfg = cfg;
    }

    public int run(Path input, Path output) {
        long started = System.nanoTime();
        LoadedJar jar = new JarReader().read(input);
        TransformContext ctx = new TransformContext(cfg, jar);
        List<Transform> chain = new TransformRegistry(cfg).build();
        new TransformExecutor().runAll(chain, ctx);
        new JarWriter().write(jar, output);
        if (cfg.writeMapping) {
            Path mappingPath = output.resolveSibling(output.getFileName().toString() + ".map");
            // partition the unified rename map into class and member halves so
            // the mapping file groups them, the way someone reading it would
            // expect.
            Map<String, String> classMap = new HashMap<>();
            Map<String, String> memberMap = new HashMap<>();
            for (Map.Entry<String, String> e : ctx.renameMap().entrySet()) {
                if (e.getKey().contains(".")) {
                    memberMap.put(e.getKey(), e.getValue());
                } else {
                    classMap.put(e.getKey(), e.getValue());
                }
            }
            new MappingWriter().write(classMap, memberMap, mappingPath);
        }
        RunSummary summary = ctx.summary().finalize(System.nanoTime() - started, jar.classes().size());
        new SummaryWriter().write(summary, System.out);
        System.out.println("seed ledger (master seed " + Long.toHexString(cfg.seed) + "):");
        for (var e : ctx.ledger().snapshot().entrySet()) {
            System.out.printf("  %-32s = %016x%n", e.getKey(), e.getValue());
        }
        return 0;
    }
}
