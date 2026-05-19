package senpai.transforms.flow;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import senpai.asm.AccessFlags;
import senpai.pipeline.Transform;
import senpai.pipeline.TransformContext;
import senpai.transforms.strings.LazyDecodeCache;

public final class OpaquePredicateInjector implements Transform {

    @Override
    public String id() {
        return "flow.opaque";
    }

    @Override
    public void apply(TransformContext ctx) {
        int injected = 0;
        for (ClassNode cls : ctx.jar().classes().values()) {
            if (ctx.keep().shouldKeep(cls.name)) {
                continue;
            }
            for (MethodNode m : cls.methods) {
                if (LazyDecodeCache.isReservedMemberName(m.name)) {
                    continue;
                }
                if (AccessFlags.isNative(m.access) || AccessFlags.isAbstract(m.access)) {
                    continue;
                }
                if (m.instructions == null || m.instructions.size() == 0) {
                    continue;
                }
                injectAtEntry(m);
                injected++;
            }
        }
        ctx.summary().recordMetric("flow.opaque.count", injected);
    }

    private void injectAtEntry(MethodNode method) {
        // pattern, x = (int) System.nanoTime(). if ((x ^ x) != 0) { throw }
        // x ^ x is provably zero, but System.nanoTime defeats verifier-time
        // folding. the dead branch throws so the verifier still accepts the
        // method body that follows (it sees a clean exit from the dead arm).
        InsnList head = new InsnList();
        LabelNode realPath = new LabelNode();
        LabelNode deadPath = new LabelNode();

        head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false));
        head.add(new InsnNode(Opcodes.L2I));
        head.add(new InsnNode(Opcodes.DUP));
        head.add(new InsnNode(Opcodes.IXOR));
        head.add(new JumpInsnNode(Opcodes.IFNE, deadPath));
        head.add(new JumpInsnNode(Opcodes.GOTO, realPath));

        head.add(deadPath);
        head.add(new TypeInsnNode(Opcodes.NEW, "java/lang/RuntimeException"));
        head.add(new InsnNode(Opcodes.DUP));
        head.add(new LdcInsnNode("senpai opaque"));
        head.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false));
        head.add(new InsnNode(Opcodes.ATHROW));

        head.add(realPath);

        AbstractInsnNode first = method.instructions.getFirst();
        if (first == null) {
            method.instructions.add(head);
        } else {
            method.instructions.insertBefore(first, head);
        }
        method.maxStack = Math.max(method.maxStack, 4);
    }
}
