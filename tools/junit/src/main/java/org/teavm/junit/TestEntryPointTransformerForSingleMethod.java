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

import org.teavm.model.ClassHierarchy;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.emit.ProgramEmitter;

class TestEntryPointTransformerForSingleMethod extends TestEntryPointTransformer {
    private MethodReference testMethod;

    TestEntryPointTransformerForSingleMethod(MethodReference testMethod, String testClassName) {
        super(testClassName);
        this.testMethod = testMethod;
    }

    @Override
    protected Program generateLaunchProgram(MethodHolder method, ClassHierarchy hierarchy) {
        ProgramEmitter pe = ProgramEmitter.create(method, hierarchy);
        generateSingleMethodLaunchProgram(testMethod, hierarchy, pe);
        return pe.getProgram();
    }
}
