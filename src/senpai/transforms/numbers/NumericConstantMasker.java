package senpai.transforms.numbers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import senpai.asm.AccessFlags;
import senpai.pipeline.Transform;
import senpai.pipeline.TransformContext;
import senpai.transforms.strings.LazyDecodeCache;
import senpai.util.DeterministicRng;

public final class NumericConstantMasker implements Transform {

    @Override
    public String id() {
        return "numbers.mask";
    }

    @Override
    public void apply(TransformContext ctx) {
        ArithmeticMixer ints = new ArithmeticMixer(ctx.rngFor("numbers.mask.ints.additive"), ctx.config().numbers.maxOperands);
        XorMath xor = new XorMath(ctx.rngFor("numbers.mask.ints.xor"));
        LongMasker longs = new LongMasker(ctx.rngFor("numbers.mask.longs"));
        DeterministicRng picker = ctx.rngFor("numbers.mask.picker");
        int touched = 0;
        for (ClassNode cls : ctx.jar().classes().values()) {
            for (MethodNode m : cls.methods) {
                if (LazyDecodeCache.isReservedMemberName(m.name)) {
                    continue;
                }
                if (AccessFlags.isNative(m.access) || AccessFlags.isAbstract(m.access)) {
                    continue;
                }
                if (m.instructions == null) {
                    continue;
                }
                touched += rewriteConstants(m, ints, xor, longs, picker);
            }
        }
        ctx.summary().recordMetric("numbers.mask.count", touched);
    }

    private int rewriteConstants(MethodNode m, ArithmeticMixer ints, XorMath xor, LongMasker longs, DeterministicRng picker) {
        // pick weights themselves drawn at construction so the additive/xor
        // ratio for each run is determined by the master seed, not by any
        // literal in this file.
        int additiveWeight = 1 + (picker.nextInt() & 0x7);
        int xorWeight = 1 + (picker.nextInt() & 0x7);
        int weightTotal = additiveWeight + xorWeight;
        int touched = 0;
        for (AbstractInsnNode ins : m.instructions.toArray()) {
            Integer iv = readIntConstant(ins);
            if (iv != null && ConstantTaste.worthMasking(iv)) {
                int roll = picker.nextInt(weightTotal);
                InsnList replacement = roll < additiveWeight ? ints.buildExpression(iv) : xor.encode(iv);
                m.instructions.insertBefore(ins, replacement);
                m.instructions.remove(ins);
                touched++;
                continue;
            }
            Long lv = readLongConstant(ins);
            if (lv != null && ConstantTaste.worthMasking(lv)) {
                InsnList replacement = longs.mask(lv);
                m.instructions.insertBefore(ins, replacement);
                m.instructions.remove(ins);
                touched++;
            }
        }
        return touched;
    }

    private Integer readIntConstant(AbstractInsnNode ins) {
        int op = ins.getOpcode();
        if (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5) {
            return op - Opcodes.ICONST_0;
        }
        if (ins instanceof IntInsnNode bi && (op == Opcodes.BIPUSH || op == Opcodes.SIPUSH)) {
            return bi.operand;
        }
        if (ins instanceof LdcInsnNode ldc && ldc.cst instanceof Integer i) {
            return i;
        }
        return null;
    }

    private Long readLongConstant(AbstractInsnNode ins) {
        int op = ins.getOpcode();
        if (op == Opcodes.LCONST_0) return 0L;
        if (op == Opcodes.LCONST_1) return 1L;
        if (ins instanceof LdcInsnNode ldc && ldc.cst instanceof Long l) {
            return l;
        }
        return null;
    }
}
