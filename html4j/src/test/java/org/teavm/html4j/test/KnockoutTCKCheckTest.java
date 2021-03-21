/*
 *  Copyright 2017 Jaroslav Tulach.
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
package org.teavm.html4j.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.netbeans.html.json.tck.KOTest;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 *
 * @author Jaroslav Tulach
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class KnockoutTCKCheckTest {
    @Test
    public void allKnockoutTestMethodsOverridden() {
        for (Class<?> c : KnockoutFXTest.allTestClasses()) {
            if (c.getName().contains("GC")) {
                continue;
            }
            for (Method m : c.getMethods()) {
                if (m.getAnnotation(KOTest.class) != null) {
                    Method override = null;
                    try {
                        override = KnockoutTCKTest.class.getMethod(m.getName());
                    } catch (NoSuchMethodException ex) {
                        fail("Cannot find " + m);
                    }
                    assertEquals("overriden: " + override, KnockoutTCKTest.class, override.getDeclaringClass());
                    assertNotNull("annotated: " + override, override.getAnnotation(Test.class));
                }
            }
        }
    }
}
