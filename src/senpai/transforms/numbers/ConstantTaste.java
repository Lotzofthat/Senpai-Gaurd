package senpai.transforms.numbers;

public final class ConstantTaste {

    public static boolean worthMasking(int value) {
        if (value == 0 || value == 1 || value == -1) {
            return false;
        }
        return true;
    }

    public static boolean worthMasking(long value) {
        return value != 0L && value != 1L && value != -1L;
    }
}
