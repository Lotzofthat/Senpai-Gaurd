package senpai.transforms.junk;

public final class JunkBudget {

    private int remaining;

    public JunkBudget(int allowance) {
        this.remaining = Math.max(0, allowance);
    }

    public boolean spend() {
        if (remaining <= 0) {
            return false;
        }
        remaining--;
        return true;
    }

    public int remaining() {
        return remaining;
    }
}
