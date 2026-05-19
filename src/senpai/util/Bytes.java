package senpai.util;

public final class Bytes {

    public static byte[] xor(byte[] payload, byte[] key) {
        if (key.length == 0) {
            throw new IllegalArgumentException("key cannot be empty");
        }
        byte[] out = new byte[payload.length];
        for (int i = 0; i < payload.length; i++) {
            out[i] = (byte) (payload[i] ^ key[i % key.length]);
        }
        return out;
    }

    // full rolled XOR with per byte rotation. each plaintext byte is xored
    // with the cycling key byte and with the prior ciphertext byte, then
    // rotated left by rot[i % rot.length] bits inside an 8 bit field. the
    // decoder runs the inverse: rotate right first, then xor with the prior
    // ciphertext byte, then xor with the key byte.
    public static byte[] rolledXor(byte[] payload, byte[] key, byte[] rot, byte seed) {
        if (key.length == 0 || rot.length == 0) {
            throw new IllegalArgumentException("key and rotation table cannot be empty");
        }
        byte[] out = new byte[payload.length];
        byte carry = seed;
        for (int i = 0; i < payload.length; i++) {
            int mixed = (payload[i] ^ key[i % key.length] ^ carry) & 0xFF;
            int r = (rot[i % rot.length] & 0x07);
            int rotated = ((mixed << r) | (mixed >>> (8 - r))) & 0xFF;
            byte cipher = (byte) rotated;
            out[i] = cipher;
            carry = cipher;
        }
        return out;
    }

    public static byte[] unrollXor(byte[] cipher, byte[] key, byte[] rot, byte seed) {
        byte[] out = new byte[cipher.length];
        byte carry = seed;
        for (int i = 0; i < cipher.length; i++) {
            int c = cipher[i] & 0xFF;
            int r = (rot[i % rot.length] & 0x07);
            int unrotated = ((c >>> r) | (c << (8 - r))) & 0xFF;
            int plain = unrotated ^ (key[i % key.length] & 0xFF) ^ (carry & 0xFF);
            out[i] = (byte) plain;
            carry = cipher[i];
        }
        return out;
    }

    public static String toHex(byte[] raw) {
        StringBuilder sb = new StringBuilder(raw.length * 2);
        for (byte b : raw) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
