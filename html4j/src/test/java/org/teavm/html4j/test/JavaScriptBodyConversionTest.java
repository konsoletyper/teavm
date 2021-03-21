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
package org.teavm.html4j.test;

import static org.junit.Assert.*;
import net.java.html.js.JavaScriptBody;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class JavaScriptBodyConversionTest {
    @Test
    public void convertsInteger() {
        assertEquals(23, returnAsInt(23));
        assertEquals(Integer.valueOf(23), returnAsInteger(23));
        assertEquals(23, returnAsObject(23));
        assertEquals(24, addOne((Object) 23));
        assertEquals(Integer.valueOf(24), addOne(Integer.valueOf(23)));
        assertEquals(24, addOne(23));
    }

    @Test
    public void convertsIntegerResult() {
        assertEquals(23, returnAsObject(23));
        assertEquals(Integer.valueOf(23), returnAsInteger(23));
        assertEquals(23, returnAsInt(23));
    }

    @Test
    public void convertsBoolean() {
        assertTrue(returnAsBoolean(true));
        assertTrue(returnAsBooleanWrapper(true));
        assertEquals(Boolean.TRUE, returnAsObject(true));
    }

    @Test
    public void convertsArray() {
        assertEquals(42, getArrayItem(new int[] { 23, 42 }, 1));
        assertEquals(42, getArrayItem(new Integer[] { 23, 42 }, 1));
    }

    @Test
    public void copiesArray() {
        Integer[] array = { 23, 42 };
        Object[] arrayCopy = (Object[]) modifyIntegerArray(array);
        assertEquals(Integer.valueOf(23), array[0]);
        assertEquals(1, arrayCopy[0]);
    }

    @Test
    public void createsArrayOfProperType() {
        assertEquals(Object[].class, returnAsObject(new int[] { 23, 42 }).getClass());
        assertEquals(Integer[].class, returnAsIntegerArray(new Integer[] { 23, 42 }).getClass());
        assertEquals(int[].class, returnAsIntArray(new Integer[] { 23, 42 }).getClass());
    }

    @JavaScriptBody(args = "value", body = "return value;")
    private native int returnAsInt(Object value);

    @JavaScriptBody(args = "value", body = "return value;")
    private native boolean returnAsBoolean(Object value);

    @JavaScriptBody(args = "value", body = "return value;")
    private native Boolean returnAsBooleanWrapper(boolean value);

    @JavaScriptBody(args = "value", body = "return value;")
    private native Integer returnAsInteger(Integer value);

    @JavaScriptBody(args = "value", body = "return value;")
    private native Object returnAsObject(int value);

    @JavaScriptBody(args = "value", body = "return value + 1;")
    private native int addOne(Object value);

    @JavaScriptBody(args = "value", body = "return value + 1;")
    private native Integer addOne(Integer value);

    @JavaScriptBody(args = "value", body = "return value + 1;")
    private native int addOne(int value);

    @JavaScriptBody(args = "array", body = "return array;")
    private native Object returnAsObject(Object array);

    @JavaScriptBody(args = "array", body = "return array;")
    private native int[] returnAsIntArray(Object array);

    @JavaScriptBody(args = "array", body = "return array;")
    private native Integer[] returnAsIntegerArray(Object array);

    @JavaScriptBody(args = "value", body = "value[0] = 1; return value;")
    private native Object modifyIntegerArray(Object value);

    @JavaScriptBody(args = { "array", "index" }, body = "return array[index];")
    private native Object getArrayItem(Object array, int index);
}
