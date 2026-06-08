/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.backend.c.transform;

import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.Instruction;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.util.BasicBlockMapper;

public class SynchronizedMethodTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        for (var method : cls.getMethods()) {
            if (method.getProgram() != null && method.hasModifier(ElementModifier.SYNCHRONIZED)) {
                transformSynchronized(method.getProgram(), method.hasModifier(ElementModifier.STATIC), cls.getName());
            }
        }
    }

    private void transformSynchronized(Program program, boolean isStatic, String className) {
        var firstBlockReplacement = program.createBasicBlock();
        var firstBlock = program.basicBlockAt(0);
        while (firstBlock.getFirstInstruction() != null) {
            var insn = firstBlock.getFirstInstruction();
            insn.delete();
            firstBlockReplacement.add(insn);
        }

        var monitorEnter = new MonitorEnterInstruction();
        monitorEnter.setObjectRef(addMonitorInstruction(firstBlock, null, isStatic, className, null));
        firstBlock.add(monitorEnter);

        var jmp = new JumpInstruction();
        jmp.setTarget(firstBlockReplacement);
        firstBlock.add(jmp);

        var handler = program.createBasicBlock();
        handler.setExceptionVariable(program.createVariable());
        var monitorExit = new MonitorExitInstruction();
        monitorExit.setObjectRef(addMonitorInstruction(handler, null, isStatic, className, null));
        handler.add(monitorExit);

        var rethrow = new RaiseInstruction();
        rethrow.setException(handler.getExceptionVariable());
        handler.add(rethrow);

        for (var i = 1; i < program.basicBlockCount() - 1; ++i) {
            var block = program.basicBlockAt(i);
            var tryCatch = new TryCatchBlock();
            tryCatch.setExceptionType(null);
            tryCatch.setHandler(handler);
            block.getTryCatchBlocks().add(tryCatch);

            if (block.getLastInstruction() instanceof ExitInstruction) {
                monitorExit = new MonitorExitInstruction();
                monitorExit.setObjectRef(addMonitorInstruction(block, block.getLastInstruction(),
                        isStatic, className, block.getLastInstruction().getLocation()));
                monitorExit.setLocation(block.getLastInstruction().getLocation());
                block.getLastInstruction().insertPrevious(monitorExit);
            }
        }

        var mapper = new BasicBlockMapper((BasicBlock b) -> b == firstBlock ? firstBlockReplacement : b);
        mapper.transform(program);
    }

    private Variable addMonitorInstruction(BasicBlock block, Instruction before, boolean isStatic, String className,
            TextLocation location) {
        if (isStatic) {
            var v = block.getProgram().createVariable();
            var cst = new ClassConstantInstruction();
            cst.setConstant(ValueType.object(className));
            cst.setReceiver(v);
            cst.setLocation(location);
            if (before != null) {
                before.insertPrevious(cst);
            } else {
                block.add(cst);
            }
            return v;
        } else {
            return block.getProgram().variableAt(0);
        }
    }
}
