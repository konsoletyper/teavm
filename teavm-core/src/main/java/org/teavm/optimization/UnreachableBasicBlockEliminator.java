package org.teavm.optimization;

import org.teavm.model.BasicBlock;
import org.teavm.model.Program;
import org.teavm.model.util.InstructionTransitionExtractor;

/**
 *
 * @author Alexey Andreev
 */
public class UnreachableBasicBlockEliminator {
    public void optimize(Program program) {
        if (program.basicBlockCount() == 0) {
            return;
        }
        InstructionTransitionExtractor transitionExtractor = new InstructionTransitionExtractor();
        boolean[] reachable = new boolean[program.basicBlockCount()];
        int[] stack = new int[program.basicBlockCount()];
        int top = 0;
        stack[top++] = 0;
        while (top > 0) {
            int i = stack[--top];
            if (reachable[i]) {
                continue;
            }
            reachable[i] = true;
            BasicBlock block = program.basicBlockAt(i);
            block.getLastInstruction().acceptVisitor(transitionExtractor);
            for (BasicBlock successor : transitionExtractor.getTargets()) {
                stack[top++] = successor.getIndex();
            }
        }
        for (int i = 0; i < reachable.length; ++i) {
            if (!reachable[i]) {
                program.deleteBasicBlock(i);
            }
        }
        program.pack();
    }
}
