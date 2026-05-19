package senpai.util;

import java.util.Random;

// extends java.util.Random so we can hand it to ASM bits or any other library
// that wants a Random, while still exposing fork/nextBytes shaped to our use.
// the splitmix64 mix is used to derive the initial state and to mix fork salts.
public final class DeterministicRng extends Random {

    private long state;

    public DeterministicRng(long seed) {
        super(seed);
        this.state = mix(seed == 0L ? 0x9E3779B97F4A7C15L : seed);
    }

    public DeterministicRng fork(long salt) {
        return new DeterministicRng(state ^ mix(salt));
    }

    public int nextInt(int boundExclusive) {
        if (boundExclusive <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        long raw = nextLong() & 0x7FFFFFFFFFFFFFFFL;
        return (int) (raw % boundExclusive);
    }

    public int nextInt() {
        return (int) nextLong();
    }

    public boolean nextBoolean() {
        return (nextLong() & 1L) == 0L;
    }

    public long nextLong() {
        long x = state;
        x ^= x << 13;
        x ^= x >>> 7;
        x ^= x << 17;
        state = x;
        return x;
    }

    public byte[] nextBytes(int count) {
        byte[] out = new byte[count];
        nextBytes(out);
        return out;
    }

    @Override
    public void nextBytes(byte[] out) {
        int i = 0;
        while (i < out.length) {
            long chunk = nextLong();
            for (int b = 0; b < 8 && i < out.length; b++, i++) {
                out[i] = (byte) (chunk & 0xFF);
                chunk >>>= 8;
            }
        }
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
