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
package org.teavm.classlib.impl.string;

import org.teavm.interop.NoSideEffects;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.ValueType;

public class JSStringTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals("java.lang.String")) {
            var charactersField = new FieldHolder("nativeString");
            charactersField.setType(ValueType.object("java.lang.Object"));
            charactersField.setLevel(AccessLevel.PRIVATE);
            cls.addField(charactersField);

            var method = cls.getMethod(new MethodDescriptor("<init>", Object.class, void.class));
            method.setProgram(null);
            method.getModifiers().add(ElementModifier.NATIVE);
            method.getAnnotations().add(new AnnotationHolder(NoSideEffects.class.getName()));
        }
    }
}
