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
package org.teavm.runtime.reflect;

import org.teavm.interop.Unmanaged;

public class GenericTypeInfo extends ReflectionInfo {
    public static class Kind {
        public static final int PARAMETERIZED_TYPE = 0;
        public static final int TYPE_VARIABLE = 1;
        public static final int GENERIC_ARRAY = 2;
        public static final int UPPER_BOUND_WILDCARD = 3;
        public static final int LOWER_BOUND_WILDCARD = 4;
        public static final int UNBOUNDED_WILDCARD = 5;
        public static final int RAW_TYPE = 6;
    }

    @Unmanaged
    public final native int kind();

    @Unmanaged
    public final native ParameterizedTypeInfo asParameterizedType();

    @Unmanaged
    public final native TypeVariableReference asTypeVariable();

    @Unmanaged
    public final native GenericArrayInfo asGenericArray();

    @Unmanaged
    public final native WildcardTypeInfo asWildcard();

    @Unmanaged
    public final native RawTypeInfo asRawType();
}
