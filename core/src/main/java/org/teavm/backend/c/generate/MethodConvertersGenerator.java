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
package org.teavm.backend.c.generate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.teavm.model.ValueType;
import org.teavm.vm.BuildTarget;

public class MethodConvertersGenerator {
    private boolean incremental;
    private Map<Signature, String> signatureCache = new HashMap<>();
    private int lastSignatureId;
    private BufferedCodeWriter singleWriter;
    private BufferedCodeWriter headerWriter;
    private CodeWriter writer;
    private IncludeManager includes;
    private IncludeManager classLocalIncludes;

    public MethodConvertersGenerator(boolean incremental, FileNameProvider fileNames) {
        this.incremental = incremental;
        singleWriter = new BufferedCodeWriter(false);
        if (!incremental) {
            headerWriter = new BufferedCodeWriter(false);
            headerWriter.println("#pragma once");
            writer = singleWriter;
            includes = new SimpleIncludeManager(fileNames, writer);
            includes.init("reflection_signatures.c");
        }
    }

    public void startForClass(CodeWriter writer, IncludeManager includes) {
        if (incremental) {
            this.writer = writer.fragment();
            this.includes = includes;
        }
        classLocalIncludes = includes;
    }

    public String getSignatureFunction(boolean hasReceiver, ValueType returnType, ValueType[] parameterTypes) {
        if (!incremental) {
            classLocalIncludes.includePath("reflection_signatures.h");
        }
        return signatureCache.computeIfAbsent(new Signature(hasReceiver, returnType, parameterTypes),
                this::createSignatureFunction);
    }

    private String createSignatureFunction(Signature signature) {
        var name = "teavm_reflect_methodConverter_" + lastSignatureId++;
        if (incremental) {
            writer.print("static ");
        }
        writer.println("void* " + name + "(void* fn, TeaVM_Object* receiver, TeaVM_Object** args) {").indent();

        writeFunctionType(signature, "actualFn");
        writer.print(" = (");
        writeFunctionType(signature, "");
        writer.println(") fn;");
        if (signature.returnType != ValueType.VOID) {
            writer.printType(signature.returnType).print(" result = ");
        }
        writer.print("(*actualFn)(");
        var commaNeeded = false;
        if (signature.hasReceiver()) {
            writer.print("receiver");
            commaNeeded = true;
        }
        for (int i = 0; i < signature.parameterTypes.length; ++i) {
            if (commaNeeded) {
                writer.print(", ");
            }
            commaNeeded = true;
            convertArg("args[" + i + "]", signature.parameterTypes[i]);
        }
        writer.println(");");
        if (signature.returnType != ValueType.VOID) {
            writer.print("return ");
            convertReturn(signature.returnType);
            writer.println(";");
        } else {
            writer.println("return NULL;");
        }
        writer.outdent().println("}");

        if (!incremental) {
            headerWriter.println("extern void* " + name + "(void*, TeaVM_Object*, TeaVM_Object**);");
        }
        return name;
    }

    private void writeFunctionType(Signature signature, String name) {
        writer.printType(signature.returnType).print(" (*" + name + ")(");
        var commaNeeded = false;
        if (signature.hasReceiver()) {
            writer.print("void*");
            commaNeeded = true;
        }
        for (var paramType : signature.parameterTypes) {
            if (commaNeeded) {
                writer.print(", ");
            }
            commaNeeded = true;
            writer.printType(paramType);
        }
        writer.print(")");
    }

    private void convertArg(String arg, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    includes.includePath("reflection_gen.h");
                    writer.print("TEAVM_REFLECTION_UNBOX_BOOLEAN(" + arg + ")");
                    return;
                case BYTE:
                    includes.includePath("reflection_gen.h");
                    writer.print("TEAVM_REFLECTION_UNBOX_BYTE(" + arg + ")");
                    return;
                case SHORT:
                    includes.includePath("reflection_gen.h");
                    writer.print("TEAVM_REFLECTION_UNBOX_SHORT(" + arg + ")");
                    return;
                case CHARACTER:
                    includes.includePath("reflection_gen.h");
                    writer.print("TEAVM_REFLECTION_UNBOX_CHAR(" + arg + ")");
                    return;
                case INTEGER:
                    includes.includePath("reflection_gen.h");
                    writer.print("TEAVM_REFLECTION_UNBOX_INT(" + arg + ")");
                    return;
                case LONG:
                    includes.includePath("reflection_gen.h");
                    writer.print("TEAVM_REFLECTION_UNBOX_LONG(" + arg + ")");
                    return;
                case FLOAT:
                    includes.includePath("reflection_gen.h");
                    writer.print("TEAVM_REFLECTION_UNBOX_FLOAT(" + arg + ")");
                    return;
                case DOUBLE:
                    includes.includePath("reflection_gen.h");
                    writer.print("TEAVM_REFLECTION_UNBOX_DOUBLE(" + arg + ")");
                    return;
            }
        }
        writer.print(arg);
    }

    private void convertReturn(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    includes.includePath("reflection_gen.h");
                    writer.print("TEAVM_REFLECTION_BOX_BOOLEAN(result)");
                    return;
                case BYTE:
                    includes.includePath("reflection_gen.h");
                    writer.print("TEAVM_REFLECTION_BOX_BYTE(result)");
                    return;
                case SHORT:
                    includes.includePath("reflection_gen.h");
                    writer.print("TEAVM_REFLECTION_BOX_SHORT(result)");
                    return;
                case CHARACTER:
                    includes.includePath("reflection_gen.h");
                    writer.print("TEAVM_REFLECTION_BOX_CHAR(result)");
                    return;
                case INTEGER:
                    includes.includePath("reflection_gen.h");
                    writer.print("TEAVM_REFLECTION_BOX_INT(result)");
                    return;
                case LONG:
                    includes.includePath("reflection_gen.h");
                    writer.print("TEAVM_REFLECTION_BOX_LONG(result)");
                    return;
                case FLOAT:
                    includes.includePath("reflection_gen.h");
                    writer.print("TEAVM_REFLECTION_BOX_FLOAT(result)");
                    return;
                case DOUBLE:
                    includes.includePath("reflection_gen.h");
                    writer.print("TEAVM_REFLECTION_BOX_DOUBLE(result)");
                    return;
            }
        }
        writer.print("result");
    }

    public void endForClass() {
        if (incremental) {
            writer.flush();
            includes = null;
            writer = null;
        }
        classLocalIncludes = null;
    }

    public void endForBuild(BuildTarget buildTarget) throws IOException {
        singleWriter.flush();
        OutputFileUtil.write(singleWriter, "reflection_signatures.c", buildTarget);
        if (headerWriter != null) {
            OutputFileUtil.write(headerWriter, "reflection_signatures.h", buildTarget);
        }
    }

    private record Signature(
            boolean hasReceiver,
            ValueType returnType,
            ValueType[] parameterTypes
    ) {
    }
}
