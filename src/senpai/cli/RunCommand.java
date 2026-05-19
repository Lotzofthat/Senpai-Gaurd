package senpai.cli;

import java.nio.file.Path;
import senpai.config.ConfigLoader;
import senpai.config.SenpaiConfig;
import senpai.pipeline.PipelineRunner;

public final class RunCommand {

    public int execute(String[] args) {
        if (args.length < 2) {
            System.err.println("usage: senpai run <input.jar> <output.jar> [config.yml]");
            return 2;
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Path configPath = args.length >= 3 ? Path.of(args[2]) : null;
        SenpaiConfig cfg = new ConfigLoader().loadOrDefault(configPath);
        return new PipelineRunner(cfg).run(input, output);
    }
}
