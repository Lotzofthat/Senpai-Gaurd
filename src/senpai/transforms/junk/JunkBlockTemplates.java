package senpai.transforms.junk;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import senpai.asm.InsnFactory;
import senpai.util.DeterministicRng;

public final class JunkBlockTemplates {

    public static InsnList deadBranch(DeterministicRng rng) {
        // (1 != 0) is statically true. we hop over the dead body using a
        // tautology so the verifier accepts a fresh stack on exit.
        LabelNode skip = new LabelNode();
        InsnList block = new InsnList();
        block.add(InsnFactory.pushInt(rng.nextInt() | 1));
        block.add(new InsnNode(Opcodes.DUP));
        block.add(new InsnNode(Opcodes.IXOR));
        block.add(new JumpInsnNode(Opcodes.IFEQ, skip));
        // dead body. produces nothing on the stack.
        block.add(new InsnNode(Opcodes.ACONST_NULL));
        block.add(new InsnNode(Opcodes.POP));
        block.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false));
        block.add(new InsnNode(Opcodes.POP));
        block.add(skip);
        return block;
    }

    public static InsnList wastefulStringHashing() {
        InsnList block = new InsnList();
        block.add(new LdcInsnNode("senpai watches"));
        block.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false));
        block.add(new InsnNode(Opcodes.POP));
        return block;
    }

    public static InsnList loopingTimewaster(int iterations) {
        InsnList block = new InsnList();
        LabelNode top = new LabelNode();
        LabelNode exit = new LabelNode();
        block.add(new InsnNode(Opcodes.ICONST_0));
        block.add(top);
        block.add(new InsnNode(Opcodes.DUP));
        block.add(InsnFactory.pushInt(iterations));
        block.add(new JumpInsnNode(Opcodes.IF_ICMPGE, exit));
        block.add(new InsnNode(Opcodes.ICONST_1));
        block.add(new InsnNode(Opcodes.IADD));
        block.add(new JumpInsnNode(Opcodes.GOTO, top));
        block.add(exit);
        block.add(new InsnNode(Opcodes.POP));
        return block;
    }
}
