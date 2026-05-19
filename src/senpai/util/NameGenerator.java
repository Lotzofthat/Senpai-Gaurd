package senpai.util;

import java.util.HashSet;
import java.util.Set;

public final class NameGenerator {

    private final String alphabet;
    private final Set<String> used = new HashSet<>();
    private long counter = 0L;

    public NameGenerator(String alphabet) {
        if (alphabet == null || alphabet.isEmpty()) {
            throw new IllegalArgumentException("alphabet cannot be empty");
        }
        this.alphabet = alphabet;
    }

    public String next() {
        while (true) {
            String candidate = encode(counter++);
            if (used.add(candidate)) {
                return candidate;
            }
        }
    }

    public String nextWithPrefix(String prefix) {
        while (true) {
            String candidate = prefix + encode(counter++);
            if (used.add(candidate)) {
                return candidate;
            }
        }
    }

    public void reserve(String name) {
        used.add(name);
    }

    private String encode(long value) {
        StringBuilder sb = new StringBuilder();
        long n = value;
        int base = alphabet.length();
        // a 5 char baseline keeps the visual collision with shorter real
        // identifiers high. base is small on purpose, e.g. "IiLl1".
        do {
            sb.append(alphabet.charAt((int) (n % base)));
            n /= base;
        } while (n > 0);
        return sb.reverse().toString();
    }
}
