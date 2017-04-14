/*
 * Copyright 2017 Jaroslav Tulach.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teavm.html4j.test;

import org.teavm.html4j.test.JavaScriptTCKTest;
import java.lang.reflect.Method;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.netbeans.html.json.tck.JavaScriptTCK;
import org.netbeans.html.json.tck.KOTest;

/**
 *
 * @author Jaroslav Tulach
 */
public class JavaScriptTCKCheckTest extends JavaScriptTCK {
    @Test
    public void allJavaScriptBodyTestMethodsOverriden() throws Exception {
        for (Class<?> c : testClasses()) {
            if (c.getName().contains("GC")) {
                continue;
            }
            for (Method m : c.getMethods()) {
                if (m.getAnnotation(KOTest.class) != null) {
                    Method override = JavaScriptTCKTest.class.getMethod(m.getName());
                    assertEquals("overriden: " + override, JavaScriptTCKTest.class, override.getDeclaringClass());
                    assertNotNull("annotated: " + override, override.getAnnotation(Test.class));
                }
            }
        }
    }
}
