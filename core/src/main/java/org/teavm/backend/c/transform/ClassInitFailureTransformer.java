/*
 *  Copyright 2026 Ethan Resnick.
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
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.util.BasicBlockMapper;
import org.teavm.runtime.ExceptionHandling;

// When a class initializer throws, the class must not stay initialized (JLS 12.4.2). The body of <clinit>
// runs in a try. The handler marks the class as failed, so later initialization throws NoClassDefFoundError,
// and rethrows the exception, wrapped in ExceptionInInitializerError unless it is an Error.
public class ClassInitFailureTransformer {
    public static final MethodReference MARK_FAILED = new MethodReference(ExceptionHandling.class,
            "markInitializerFailed", void.class);
    public static final MethodReference INITIALIZER_ERROR = new MethodReference(ExceptionHandling.class,
            "initializerError", Throwable.class, Throwable.class);

    public void apply(Program program) {
        var firstBlockReplacement = program.createBasicBlock();
        var firstBlock = program.basicBlockAt(0);
        while (firstBlock.getFirstInstruction() != null) {
            var insn = firstBlock.getFirstInstruction();
            insn.delete();
            firstBlockReplacement.add(insn);
        }
        var jmp = new JumpInstruction();
        jmp.setTarget(firstBlockReplacement);
        firstBlock.add(jmp);

        var handler = program.createBasicBlock();
        handler.setExceptionVariable(program.createVariable());
        var markFailed = new InvokeInstruction();
        markFailed.setType(InvocationType.SPECIAL);
        markFailed.setMethod(MARK_FAILED);
        handler.add(markFailed);
        var error = new InvokeInstruction();
        error.setType(InvocationType.SPECIAL);
        error.setMethod(INITIALIZER_ERROR);
        error.setArguments(handler.getExceptionVariable());
        error.setReceiver(program.createVariable());
        handler.add(error);
        var raise = new RaiseInstruction();
        raise.setException(error.getReceiver());
        handler.add(raise);

        for (var i = 1; i < program.basicBlockCount() - 1; ++i) {
            var tryCatch = new TryCatchBlock();
            tryCatch.setHandler(handler);
            program.basicBlockAt(i).getTryCatchBlocks().add(tryCatch);
        }

        var mapper = new BasicBlockMapper((BasicBlock b) -> b == firstBlock ? firstBlockReplacement : b);
        mapper.transform(program);
    }
}
