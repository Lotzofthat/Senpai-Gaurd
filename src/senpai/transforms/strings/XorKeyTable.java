package senpai.transforms.strings;

import java.util.ArrayList;
import java.util.List;
import senpai.util.DeterministicRng;

public final class XorKeyTable {

    private final DeterministicRng rng;
    private final List<byte[]> keys = new ArrayList<>();
    private final byte seed;
    private final int minKeyLength;
    private final int keyLengthSpan;

    public XorKeyTable(DeterministicRng rng) {
        this.rng = rng;
        byte s;
        do {
            s = (byte) (rng.nextInt() & 0xFF);
        } while (s == 0);
        this.seed = s;
        // both the minimum length and the span above it come from the rng.
        // the only floor we honour is 1, because a zero length key would
        // crash the decoder. the span is bounded so an unlucky draw cannot
        // grow the per class init method past the jvm method size limit.
        this.minKeyLength = 1 + (rng.nextInt() & 0x7);
        this.keyLengthSpan = 1 + (rng.nextInt() & 0x1F);
    }

    public byte[] nextKey() {
        int length = minKeyLength + rng.nextInt(keyLengthSpan);
        byte[] k = new byte[length];
        rng.nextBytes(k);
        for (int i = 0; i < k.length; i++) {
            if (k[i] == 0) {
                k[i] = 1;
            }
        }
        keys.add(k);
        return k;
    }

    public byte seed() {
        return seed;
    }

    public List<byte[]> all() {
        return List.copyOf(keys);
    }
}
