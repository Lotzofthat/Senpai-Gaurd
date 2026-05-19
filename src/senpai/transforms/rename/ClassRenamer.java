package senpai.transforms.rename;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import senpai.pipeline.Transform;
import senpai.pipeline.TransformContext;
import senpai.util.NameGenerator;

public final class ClassRenamer implements Transform {

    @Override
    public String id() {
        return "rename.classes";
    }

    @Override
    public void apply(TransformContext ctx) {
        // SimpleRemapper takes a flat map keyed by old name. classes get the
        // bare internal name, method and field entries get "owner.name(desc)"
        // and "owner.name". we only fill in the class half here, the method
        // and field renamers contribute to the same map (it lives on the
        // context) so we can run them all in one ClassRemapper pass.
        Map<String, String> mapping = ctx.renameMap();
        NameGenerator gen = new NameGenerator(ctx.config().renaming.alphabet);
        for (String original : ctx.jar().classes().keySet()) {
            if (ctx.keep().shouldKeep(original)) {
                continue;
            }
            if (!RenameGuards.isRenameableClassName(original)) {
                continue;
            }
            mapping.put(original, "o/" + gen.nextWithPrefix(""));
        }
        if (mapping.isEmpty()) {
            return;
        }
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
}
