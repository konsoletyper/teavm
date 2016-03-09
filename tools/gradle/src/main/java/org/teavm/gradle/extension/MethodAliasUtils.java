/*
 *  Copyright 2016 MJ.
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

package org.teavm.gradle.extension;

import java.util.List;
import java.util.Map;

import org.teavm.tooling.MethodAlias;

/** Allows to convert {@link Map} with properties into a {@link MethodAlias}.
 *
 * @author MJ */
public class MethodAliasUtils {
    private static final String ALIAS = "alias";
    private static final String CLASS_NAME = "className";
    private static final String METHOD_NAME = "methodName";
    private static final String DESCRIPTOR = "descriptor";
    private static final String TYPES = "types";

    private MethodAliasUtils() {
    }

    public static MethodAlias convert(final Map<String, Object> data) {
        final MethodAlias alias = new MethodAlias();
        alias.setAlias(data.get(ALIAS).toString());
        alias.setClassName(data.get(CLASS_NAME).toString());
        alias.setMethodName(data.get(METHOD_NAME).toString());
        alias.setDescriptor(data.get(DESCRIPTOR).toString());
        @SuppressWarnings("unchecked") final List<String> types = (List<String>) data.get(TYPES);
        if (types != null) {
            alias.setTypes(types.toArray(new String[types.size()]));
        } else {
            alias.setTypes(new String[] {});
        }
        return alias;
    }
}
