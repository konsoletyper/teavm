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
package org.teavm.backend.wasm.transformation.gc;

import org.teavm.backend.wasm.runtime.WasmGCSupport;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.emit.ProgramEmitter;

public class BaseClassesTransformation implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals("java.lang.Object")) {
            for (var method : cls.getMethods()) {
                switch (method.getName()) {
                    case "getClass":
                        method.setProgram(null);
                        method.getModifiers().add(ElementModifier.NATIVE);
                        break;
                    case "clone": {
                        var em = ProgramEmitter.create(method, context.getHierarchy());
                        em.invoke(WasmGCSupport.class, "cnse", CloneNotSupportedException.class).raise();
                        break;
                    }
                }
            }
        } else if (cls.getName().equals("java.lang.Class")) {
            for (var method : cls.getMethods()) {
                switch (method.getName()) {
                    case "getComponentType":
                    case "isInstance":
                        method.setProgram(null);
                        method.getModifiers().add(ElementModifier.NATIVE);
                        break;
                }
            }
        }
    }
}
