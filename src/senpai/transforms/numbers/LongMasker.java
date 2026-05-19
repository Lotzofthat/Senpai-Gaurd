package senpai.transforms.numbers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import senpai.util.DeterministicRng;

public final class LongMasker {

    private final DeterministicRng rng;

    public LongMasker(DeterministicRng rng) {
        this.rng = rng;
    }

    public InsnList mask(long target) {
        // n layers of mixed LXOR and LADD operations. n is drawn here, the
        // shape of each layer is drawn per layer, and the residual carries
        // the inverse of the cumulative effect.
        int layers = 2 + (rng.nextInt() & 0x7);
        int[] ops = new int[layers];
        long[] operands = new long[layers];
        long residual = target;
        for (int i = 0; i < layers; i++) {
            boolean useXor = (rng.nextInt() & 1) == 0;
            long arg = rng.nextLong();
            ops[i] = useXor ? Opcodes.LXOR : Opcodes.LADD;
            operands[i] = arg;
            if (useXor) {
                residual ^= arg;
            } else {
                residual -= arg;
            }
        }
        InsnList out = new InsnList();
        out.add(new LdcInsnNode(residual));
        for (int i = 0; i < layers; i++) {
            out.add(new LdcInsnNode(operands[i]));
            out.add(new InsnNode(ops[i]));
        }
        return out;
    }
}
