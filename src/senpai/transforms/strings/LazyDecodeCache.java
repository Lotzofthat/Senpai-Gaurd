package senpai.transforms.strings;

// canonical names for every synthetic member the string encryption pass
// installs on each host class. consulted by the rename family so the
// applier's SimpleRemapper never touches them, and by the flow and junk
// passes so they skip over the synthetic methods entirely.
public final class LazyDecodeCache {

    public static final String CACHE_FIELD = "$sg$cache";
    public static final String CACHE_DESC = "[Ljava/lang/String;";
    public static final String ROT_FIELD = "$sg$r";
    public static final String ROT_DESC = "[[B";

    public static boolean isReservedMemberName(String name) {
        return name.equals(DecoderInjector.DECODER_NAME)
            || name.equals(DecoderInjector.INIT_NAME)
            || name.equals(DecoderInjector.CIPHER_FIELD)
            || name.equals(DecoderInjector.KEY_FIELD)
            || name.equals(DecoderInjector.SEED_FIELD)
            || name.equals(CACHE_FIELD)
            || name.equals(ROT_FIELD);
    }
}
