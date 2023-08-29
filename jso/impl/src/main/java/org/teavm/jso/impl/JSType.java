/*
 *  Copyright 2023 Alexey Andreev.
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

import java.util.Objects;

public class JSType {
    private JSType() {
    }

    public static final JSType NULL = new JSType();
    public static final JSType JS = new JSType();
    public static final JSType JAVA = new JSType();
    public static final JSType MIXED = new JSType();

    public static final class ArrayType extends JSType {
        public final JSType elementType;

        private ArrayType(JSType elementType) {
            this.elementType = elementType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ArrayType arrayType = (ArrayType) o;
            return Objects.equals(elementType, arrayType.elementType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(elementType);
        }
    }

    public static JSType arrayOf(JSType elementType) {
        return new ArrayType(elementType);
    }
}
