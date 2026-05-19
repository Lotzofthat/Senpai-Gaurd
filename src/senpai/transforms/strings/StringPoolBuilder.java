package senpai.transforms.strings;

import java.util.ArrayList;
import java.util.List;
import senpai.util.Bytes;
import senpai.util.DeterministicRng;

public final class StringPoolBuilder {

    private final List<byte[]> encrypted = new ArrayList<>();
    private final List<byte[]> keys = new ArrayList<>();
    private final List<byte[]> rotations = new ArrayList<>();
    private final byte seed;
    private final DeterministicRng rotationSource;

    public StringPoolBuilder(byte seed, DeterministicRng rotationSource) {
        this.seed = seed;
        this.rotationSource = rotationSource;
    }

    public int add(String literal, byte[] key) {
        byte[] payload = literal.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        // rotation table length itself comes from the rng for this slot, so
        // two slots with the same key bytes still produce different ciphers.
        int rotLength = 1 + (rotationSource.nextInt() & 0xF);
        byte[] rot = new byte[rotLength];
        rotationSource.nextBytes(rot);
        for (int i = 0; i < rot.length; i++) {
            if ((rot[i] & 0x07) == 0) {
                rot[i] = (byte) ((rot[i] & 0xF8) | 1);
            }
        }
        encrypted.add(Bytes.rolledXor(payload, key, rot, seed));
        keys.add(key);
        rotations.add(rot);
        return encrypted.size() - 1;
    }

    public int size() {
        return encrypted.size();
    }

    public byte[] entry(int slot) {
        return encrypted.get(slot);
    }

    public byte[] keyFor(int slot) {
        return keys.get(slot);
    }

    public byte[] rotFor(int slot) {
        return rotations.get(slot);
    }

    public byte seed() {
        return seed;
    }

    public byte[][] entries() {
        return encrypted.toArray(new byte[0][]);
    }

    public byte[][] keysArray() {
        return keys.toArray(new byte[0][]);
    }

    public byte[][] rotsArray() {
        return rotations.toArray(new byte[0][]);
    }
}
