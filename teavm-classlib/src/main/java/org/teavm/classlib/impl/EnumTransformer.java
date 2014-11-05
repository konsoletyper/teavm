/*
 *  Copyright 2014 Alexey Andreev.
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

import org.teavm.javascript.ni.PreserveOriginalName;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class EnumTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        if (cls.getParent() != null && !cls.getParent().equals("java.lang.Enum")) {
            return;
        }
        MethodHolder method = cls.getMethod(new MethodDescriptor("values",
                ValueType.arrayOf(ValueType.object(cls.getName()))));
        if (method == null) {
            return;
        }
        method.getAnnotations().add(new AnnotationHolder(PreserveOriginalName.class.getName()));
    }
}
