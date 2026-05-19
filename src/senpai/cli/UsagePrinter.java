package senpai.cli;

import java.io.PrintStream;

public final class UsagePrinter {

    public void print(PrintStream out) {
        out.println("senpai guard");
        out.println();
        out.println("commands:");
        out.println("  run <input.jar> <output.jar> [config.yml]   obfuscate a jar");
        out.println("  verify <jar>                                 verify a guarded jar");
        out.println("  help                                         show this message");
    }
}
