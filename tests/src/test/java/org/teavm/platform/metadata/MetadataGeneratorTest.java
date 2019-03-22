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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
@Ignore
public class MetadataGeneratorTest {
    private native TestResource getNull();

    @Test
    public void nullExposed() {
        assertNull(getNull());
    }

    private native IntResource getInt();

    @Test
    public void intExposed() {
        assertEquals(23, getInt().getValue());
    }

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

        assertEquals("qwe", res.getFoo());

        assertEquals(2, res.getArrayA().size());
        assertEquals(2, res.getArrayA().get(0).getValue());
        assertEquals(3, res.getArrayA().get(1).getValue());
        assertEquals(1, res.getArrayB().size());
        assertEquals("baz", res.getArrayB().get(0).getBar());
        assertNull(res.getArrayC());
    }

    private native TestResource getEmptyResource();

    @Test
    public void resourceDefaultsSet() {
        TestResource res = getEmptyResource();
        assertEquals(0, res.getA());
        assertFalse(res.getB());
        assertEquals(0, res.getD());
        assertEquals(0, res.getE());
        assertEquals(0, res.getF(), 1E-10);
        assertEquals(0, res.getG(), 1E-10);
        assertNull(res.getFoo());
        assertNull(res.getArrayA());
        assertNull(res.getArrayB());
        assertNull(res.getArrayC());
        assertNull(res.getMapA());
        assertNull(res.getMapB());
        assertNull(res.getMapC());
    }

    @Test
    public void resourceModifiedInRunTime() {
        TestResource res = getEmptyResource();
        res.setA(23);
        res.setB(true);
        res.setD((byte) 24);
        res.setE((short) 25);
        res.setF(3.14f);
        res.setG(2.72);

        assertEquals(23, res.getA());
        assertTrue(res.getB());
        assertEquals(24, res.getD());
        assertEquals(25, res.getE());
        assertEquals(3.14, res.getF(), 0.001);
        assertEquals(2.72, res.getG(), 0.001);
    }
}

