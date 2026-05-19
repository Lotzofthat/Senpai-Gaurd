package senpai.transforms.numbers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import senpai.asm.InsnFactory;
import senpai.util.DeterministicRng;

public final class XorMath {

    private final DeterministicRng rng;

    public XorMath(DeterministicRng rng) {
        this.rng = rng;
    }

    public InsnList encode(int target) {
        // n layered XOR masks. n itself is drawn from the rng with a minimum
        // of two so the result is always at least one IXOR. each mask is a
        // fresh int. the residual carries the bits that bring the chain back
        // to target.
        int layers = 2 + (rng.nextInt() & 0x7);
        int[] masks = new int[layers];
        int residual = target;
        for (int i = 0; i < layers; i++) {
            masks[i] = rng.nextInt();
            residual ^= masks[i];
        }
        InsnList out = new InsnList();
        out.add(InsnFactory.pushInt(residual));
        for (int i = 0; i < layers; i++) {
            out.add(InsnFactory.pushInt(masks[i]));
            out.add(new InsnNode(Opcodes.IXOR));
        }
        return out;
    }
}
