package senpai.reporting;

import java.util.List;
import java.util.Map;

public final class RunSummary {

    public final List<TransformResult> results;
    public final Map<String, Long> metrics;
    public final long totalNanos;
    public final int classCount;

    public RunSummary(List<TransformResult> results, Map<String, Long> metrics, long totalNanos, int classCount) {
        this.results = List.copyOf(results);
        this.metrics = Map.copyOf(metrics);
        this.totalNanos = totalNanos;
        this.classCount = classCount;
    }

    public static final class TransformResult {
        public final String id;
        public final long nanos;
        public final String error;

        public TransformResult(String id, long nanos, String error) {
            this.id = id;
            this.nanos = nanos;
            this.error = error;
        }
    }
}
