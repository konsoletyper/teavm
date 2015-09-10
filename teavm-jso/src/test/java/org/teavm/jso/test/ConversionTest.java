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
package org.teavm.jso.test;

import static org.junit.Assert.*;
import org.junit.Test;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
public class ConversionTest {
    @Test
    public void convertsPrimitivesToJavaScript() {
        assertEquals("true:2:3:64:4:5.5:6.5:foo", combinePrimitives(true, (byte) 2, (short) 3,
                '@', 4, 5.5F, 6.5, "foo"));
    }

    @Test
    public void convertsPrimitivesToJava() {
        Primitives map = getPrimitives();
        assertTrue(map.getA());
        assertEquals(2, map.getB());
        assertEquals(3, map.getC());
        assertEquals('@', map.getD());
        assertEquals(4, map.getE());
        assertEquals(5.5, map.getF(), 0.01);
        assertEquals(6.5, map.getG(), 0.01);
        assertEquals("foo", map.getH());
    }

    @Test
    public void convertsArraysToJava() {
        PrimitiveArrays arrays = getPrimitiveArrays();

        boolean[] booleanArray = arrays.getA();
        assertEquals(4, booleanArray.length);
        assertTrue(booleanArray[0]);
        assertFalse(booleanArray[1]);

        assertArrayEquals(new byte[] { 2 }, arrays.getB());
        assertArrayEquals(new short[] { 3 }, arrays.getC());
        assertArrayEquals(new char[] { '@' }, arrays.getD());
        assertArrayEquals(new int[] { 4 }, arrays.getE());
        assertArrayEquals(new float[] { 5.5F }, arrays.getF(), 0.01F);
        assertArrayEquals(new double[] { 6.5 }, arrays.getG(), 0.01);
        assertArrayEquals(new String[] { "foo" }, arrays.getH());
    }

    @JSBody(params = { "a", "b", "c", "d", "e", "f", "g", "h" }, script = ""
            + "return '' + a + ':' + b + ':' + c + ':' + d + ':' + e + ':' + f.toFixed(1) + ':'"
                    + "+ g.toFixed(1) + ':' + h;")
    private static native String combinePrimitives(boolean a, byte b, short c, char d, int e, float f, double g,
            String h);

    @JSBody(params = {}, script = "return { a : true, b : 2, c : 3, d : 64, e : 4, f : 5.5, g : 6.5, h : 'foo' };")
    private static native Primitives getPrimitives();

    @JSBody(params = {}, script = "return { a : [true], b : [2], c : [3], d : [64], e : [4], f : [5.5], "
            + "g : [6.5], h : ['foo'] };")
    private static native PrimitiveArrays getPrimitiveArrays();

    interface Primitives extends JSObject {
        @JSProperty
        boolean getA();

        @JSProperty
        byte getB();

        @JSProperty
        short getC();

        @JSProperty
        char getD();

        @JSProperty
        int getE();

        @JSProperty
        float getF();

        @JSProperty
        double getG();

        @JSProperty
        String getH();
    }

    interface PrimitiveArrays extends JSObject {
        @JSProperty
        boolean[] getA();

        @JSProperty
        byte[] getB();

        @JSProperty
        short[] getC();

        @JSProperty
        char[] getD();

        @JSProperty
        int[] getE();

        @JSProperty
        float[] getF();

        @JSProperty
        double[] getG();

        @JSProperty
        String[] getH();
    }
}
