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
package org.teavm.classlib.java.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
public class ClassLoaderTest {
    @Test
    @SkipPlatform({TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI})
    public void loadsResources() {
        assertEquals("q", loadResource("1"));
        assertEquals("qw", loadResource("2"));
        assertEquals("qwe", loadResource("3"));
        assertEquals("qwer", loadResource("4"));
        assertEquals("qwert", loadResource("5"));
        assertEquals("qwerty", loadResource("6"));
        assertEquals("qwertyu", loadResource("7"));
        assertEquals("qwertyui", loadResource("8"));
        assertEquals("qwertyuiopasdfghjklzxcvbnm", loadResource("9"));
    }

    @Test
    @SkipPlatform({TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI})
    public void returnsNullForNonExistentResource() {
        InputStream input = ClassLoader.getSystemClassLoader().getResourceAsStream("non-existent-resource.txt");
        assertNull(input);
    }

    private static String loadResource(String name) {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(classLoader.getResourceAsStream(
                "resources-for-test/" + name), "UTF-8"))) {
            return reader.readLine();
        } catch (IOException e) {
            return "";
        }
    }
}
