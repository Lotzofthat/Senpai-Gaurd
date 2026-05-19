package senpai.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public final class ConfigDocument {

    @JsonProperty("renaming")
    public RenamingDoc renaming = new RenamingDoc();

    @JsonProperty("strings")
    public StringsDoc strings = new StringsDoc();

    @JsonProperty("flow")
    public FlowDoc flow = new FlowDoc();

    @JsonProperty("numbers")
    public NumbersDoc numbers = new NumbersDoc();

    @JsonProperty("junk")
    public JunkDoc junk = new JunkDoc();

    @JsonProperty("keep")
    public List<String> keep = new ArrayList<>();

    @JsonProperty("writeMapping")
    public boolean writeMapping = true;

    @JsonProperty("seed")
    public long seed = 0xC0FFEEL;

    public SenpaiConfig toConfig() {
        return new SenpaiConfig(
            new SenpaiConfig.RenamingOptions(renaming.classes, renaming.methods, renaming.fields, renaming.locals, renaming.alphabet),
            new SenpaiConfig.StringOptions(strings.encrypt, strings.minLength, strings.lazyDecode),
            new SenpaiConfig.FlowOptions(flow.flatten, flow.opaquePredicates, flow.maxBlocksPerMethod),
            new SenpaiConfig.NumberOptions(numbers.mask, numbers.maxOperands),
            new SenpaiConfig.JunkOptions(junk.fakeSwitches, junk.deadBranches, junk.densityPerMethod),
            keep,
            writeMapping,
            seed
        );
    }

    public static final class RenamingDoc {
        public boolean classes = true;
        public boolean methods = true;
        public boolean fields = true;
        public boolean locals = true;
        public String alphabet = "IiLl1";
    }

    public static final class StringsDoc {
        public boolean encrypt = true;
        public int minLength = 2;
        public boolean lazyDecode = true;
    }

    public static final class FlowDoc {
        public boolean flatten = true;
        public boolean opaquePredicates = true;
        public int maxBlocksPerMethod = 4096;
    }

    public static final class NumbersDoc {
        public boolean mask = true;
        public int maxOperands = 3;
    }

    public static final class JunkDoc {
        public boolean fakeSwitches = true;
        public boolean deadBranches = true;
        public int densityPerMethod = 2;
    }
}
