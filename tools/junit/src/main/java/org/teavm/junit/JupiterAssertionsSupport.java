/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.junit;

import java.util.Arrays;

/**
 * Lightweight replacements for utility methods used by JUnit Jupiter assertions,
 * see {@link JupiterAssertionsTransformer}.
 */
final class JupiterAssertionsSupport {
    private JupiterAssertionsSupport() {
    }

    static boolean isArray(Object obj) {
        return obj != null && obj.getClass().isArray();
    }

    static void rethrowIfUnrecoverable(Throwable exception) {
        if (exception instanceof OutOfMemoryError) {
            throw (OutOfMemoryError) exception;
        }
    }

    static boolean isNotBlank(String str) {
        return str != null && !str.isBlank();
    }

    static String nullSafeToString(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            if (obj.getClass().isArray()) {
                if (obj instanceof boolean[]) {
                    return Arrays.toString((boolean[]) obj);
                } else if (obj instanceof char[]) {
                    return Arrays.toString((char[]) obj);
                } else if (obj instanceof byte[]) {
                    return Arrays.toString((byte[]) obj);
                } else if (obj instanceof short[]) {
                    return Arrays.toString((short[]) obj);
                } else if (obj instanceof int[]) {
                    return Arrays.toString((int[]) obj);
                } else if (obj instanceof long[]) {
                    return Arrays.toString((long[]) obj);
                } else if (obj instanceof float[]) {
                    return Arrays.toString((float[]) obj);
                } else if (obj instanceof double[]) {
                    return Arrays.toString((double[]) obj);
                } else {
                    return Arrays.deepToString((Object[]) obj);
                }
            }
            var result = obj.toString();
            return result != null ? result : "null";
        } catch (RuntimeException e) {
            return obj.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(obj));
        }
    }

    /**
     * Supports only {@code %s}, {@code %d} and {@code %%}, which is enough for messages produced by assertions.
     */
    static String format(String format, Object... args) {
        var sb = new StringBuilder();
        var argIndex = 0;
        for (var i = 0; i < format.length(); ++i) {
            var c = format.charAt(i);
            if (c == '%' && i + 1 < format.length()) {
                var specifier = format.charAt(i + 1);
                if (specifier == '%') {
                    sb.append('%');
                    ++i;
                    continue;
                } else if (specifier == 's' || specifier == 'd') {
                    sb.append(args != null && argIndex < args.length ? args[argIndex++] : null);
                    ++i;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
