package senpai.util;

public final class Strings {

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static String orDefault(String s, String fallback) {
        return isBlank(s) ? fallback : s;
    }

    public static String stripInternalSuffix(String internalName) {
        int dollar = internalName.indexOf('$');
        if (dollar < 0) {
            return internalName;
        }
        return internalName.substring(0, dollar);
    }
}
