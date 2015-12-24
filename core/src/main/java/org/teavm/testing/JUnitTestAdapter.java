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
package org.teavm.testing;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import org.junit.Test;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.MethodReader;
import org.teavm.model.ValueType;

/**
 *
 * @author Alexey Andreev
 */
public class JUnitTestAdapter implements TestAdapter {
    @Override
    public boolean acceptClass(Class<?> cls) {
        for (Method method : cls.getDeclaredMethods()) {
            for (Annotation annot : method.getAnnotations()) {
                if (annot.annotationType().getName().equals(Test.class.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean acceptMethod(MethodReader method) {
        return method.getAnnotations().get(Test.class.getName()) != null;
    }

    @Override
    public Iterable<String> getExpectedExceptions(MethodReader method) {
        AnnotationReader annot = method.getAnnotations().get(Test.class.getName());
        AnnotationValue expectedAnnot = annot.getValue("expected");
        if (expectedAnnot != null) {
            String className = ((ValueType.Object) expectedAnnot.getJavaClass()).getClassName();
            return Collections.singletonList(className);
        }
        return Collections.emptyList();
    }

    @Override
    public Class<? extends TestRunner> getRunner(MethodReader method) {
        return SimpleTestRunner.class;
    }
}
