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
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.*;
import org.teavm.model.util.ModelUtils;

/**
 *
 * @author Alexey Andreev
 */
public class LoggerFactoryTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        if (!cls.getName().equals(LoggerFactory.class.getName())) {
            return;
        }
        substitute(cls, innerSource);
    }

    private void substitute(ClassHolder cls, ClassReaderSource classSource) {
        ClassReader subst = classSource.get(TeaVMLoggerFactorySubstitution.class.getName());
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
