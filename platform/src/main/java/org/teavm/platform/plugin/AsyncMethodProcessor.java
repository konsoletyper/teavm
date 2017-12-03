/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.platform.plugin;

import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.Async;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.ValueType;
import org.teavm.platform.async.AsyncCallback;

public class AsyncMethodProcessor implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        for (MethodHolder method : cls.getMethods()) {
            if (method.hasModifier(ElementModifier.NATIVE)
                    && method.getAnnotations().get(Async.class.getName()) != null
                    && method.getAnnotations().get(GeneratedBy.class.getName()) == null) {
                ValueType[] signature = new ValueType[method.parameterCount() + 2];
                for (int i = 0; i < method.parameterCount(); ++i) {
                    signature[i] = method.parameterType(i);
                }
                signature[method.parameterCount()] = ValueType.parse(AsyncCallback.class);
                signature[method.parameterCount() + 1] = ValueType.VOID;
                MethodDescriptor asyncDesc = new MethodDescriptor(method.getName(), signature);
                MethodHolder asyncMethod = cls.getMethod(asyncDesc);
                if (asyncMethod != null) {
                    if (asyncMethod.hasModifier(ElementModifier.STATIC)
                            != method.hasModifier(ElementModifier.STATIC)) {
                        diagnostics.error(new CallLocation(method.getReference()), "Methods {{m0}} and {{m1}} must "
                                + "both be either static or non-static",
                                method.getReference(), asyncMethod.getReference());
                    }
                }
            }
        }
    }
}
