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
import org.teavm.interop.Address;
import org.teavm.model.MethodReference;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.reflect.ClassInfo;

public class ClassInfoIntrinsic implements Intrinsic {
    @Override
    public boolean canHandle(MethodReference method) {
        return method.getClassName().equals(ClassInfo.class.getName());
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        switch (invocation.getMethod().getName()) {
            case "primitiveKind":
                context.includes().includePath("core.h");
                context.writer().print("teavm_primitiveKind((TeaVM_Class*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")");
                break;
            case "name":
            case "simpleName":
            case "enclosingClass":
            case "declaringClass":
            case "itemType":
            case "modifiers":
                context.includes().includePath("core.h");
                context.writer().print("(((TeaVM_Class*) (");
                context.emit(invocation.getArguments().get(0));
                context.writer().print("))->" + invocation.getMethod().getName() + ")");
                break;
            case "arrayType":
                context.includes().includePath("arrayclass.h");
                context.writer().print("teavm_getArrayClass(");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")");
                break;
            case "parent":
                context.writer().print("(((TeaVM_Class*) (");
                context.emit(invocation.getArguments().get(0));
                context.writer().print("))->superclass)");
                break;
            case "isSuperTypeOf":
                context.includes().includePath("core.h");
                context.includes().includePath("core.h");
                context.writer().print("teavm_isSupertypeOf((TeaVM_Class*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(", (TeaVM_Class*) ");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(")");
                break;
            case "newArrayInstance": {
                context.includes().includePath("arrayclass.h");
                var ref = new MethodReference(Allocator.class, "allocateArray", RuntimeClass.class, int.class,
                        Address.class);
                context.importMethod(ref, true);
                context.writer().print(context.names().forMethod(ref)).print("(teavm_getArrayClass(");
                context.emit(invocation.getArguments().get(0));
                context.writer().print("), ");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(")");
                break;
            }
            case "getItem": {
                context.includes().includePath("reflection.h");
                context.writer().print("teavm_reflection_getItem(");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(", ");
                context.emit(invocation.getArguments().get(2));
                context.writer().print(")");
                break;
            }
            case "putItem": {
                context.includes().includePath("reflection.h");
                context.writer().print("teavm_reflection_putItem(");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(", ");
                context.emit(invocation.getArguments().get(2));
                context.writer().print(", ");
                context.emit(invocation.getArguments().get(3));
                context.writer().print(")");
                break;
            }
            case "arrayLength":
                context.includes().includePath("core.h");
                context.writer().print("TEAVM_ARRAY_LENGTH(");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(")");
                break;
            case "classObject":
                context.includes().includePath("core.h");
                context.writer().print("teavm_getClassObject((TeaVM_Class*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")");
                break;
            case "enumConstantCount": {
                context.includes().includePath("core.h");
                context.writer().print("teavm_enumConstantCount((TeaVM_Class*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")");
                break;
            }
            case "enumConstant": {
                context.includes().includePath("core.h");
                context.writer().print("teavm_enumConstant((TeaVM_Class*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(", ");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(")");
                break;
            }
            case "initialize": {
                context.includes().includePath("core.h");
                context.writer().print("teavm_initializeClassDefault((TeaVM_Class*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")");
                break;
            }
            case "superinterfaceCount":
                context.writer().print("(((TeaVM_Class*) (");
                context.emit(invocation.getArguments().get(0));
                context.writer().print("))->superinterfaceCount)");
                break;
            case "superinterface":
                context.writer().print("((TeaVM_Class*) (");
                context.emit(invocation.getArguments().get(0));
                context.writer().print("))->superinterfaces[");
                context.emit(invocation.getArguments().get(1));
                context.writer().print("]");
                break;
            case "reflection":
                context.writer().print("((TeaVM_Class*) (");
                context.emit(invocation.getArguments().get(0));
                context.writer().print("))->reflection");
                break;
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }
}
