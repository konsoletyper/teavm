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
package org.teavm.wasm.generate;

import org.teavm.ast.RegularMethodNode;
import org.teavm.ast.decompilation.Decompiler;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.util.TypeInferer;
import org.teavm.model.util.VariableType;
import org.teavm.wasm.model.WasmFunction;
import org.teavm.wasm.model.WasmLocal;

public class WasmGenerator {
    private Decompiler decompiler;
    private ClassHolderSource classSource;

    public WasmGenerator(Decompiler decompiler, ClassHolderSource classSource) {
        this.decompiler = decompiler;
        this.classSource = classSource;
    }

    public WasmFunction generate(MethodReference methodReference) {
        ClassHolder cls = classSource.get(methodReference.getClassName());
        MethodHolder method = cls.getMethod(methodReference.getDescriptor());
        Program program = method.getProgram();

        RegularMethodNode methodAst = decompiler.decompileRegular(method);
        TypeInferer inferer = new TypeInferer();
        inferer.inferTypes(program, methodReference);

        WasmFunction function = new WasmFunction(WasmMangling.mangleMethod(methodReference));
        int firstVariable = method.hasModifier(ElementModifier.STATIC) ? 1 : 0;
        for (int i = firstVariable; i < methodAst.getVariables().size(); ++i) {
            int varIndex = methodAst.getVariables().get(i);
            VariableType type = inferer.typeOf(varIndex);
            function.add(new WasmLocal(WasmGeneratorUtil.mapType(type)));
        }

        for (int i = firstVariable; i <= methodReference.parameterCount(); ++i) {
            function.getParameters().add(function.getLocalVariables().get(i - firstVariable).getType());
        }
        if (methodReference.getReturnType() != ValueType.VOID) {
            function.setResult(WasmGeneratorUtil.mapType(methodReference.getReturnType()));
        }

        WasmGenerationVisitor visitor = new WasmGenerationVisitor(function, firstVariable);
        methodAst.getBody().acceptVisitor(visitor);
        function.getBody().add(visitor.result);

        return function;
    }
}
