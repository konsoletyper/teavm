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

import java.util.Map;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.interop.Function;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.lowlevel.Characteristics;
import org.teavm.model.lowlevel.ExportedMethodKey;

public class FunctionIntrinsic implements Intrinsic {
    private Characteristics characteristics;
    private Map<? extends ExportedMethodKey, ? extends MethodReference> resolvedMethods;

    public FunctionIntrinsic(Characteristics characteristics,
            Map<? extends ExportedMethodKey, ? extends MethodReference> resolvedMethods) {
        this.characteristics = characteristics;
        this.resolvedMethods = resolvedMethods;
    }

    @Override
    public boolean canHandle(MethodReference method) {
        if (method.getClassName().equals(Function.class.getName()) && method.getName().equals("get")) {
            return true;
        }
        return characteristics.isFunction(method.getClassName());
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        MethodReference method = invocation.getMethod();
        if (method.getClassName().equals(Function.class.getName())) {
            generateGetFunction(context, invocation);
            return;
        }

        context.writer().print("(((").printType(method.getReturnType()).print(" (*)(");
        if (method.parameterCount() > 0) {
            context.writer().printType(method.parameterType(0));
            for (int i = 1; i < method.parameterCount(); ++i) {
                context.writer().print(", ").printType(method.parameterType(i));
            }
        }
        context.writer().print(")) ");

        context.emit(invocation.getArguments().get(0));
        context.writer().print(")(");
        if (invocation.getArguments().size() > 1) {
            context.emit(invocation.getArguments().get(1));
            for (int i = 2; i < invocation.getArguments().size(); ++i) {
                context.writer().print(", ");
                context.emit(invocation.getArguments().get(i));
            }
        }
        context.writer().print("))");
    }

    private void generateGetFunction(IntrinsicContext context, InvocationExpr invocation) {
        if (!(invocation.getArguments().get(0) instanceof ConstantExpr)
                || !(invocation.getArguments().get(1) instanceof ConstantExpr)
                || !(invocation.getArguments().get(2) instanceof ConstantExpr)) {
            return;
        }

        Object functionClassValue = ((ConstantExpr) invocation.getArguments().get(0)).getValue();
        Object classValue = ((ConstantExpr) invocation.getArguments().get(1)).getValue();
        Object methodValue = ((ConstantExpr) invocation.getArguments().get(2)).getValue();
        if (!(functionClassValue instanceof ValueType.Object)
                || !(classValue instanceof ValueType.Object)
                || !(methodValue instanceof String)) {
            return;
        }

        String functionClassName = ((ValueType.Object) functionClassValue).getClassName();
        String className = ((ValueType.Object) classValue).getClassName();
        String methodName = (String) methodValue;
        ExportedMethodKey key = new ExportedMethodKey(functionClassName, className, methodName);
        MethodReference method = resolvedMethods.get(key);
        if (method == null) {
            return;
        }

        context.writer().print("&").print(context.names().forMethod(method));
    }
}
