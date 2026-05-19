package senpai.asm;

import java.util.Objects;

public final class MemberKey {

    public final String owner;
    public final String name;
    public final String descriptor;

    public MemberKey(String owner, String name, String descriptor) {
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MemberKey k)) {
            return false;
        }
        return owner.equals(k.owner) && name.equals(k.name) && descriptor.equals(k.descriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, name, descriptor);
    }

    @Override
    public String toString() {
        return owner + "." + name + descriptor;
    }
}
