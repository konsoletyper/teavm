/*
 *  Copyright 2026 Ethan Resnick.
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
package org.teavm.classlib.java.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
@SuppressWarnings("removal")
public class AccessControllerTest {
    // Java 24 Javadoc for checkPermission: "Throws AccessControlException." / "AccessControlException - always".
    @Test
    public void checkPermissionDeniesEveryPermission() {
        // TestPermission.implies always returns true: the denial must not depend on the permission itself.
        List<Permission> permissions = List.of(new TestPermission("test"), new TestPermission(""),
                new TestPermission("*"), new TestPermission("exitVM"));
        for (var permission : permissions) {
            var e = assertThrows(AccessControlException.class, () -> AccessController.checkPermission(permission));
            assertTrue(e instanceof SecurityException);
        }
    }

    // "always" includes null (Java 21 threw NullPointerException only "if the specified permission is null and
    // is checked based on the security policy currently in effect"; since Java 24 nothing is checked).
    @Test
    public void checkPermissionDeniesNull() {
        assertThrows(AccessControlException.class, () -> AccessController.checkPermission(null));
    }

    // Java 21 Javadoc for doPrivileged(PrivilegedExceptionAction): "PrivilegedActionException - if the specified
    // action's run method threw a checked exception". PrivilegedActionException.getException: "Returns the
    // exception thrown by the privileged computation"; "Throwable.getCause() method is now the preferred means
    // of obtaining this information".
    @Test
    public void doPrivilegedWrapsCheckedException() {
        List<Supplier<Exception>> exceptions = List.of(IOException::new, Exception::new, InterruptedException::new,
                CloneNotSupportedException::new, TestCheckedException::new, () -> new PrivilegedActionException(null));
        for (var supplier : exceptions) {
            var exception = supplier.get();
            var e = assertThrows(PrivilegedActionException.class,
                    () -> AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                        throw exception;
                    }));
            assertSame(exception, e.getException());
            assertSame(exception, e.getCause());
        }
    }

    // Java 21 Javadoc for doPrivileged(PrivilegedExceptionAction): "If the action's run method throws an
    // unchecked exception, it will propagate through this method."
    @Test
    public void doPrivilegedPropagatesUncheckedException() {
        List<Supplier<Throwable>> exceptions = List.of(RuntimeException::new, IllegalStateException::new,
                NullPointerException::new, SecurityException::new, TestUncheckedException::new, AssertionError::new,
                TestError::new);
        for (var supplier : exceptions) {
            var exception = supplier.get();
            var thrown = assertThrows(Throwable.class,
                    () -> AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                        throwUnchecked(exception);
                        return null;
                    }));
            assertSame(exception, thrown);
            var thrown2 = assertThrows(Throwable.class,
                    () -> AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                        throwUnchecked(exception);
                        return null;
                    }));
            assertSame(exception, thrown2);
        }
    }

    @Test
    public void doPrivilegedReturnsResult() throws PrivilegedActionException {
        assertEquals("ok", AccessController.doPrivileged((PrivilegedExceptionAction<String>) () -> "ok"));
        assertNull(AccessController.doPrivileged((PrivilegedExceptionAction<String>) () -> null));
        assertEquals("ok", AccessController.doPrivileged((PrivilegedAction<String>) () -> "ok"));
    }

    // PrivilegedActionException(Exception): "Constructs a new PrivilegedActionException "wrapping" the
    // specific Exception."
    @Test
    public void privilegedActionExceptionConstructor() {
        var cause = new IOException();
        var e = new PrivilegedActionException(cause);
        assertSame(cause, e.getException());
        assertSame(cause, e.getCause());
        var empty = new PrivilegedActionException(null);
        assertNull(empty.getException());
        assertNull(empty.getCause());
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw (Error) t;
    }

    static class TestCheckedException extends Exception {
    }

    static class TestUncheckedException extends IllegalArgumentException {
    }

    static class TestError extends Error {
    }

    static class TestPermission extends Permission {
        TestPermission(String name) {
            super(name);
        }

        @Override
        public boolean implies(Permission permission) {
            return true;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public String getActions() {
            return "";
        }
    }
}
