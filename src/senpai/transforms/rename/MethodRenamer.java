package senpai.transforms.rename;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import senpai.pipeline.Transform;
import senpai.pipeline.TransformContext;
import senpai.util.NameGenerator;

public final class MethodRenamer implements Transform {

    @Override
    public String id() {
        return "rename.methods";
    }

    @Override
    public void apply(TransformContext ctx) {
        Map<String, String> mapping = ctx.renameMap();
        NameGenerator gen = new NameGenerator(ctx.config().renaming.alphabet);

        // group overrides by (name, desc) so we hand the same target name to
        // every class in the hierarchy that exposes the same signature. if we
        // pick fresh names per class we'd break dynamic dispatch immediately.
        Map<String, String> sigToNewName = new HashMap<>();
        Set<String> reservedSigs = collectReservedSignatures(ctx);

        for (ClassNode cls : ctx.jar().classes().values()) {
            if (ctx.keep().shouldKeep(cls.name)) {
                continue;
            }
            for (MethodNode m : cls.methods) {
                if (!RenameGuards.isRenameableMethod(m)) {
                    continue;
                }
                if (senpai.transforms.strings.LazyDecodeCache.isReservedMemberName(m.name)) {
                    continue;
                }
                String signature = m.name + m.desc;
                if (reservedSigs.contains(signature)) {
                    continue;
                }
                String fresh = sigToNewName.computeIfAbsent(signature, s -> gen.next());
                mapping.put(cls.name + "." + m.name + m.desc, fresh);
            }
        }
    }

    // any signature that matches a non internal supertype member is something
    // we cannot rename safely. without a full hierarchy walk we approximate by
    // pinning Object's signatures and anything the keep list pins.
    private Set<String> collectReservedSignatures(TransformContext ctx) {
        Set<String> reserved = new HashSet<>();
        reserved.add("toString()Ljava/lang/String;");
        reserved.add("hashCode()I");
        reserved.add("equals(Ljava/lang/Object;)Z");
        reserved.add("clone()Ljava/lang/Object;");
        reserved.add("finalize()V");
        reserved.add("getClass()Ljava/lang/Class;");
        reserved.add("notify()V");
        reserved.add("notifyAll()V");
        reserved.add("wait()V");
        reserved.add("wait(J)V");
        reserved.add("wait(JI)V");
        for (ClassNode cls : ctx.jar().classes().values()) {
            if (!ctx.keep().shouldKeep(cls.name)) {
                continue;
            }
            for (MethodNode m : cls.methods) {
                reserved.add(m.name + m.desc);
            }
        }
        return reserved;
    }
}
