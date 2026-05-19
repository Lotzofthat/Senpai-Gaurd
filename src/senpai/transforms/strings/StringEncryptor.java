package senpai.transforms.strings;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import senpai.pipeline.Transform;
import senpai.pipeline.TransformContext;
import senpai.util.DeterministicRng;

public final class StringEncryptor implements Transform {

    @Override
    public String id() {
        return "strings.encrypt";
    }

    @Override
    public void apply(TransformContext ctx) {
        int minLength = ctx.config().strings.minLength;
        DecoderInjector injector = new DecoderInjector();
        // the class fork salt itself is drawn from the master rng. ledger
        // records the master draw, the per class salts come from a fork
        // dedicated to this pass.
        DeterministicRng saltSource = ctx.rngFor("strings.encrypt.classSalt");
        for (ClassNode cls : ctx.jar().classes().values()) {
            DeterministicRng classRng = saltSource.fork(saltSource.nextLong());
            XorKeyTable keys = new XorKeyTable(classRng);
            StringPoolBuilder pool = new StringPoolBuilder(keys.seed(), classRng.fork(classRng.nextLong()));
            for (MethodNode m : cls.methods) {
                if (m.instructions == null) {
                    continue;
                }
                for (AbstractInsnNode ins : m.instructions.toArray()) {
                    if (!(ins instanceof LdcInsnNode ldc) || !(ldc.cst instanceof String literal)) {
                        continue;
                    }
                    if (literal.length() < minLength) {
                        continue;
                    }
                    byte[] key = keys.nextKey();
                    int slot = pool.add(literal, key);
                    injector.replaceLdcWithDecoderCall(m, ldc, cls.name, slot);
                }
            }
            if (pool.size() > 0) {
                injector.install(cls, pool);
                ctx.stringPools().put(cls.name, pool.entries());
            }
        }
    }
}
