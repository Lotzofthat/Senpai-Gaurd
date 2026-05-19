package senpai.transforms.junk;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import senpai.asm.AccessFlags;
import senpai.asm.InsnFactory;
import senpai.pipeline.Transform;
import senpai.pipeline.TransformContext;
import senpai.transforms.strings.LazyDecodeCache;
import senpai.util.DeterministicRng;

public final class DeadBranchInjector implements Transform {

    @Override
    public String id() {
        return "junk.deadBranches";
    }

    @Override
    public void apply(TransformContext ctx) {
        int density = ctx.config().junk.densityPerMethod;
        DeterministicRng forked = ctx.rngFor("junk.deadBranches");
        JunkPlacement place = new JunkPlacement(forked);
        int injected = 0;
        for (ClassNode cls : ctx.jar().classes().values()) {
            for (MethodNode m : cls.methods) {
                if (skip(m)) {
                    continue;
                }
                int perMethod = density == 0 ? 0 : forked.nextInt(density * 2 + 1);
                JunkBudget local = new JunkBudget(perMethod);
                while (local.spend()) {
                    place.insertAtRandomLabel(m, JunkBlockTemplates.deadBranch(forked));
                    injected++;
                }
            }
        }
        ctx.summary().recordMetric("junk.deadBranches.count", injected);
    }

    private boolean skip(MethodNode m) {
        if (LazyDecodeCache.isReservedMemberName(m.name)) return true;
        if (AccessFlags.isNative(m.access) || AccessFlags.isAbstract(m.access)) return true;
        if (m.instructions == null || m.instructions.size() == 0) return true;
        if (m.name.equals("<init>") || m.name.equals("<clinit>")) return true;
        return false;
    }
}
