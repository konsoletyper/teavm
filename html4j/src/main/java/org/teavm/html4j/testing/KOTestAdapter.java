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
package org.teavm.html4j.testing;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import org.netbeans.html.json.tck.KOTest;
import org.teavm.model.MethodReader;
import org.teavm.testing.TestAdapter;
import org.teavm.testing.TestRunner;

/**
 *
 * @author Alexey Andreev
 */
public class KOTestAdapter implements TestAdapter {
    @Override
    public boolean acceptClass(Class<?> cls) {
        for (Method method : cls.getDeclaredMethods()) {
            for (Annotation annot : method.getAnnotations()) {
                if (annot.annotationType().getName().equals(KOTest.class.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean acceptMethod(MethodReader method) {
        return method.getAnnotations().get(KOTest.class.getName()) != null;
    }

    @Override
    public Iterable<String> getExpectedExceptions(MethodReader method) {
        return Collections.emptyList();
    }

    @Override
    public Class<? extends TestRunner> getRunner(MethodReader method) {
        return KOTestRunner.class;
    }
}
