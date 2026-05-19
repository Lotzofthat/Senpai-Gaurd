package senpai.transforms.strings;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import senpai.asm.InsnFactory;

public final class DecoderInjector {

    public static final String DECODER_NAME = "$sg$decode";
    public static final String DECODER_DESC = "(I)Ljava/lang/String;";
    public static final String INIT_NAME = "$sg$init";
    public static final String INIT_DESC = "()V";
    public static final String CIPHER_FIELD = "$sg$c";
    public static final String KEY_FIELD = "$sg$k";
    public static final String SEED_FIELD = "$sg$s";

    public void replaceLdcWithDecoderCall(MethodNode method, LdcInsnNode ldc, String ownerInternalName, int slot) {
        method.instructions.insertBefore(ldc, InsnFactory.pushInt(slot));
        method.instructions.insertBefore(ldc, new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            ownerInternalName,
            DECODER_NAME,
            DECODER_DESC,
            false
        ));
        method.instructions.remove(ldc);
    }

    public void install(ClassNode owner, StringPoolBuilder pool) {
        if (pool.size() == 0) {
            return;
        }
        for (MethodNode existing : owner.methods) {
            if (existing.name.equals(DECODER_NAME) && existing.desc.equals(DECODER_DESC)) {
                return;
            }
        }
        installFields(owner);
        owner.methods.add(buildInitMethod(owner, pool));
        owner.methods.add(buildDecoderMethod(owner));
        wireClinit(owner);
    }

    private void installFields(ClassNode owner) {
        int access = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;
        owner.fields.add(new FieldNode(access, CIPHER_FIELD, "[[B", null, null));
        owner.fields.add(new FieldNode(access, KEY_FIELD, "[[B", null, null));
        owner.fields.add(new FieldNode(access, LazyDecodeCache.ROT_FIELD, LazyDecodeCache.ROT_DESC, null, null));
        owner.fields.add(new FieldNode(access, SEED_FIELD, "B", null, null));
        owner.fields.add(new FieldNode(access, LazyDecodeCache.CACHE_FIELD, LazyDecodeCache.CACHE_DESC, null, null));
    }

    private MethodNode buildInitMethod(ClassNode owner, StringPoolBuilder pool) {
        MethodNode init = new MethodNode(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
            INIT_NAME,
            INIT_DESC,
            null,
            null
        );
        InsnList code = new InsnList();

        code.add(InsnFactory.pushInt(pool.seed()));
        code.add(new InsnNode(Opcodes.I2B));
        code.add(new FieldInsnNode(Opcodes.PUTSTATIC, owner.name, SEED_FIELD, "B"));

        appendTable(code, owner.name, CIPHER_FIELD, "[[B", pool.entries());
        appendTable(code, owner.name, KEY_FIELD, "[[B", pool.keysArray());
        appendTable(code, owner.name, LazyDecodeCache.ROT_FIELD, LazyDecodeCache.ROT_DESC, pool.rotsArray());

        code.add(InsnFactory.pushInt(pool.size()));
        code.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
        code.add(new FieldInsnNode(Opcodes.PUTSTATIC, owner.name, LazyDecodeCache.CACHE_FIELD, LazyDecodeCache.CACHE_DESC));

        code.add(new InsnNode(Opcodes.RETURN));
        init.instructions = code;
        init.maxStack = 5;
        init.maxLocals = 0;
        return init;
    }

    private void appendTable(InsnList code, String ownerName, String field, String desc, byte[][] rows) {
        code.add(InsnFactory.pushInt(rows.length));
        code.add(new TypeInsnNode(Opcodes.ANEWARRAY, "[B"));
        for (int i = 0; i < rows.length; i++) {
            code.add(new InsnNode(Opcodes.DUP));
            code.add(InsnFactory.pushInt(i));
            appendByteArrayLiteral(code, rows[i]);
            code.add(new InsnNode(Opcodes.AASTORE));
        }
        code.add(new FieldInsnNode(Opcodes.PUTSTATIC, ownerName, field, desc));
    }

    private void appendByteArrayLiteral(InsnList code, byte[] bytes) {
        code.add(InsnFactory.pushInt(bytes.length));
        code.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_BYTE));
        for (int i = 0; i < bytes.length; i++) {
            code.add(new InsnNode(Opcodes.DUP));
            code.add(InsnFactory.pushInt(i));
            code.add(InsnFactory.pushInt(bytes[i]));
            code.add(new InsnNode(Opcodes.I2B));
            code.add(new InsnNode(Opcodes.BASTORE));
        }
    }

    private MethodNode buildDecoderMethod(ClassNode owner) {
        // private static String $sg$decode(int slot)
        // locals:
        //   0 slot (int)
        //   1 cipher (byte[])
        //   2 key (byte[])
        //   3 rot (byte[])
        //   4 plain (byte[])
        //   5 carry (int)
        //   6 i (int)
        //   7 cipherByte (int)
        //   8 cache (String[])
        //   9 hit (String)
        MethodNode decoder = new MethodNode(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
            DECODER_NAME,
            DECODER_DESC,
            null,
            null
        );
        InsnList c = new InsnList();

        LabelNode loopHead = new LabelNode();
        LabelNode loopExit = new LabelNode();
        LabelNode notCached = new LabelNode();

        // cache = $sg$cache; hit = cache[slot]; if (hit != null) return hit
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, owner.name, LazyDecodeCache.CACHE_FIELD, LazyDecodeCache.CACHE_DESC));
        c.add(new VarInsnNode(Opcodes.ASTORE, 8));
        c.add(new VarInsnNode(Opcodes.ALOAD, 8));
        c.add(new VarInsnNode(Opcodes.ILOAD, 0));
        c.add(new InsnNode(Opcodes.AALOAD));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ASTORE, 9));
        c.add(new JumpInsnNode(Opcodes.IFNULL, notCached));
        c.add(new VarInsnNode(Opcodes.ALOAD, 9));
        c.add(new InsnNode(Opcodes.ARETURN));

        c.add(notCached);

        // cipher = $sg$c[slot]
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, owner.name, CIPHER_FIELD, "[[B"));
        c.add(new VarInsnNode(Opcodes.ILOAD, 0));
        c.add(new InsnNode(Opcodes.AALOAD));
        c.add(new VarInsnNode(Opcodes.ASTORE, 1));

        // key = $sg$k[slot]
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, owner.name, KEY_FIELD, "[[B"));
        c.add(new VarInsnNode(Opcodes.ILOAD, 0));
        c.add(new InsnNode(Opcodes.AALOAD));
        c.add(new VarInsnNode(Opcodes.ASTORE, 2));

        // rot = $sg$r[slot]
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, owner.name, LazyDecodeCache.ROT_FIELD, LazyDecodeCache.ROT_DESC));
        c.add(new VarInsnNode(Opcodes.ILOAD, 0));
        c.add(new InsnNode(Opcodes.AALOAD));
        c.add(new VarInsnNode(Opcodes.ASTORE, 3));

        // plain = new byte[cipher.length]
        c.add(new VarInsnNode(Opcodes.ALOAD, 1));
        c.add(new InsnNode(Opcodes.ARRAYLENGTH));
        c.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_BYTE));
        c.add(new VarInsnNode(Opcodes.ASTORE, 4));

        // carry = seed
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, owner.name, SEED_FIELD, "B"));
        c.add(new VarInsnNode(Opcodes.ISTORE, 5));

        // i = 0
        c.add(new InsnNode(Opcodes.ICONST_0));
        c.add(new VarInsnNode(Opcodes.ISTORE, 6));

        c.add(loopHead);
        c.add(new VarInsnNode(Opcodes.ILOAD, 6));
        c.add(new VarInsnNode(Opcodes.ALOAD, 1));
        c.add(new InsnNode(Opcodes.ARRAYLENGTH));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, loopExit));

        // cipherByte = cipher[i] & 0xFF
        c.add(new VarInsnNode(Opcodes.ALOAD, 1));
        c.add(new VarInsnNode(Opcodes.ILOAD, 6));
        c.add(new InsnNode(Opcodes.BALOAD));
        c.add(InsnFactory.pushInt(0xFF));
        c.add(new InsnNode(Opcodes.IAND));
        c.add(new VarInsnNode(Opcodes.ISTORE, 7));

        // rBits = rot[i % rot.length] & 7
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new VarInsnNode(Opcodes.ILOAD, 6));
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new InsnNode(Opcodes.ARRAYLENGTH));
        c.add(new InsnNode(Opcodes.IREM));
        c.add(new InsnNode(Opcodes.BALOAD));
        c.add(InsnFactory.pushInt(7));
        c.add(new InsnNode(Opcodes.IAND));
        // stack: rBits
        // unrotated = ((cipherByte >>> rBits) | (cipherByte << (8 - rBits))) & 0xFF
        // duplicate rBits because we need it twice
        c.add(new InsnNode(Opcodes.DUP));
        // top: rBits, rBits
        // compute cipherByte >>> rBits
        c.add(new VarInsnNode(Opcodes.ILOAD, 7));
        c.add(new InsnNode(Opcodes.SWAP));
        c.add(new InsnNode(Opcodes.IUSHR));
        // stack: rBits, (cipherByte >>> rBits)
        c.add(new InsnNode(Opcodes.SWAP));
        // stack: (cipherByte >>> rBits), rBits
        c.add(InsnFactory.pushInt(8));
        c.add(new InsnNode(Opcodes.SWAP));
        c.add(new InsnNode(Opcodes.ISUB));
        // stack: (cipherByte >>> rBits), (8 - rBits)
        c.add(new VarInsnNode(Opcodes.ILOAD, 7));
        c.add(new InsnNode(Opcodes.SWAP));
        c.add(new InsnNode(Opcodes.ISHL));
        // stack: (cipherByte >>> rBits), (cipherByte << (8 - rBits))
        c.add(new InsnNode(Opcodes.IOR));
        c.add(InsnFactory.pushInt(0xFF));
        c.add(new InsnNode(Opcodes.IAND));
        // stack: unrotated
        // xor with key byte and carry
        c.add(new VarInsnNode(Opcodes.ALOAD, 2));
        c.add(new VarInsnNode(Opcodes.ILOAD, 6));
        c.add(new VarInsnNode(Opcodes.ALOAD, 2));
        c.add(new InsnNode(Opcodes.ARRAYLENGTH));
        c.add(new InsnNode(Opcodes.IREM));
        c.add(new InsnNode(Opcodes.BALOAD));
        c.add(InsnFactory.pushInt(0xFF));
        c.add(new InsnNode(Opcodes.IAND));
        c.add(new InsnNode(Opcodes.IXOR));
        c.add(new VarInsnNode(Opcodes.ILOAD, 5));
        c.add(InsnFactory.pushInt(0xFF));
        c.add(new InsnNode(Opcodes.IAND));
        c.add(new InsnNode(Opcodes.IXOR));
        c.add(new InsnNode(Opcodes.I2B));
        // stack: plainByte
        // stash plainByte into a temp slot, then BASTORE in the natural order.
        // slot 7 was cipherByte; we no longer need it here, reuse it.
        c.add(new VarInsnNode(Opcodes.ISTORE, 7));
        c.add(new VarInsnNode(Opcodes.ALOAD, 4));
        c.add(new VarInsnNode(Opcodes.ILOAD, 6));
        c.add(new VarInsnNode(Opcodes.ILOAD, 7));
        c.add(new InsnNode(Opcodes.BASTORE));

        // carry = cipher[i] (raw, not the unrotated one)
        c.add(new VarInsnNode(Opcodes.ALOAD, 1));
        c.add(new VarInsnNode(Opcodes.ILOAD, 6));
        c.add(new InsnNode(Opcodes.BALOAD));
        c.add(new VarInsnNode(Opcodes.ISTORE, 5));

        c.add(new IincInsnNode(6, 1));
        c.add(new JumpInsnNode(Opcodes.GOTO, loopHead));

        c.add(loopExit);
        // hit = new String(plain, "UTF-8")
        c.add(new TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ALOAD, 4));
        c.add(new LdcInsnNode("UTF-8"));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>",
            "([BLjava/lang/String;)V", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 9));

        // cache[slot] = hit
        c.add(new VarInsnNode(Opcodes.ALOAD, 8));
        c.add(new VarInsnNode(Opcodes.ILOAD, 0));
        c.add(new VarInsnNode(Opcodes.ALOAD, 9));
        c.add(new InsnNode(Opcodes.AASTORE));

        c.add(new VarInsnNode(Opcodes.ALOAD, 9));
        c.add(new InsnNode(Opcodes.ARETURN));

        decoder.exceptions = new java.util.ArrayList<>();
        decoder.exceptions.add("java/io/UnsupportedEncodingException");
        decoder.instructions = c;
        decoder.maxStack = 8;
        decoder.maxLocals = 10;
        return decoder;
    }

    private void wireClinit(ClassNode owner) {
        MethodNode clinit = null;
        for (MethodNode m : owner.methods) {
            if (m.name.equals("<clinit>") && m.desc.equals("()V")) {
                clinit = m;
                break;
            }
        }
        if (clinit == null) {
            clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            clinit.instructions = new InsnList();
            clinit.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner.name, INIT_NAME, INIT_DESC, false));
            clinit.instructions.add(new InsnNode(Opcodes.RETURN));
            clinit.maxStack = 5;
            clinit.maxLocals = 0;
            owner.methods.add(clinit);
            return;
        }
        InsnList prologue = new InsnList();
        prologue.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner.name, INIT_NAME, INIT_DESC, false));
        AbstractInsnNode first = clinit.instructions.getFirst();
        clinit.instructions.insertBefore(first, prologue);
        clinit.maxStack = Math.max(clinit.maxStack, 5);
    }
}
