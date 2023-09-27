/*
 *  Copyright 2023 ihromant.
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
package org.teavm.classlib.impl;

import org.teavm.dependency.BootstrapMethodSubstitutor;
import org.teavm.dependency.DynamicCallSite;
import org.teavm.model.BasicBlock;
import org.teavm.model.ValueType;
import org.teavm.model.emit.PhiEmitter;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;

public class PatternMatchingSubstitutor implements BootstrapMethodSubstitutor {
    @Override
    public ValueEmitter substitute(DynamicCallSite callSite, ProgramEmitter pe) {
        System.out.println(callSite.getBootstrapArguments().size());
        System.out.println(callSite.getBootstrapArguments().get(0).getValueType());
        System.out.println(callSite.getBootstrapArguments().get(5).getValueType());
        ValueEmitter target = callSite.getArguments().get(0);
        ValueEmitter restartIdx = callSite.getArguments().get(1);
        BasicBlock joint = pe.prepareBlock();
        PhiEmitter result = pe.phi(ValueType.INTEGER, joint);
        pe.when(() -> target.isNull()).thenDo(() -> {
            pe.constant(-1).propagateTo(result);
            pe.jump(joint);
        });
        pe.constant(10).propagateTo(result);
        pe.jump(joint);
        pe.enter(joint);
        return result.getValue();
    }
}
