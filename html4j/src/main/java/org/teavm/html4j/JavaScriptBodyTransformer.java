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
package org.teavm.html4j;

import net.java.html.js.JavaScriptBody;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodHolder;
import org.teavm.model.ValueType;

public class JavaScriptBodyTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        for (MethodHolder method : cls.getMethods()) {
            if (method.getAnnotations().get(JavaScriptBody.class.getName()) != null) {
                AnnotationHolder genAnnot = new AnnotationHolder(GeneratedBy.class.getName());
                genAnnot.getValues().put("value", new AnnotationValue(ValueType.object(
                        JavaScriptBodyGenerator.class.getName())));
                method.getAnnotations().add(genAnnot);
                method.setProgram(null);
                method.getModifiers().add(ElementModifier.NATIVE);
            }
        }
    }
}
