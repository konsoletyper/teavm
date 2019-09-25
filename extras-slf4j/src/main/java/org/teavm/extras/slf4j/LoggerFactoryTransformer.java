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
package org.teavm.extras.slf4j;

import org.slf4j.LoggerFactory;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ClassReader;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReader;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.util.ModelUtils;

public class LoggerFactoryTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (!cls.getName().equals(LoggerFactory.class.getName())) {
            return;
        }
        substitute(cls, context.getHierarchy());
    }

    private void substitute(ClassHolder cls, ClassHierarchy hierarchy) {
        ClassReader subst = hierarchy.getClassSource().get(TeaVMLoggerFactorySubstitution.class.getName());
        for (FieldHolder field : cls.getFields().toArray(new FieldHolder[0])) {
            cls.removeField(field);
        }
        for (MethodHolder method : cls.getMethods().toArray(new MethodHolder[0])) {
            cls.removeMethod(method);
        }
        for (FieldReader field : subst.getFields()) {
            cls.addField(ModelUtils.copyField(field));
        }
        for (MethodReader method : subst.getMethods()) {
            cls.addMethod(ModelUtils.copyMethod(method));
        }
    }
}
