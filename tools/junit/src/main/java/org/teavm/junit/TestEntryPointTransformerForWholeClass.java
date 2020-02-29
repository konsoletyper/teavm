/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.junit;

import java.util.List;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.emit.ForkEmitter;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.model.instructions.BranchingCondition;

class TestEntryPointTransformerForWholeClass extends TestEntryPointTransformer {
    private List<MethodReference> testMethods;

    TestEntryPointTransformerForWholeClass(List<MethodReference> testMethods, String testClassName) {
        super(testClassName);
        this.testMethods = testMethods;
    }

    @Override
    protected Program generateLaunchProgram(MethodHolder method, ClassHierarchy hierarchy) {
        ProgramEmitter pe = ProgramEmitter.create(method, hierarchy);
        ValueEmitter testName = pe.var(1, String.class);

        for (MethodReference testMethod : testMethods) {
            ValueEmitter isTest = testName.invokeSpecial("equals", boolean.class,
                    pe.constant(testMethod.toString()).cast(Object.class));
            ForkEmitter fork = isTest.fork(BranchingCondition.NOT_EQUAL);
            pe.enter(pe.getProgram().createBasicBlock());
            fork.setThen(pe.getBlock());

            generateSingleMethodLaunchProgram(testMethod, hierarchy, pe);
            pe.enter(pe.getProgram().createBasicBlock());
            fork.setElse(pe.getBlock());
        }

        pe.construct(IllegalArgumentException.class, pe.constant("Invalid test name")).raise();

        return pe.getProgram();
    }
}
