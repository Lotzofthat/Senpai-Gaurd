package senpai.transforms.flow;

import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import senpai.asm.InsnFactory;
import senpai.util.DeterministicRng;

public final class SwitchDispatchBuilder {

    private final DeterministicRng rng;

    public SwitchDispatchBuilder(DeterministicRng rng) {
        this.rng = rng;
    }

    public boolean rewriteAsDispatch(MethodNode method, BasicBlockSplitter.Blocks blocks) {
        // refuse anything that would need real CFG-aware reasoning. these are
        // not "todo, will improve later" gates, they are the contract under
        // which the rewrite is sound.
        if (method.tryCatchBlocks != null && !method.tryCatchBlocks.isEmpty()) {
            return false;
        }
        for (BasicBlockSplitter.Block b : blocks.blocks) {
            for (AbstractInsnNode ins : b.body) {
                int op = ins.getOpcode();
                if (op == Opcodes.JSR || op == Opcodes.RET) {
                    return false;
                }
                if (op == Opcodes.LLOAD || op == Opcodes.LSTORE
                    || op == Opcodes.DLOAD || op == Opcodes.DSTORE) {
                    // long and double locals occupy two slots. once blocks
                    // are reordered the verifier sees the second half slot
                    // as uninitialized on at least one path, which is the
                    // bug behind the bad_local_variable_type error. skip.
                    return false;
                }
                if (op == Opcodes.IINC) {
                    // IINC reads then writes a local that may not have been
                    // defined along the dispatched path. cheapest correct
                    // answer is to skip the method.
                    return false;
                }
            }
        }
        if (blocks.count() < 2) {
            return false;
        }

        int stateLocal = nextLocalSlot(method);
        DispatchPlan plan = new DispatchPlan(blocks.count(), rng);
        plan.assignStateIds();

        InsnList rebuilt = new InsnList();
        LabelNode dispatchHead = new LabelNode();

        // prologue, state = id(0). goto dispatch
        rebuilt.add(InsnFactory.pushInt(plan.stateIdAt(0)));
        rebuilt.add(new VarInsnNode(Opcodes.ISTORE, stateLocal));
        rebuilt.add(dispatchHead);

        // build the lookupswitch over shuffled state ids
        int[] keys = new int[blocks.count()];
        LabelNode[] caseLabels = new LabelNode[blocks.count()];
        Integer[] order = new Integer[blocks.count()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        // fisher yates over the block index so the case order is unrelated
        // to the original block layout.
        for (int i = order.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Integer tmp = order[i];
            order[i] = order[j];
            order[j] = tmp;
        }
        LabelNode[] blockEntryLabels = new LabelNode[blocks.count()];
        for (int i = 0; i < blocks.count(); i++) {
            blockEntryLabels[i] = new LabelNode();
        }
        for (int slot = 0; slot < blocks.count(); slot++) {
            int blockIndex = order[slot];
            keys[slot] = plan.stateIdAt(blockIndex);
            caseLabels[slot] = blockEntryLabels[blockIndex];
        }
        // lookupswitch demands sorted keys. sort key+label together.
        for (int i = 0; i < keys.length; i++) {
            for (int j = i + 1; j < keys.length; j++) {
                if (keys[j] < keys[i]) {
                    int tk = keys[i]; keys[i] = keys[j]; keys[j] = tk;
                    LabelNode tl = caseLabels[i]; caseLabels[i] = caseLabels[j]; caseLabels[j] = tl;
                }
            }
        }
        LabelNode defaultLabel = new LabelNode();
        rebuilt.add(new VarInsnNode(Opcodes.ILOAD, stateLocal));
        rebuilt.add(new LookupSwitchInsnNode(defaultLabel, keys, caseLabels));

        for (int blockIndex = 0; blockIndex < blocks.count(); blockIndex++) {
            BasicBlockSplitter.Block block = blocks.blocks.get(blockIndex);
            rebuilt.add(blockEntryLabels[blockIndex]);
            // emit the block body, but rewrite the trailing terminator.
            List<AbstractInsnNode> body = block.body;
            int terminatorAt = -1;
            for (int i = 0; i < body.size(); i++) {
                if (isTerminator(body.get(i))) {
                    terminatorAt = i;
                    break;
                }
            }
            int emitUpTo = terminatorAt < 0 ? body.size() : terminatorAt;
            for (int i = 0; i < emitUpTo; i++) {
                rebuilt.add(body.get(i));
            }
            if (terminatorAt < 0) {
                // straight line block, fall through to next block in original order.
                int fallthrough = blockIndex + 1;
                if (fallthrough < blocks.count()) {
                    rebuilt.add(InsnFactory.pushInt(plan.stateIdAt(fallthrough)));
                    rebuilt.add(new VarInsnNode(Opcodes.ISTORE, stateLocal));
                    rebuilt.add(new JumpInsnNode(Opcodes.GOTO, dispatchHead));
                } else {
                    // last block with no terminator. should not occur in valid
                    // bytecode but we add an ATHROW to keep the verifier happy.
                    rebuilt.add(new InsnNode(Opcodes.ACONST_NULL));
                    rebuilt.add(new InsnNode(Opcodes.ATHROW));
                }
                continue;
            }
            AbstractInsnNode terminator = body.get(terminatorAt);
            rewriteTerminator(terminator, blockIndex, blocks, plan, dispatchHead, stateLocal, rebuilt);
        }

        rebuilt.add(defaultLabel);
        // default branch should be unreachable. emit an ATHROW of null so the
        // class verifies cleanly without us pulling in an exception type.
        rebuilt.add(new InsnNode(Opcodes.ACONST_NULL));
        rebuilt.add(new InsnNode(Opcodes.ATHROW));

        method.instructions = rebuilt;
        if (method.maxLocals <= stateLocal) {
            method.maxLocals = stateLocal + 1;
        }
        method.maxStack = Math.max(method.maxStack, 4);
        return true;
    }

    private boolean isTerminator(AbstractInsnNode ins) {
        if (ins instanceof JumpInsnNode) {
            return true;
        }
        int op = ins.getOpcode();
        return op == Opcodes.IRETURN || op == Opcodes.LRETURN || op == Opcodes.FRETURN
            || op == Opcodes.DRETURN || op == Opcodes.ARETURN || op == Opcodes.RETURN
            || op == Opcodes.ATHROW;
    }

    private void rewriteTerminator(
        AbstractInsnNode terminator,
        int blockIndex,
        BasicBlockSplitter.Blocks blocks,
        DispatchPlan plan,
        LabelNode dispatchHead,
        int stateLocal,
        InsnList out
    ) {
        if (terminator instanceof JumpInsnNode jmp) {
            int op = jmp.getOpcode();
            LabelNode target = jmp.label;
            Integer targetBlock = blocks.labelToIndex.get(target);
            if (targetBlock == null) {
                // jump into the middle of a block we did not split on, or to a
                // label we erased. fall back, keep the original jump.
                out.add(terminator);
                return;
            }
            if (op == Opcodes.GOTO) {
                out.add(InsnFactory.pushInt(plan.stateIdAt(targetBlock)));
                out.add(new VarInsnNode(Opcodes.ISTORE, stateLocal));
                out.add(new JumpInsnNode(Opcodes.GOTO, dispatchHead));
                return;
            }
            // conditional jump. shape becomes,
            //   <cond inverted skip>
            //   state = id(target)
            //   goto dispatch
            //   skip,
            //   state = id(fallthrough)
            //   goto dispatch
            LabelNode skip = new LabelNode();
            int inverted = invertJump(op);
            out.add(new JumpInsnNode(inverted, skip));
            out.add(InsnFactory.pushInt(plan.stateIdAt(targetBlock)));
            out.add(new VarInsnNode(Opcodes.ISTORE, stateLocal));
            out.add(new JumpInsnNode(Opcodes.GOTO, dispatchHead));
            out.add(skip);
            int fallthrough = blockIndex + 1;
            if (fallthrough < blocks.count()) {
                out.add(InsnFactory.pushInt(plan.stateIdAt(fallthrough)));
                out.add(new VarInsnNode(Opcodes.ISTORE, stateLocal));
                out.add(new JumpInsnNode(Opcodes.GOTO, dispatchHead));
            } else {
                out.add(new InsnNode(Opcodes.ACONST_NULL));
                out.add(new InsnNode(Opcodes.ATHROW));
            }
            return;
        }
        // returns and throws are kept as-is. the dispatcher does not loop back.
        out.add(terminator);
    }

    private int invertJump(int op) {
        switch (op) {
            case Opcodes.IFEQ: return Opcodes.IFNE;
            case Opcodes.IFNE: return Opcodes.IFEQ;
            case Opcodes.IFLT: return Opcodes.IFGE;
            case Opcodes.IFGE: return Opcodes.IFLT;
            case Opcodes.IFGT: return Opcodes.IFLE;
            case Opcodes.IFLE: return Opcodes.IFGT;
            case Opcodes.IF_ICMPEQ: return Opcodes.IF_ICMPNE;
            case Opcodes.IF_ICMPNE: return Opcodes.IF_ICMPEQ;
            case Opcodes.IF_ICMPLT: return Opcodes.IF_ICMPGE;
            case Opcodes.IF_ICMPGE: return Opcodes.IF_ICMPLT;
            case Opcodes.IF_ICMPGT: return Opcodes.IF_ICMPLE;
            case Opcodes.IF_ICMPLE: return Opcodes.IF_ICMPGT;
            case Opcodes.IF_ACMPEQ: return Opcodes.IF_ACMPNE;
            case Opcodes.IF_ACMPNE: return Opcodes.IF_ACMPEQ;
            case Opcodes.IFNULL: return Opcodes.IFNONNULL;
            case Opcodes.IFNONNULL: return Opcodes.IFNULL;
            default:
                throw new IllegalStateException("unsupported jump opcode for inversion: " + op);
        }
    }

    private int nextLocalSlot(MethodNode method) {
        // arguments occupy slots from 0. static or instance does not matter
        // because maxLocals already accounts for them. we reserve one int slot
        // for the dispatch state.
        return method.maxLocals;
    }

    public static final class DispatchPlan {
        private final int blockCount;
        private final DeterministicRng rng;
        private final int[] stateIds;

        public DispatchPlan(int blockCount, DeterministicRng rng) {
            this.blockCount = blockCount;
            this.rng = rng;
            this.stateIds = new int[blockCount];
        }

        public void assignStateIds() {
            // ids must be unique. use a hash set to avoid collisions cheaply.
            java.util.HashSet<Integer> used = new java.util.HashSet<>();
            for (int i = 0; i < blockCount; i++) {
                int candidate;
                do {
                    candidate = rng.nextInt() & 0x7FFFFFFF;
                } while (!used.add(candidate));
                stateIds[i] = candidate;
            }
        }

        public int stateIdAt(int blockIndex) {
            return stateIds[blockIndex];
        }
    }
}
