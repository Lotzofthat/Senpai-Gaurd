package senpai.reporting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RunSummaryBuilder {

    private final List<RunSummary.TransformResult> results = new ArrayList<>();
    private final Map<String, Long> metrics = new LinkedHashMap<>();

    public void recordSuccess(String id, long nanos) {
        results.add(new RunSummary.TransformResult(id, nanos, null));
    }

    public void recordFailure(String id, Throwable cause) {
        results.add(new RunSummary.TransformResult(id, 0L, cause.getClass().getSimpleName() + ": " + cause.getMessage()));
    }

    public void recordMetric(String key, long value) {
        metrics.merge(key, value, Long::sum);
    }

    public RunSummary finalize(long totalNanos, int classCount) {
        return new RunSummary(results, metrics, totalNanos, classCount);
    }
}
