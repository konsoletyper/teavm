/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.classlib.impl;

import java.nio.Buffer;
import org.teavm.classlib.impl.nio.Buffers;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;

public class BuffersTransformer implements ClassHolderTransformer {
    private static final String NATIVE_BUFFER = "java.nio.NativeBuffer";

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals(Buffers.class.getName())) {
            var method = cls.getMethod(new MethodDescriptor("releaseNative", Buffer.class, void.class));
            method.getModifiers().remove(ElementModifier.NATIVE);
            var pe = ProgramEmitter.create(method, context.getHierarchy());
            var bufferVar = pe.var(1, Buffer.class);
            pe.when(bufferVar.instanceOf(ValueType.object(NATIVE_BUFFER)).isTrue()).thenDo(() -> {
                bufferVar.cast(ValueType.object(NATIVE_BUFFER)).invokeVirtual("release");
            });
            pe.exit();
        }
    }
}
