package senpai.analysis;

import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import senpai.io.LoadedJar;

public final class SerializationFinder {

    public Set<String> findSerializableClasses(LoadedJar jar) {
        Set<String> hits = new HashSet<>();
        for (ClassNode cls : jar.classes().values()) {
            if (cls.interfaces != null && cls.interfaces.contains("java/io/Serializable")) {
                hits.add(cls.name);
                continue;
            }
            for (MethodNode m : cls.methods) {
                if (m.name.equals("writeObject") || m.name.equals("readObject") || m.name.equals("readResolve") || m.name.equals("writeReplace")) {
                    hits.add(cls.name);
                    break;
                }
            }
        }
        return hits;
    }
}
