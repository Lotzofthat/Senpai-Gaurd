package senpai.transforms.flow;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import senpai.asm.AccessFlags;
import senpai.pipeline.Transform;
import senpai.pipeline.TransformContext;
import senpai.transforms.strings.LazyDecodeCache;

public final class ControlFlowFlattener implements Transform {

    @Override
    public String id() {
        return "flow.flatten";
    }

    @Override
    public void apply(TransformContext ctx) {
        BasicBlockSplitter splitter = new BasicBlockSplitter();
        SwitchDispatchBuilder dispatch = new SwitchDispatchBuilder(ctx.rngFor("flow.flatten"));
        SgFlowMarker marker = new SgFlowMarker();
        int cap = ctx.config().flow.maxBlocksPerMethod;
        for (ClassNode cls : ctx.jar().classes().values()) {
            if (ctx.keep().shouldKeep(cls.name)) {
                continue;
            }
            for (MethodNode m : cls.methods) {
                // skip our own synthetics, plus anything the rename guards
                // already said is special. flattening <init> can confuse the
                // verifier when the superclass call moves across the switch.
                if (LazyDecodeCache.isReservedMemberName(m.name)) {
                    continue;
                }
                if (m.name.equals("<init>") || m.name.equals("<clinit>")) {
                    continue;
                }
                if (AccessFlags.isNative(m.access) || AccessFlags.isAbstract(m.access)) {
                    continue;
                }
                if (m.instructions == null || m.instructions.size() == 0) {
                    continue;
                }
                BasicBlockSplitter.Blocks blocks = splitter.split(m);
                if (blocks.count() <= 1 || blocks.count() > cap) {
                    continue;
                }
                if (dispatch.rewriteAsDispatch(m, blocks)) {
                    marker.record(cls.name, m.name, m.desc);
                }
            }
        }
        ctx.summary().recordMetric("flow.flatten.count", marker.count());
    }
}
