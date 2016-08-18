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
import org.teavm.ast.VariableNode;
import org.teavm.ast.decompilation.Decompiler;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.wasm.model.WasmFunction;
import org.teavm.wasm.model.WasmLocal;

public class WasmGenerator {
    private Decompiler decompiler;
    private ClassHolderSource classSource;
    private WasmGenerationContext context;
    private WasmClassGenerator classGenerator;

    public WasmGenerator(Decompiler decompiler, ClassHolderSource classSource, WasmGenerationContext context,
            WasmClassGenerator classGenerator) {
        this.decompiler = decompiler;
        this.classSource = classSource;
        this.context = context;
        this.classGenerator = classGenerator;
    }

    public WasmFunction generate(MethodReference methodReference) {
        ClassHolder cls = classSource.get(methodReference.getClassName());
        MethodHolder method = cls.getMethod(methodReference.getDescriptor());

        RegularMethodNode methodAst = decompiler.decompileRegular(method);
        WasmFunction function = new WasmFunction(WasmMangling.mangleMethod(methodReference));
        int firstVariable = method.hasModifier(ElementModifier.STATIC) ? 1 : 0;
        for (int i = firstVariable; i < methodAst.getVariables().size(); ++i) {
            VariableNode variable = methodAst.getVariables().get(i);
            function.add(new WasmLocal(WasmGeneratorUtil.mapType(variable.getType())));
        }

        for (int i = firstVariable; i <= methodReference.parameterCount(); ++i) {
            function.getParameters().add(function.getLocalVariables().get(i - firstVariable).getType());
        }
        if (methodReference.getReturnType() != ValueType.VOID) {
            function.setResult(WasmGeneratorUtil.mapType(methodReference.getReturnType()));
        }

        WasmGenerationVisitor visitor = new WasmGenerationVisitor(context, classGenerator, function, firstVariable);
        methodAst.getBody().acceptVisitor(visitor);
        function.getBody().add(visitor.result);

        return function;
    }

    public WasmFunction generateNative(MethodReference methodReference) {
        WasmFunction function = new WasmFunction(WasmMangling.mangleMethod(methodReference));
        for (int i = 0; i < methodReference.parameterCount(); ++i) {
            function.getParameters().add(WasmGeneratorUtil.mapType(methodReference.parameterType(i)));
        }

        WasmGenerationContext.ImportedMethod importedMethod = context.getImportedMethod(methodReference);
        if (importedMethod != null) {
            function.setImportName(importedMethod.name);
            function.setImportModule(importedMethod.module);
        }

        return function;
    }
}
