/*
 *  Copyright 2024 Alexey Andreev.
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

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.runtime.ExceptionHandling;

public class WasmExceptionHandlingTransform implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals(ExceptionHandling.class.getName())) {
            processClass(cls, context);
        }
    }

    private void processClass(ClassHolder cls, ClassHolderTransformerContext context) {
        var method = cls.getMethod(new MethodDescriptor("throwException", Throwable.class, void.class));
        var pm = ProgramEmitter.create(method, context.getHierarchy());
        pm.var(1, Throwable.class).raise();
    }
}
