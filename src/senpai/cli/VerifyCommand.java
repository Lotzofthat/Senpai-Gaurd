package senpai.cli;

import java.nio.file.Path;
import senpai.io.JarReader;
import senpai.reporting.IntegrityReport;

public final class VerifyCommand {

    public int execute(String[] args) {
        if (args.length < 1) {
            System.err.println("usage: senpai verify <jar>");
            return 2;
        }
        Path jar = Path.of(args[0]);
        IntegrityReport report = new JarReader().scanForIntegrity(jar);
        report.writeTo(System.out);
        return report.hasFailures() ? 1 : 0;
    }
}
