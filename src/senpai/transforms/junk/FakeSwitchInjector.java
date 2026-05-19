package senpai.transforms.junk;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import senpai.asm.AccessFlags;
import senpai.asm.InsnFactory;
import senpai.pipeline.Transform;
import senpai.pipeline.TransformContext;
import senpai.transforms.strings.LazyDecodeCache;
import senpai.util.DeterministicRng;

public final class FakeSwitchInjector implements Transform {

    @Override
    public String id() {
        return "junk.fakeSwitches";
    }

    @Override
    public void apply(TransformContext ctx) {
        int density = ctx.config().junk.densityPerMethod;
        DeterministicRng forked = ctx.rngFor("junk.fakeSwitches");
        JunkPlacement place = new JunkPlacement(forked);
        int budget = 0;
        for (ClassNode cls : ctx.jar().classes().values()) {
            for (MethodNode m : cls.methods) {
                if (skip(m)) {
                    continue;
                }
                // density value drawn from the rng with the configured value
                // as the upper bound of the open range. zero is allowed, so a
                // method can pass through untouched.
                int perMethod = density == 0 ? 0 : forked.nextInt(density * 2 + 1);
                JunkBudget local = new JunkBudget(perMethod);
                while (local.spend()) {
                    place.insertAtRandomLabel(m, buildSwitchTrap(forked));
                    budget++;
                }
            }
        }
        ctx.summary().recordMetric("junk.fakeSwitches.count", budget);
    }

    private boolean skip(MethodNode m) {
        if (LazyDecodeCache.isReservedMemberName(m.name)) return true;
        if (AccessFlags.isNative(m.access) || AccessFlags.isAbstract(m.access)) return true;
        if (m.instructions == null || m.instructions.size() == 0) return true;
        if (m.name.equals("<init>") || m.name.equals("<clinit>")) return true;
        return false;
    }

    private InsnList buildSwitchTrap(DeterministicRng rng) {
        // case count and pick value both come from the rng. the tautological
        // skip in front means the switch never runs, but it still has to
        // verify cleanly under every arm, so each case is built end to end.
        int caseCount = 2 + rng.nextInt(4);
        LabelNode after = new LabelNode();
        LabelNode defaultLabel = new LabelNode();
        LabelNode[] cases = new LabelNode[caseCount];
        for (int i = 0; i < caseCount; i++) {
            cases[i] = new LabelNode();
        }
        int pick = rng.nextInt(caseCount);

        InsnList block = new InsnList();
        block.add(InsnFactory.pushInt(rng.nextInt() | 1));
        block.add(new InsnNode(Opcodes.DUP));
        block.add(new InsnNode(Opcodes.IXOR));
        block.add(new JumpInsnNode(Opcodes.IFEQ, after));

        block.add(InsnFactory.pushInt(pick));
        block.add(new TableSwitchInsnNode(0, caseCount - 1, defaultLabel, cases));
        for (int i = 0; i < caseCount; i++) {
            block.add(cases[i]);
            block.add(new InsnNode(Opcodes.NOP));
            block.add(new JumpInsnNode(Opcodes.GOTO, after));
        }
        block.add(defaultLabel);
        block.add(new InsnNode(Opcodes.NOP));
        block.add(after);
        return block;
    }
}
