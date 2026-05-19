package senpai.util;

import java.util.LinkedHashMap;
import java.util.Map;

// records every rng fork salt drawn from the master stream. each transform
// asks the ledger for a salt by a stable string label, and the ledger
// remembers the value so two consecutive runs with the same master seed
// produce identical bytes. the ledger also exposes the recorded salts so
// the summary writer can print them, which is the only way a reverser
// could ever reproduce a build without the original config.
public final class Banner {

    private final DeterministicRng source;
    private final Map<String, Long> draws = new LinkedHashMap<>();

    public Banner(DeterministicRng source) {
        this.source = source;
    }

    public long saltFor(String label) {
        Long known = draws.get(label);
        if (known != null) {
            return known;
        }
        long drawn = source.nextLong();
        draws.put(label, drawn);
        return drawn;
    }

    public Map<String, Long> snapshot() {
        return Map.copyOf(draws);
    }
}
