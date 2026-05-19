package senpai.transforms.rename;

import org.objectweb.asm.tree.MethodNode;
import senpai.asm.AccessFlags;

public final class RenameGuards {

    public static boolean isRenameableMethod(MethodNode m) {
        if (m.name.startsWith("<")) {
            return false;
        }
        if (AccessFlags.isNative(m.access)) {
            return false;
        }
        // main is the JVM contract entry. enum bridge helpers are reflected
        // on by language internals, so we leave those alone too.
        if (m.name.equals("main") && m.desc.equals("([Ljava/lang/String;)V")) {
            return false;
        }
        if (m.name.equals("values") || m.name.equals("valueOf")) {
            return false;
        }
        return true;
    }

    public static boolean isRenameableClassName(String internalName) {
        return !internalName.startsWith("java/")
            && !internalName.startsWith("javax/")
            && !internalName.startsWith("jdk/")
            && !internalName.startsWith("sun/");
    }
}
