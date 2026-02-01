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

import java.util.Objects;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.c.intrinsic.Intrinsic;
import org.teavm.backend.c.intrinsic.IntrinsicContext;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.reflection.AnnotationGenerationHelper;
import org.teavm.runtime.reflect.AnnotationData;
import org.teavm.runtime.reflect.DerivedClassInfo;

public class AnnotationDataIntrinsic implements Intrinsic {
    private ClassReaderSource classes;

    public AnnotationDataIntrinsic(ClassReaderSource classes) {
        this.classes = classes;
    }

    @Override
    public boolean canHandle(MethodReference method) {
        if (!method.getClassName().endsWith(AnnotationGenerationHelper.ANNOTATION_DATA_SUFFIX)) {
            return false;
        }
        var cls = classes.get(method.getClassName());
        if (cls == null) {
            return false;
        }
        return Objects.equals(cls.getParent(), AnnotationData.class.getName());
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        if (invocation.getMethod().getName().equals("constructor") && invocation.getArguments().isEmpty()) {
            var lengthWithoutSuffix = invocation.getMethod().getClassName().length()
                    - AnnotationGenerationHelper.ANNOTATION_DATA_SUFFIX.length();
            var baseName = invocation.getMethod().getClassName().substring(0, lengthWithoutSuffix);
            var implName = baseName + AnnotationGenerationHelper.ANNOTATION_IMPLEMENTOR_SUFFIX;
            var ctor = new MethodReference(implName, "create", ValueType.object(
                    invocation.getMethod().getClassName()), ValueType.object(implName));
            context.writer().print(context.names().forMethod(ctor));
        } else {
            context.includes().includeClass(invocation.getMethod().getClassName());
            var needRef = false;
            if (invocation.getMethod().getReturnType() instanceof ValueType.Object) {
                var className = ((ValueType.Object) invocation.getMethod().getReturnType()).getClassName();
                if (className.equals(DerivedClassInfo.class.getName())) {
                    needRef = true;
                }
            }
            if (needRef) {
                context.writer().print("(&");
            }
            context.writer().print("((").print(context.names().forClass(invocation.getMethod().getClassName()))
                    .print("*) (");
            context.emit(invocation.getArguments().get(0));
            context.writer().print("))->");
            var fieldName = context.names().forMemberField(new FieldReference(invocation.getMethod().getClassName(),
                    invocation.getMethod().getName()));
            context.writer().print(fieldName);
            if (needRef) {
                context.writer().print(")");
            }
        }
    }
}
