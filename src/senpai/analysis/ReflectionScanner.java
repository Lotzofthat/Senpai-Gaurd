package senpai.analysis;

import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import senpai.io.LoadedJar;

public final class ReflectionScanner {

    private static final Set<String> SUSPICIOUS_OWNERS = Set.of(
        "java/lang/Class",
        "java/lang/reflect/Method",
        "java/lang/reflect/Field",
        "java/lang/reflect/Constructor",
        "java/lang/invoke/MethodHandles$Lookup"
    );

    public Set<String> findReferencedClassNames(LoadedJar jar) {
        Set<String> referenced = new HashSet<>();
        for (ClassNode cls : jar.classes().values()) {
            for (MethodNode m : cls.methods) {
                if (m.instructions == null) {
                    continue;
                }
                scanMethod(m, referenced);
            }
        }
        return referenced;
    }

    private void scanMethod(MethodNode m, Set<String> referenced) {
        AbstractInsnNode prev = null;
        for (AbstractInsnNode ins : m.instructions) {
            if (ins instanceof MethodInsnNode call && SUSPICIOUS_OWNERS.contains(call.owner)) {
                if (prev instanceof LdcInsnNode ldc && ldc.cst instanceof String s) {
                    referenced.add(s.replace('.', '/'));
                }
            }
            prev = ins;
        }
    }
}
