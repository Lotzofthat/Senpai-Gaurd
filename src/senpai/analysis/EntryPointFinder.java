package senpai.analysis;

import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import senpai.asm.MemberKey;
import senpai.io.LoadedJar;

public final class EntryPointFinder {

    public Set<MemberKey> find(LoadedJar jar) {
        Set<MemberKey> entries = new HashSet<>();
        String mainClass = readMainAttribute(jar);
        for (ClassNode cls : jar.classes().values()) {
            for (MethodNode m : cls.methods) {
                if (m.name.equals("main") && m.desc.equals("([Ljava/lang/String;)V")) {
                    entries.add(new MemberKey(cls.name, m.name, m.desc));
                }
                if (m.name.equals("premain") || m.name.equals("agentmain")) {
                    entries.add(new MemberKey(cls.name, m.name, m.desc));
                }
            }
            if (mainClass != null && cls.name.equals(mainClass.replace('.', '/'))) {
                for (MethodNode m : cls.methods) {
                    if (m.name.equals("main")) {
                        entries.add(new MemberKey(cls.name, m.name, m.desc));
                    }
                }
            }
        }
        return entries;
    }

    private static String readMainAttribute(LoadedJar jar) {
        if (!jar.manifest().present) {
            return null;
        }
        return jar.manifest().manifest.getMainAttributes().getValue("Main-Class");
    }
}
