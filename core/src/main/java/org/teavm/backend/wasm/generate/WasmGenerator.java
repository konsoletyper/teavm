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
package org.teavm.backend.wasm.generate;

import org.teavm.ast.RegularMethodNode;
import org.teavm.ast.VariableNode;
import org.teavm.ast.decompilation.Decompiler;
import org.teavm.backend.wasm.binary.BinaryWriter;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmFloat32Constant;
import org.teavm.backend.wasm.model.expression.WasmFloat64Constant;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt64Constant;
import org.teavm.interop.Export;
import org.teavm.model.AnnotationReader;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class WasmGenerator {
    private Decompiler decompiler;
    private ClassHolderSource classSource;
    private WasmGenerationContext context;
    private WasmClassGenerator classGenerator;
    private BinaryWriter binaryWriter;

    public WasmGenerator(Decompiler decompiler, ClassHolderSource classSource,
            WasmGenerationContext context, WasmClassGenerator classGenerator, BinaryWriter binaryWriter) {
        this.decompiler = decompiler;
        this.classSource = classSource;
        this.context = context;
        this.classGenerator = classGenerator;
        this.binaryWriter = binaryWriter;
    }

    public WasmFunction generateDefinition(MethodReference methodReference) {
        ClassHolder cls = classSource.get(methodReference.getClassName());
        MethodHolder method = cls.getMethod(methodReference.getDescriptor());
        WasmFunction function = new WasmFunction(WasmMangling.mangleMethod(method.getReference()));

        if (!method.hasModifier(ElementModifier.STATIC)) {
            function.getParameters().add(WasmType.INT32);
        }
        for (int i = 0; i < method.parameterCount(); ++i) {
            function.getParameters().add(WasmGeneratorUtil.mapType(method.parameterType(i)));
        }
        if (method.getResultType() != ValueType.VOID) {
            function.setResult(WasmGeneratorUtil.mapType(method.getResultType()));
        }

        return function;
    }

    public WasmFunction generate(MethodReference methodReference, MethodHolder bodyMethod) {
        ClassHolder cls = classSource.get(methodReference.getClassName());
        MethodHolder method = cls.getMethod(methodReference.getDescriptor());

        RegularMethodNode methodAst = decompiler.decompileRegular(bodyMethod);
        WasmFunction function = context.getFunction(WasmMangling.mangleMethod(methodReference));
        int firstVariable = method.hasModifier(ElementModifier.STATIC) ? 1 : 0;
        for (int i = firstVariable; i < methodAst.getVariables().size(); ++i) {
            VariableNode variable = methodAst.getVariables().get(i);
            WasmType type = variable.getType() != null
                    ? WasmGeneratorUtil.mapType(variable.getType())
                    : WasmType.INT32;
            function.add(new WasmLocal(type, variable.getName()));
        }

        WasmGenerationVisitor visitor = new WasmGenerationVisitor(context, classGenerator, binaryWriter, function,
                firstVariable);
        methodAst.getBody().acceptVisitor(visitor);
        function.getBody().add(visitor.result);

        if (function.getResult() != null) {
            WasmExpression finalExpr;
            switch (function.getResult()) {
                case INT32:
                    finalExpr = new WasmInt32Constant(0);
                    break;
                case INT64:
                    finalExpr = new WasmInt64Constant(0);
                    break;
                case FLOAT32:
                    finalExpr = new WasmFloat32Constant(0);
                    break;
                case FLOAT64:
                    finalExpr = new WasmFloat64Constant(0);
                    break;
                default:
                    throw new AssertionError();
            }
            function.getBody().add(finalExpr);
        }

        AnnotationReader exportAnnot = method.getAnnotations().get(Export.class.getName());
        if (exportAnnot != null) {
            function.setExportName(exportAnnot.getValue("name").getString());
        }

        return function;
    }

    public WasmFunction generateNative(MethodReference methodReference) {
        WasmFunction function = context.getFunction(WasmMangling.mangleMethod(methodReference));

        WasmGenerationContext.ImportedMethod importedMethod = context.getImportedMethod(methodReference);
        if (importedMethod != null) {
            function.setImportName(importedMethod.name);
            function.setImportModule(importedMethod.module);
        } else {
            function.setImportName("<unknown>");
        }

        return function;
    }
}
