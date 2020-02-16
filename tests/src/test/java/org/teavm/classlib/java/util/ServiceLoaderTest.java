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
package org.teavm.classlib.java.util;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class ServiceLoaderTest {
    @Test
    public void loadsService() {
        TestService instance = ServiceLoader.load(TestService.class).iterator().next();
        instance.foo();
        assertEquals(TestServiceImpl.class, instance.getClass());
        assertEquals(1, ((TestServiceImpl) instance).getCounter());
    }

    @Test
    public void loadsMultipleServices() {
        List<String> strings = new ArrayList<>();
        for (MultipleTestService instance : ServiceLoader.load(MultipleTestService.class)) {
            strings.add(instance.foo());
        }
        strings.sort(String::compareTo);
        assertEquals(Arrays.asList("A", "B"), strings);
    }
}
