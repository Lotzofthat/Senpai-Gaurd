package senpai.pipeline;

import java.util.List;

public final class TransformExecutor {

    public void runAll(List<Transform> chain, TransformContext ctx) {
        for (Transform t : chain) {
            long t0 = System.nanoTime();
            try {
                t.apply(ctx);
                ctx.summary().recordSuccess(t.id(), System.nanoTime() - t0);
            } catch (RuntimeException ex) {
                ctx.summary().recordFailure(t.id(), ex);
                throw ex;
            }
        }
    }
}
