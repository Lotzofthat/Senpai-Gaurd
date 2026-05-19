package senpai.analysis;

import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import senpai.asm.AccessFlags;
import senpai.asm.MemberKey;
import senpai.io.LoadedJar;

public final class NativeMethodFinder {

    public Set<MemberKey> findNativeMethods(LoadedJar jar) {
        Set<MemberKey> hits = new HashSet<>();
        for (ClassNode cls : jar.classes().values()) {
            for (MethodNode m : cls.methods) {
                if (AccessFlags.isNative(m.access)) {
                    hits.add(new MemberKey(cls.name, m.name, m.desc));
                }
            }
        }
        return hits;
    }
}
