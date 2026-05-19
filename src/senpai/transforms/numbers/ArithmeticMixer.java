package senpai.transforms.numbers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import senpai.asm.InsnFactory;
import senpai.util.DeterministicRng;

public final class ArithmeticMixer {

    private final DeterministicRng rng;
    private final int maxOperands;

    public ArithmeticMixer(DeterministicRng rng, int maxOperands) {
        if (maxOperands < 2) {
            throw new IllegalArgumentException("maxOperands must be at least 2 for the mixer to produce a real addition chain");
        }
        this.rng = rng;
        this.maxOperands = maxOperands;
    }

    public InsnList buildExpression(int target) {
        int operands = 2 + rng.nextInt(maxOperands - 1);
        int[] parts = new int[operands];
        int runningSum = 0;
        for (int i = 0; i < operands - 1; i++) {
            int chunk = rng.nextInt();
            // half chance of going negative, but choose from the rng, not
            // from a hardcoded i&1 parity check.
            if ((rng.nextInt() & 1) == 0) {
                chunk = -chunk;
            }
            parts[i] = chunk;
            runningSum += chunk;
        }
        parts[operands - 1] = target - runningSum;

        InsnList out = new InsnList();
        out.add(InsnFactory.pushInt(parts[0]));
        for (int i = 1; i < operands; i++) {
            out.add(InsnFactory.pushInt(parts[i]));
            out.add(new InsnNode(Opcodes.IADD));
        }
        return out;
    }
}
