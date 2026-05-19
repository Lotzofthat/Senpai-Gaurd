package senpai.asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.tree.ClassNode;

public final class ClassGraph {

    private final Map<String, ClassNode> byName;
    private final Map<String, List<String>> children = new HashMap<>();

    public ClassGraph(Map<String, ClassNode> classes) {
        this.byName = new HashMap<>(classes);
        for (ClassNode node : byName.values()) {
            register(node.superName, node.name);
            if (node.interfaces != null) {
                for (String iface : node.interfaces) {
                    register(iface, node.name);
                }
            }
        }
    }

    public ClassNode get(String internalName) {
        return byName.get(internalName);
    }

    public List<String> directChildrenOf(String parent) {
        return children.getOrDefault(parent, List.of());
    }

    public boolean isInternal(String internalName) {
        return byName.containsKey(internalName);
    }

    private void register(String parent, String child) {
        if (parent == null) {
            return;
        }
        children.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
    }
}
