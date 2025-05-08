/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.cinterop;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.interop.Import;
import org.teavm.interop.c.Include;
import org.teavm.junit.AttachC;
import org.teavm.junit.IncludeC;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@IncludeC("org/teavm/c/test/convert-params.h")
@AttachC("org/teavm/c/test/convert-params.c")
@SkipJVM
@OnlyPlatform(TestPlatform.C)
public class ConvertParametersTest {
    @Test
    public void passByteBuffer() {
        var buffer = ByteBuffer.allocate(5);
        buffer.put(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(1, incrementAllBytes(buffer, buffer.capacity()));

        var result = new byte[5];
        buffer.get(0, result);
        assertArrayEquals(new byte[] { 2, 3, 4, 5, 6 }, result);
        assertEquals(0, incrementAllBytes(null, 0));
    }

    @Import(name = "incrementAllBytes")
    @Include("convert-params.h")
    private static native int incrementAllBytes(ByteBuffer buffer, int count);

    @Test
    public void passShortBuffer() {
        var buffer = ShortBuffer.allocate(5);
        buffer.put(new short[] { 1, 2, 3, 4, 5 });
        incrementAllShorts(buffer, buffer.capacity());

        var result = new short[5];
        buffer.get(0, result);
        assertArrayEquals(new short[] { 2, 3, 4, 5, 6 }, result);
    }

    @Import(name = "incrementAllShorts")
    @Include("convert-params.h")
    private static native void incrementAllShorts(ShortBuffer buffer, int count);

    @Test
    public void passCharBuffer() {
        var buffer = CharBuffer.allocate(5);
        buffer.put(new char[] { 1, 2, 3, 4, 5 });
        incrementAllChars(buffer, buffer.capacity());

        var result = new char[5];
        buffer.get(0, result);
        assertArrayEquals(new char[] { 2, 3, 4, 5, 6 }, result);
    }

    @Import(name = "incrementAllChars")
    @Include("convert-params.h")
    private static native void incrementAllChars(CharBuffer buffer, int count);

    @Test
    public void passIntBuffer() {
        var buffer = IntBuffer.allocate(5);
        buffer.put(new int[] { 1, 2, 3, 4, 5 });
        incrementAllInts(buffer, buffer.capacity());

        var result = new int[5];
        buffer.get(0, result);
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6 }, result);
    }

    @Import(name = "incrementAllInts")
    @Include("convert-params.h")
    private static native void incrementAllInts(IntBuffer buffer, int count);

    @Test
    public void passLongBuffer() {
        var buffer = LongBuffer.allocate(5);
        buffer.put(new long[] { 1, 2, 3, 4, 5 });
        incrementAllLongs(buffer, buffer.capacity());

        var result = new long[5];
        buffer.get(0, result);
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6 }, result);
    }

    @Import(name = "incrementAllLongs")
    @Include("convert-params.h")
    private static native void incrementAllLongs(LongBuffer buffer, int count);

    @Test
    public void passFloatBuffer() {
        var buffer = FloatBuffer.allocate(5);
        buffer.put(new float[] { 1, 2, 3, 4, 5 });
        incrementAllFloats(buffer, buffer.capacity());

        var result = new float[5];
        buffer.get(0, result);
        assertArrayEquals(new float[] { 2, 3, 4, 5, 6 }, result, 0.1f);
    }

    @Import(name = "incrementAllFloats")
    @Include("convert-params.h")
    private static native void incrementAllFloats(FloatBuffer buffer, int count);

    @Test
    public void passDoubleBuffer() {
        var buffer = DoubleBuffer.allocate(5);
        buffer.put(new double[] { 1, 2, 3, 4, 5 });
        incrementAllDoubles(buffer, buffer.capacity());

        var result = new double[5];
        buffer.get(0, result);
        assertArrayEquals(new double[] { 2, 3, 4, 5, 6 }, result, 0.1f);
    }

    @Import(name = "incrementAllDoubles")
    @Include("convert-params.h")
    private static native void incrementAllDoubles(DoubleBuffer buffer, int count);
}
