/*
 *  Copyright 2026 TeaVM contributors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

@TeaVMTest
@SkipJVM
@EachTestCompiledSeparately
@OnlyPlatform({TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY_GC})
public class InteropSwitchTest {
    @Test
    public void siblingTypes() {
        assertEquals(1, select(Kind.FIRST).getValue());
        assertEquals(2, select(Kind.SECOND).getValue());
    }

    @Test
    public void reversedSiblingTypes() {
        assertEquals(1, selectReversed(Kind.FIRST).getValue());
        assertEquals(2, selectReversed(Kind.SECOND).getValue());
    }

    @Test
    public void nullableResult() {
        assertEquals(1, selectNullable(Kind.FIRST).getValue());
        assertNull(selectNullable(Kind.SECOND));
    }

    private static Shared select(Kind kind) {
        return switch (kind) {
            case FIRST -> createFirst();
            case SECOND -> createSecond();
        };
    }

    private static Shared selectReversed(Kind kind) {
        return switch (kind) {
            case SECOND -> createSecond();
            case FIRST -> createFirst();
        };
    }

    private static Shared selectNullable(Kind kind) {
        return switch (kind) {
            case FIRST -> createFirst();
            case SECOND -> null;
        };
    }

    private static First createFirst() {
        return firstNative();
    }

    private static Second createSecond() {
        return secondNative();
    }

    @JSBody(script = "return { value: 1 };")
    private static native First firstNative();

    @JSBody(script = "return { value: 2 };")
    private static native Second secondNative();

    private enum Kind {
        FIRST, SECOND
    }

    interface Shared extends JSObject {
        @JSProperty
        int getValue();
    }

    interface First extends Shared {
    }

    interface Second extends Shared {
    }
}
