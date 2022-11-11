/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.wasm.transformation;

import org.teavm.backend.wasm.runtime.WasiSupport;
import org.teavm.backend.wasm.runtime.WasmSupport;
import org.teavm.interop.Import;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;

public class WasiSupportClassTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals(WasmSupport.class.getName())) {
            transformWasm(cls, context.getHierarchy());
        }
    }

    private void transformWasm(ClassHolder cls, ClassHierarchy classHierarchy) {
        ClassReader sourceCls = classHierarchy.getClassSource().get(WasiSupport.class.getName());
        for (MethodHolder method : cls.getMethods()) {
            MethodReader sourceMethod = sourceCls.getMethod(method.getDescriptor());
            if (sourceMethod != null) {
                if (method.hasModifier(ElementModifier.NATIVE)) {
                    method.getModifiers().remove(ElementModifier.NATIVE);
                }
                method.getAnnotations().remove(Import.class.getName());
                ProgramEmitter pe = ProgramEmitter.create(method, classHierarchy);
                ValueEmitter[] args = new ValueEmitter[method.parameterCount()];
                for (int i = 0; i < args.length; ++i) {
                    args[i] = pe.var(i + 1, method.parameterType(i));
                }
                ValueEmitter result = pe.invoke(sourceMethod.getReference(), args);
                if (method.getResultType() != ValueType.VOID) {
                    result.returnValue();
                } else {
                    pe.exit();
                }
            }
        }
    }
}
