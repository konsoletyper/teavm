/*
 *  Copyright 2017 Alexey Andreev.
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
public class ThrowableTest {
    @Test
    public void causeWorks() {
        RuntimeException e = new RuntimeException("fail", new RuntimeException("OK"));
        assertTrue(e.getCause() instanceof RuntimeException);
        assertEquals("OK", e.getCause().getMessage());
    }

    @Test
    public void toStringWorks() {
        assertEquals("java.lang.RuntimeException: fail", new RuntimeException("fail").toString());
        assertEquals("java.lang.RuntimeException", new RuntimeException().toString());
    }

    @Test
    public void suppressedExceptionsInitiallyEmpty() {
        for (Throwable exception : createThrowables()) {
            assertEquals(0, exception.getSuppressed().length);
        }
    }

    @Test
    public void suppressedExceptionsRetained() {
        Throwable first = new Throwable("first");
        Throwable second = new Throwable("second");
        for (Throwable exception : createThrowables()) {
            exception.addSuppressed(first);
            exception.addSuppressed(second);
            assertEquals(2, exception.getSuppressed().length);
            assertSame(first, exception.getSuppressed()[0]);
            assertSame(second, exception.getSuppressed()[1]);
        }
    }

    @Test
    public void suppressedExceptionsReturnedAsCopy() {
        Throwable exception = new Throwable();
        Throwable suppressed = new Throwable("suppressed");
        exception.addSuppressed(suppressed);
        exception.getSuppressed()[0] = null;
        assertSame(suppressed, exception.getSuppressed()[0]);
    }

    @Test
    public void suppressionCanBeDisabled() {
        for (boolean writableStackTrace : new boolean[] {false, true}) {
            Throwable exception = new ConfigurableThrowable(false, writableStackTrace);
            assertEquals(0, exception.getSuppressed().length);
            exception.addSuppressed(new Throwable("ignored"));
            assertEquals(0, exception.getSuppressed().length);
        }
    }

    @Test
    public void tryWithResourcesRetainsCloseFailure() {
        RuntimeException primary = new RuntimeException("primary");
        RuntimeException secondary = new RuntimeException("close");
        try (AutoCloseable resource = () -> {
            throw secondary;
        }) {
            throw primary;
        } catch (Exception exception) {
            assertSame(primary, exception);
            assertEquals(1, exception.getSuppressed().length);
            assertSame(secondary, exception.getSuppressed()[0]);
        }
    }

    private Throwable[] createThrowables() {
        Throwable cause = new Throwable("cause");
        return new Throwable[] {
                new Throwable(), new Throwable("message"), new Throwable(cause),
                new Throwable("message", cause), new ConfigurableThrowable(true, true),
                new ConfigurableThrowable(true, false)
        };
    }

    private static class ConfigurableThrowable extends Throwable {
        ConfigurableThrowable(boolean enableSuppression, boolean writableStackTrace) {
            super("configured", null, enableSuppression, writableStackTrace);
        }
    }
}
