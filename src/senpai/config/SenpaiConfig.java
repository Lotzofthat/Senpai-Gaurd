package senpai.config;

import java.util.List;

public final class SenpaiConfig {

    public final RenamingOptions renaming;
    public final StringOptions strings;
    public final FlowOptions flow;
    public final NumberOptions numbers;
    public final JunkOptions junk;
    public final List<String> keepPatterns;
    public final boolean writeMapping;
    public final long seed;

    public SenpaiConfig(
        RenamingOptions renaming,
        StringOptions strings,
        FlowOptions flow,
        NumberOptions numbers,
        JunkOptions junk,
        List<String> keepPatterns,
        boolean writeMapping,
        long seed
    ) {
        this.renaming = renaming;
        this.strings = strings;
        this.flow = flow;
        this.numbers = numbers;
        this.junk = junk;
        this.keepPatterns = List.copyOf(keepPatterns);
        this.writeMapping = writeMapping;
        this.seed = seed;
    }

    public static SenpaiConfig defaults() {
        return new SenpaiConfig(
            RenamingOptions.defaults(),
            StringOptions.defaults(),
            FlowOptions.defaults(),
            NumberOptions.defaults(),
            JunkOptions.defaults(),
            List.of(),
            true,
            0xC0FFEEL
        );
    }

    public static final class RenamingOptions {
        public final boolean classes;
        public final boolean methods;
        public final boolean fields;
        public final boolean locals;
        public final String alphabet;

        public RenamingOptions(boolean classes, boolean methods, boolean fields, boolean locals, String alphabet) {
            this.classes = classes;
            this.methods = methods;
            this.fields = fields;
            this.locals = locals;
            this.alphabet = alphabet;
        }

        public static RenamingOptions defaults() {
            return new RenamingOptions(true, true, true, true, "IiLl1");
        }
    }

    public static final class StringOptions {
        public final boolean encrypt;
        public final int minLength;
        public final boolean lazyDecode;

        public StringOptions(boolean encrypt, int minLength, boolean lazyDecode) {
            this.encrypt = encrypt;
            this.minLength = minLength;
            this.lazyDecode = lazyDecode;
        }

        public static StringOptions defaults() {
            return new StringOptions(true, 2, true);
        }
    }

    public static final class FlowOptions {
        public final boolean flatten;
        public final boolean opaquePredicates;
        public final int maxBlocksPerMethod;

        public FlowOptions(boolean flatten, boolean opaquePredicates, int maxBlocksPerMethod) {
            this.flatten = flatten;
            this.opaquePredicates = opaquePredicates;
            this.maxBlocksPerMethod = maxBlocksPerMethod;
        }

        public static FlowOptions defaults() {
            // flatten is off by default because the rewrite needs a full
            // dataflow pass to correctly detect cross block local definitions,
            // and we ship a conservative checker that refuses any method
            // with long or double locals or IINC. opaque predicates are on
            // because they are universally safe.
            return new FlowOptions(false, true, 4096);
        }
    }

    public static final class NumberOptions {
        public final boolean mask;
        public final int maxOperands;

        public NumberOptions(boolean mask, int maxOperands) {
            this.mask = mask;
            this.maxOperands = maxOperands;
        }

        public static NumberOptions defaults() {
            return new NumberOptions(true, 3);
        }
    }

    public static final class JunkOptions {
        public final boolean fakeSwitches;
        public final boolean deadBranches;
        public final int densityPerMethod;

        public JunkOptions(boolean fakeSwitches, boolean deadBranches, int densityPerMethod) {
            this.fakeSwitches = fakeSwitches;
            this.deadBranches = deadBranches;
            this.densityPerMethod = densityPerMethod;
        }

        public static JunkOptions defaults() {
            return new JunkOptions(true, true, 2);
        }
    }
}
