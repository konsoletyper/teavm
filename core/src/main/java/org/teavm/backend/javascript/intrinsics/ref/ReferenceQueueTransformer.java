/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.backend.javascript.intrinsics.ref;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.ValueType;

public class ReferenceQueueTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals(ReferenceQueue.class.getName())) {
            var field = new FieldHolder("inner");
            field.setType(ValueType.parse(Object.class));
            cls.addField(field);

            field = new FieldHolder("registry");
            field.setType(ValueType.parse(Object.class));
            cls.addField(field);

            var pollMethod = cls.getMethod(new MethodDescriptor("poll", Reference.class));
            pollMethod.setProgram(null);
            pollMethod.getModifiers().add(ElementModifier.NATIVE);

            var constructor = cls.getMethod(new MethodDescriptor("<init>", void.class));
            constructor.setProgram(null);
            constructor.getModifiers().add(ElementModifier.NATIVE);
        }
    }
}
