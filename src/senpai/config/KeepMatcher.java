package senpai.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class KeepMatcher {

    private final List<Pattern> compiled;

    public KeepMatcher(List<String> patterns) {
        this.compiled = new ArrayList<>(patterns.size());
        for (String raw : patterns) {
            compiled.add(Pattern.compile(globToRegex(raw)));
        }
    }

    public boolean shouldKeep(String internalName) {
        for (Pattern p : compiled) {
            if (p.matcher(internalName).matches()) {
                return true;
            }
        }
        return false;
    }

    private static String globToRegex(String glob) {
        StringBuilder out = new StringBuilder(glob.length() + 8);
        out.append('^');
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*' -> {
                    if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                        out.append(".*");
                        i++;
                    } else {
                        out.append("[^/]*");
                    }
                }
                case '?' -> out.append("[^/]");
                case '.', '(', ')', '+', '|', '^', '$', '@', '%', '{', '}', '[', ']', '\\' -> {
                    out.append('\\').append(c);
                }
                default -> out.append(c);
            }
        }
        out.append('$');
        return out.toString();
    }
}
