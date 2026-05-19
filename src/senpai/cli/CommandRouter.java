package senpai.cli;

public final class CommandRouter {

    public int dispatch(String[] args) {
        if (args.length == 0) {
            new UsagePrinter().print(System.out);
            return 0;
        }
        String head = args[0];
        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        return switch (head) {
            case "run" -> new RunCommand().execute(rest);
            case "verify" -> new VerifyCommand().execute(rest);
            case "help", "--help", "-h" -> {
                new UsagePrinter().print(System.out);
                yield 0;
            }
            default -> {
                System.err.println("unknown command: " + head);
                new UsagePrinter().print(System.err);
                yield 2;
            }
        };
    }
}
