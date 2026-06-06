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
package org.teavm.jso.impl;

import org.teavm.model.MethodReader;
import org.teavm.model.ValueType;

final class JSPropertyUtil {
    private JSPropertyUtil() {
    }

    static boolean isProperGetter(MethodReader method) {
        return method.parameterCount() == 0 && method.getResultType() != ValueType.VOID;
    }

    static boolean isProperSetter(MethodReader method, String suggestedName) {
        return method.parameterCount() == 1 && method.getResultType() == ValueType.VOID
                && (suggestedName != null || isProperPrefix(method.getName(), "set"));
    }

    static String getGetterName(MethodReader method) {
        if (method.getResultType() == ValueType.BOOLEAN && isProperPrefix(method.getName(), "is")) {
            return cutPrefix(method.getName(), 2);
        }
        if (isProperPrefix(method.getName(), "get")) {
            return cutPrefix(method.getName(), 3);
        }
        return method.getName();
    }

    static String getSetterName(MethodReader method) {
        return cutPrefix(method.getName(), 3);
    }

    private static boolean isProperPrefix(String name, String prefix) {
        if (!name.startsWith(prefix) || name.length() == prefix.length()) {
            return false;
        }
        char c = name.charAt(prefix.length());
        return Character.isUpperCase(c) || !Character.isAlphabetic(c) && Character.isJavaIdentifierStart(c);
    }

    private static String cutPrefix(String name, int prefixLength) {
        if (name.length() == prefixLength + 1) {
            return name.substring(prefixLength).toLowerCase();
        }
        char c = name.charAt(prefixLength + 1);
        if (Character.isUpperCase(c)) {
            return name.substring(prefixLength);
        }
        return Character.toLowerCase(name.charAt(prefixLength)) + name.substring(prefixLength + 1);
    }
}
