package senpai.transforms.rename;

import java.util.Map;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import senpai.asm.AccessFlags;
import senpai.pipeline.Transform;
import senpai.pipeline.TransformContext;
import senpai.util.NameGenerator;

public final class FieldRenamer implements Transform {

    @Override
    public String id() {
        return "rename.fields";
    }

    @Override
    public void apply(TransformContext ctx) {
        Map<String, String> mapping = ctx.renameMap();
        NameGenerator gen = new NameGenerator(ctx.config().renaming.alphabet);
        for (ClassNode cls : ctx.jar().classes().values()) {
            if (ctx.keep().shouldKeep(cls.name)) {
                continue;
            }
            for (FieldNode f : cls.fields) {
                // enum sentinels carry the constant names that Enum.valueOf
                // reflects on. touching them breaks every reflective consumer.
                if (AccessFlags.isStatic(f.access) && (f.access & org.objectweb.asm.Opcodes.ACC_ENUM) != 0) {
                    continue;
                }
                if (f.name.equals("serialVersionUID")) {
                    continue;
                }
                if (senpai.transforms.strings.LazyDecodeCache.isReservedMemberName(f.name)) {
                    continue;
                }
                mapping.put(cls.name + "." + f.name, gen.next());
            }
        }
    }
}
