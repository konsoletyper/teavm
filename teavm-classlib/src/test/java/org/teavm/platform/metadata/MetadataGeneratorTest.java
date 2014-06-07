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
package org.teavm.platform.metadata;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
public class MetadataGeneratorTest {
    @MetadataProvider(TestResourceGenerator.class)
    private native TestResource getNull();

    @Test
    public void nullExposed() {
        assertNull(getNull());
    }

    @MetadataProvider(TestResourceGenerator.class)
    private native int getInt();

    @Test
    public void intExposed() {
        assertEquals(23, getInt());
    }

    @MetadataProvider(TestResourceGenerator.class)
    private native TestResource getResource();

    @Test
    public void resourceObjectExposed() {
        TestResource res = getResource();
        assertEquals(23, res.getA());
        assertFalse(res.getB());
        assertEquals(24, res.getD());
        assertEquals(25, res.getE());
        assertEquals(3.14, res.getF(), 0.001);
        assertEquals(2.72, res.getG(), 0.001);
        assertEquals(Integer.valueOf(26), res.getH());
        assertNull(res.getI());
        assertEquals(Byte.valueOf((byte)27), res.getJ());
        assertEquals(Short.valueOf((short)28), res.getK());
    }
}
