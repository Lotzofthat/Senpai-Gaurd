package senpai.reporting;

import java.io.PrintStream;

public final class SummaryWriter {

    public void write(RunSummary summary, PrintStream out) {
        out.println();
        out.println("senpai run summary");
        out.printf("classes processed: %d%n", summary.classCount);
        out.printf("total time: %.2f ms%n", summary.totalNanos / 1_000_000.0);
        if (!summary.metrics.isEmpty()) {
            out.println("metrics:");
            for (var e : summary.metrics.entrySet()) {
                out.printf("  %s = %d%n", e.getKey(), e.getValue());
            }
        }
        for (RunSummary.TransformResult r : summary.results) {
            if (r.error == null) {
                out.printf("  ok    %s (%.2f ms)%n", r.id, r.nanos / 1_000_000.0);
            } else {
                out.printf("  fail  %s (%s)%n", r.id, r.error);
            }
        }
    }
}
