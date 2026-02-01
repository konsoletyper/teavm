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
import org.teavm.runtime.reflect.AnnotationValueArray;

public class AnnotationValueArrayIntrinsic implements Intrinsic {
    @Override
    public boolean canHandle(MethodReference method) {
        return method.getClassName().equals(AnnotationValueArray.class.getName());
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        switch (invocation.getMethod().getName()) {
            case "size":
                context.includes().addInclude("<stdint.h>");
                context.writer().print("(*((int32_t*) (");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")))");
                break;
            case "getBoolean":
            case "getByte":
                writeArrayAccess(context, invocation, "TeaVM_ByteArray");
                break;
            case "getShort":
            case "getEnum":
                writeArrayAccess(context, invocation, "TeaVM_ShortArray");
                break;
            case "getChar":
                writeArrayAccess(context, invocation, "TeaVM_CharArray");
                break;
            case "getInt":
                writeArrayAccess(context, invocation, "TeaVM_IntArray");
                break;
            case "getLong":
                writeArrayAccess(context, invocation, "TeaVM_LongArray");
                break;
            case "getFloat":
                writeArrayAccess(context, invocation, "TeaVM_FloatArray");
                break;
            case "getDouble":
                writeArrayAccess(context, invocation, "TeaVM_DoubleArray");
                break;
            case "getString":
            case "getAnnotation":
                writeArrayAccess(context, invocation, "TeaVM_RefArray");
                break;
            case "getClass":
                context.writer().print("(&");
                writeArrayAccess(context, invocation, "TeaVM_ClassArray");
                context.writer().print(")");
                break;
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }

    private void writeArrayAccess(IntrinsicContext context, InvocationExpr invocation, String arrayType) {
        context.includes().includePath("reflection.h");
        context.writer().print("((" + arrayType + "*) (");
        context.emit(invocation.getArguments().get(0));
        context.writer().print("))->data[");
        context.emit(invocation.getArguments().get(1));
        context.writer().print("]");
    }
}
