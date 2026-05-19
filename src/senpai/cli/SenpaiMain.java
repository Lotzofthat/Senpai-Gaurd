package senpai.cli;

public final class SenpaiMain {

    public static final String MOTTO = "It's not like I want to protect your code or anything...";

    private SenpaiMain() {
    }

    public static void main(String[] args) {
        System.out.println(MOTTO);
        int exitCode = new CommandRouter().dispatch(args);
        System.exit(exitCode);
    }
}
