package senpai.transforms.junk;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import senpai.util.DeterministicRng;

public final class JunkPlacement {

    private final DeterministicRng rng;

    public JunkPlacement(DeterministicRng rng) {
        this.rng = rng;
    }

    public void insertAtRandomLabel(MethodNode method, InsnList payload) {
        AbstractInsnNode anchor = pickRandomLabel(method);
        if (anchor == null) {
            method.instructions.insert(payload);
            return;
        }
        method.instructions.insert(anchor, payload);
    }

    private AbstractInsnNode pickRandomLabel(MethodNode method) {
        AbstractInsnNode[] all = method.instructions.toArray();
        int candidates = 0;
        for (AbstractInsnNode ins : all) {
            if (ins instanceof LabelNode) {
                candidates++;
            }
        }
        if (candidates == 0) {
            return null;
        }
        int target = rng.nextInt(candidates);
        int seen = 0;
        for (AbstractInsnNode ins : all) {
            if (ins instanceof LabelNode) {
                if (seen == target) {
                    return ins;
                }
                seen++;
            }
        }
        return null;
    }
}
