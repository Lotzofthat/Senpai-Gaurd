package senpai.transforms.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;

public final class BasicBlockSplitter {

    public Blocks split(MethodNode method) {
        // walk the instruction list. start a new block at each label, and end
        // a block after any control transfer (jump, switch, return, throw).
        // every block keeps its head label so other blocks can target it.
        List<Block> blocks = new ArrayList<>();
        Map<LabelNode, Integer> labelToBlock = new HashMap<>();
        AbstractInsnNode cursor = method.instructions.getFirst();
        LabelNode head = new LabelNode();
        Block current = new Block(head);
        boolean fresh = true;
        while (cursor != null) {
            if (cursor instanceof LabelNode lbl) {
                if (!fresh) {
                    blocks.add(current);
                    current = new Block(lbl);
                } else {
                    current = new Block(lbl);
                }
                labelToBlock.put(lbl, blocks.size());
                fresh = true;
                cursor = cursor.getNext();
                continue;
            }
            current.body.add(cursor);
            fresh = false;
            if (isTerminator(cursor)) {
                blocks.add(current);
                LabelNode synthetic = new LabelNode();
                current = new Block(synthetic);
                fresh = true;
            }
            cursor = cursor.getNext();
        }
        if (!current.body.isEmpty()) {
            blocks.add(current);
        }
        return new Blocks(blocks, labelToBlock);
    }

    private boolean isTerminator(AbstractInsnNode ins) {
        int op = ins.getOpcode();
        if (ins instanceof JumpInsnNode) {
            return true;
        }
        if (ins instanceof TableSwitchInsnNode || ins instanceof LookupSwitchInsnNode) {
            return true;
        }
        switch (op) {
            case org.objectweb.asm.Opcodes.IRETURN:
            case org.objectweb.asm.Opcodes.LRETURN:
            case org.objectweb.asm.Opcodes.FRETURN:
            case org.objectweb.asm.Opcodes.DRETURN:
            case org.objectweb.asm.Opcodes.ARETURN:
            case org.objectweb.asm.Opcodes.RETURN:
            case org.objectweb.asm.Opcodes.ATHROW:
                return true;
            default:
                return false;
        }
    }

    public static final class Block {
        public final LabelNode head;
        public final List<AbstractInsnNode> body = new ArrayList<>();

        public Block(LabelNode head) {
            this.head = head;
        }
    }

    public static final class Blocks {
        public final List<Block> blocks;
        public final Map<LabelNode, Integer> labelToIndex;

        public Blocks(List<Block> blocks, Map<LabelNode, Integer> labelToIndex) {
            this.blocks = blocks;
            this.labelToIndex = labelToIndex;
        }

        public int count() {
            return blocks.size();
        }

        public List<Block> raw() {
            return blocks;
        }
    }
}
