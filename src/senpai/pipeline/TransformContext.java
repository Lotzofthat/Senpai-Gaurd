package senpai.pipeline;

import java.util.LinkedHashMap;
import java.util.Map;
import senpai.config.KeepMatcher;
import senpai.config.SenpaiConfig;
import senpai.io.LoadedJar;
import senpai.reporting.RunSummaryBuilder;
import senpai.util.Banner;
import senpai.util.DeterministicRng;

public final class TransformContext {

    private final SenpaiConfig cfg;
    private final LoadedJar jar;
    private final KeepMatcher keep;
    private final DeterministicRng rng;
    private final Banner ledger;
    private final RunSummaryBuilder summary;

    private final Map<String, String> renameMap = new LinkedHashMap<>();
    private final Map<String, byte[][]> stringPools = new LinkedHashMap<>();

    public TransformContext(SenpaiConfig cfg, LoadedJar jar) {
        this.cfg = cfg;
        this.jar = jar;
        this.keep = new KeepMatcher(cfg.keepPatterns);
        this.rng = new DeterministicRng(cfg.seed);
        this.ledger = new Banner(rng.fork(rng.nextLong()));
        this.summary = new RunSummaryBuilder();
    }

    public SenpaiConfig config() {
        return cfg;
    }

    public LoadedJar jar() {
        return jar;
    }

    public KeepMatcher keep() {
        return keep;
    }

    public DeterministicRng rng() {
        return rng;
    }

    // every pass that needs a private rng asks here. label uniqueness is the
    // caller's responsibility, and the ledger guarantees that the same label
    // resolves to the same salt across calls.
    public DeterministicRng rngFor(String label) {
        return rng.fork(ledger.saltFor(label));
    }

    public Banner ledger() {
        return ledger;
    }

    public RunSummaryBuilder summary() {
        return summary;
    }

    public Map<String, String> renameMap() {
        return renameMap;
    }

    public Map<String, byte[][]> stringPools() {
        return stringPools;
    }
}
