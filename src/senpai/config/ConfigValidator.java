package senpai.config;

public final class ConfigValidator {

    public void validate(SenpaiConfig cfg) {
        if (cfg.renaming.alphabet == null || cfg.renaming.alphabet.isEmpty()) {
            throw new IllegalArgumentException("renaming.alphabet must contain at least one character");
        }
        if (cfg.strings.minLength < 0) {
            throw new IllegalArgumentException("strings.minLength cannot be negative");
        }
        if (cfg.flow.maxBlocksPerMethod < 1) {
            throw new IllegalArgumentException("flow.maxBlocksPerMethod must be at least 1");
        }
        if (cfg.numbers.maxOperands < 1) {
            throw new IllegalArgumentException("numbers.maxOperands must be at least 1");
        }
        if (cfg.junk.densityPerMethod < 0) {
            throw new IllegalArgumentException("junk.densityPerMethod cannot be negative");
        }
    }
}
