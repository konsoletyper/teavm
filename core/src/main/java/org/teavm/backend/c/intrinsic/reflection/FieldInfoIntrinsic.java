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
package org.teavm.backend.c.intrinsic.reflection;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.c.intrinsic.Intrinsic;
import org.teavm.backend.c.intrinsic.IntrinsicContext;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.runtime.reflect.FieldInfo;

public class FieldInfoIntrinsic implements Intrinsic {
    @Override
    public boolean canHandle(MethodReference method) {
        return method.getClassName().equals(FieldInfo.class.getName());
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        switch (invocation.getMethod().getName()) {
            case "name":
            case "modifiers":
                context.includes().includePath("reflection.h");
                context.writer().print("((TeaVM_FieldInfo*) (");
                context.emit(invocation.getArguments().get(0));
                context.writer().print("))->").print(invocation.getMethod().getName());
                break;
            case "type":
                context.includes().includePath("reflection.h");
                context.writer().print("(&((TeaVM_FieldInfo*) (");
                context.emit(invocation.getArguments().get(0));
                context.writer().print("))->type)");
                break;
            case "read":
                generateReadField(context, invocation, "teavm_reflection_readField");
                break;
            case "readAsByte":
            case "readAsBoolean":
                generateReadField(context, invocation, "teavm_reflection_readFieldAsByte");
                break;
            case "readAsShort":
                generateReadField(context, invocation, "teavm_reflection_readFieldAsShort");
                break;
            case "readAsChar":
                generateReadField(context, invocation, "teavm_reflection_readFieldAsChar");
                break;
            case "readAsInt":
                generateReadField(context, invocation, "teavm_reflection_readFieldAsInt");
                break;
            case "readAsLong":
                generateReadField(context, invocation, "teavm_reflection_readFieldAsLong");
                break;
            case "readAsFloat":
                generateReadField(context, invocation, "teavm_reflection_readFieldAsFloat");
                break;
            case "readAsDouble":
                generateReadField(context, invocation, "teavm_reflection_readFieldAsDouble");
                break;
            case "write": {
                var type = invocation.getMethod().parameterType(1);
                if (type instanceof ValueType.Primitive primitiveType) {
                    switch (primitiveType.getKind()) {
                        case BOOLEAN, BYTE -> {
                            generateWriteField(context, invocation, "teavm_reflection_writeFieldAsByte");
                        }
                        case CHARACTER -> {
                            generateWriteField(context, invocation, "teavm_reflection_writeFieldAsChar");
                        }
                        case SHORT -> {
                            generateWriteField(context, invocation, "teavm_reflection_writeFieldAsChar");
                        }
                        case INTEGER -> {
                            generateWriteField(context, invocation, "teavm_reflection_writeFieldAsInt");
                        }
                        case LONG -> {
                            generateWriteField(context, invocation, "teavm_reflection_writeFieldAsLong");
                        }
                        case FLOAT -> {
                            generateWriteField(context, invocation, "teavm_reflection_writeFieldAsFloat");
                        }
                        case DOUBLE -> {
                            generateWriteField(context, invocation, "teavm_reflection_writeFieldAsDouble");
                        }
                    }
                } else {
                    generateWriteField(context, invocation, "teavm_reflection_writeField");
                }
                break;
            }
            case "reflection":
                context.includes().includePath("reflection.h");
                context.writer().print("((TeaVM_FieldInfo*) (");
                context.emit(invocation.getArguments().get(0));
                context.writer().print("))->reflection");
                break;
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }

    private void generateReadField(IntrinsicContext context, InvocationExpr invocation, String functionName) {
        context.includes().includePath("reflection.h");
        context.writer().print(functionName).print("(");
        context.emit(invocation.getArguments().get(1));
        context.writer().print(", (TeaVM_FieldInfo*)");
        context.emit(invocation.getArguments().get(0));
        context.writer().print(")");
    }

    private void generateWriteField(IntrinsicContext context, InvocationExpr invocation, String functionName) {
        context.includes().includePath("reflection.h");
        context.writer().print(functionName).print("(");
        context.emit(invocation.getArguments().get(1));
        context.writer().print(", (TeaVM_FieldInfo*) ");
        context.emit(invocation.getArguments().get(0));
        context.writer().print(", ");
        context.emit(invocation.getArguments().get(2));
        context.writer().print(")");
    }
}
