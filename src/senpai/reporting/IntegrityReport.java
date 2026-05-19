package senpai.reporting;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public final class IntegrityReport {

    private final List<String> failures = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();

    public void fail(String location, String message) {
        failures.add(location + ": " + message);
    }

    public void note(String message) {
        notes.add(message);
    }

    public boolean hasFailures() {
        return !failures.isEmpty();
    }

    public void writeTo(PrintStream out) {
        out.println("senpai integrity report");
        for (String n : notes) {
            out.println("note: " + n);
        }
        if (failures.isEmpty()) {
            out.println("result: ok");
            return;
        }
        for (String f : failures) {
            out.println("fail: " + f);
        }
        out.println("result: failed (" + failures.size() + " issues)");
    }
}
