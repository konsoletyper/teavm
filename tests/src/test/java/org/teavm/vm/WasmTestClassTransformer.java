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
package org.teavm.vm;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.RaiseInstruction;

public class WasmTestClassTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals(WasmTest.class.getName())) {
            var method = cls.getMethod(new MethodDescriptor("tryCatchWithReturnInside", int.class));
            method.getModifiers().remove(ElementModifier.NATIVE);
            method.setProgram(createTryCatchWithReturnInside());
        }
    }
    
    private Program createTryCatchWithReturnInside() {
        var program = new Program();
        program.createVariable();
        
        var mainBlock = program.createBasicBlock();
        var thenBlock = program.createBasicBlock();
        var elseBlock = program.createBasicBlock();
        var call = new InvokeInstruction();
        call.setType(InvocationType.SPECIAL);
        call.setMethod(new MethodReference(WasmTest.class, "foo", int.class));
        call.setReceiver(program.createVariable());
        mainBlock.add(call);
        
        var branch = new BranchingInstruction(BranchingCondition.EQUAL);
        branch.setOperand(call.getReceiver());
        branch.setConsequent(thenBlock);
        branch.setAlternative(elseBlock);
        mainBlock.add(branch);
        
        var exit = new ExitInstruction();
        exit.setValueToReturn(call.getReceiver());
        thenBlock.add(exit);

        exit = new ExitInstruction();
        exit.setValueToReturn(call.getReceiver());
        elseBlock.add(exit);
        
        var catchBlock = program.createBasicBlock();
        var exceptionConstructor = new ConstructInstruction();
        exceptionConstructor.setType("java.lang.RuntimeException");
        exceptionConstructor.setReceiver(program.createVariable());
        catchBlock.add(exceptionConstructor);
        
        var initException = new InvokeInstruction();
        initException.setType(InvocationType.SPECIAL);
        initException.setMethod(new MethodReference(RuntimeException.class, "<init>", void.class));
        initException.setInstance(exceptionConstructor.getReceiver());
        catchBlock.add(initException);
        
        var throwException = new RaiseInstruction();
        throwException.setException(exceptionConstructor.getReceiver());
        catchBlock.add(throwException);
        
        var tryCatch = new TryCatchBlock();
        tryCatch.setHandler(catchBlock);
        tryCatch.setExceptionType("java.lang.Throwable");
        mainBlock.getTryCatchBlocks().add(tryCatch);
        thenBlock.getTryCatchBlocks().add(tryCatch);
        elseBlock.getTryCatchBlocks().add(tryCatch);
        
        return program;
    }
}
