/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.optimization;

import org.teavm.model.BasicBlock;
import org.teavm.model.MethodReader;
import org.teavm.model.Program;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.util.BasicBlockMapper;

/**
 *
 * @author Alexey Andreev
 */
public class EmptyBlockElimination implements MethodOptimization {
    @Override
    public void optimize(MethodReader method, final Program program) {
        final int[] blockMapping = new int[program.basicBlockCount()];
        for (int i = 0; i < blockMapping.length; ++i) {
            blockMapping[i] = i;
        }
        int lastNonEmpty = program.basicBlockCount() - 1;
        for (int i = program.basicBlockCount() - 2; i > 0; --i) {
            BasicBlock block = program.basicBlockAt(i);
            if (block.getPhis().isEmpty() && block.getInstructions().size() == 1 &&
                    block.getLastInstruction() instanceof JumpInstruction) {
                JumpInstruction insn = (JumpInstruction)block.getLastInstruction();
                if (insn.getTarget().getIndex() == i + 1) {
                    blockMapping[i] = lastNonEmpty;
                }
            }
            lastNonEmpty = blockMapping[i];
        }
        new BasicBlockMapper() {
            @Override protected BasicBlock map(BasicBlock block) {
                return program.basicBlockAt(blockMapping[block.getIndex()]);
            }
        }.transform(program);
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            if (blockMapping[i] != i) {
                program.deleteBasicBlock(i);
            }
        }
        program.pack();
    }
}
