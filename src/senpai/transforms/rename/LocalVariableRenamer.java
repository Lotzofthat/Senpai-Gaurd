package senpai.transforms.rename;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import senpai.pipeline.Transform;
import senpai.pipeline.TransformContext;
import senpai.util.NameGenerator;

// despite the file name this pass owns two responsibilities the rename family
// needs to finish: apply the accumulated method and field rename map to every
// class, and rewrite local variable names. doing both in one walk avoids
// rebuilding ClassNodes twice.
public final class LocalVariableRenamer implements Transform {

    @Override
    public String id() {
        return "rename.apply";
    }

    @Override
    public void apply(TransformContext ctx) {
        Map<String, String> mapping = ctx.renameMap();
        if (!mapping.isEmpty()) {
            SimpleRemapper remapper = new SimpleRemapper(mapping);
            Map<String, ClassNode> rebuilt = new HashMap<>();
            for (ClassNode node : ctx.jar().classes().values()) {
                ClassNode copy = new ClassNode();
                node.accept(new ClassRemapper(copy, remapper));
                rebuilt.put(copy.name, copy);
            }
            ctx.jar().classes().clear();
            ctx.jar().classes().putAll(rebuilt);
        }

        if (!ctx.config().renaming.locals) {
            return;
        }
        NameGenerator gen = new NameGenerator(ctx.config().renaming.alphabet);
        for (ClassNode cls : ctx.jar().classes().values()) {
            for (MethodNode m : cls.methods) {
                if (m.localVariables == null) {
                    continue;
                }
                for (LocalVariableNode local : m.localVariables) {
                    if (local.name.equals("this")) {
                        continue;
                    }
                    local.name = gen.next();
                }
            }
        }
    }
}
