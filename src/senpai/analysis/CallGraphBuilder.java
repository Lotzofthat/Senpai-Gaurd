package senpai.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import senpai.asm.MemberKey;
import senpai.io.LoadedJar;

public final class CallGraphBuilder {

    public Map<MemberKey, Set<MemberKey>> build(LoadedJar jar) {
        Map<MemberKey, Set<MemberKey>> outgoing = new HashMap<>();
        for (ClassNode cls : jar.classes().values()) {
            for (MethodNode m : cls.methods) {
                MemberKey from = new MemberKey(cls.name, m.name, m.desc);
                Set<MemberKey> targets = outgoing.computeIfAbsent(from, k -> new HashSet<>());
                if (m.instructions == null) {
                    continue;
                }
                for (AbstractInsnNode ins : m.instructions) {
                    if (ins instanceof MethodInsnNode call) {
                        targets.add(new MemberKey(call.owner, call.name, call.desc));
                    }
                }
            }
        }
        return outgoing;
    }
}
