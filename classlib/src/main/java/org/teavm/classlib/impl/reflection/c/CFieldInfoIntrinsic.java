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
package org.teavm.classlib.impl.reflection.c;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.c.intrinsic.Intrinsic;
import org.teavm.backend.c.intrinsic.IntrinsicContext;
import org.teavm.classlib.impl.reflection.FieldInfo;
import org.teavm.classlib.impl.reflection.FieldInfoList;
import org.teavm.classlib.impl.reflection.FieldReader;
import org.teavm.classlib.impl.reflection.FieldWriter;
import org.teavm.model.MethodReference;

public class CFieldInfoIntrinsic implements Intrinsic {
    @Override
    public boolean canHandle(MethodReference method) {
        return method.getClassName().equals(FieldInfo.class.getName())
                || method.getClassName().equals(FieldInfoList.class.getName())
                || method.getClassName().equals(FieldReader.class.getName())
                || method.getClassName().equals(FieldWriter.class.getName());
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        context.includes().includePath("reflection.h");
        switch (invocation.getMethod().getName()) {
            case "count":
                context.writer().print("((TeaVM_FieldInfoList*) (");
                context.emit(invocation.getArguments().get(0));
                context.writer().print("))->count");
                break;
            case "get":
                context.writer().print("(&(((TeaVM_FieldInfoList*) (");
                context.emit(invocation.getArguments().get(0));
                context.writer().print("))->data[");
                context.emit(invocation.getArguments().get(1));
                context.writer().print("]))");
                break;

            case "modifiers":
            case "accessLevel":
                context.writer().print("((TeaVM_FieldInfo*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")->").print(invocation.getMethod().getName());
                break;
            case "name":
                context.writer().print("(*((TeaVM_FieldInfo*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")->").print(invocation.getMethod().getName()).print(")");
                break;
            case "type":
                context.writer().print("teavm_reflection_extractType(&((TeaVM_FieldInfo*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")->type)");
                break;
            case "reader":
            case "writer":
                context.writer().print("(&((TeaVM_FieldInfo*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")->readerWriter)");
                break;
            case "genericType":
            case "annotations":
                context.writer().print("NULL");
                break;

            case "read":
                context.writer().print("teavm_reflection_readField(");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(", (TeaVM_FieldReaderWriter*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")");
                break;
            case "write":
                context.writer().print("teavm_reflection_writeField(");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(", (TeaVM_FieldReaderWriter*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(", ");
                context.emit(invocation.getArguments().get(2));
                context.writer().print(")");
                break;
        }
    }
}
