package senpai.transforms.flow;

import java.util.ArrayList;
import java.util.List;

// per run record of which methods the flattener rewrote. consulted by the
// integrity verifier so a verify pass can spot classes whose dispatch field
// disappeared (which would indicate post processing tampered with the jar).
public final class SgFlowMarker {

    private final List<String> flattenedMethods = new ArrayList<>();

    public void record(String ownerInternalName, String methodName, String descriptor) {
        flattenedMethods.add(ownerInternalName + "." + methodName + descriptor);
    }

    public List<String> snapshot() {
        return List.copyOf(flattenedMethods);
    }

    public int count() {
        return flattenedMethods.size();
    }
}
