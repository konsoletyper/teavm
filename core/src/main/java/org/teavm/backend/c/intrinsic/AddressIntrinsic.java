/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.backend.c.intrinsic;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.c.generate.CodeGeneratorUtil;
import org.teavm.backend.c.util.ConstantUtil;
import org.teavm.interop.Address;
import org.teavm.model.ClassReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class AddressIntrinsic implements Intrinsic {
    @Override
    public boolean canHandle(MethodReference method) {
        if (!method.getClassName().equals(Address.class.getName())) {
            return false;
        }

        switch (method.getName()) {
            case "fromInt":
            case "fromLong":
            case "toInt":
            case "toLong":
            case "toStructure":
            case "ofObject":

            case "getByte":
            case "getShort":
            case "getChar":
            case "getInt":
            case "getLong":
            case "getFloat":
            case "getDouble":
            case "getAddress":

            case "putByte":
            case "putShort":
            case "putChar":
            case "putInt":
            case "putLong":
            case "putFloat":
            case "putDouble":
            case "putAddress":

            case "add":
            case "isLessThan":
            case "align":
            case "sizeOf":

            case "ofData":
                return true;
            default:
                return false;
        }
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        switch (invocation.getMethod().getName()) {
            case "fromInt":
            case "fromLong":
                context.writer().print("((void*) (intptr_t) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")");
                break;
            case "toInt":
                context.writer().print("((int32_t) (intptr_t) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")");
                break;
            case "toLong":
                context.writer().print("((int64_t) (intptr_t) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")");
                break;
            case "toStructure":
            case "ofObject":
                context.emit(invocation.getArguments().get(0));
                break;

            case "getByte":
                context.writer().print("((int32_t) *(int8_t*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")");
                break;
            case "getShort":
                context.writer().print("((int32_t) *(int16_t*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")");
                break;
            case "getChar":
                context.writer().print("((int32_t) *(char16_t*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")");
                break;
            case "getInt":
                getValue(context, invocation, "int32_t");
                break;
            case "getLong":
                getValue(context, invocation, "int64_t");
                break;
            case "getFloat":
                getValue(context, invocation, "float");
                break;
            case "getDouble":
                getValue(context, invocation, "double");
                break;
            case "getAddress":
                getValue(context, invocation, "void*");
                break;

            case "putByte":
                context.writer().print("(*(int8_t*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(" = (int8_t) ");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(")");
                break;
            case "putShort":
                context.writer().print("(*(int16_t*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(" = (int16_t) ");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(")");
                break;
            case "putChar":
                context.writer().print("(*(char16_t*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(" = (char16_t) ");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(")");
                break;
            case "putInt":
                putValue(context, invocation, "int32_t");
                break;
            case "putLong":
                putValue(context, invocation, "int64_t");
                break;
            case "putFloat":
                putValue(context, invocation, "float");
                break;
            case "putDouble":
                putValue(context, invocation, "double");
                break;
            case "putAddress":
                putValue(context, invocation, "void*");
                break;

            case "add":
                if (invocation.getArguments().size() == 2) {
                    context.writer().print("TEAVM_ADDRESS_ADD(");
                    context.emit(invocation.getArguments().get(0));
                    context.writer().print(", ");
                    context.emit(invocation.getArguments().get(1));
                    context.writer().print(")");
                } else {
                    context.writer().print("TEAVM_ADDRESS_ADD(");
                    context.emit(invocation.getArguments().get(0));
                    context.writer().print(", ");
                    String className = ConstantUtil.getClassLiteral(context, invocation,
                            invocation.getArguments().get(1));
                    context.emit(invocation.getArguments().get(2));

                    context.writer().print(" * sizeof(");

                    if (className != null) {
                        ClassReader cls = context.classes().get(className);
                        CodeGeneratorUtil.printClassReference(context.writer(), context.includes(),
                                context.names(), cls, className);
                    } else {
                        context.writer().print("**");
                    }

                    context.writer().print(")");
                    context.writer().print(")");
                }
                break;
            case "isLessThan":
                context.writer().print("((uintptr_t) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(" < (uintptr_t) ");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(")");
                break;
            case "align":
                context.writer().print("TEAVM_ALIGN(");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(", ");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(")");
                break;
            case "sizeOf":
                context.writer().print("sizeof(void*)");
                break;
            case "ofData": {
                ValueType.Array type = (ValueType.Array) invocation.getMethod().parameterType(0);
                context.writer().print("((char*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(" + sizeof(TeaVM_Array) + (intptr_t) TEAVM_ALIGN(NULL, "
                        + sizeOf(type.getItemType()) + "))");
                break;
            }
        }
    }

    private void getValue(IntrinsicContext context, InvocationExpr invocation, String type) {
        context.writer().print("(*(" + type + "*) ");
        context.emit(invocation.getArguments().get(0));
        context.writer().print(")");
    }

    private void putValue(IntrinsicContext context, InvocationExpr invocation, String type) {
        context.writer().print("(*(" + type + "*) ");
        context.emit(invocation.getArguments().get(0));
        context.writer().print(" = ");
        context.emit(invocation.getArguments().get(1));
        context.writer().print(")");
    }

    private int sizeOf(ValueType type) {
        switch (((ValueType.Primitive) type).getKind()) {
            case BYTE:
                return 1;
            case SHORT:
            case CHARACTER:
                return 2;
            case INTEGER:
            case FLOAT:
                return 4;
            case LONG:
            case DOUBLE:
                return 8;
            default:
                break;
        }
        return 0;
    }
}
