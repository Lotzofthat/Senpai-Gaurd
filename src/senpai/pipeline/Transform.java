package senpai.pipeline;

public interface Transform {

    String id();

    void apply(TransformContext ctx);
}
