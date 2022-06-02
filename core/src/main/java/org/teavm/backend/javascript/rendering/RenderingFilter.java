/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.javascript.rendering;

import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;

public interface RenderingFilter {
    boolean isExternal(MethodReference method);

    boolean isShared(MethodReference method);

    boolean isExternal(String className);

    boolean isShared(String className);

    boolean isExternal(FieldReference field);

    boolean isShared(FieldReference field);

    boolean isExternalString(String string);

    boolean isSharedString(String string);

    RenderingFilter EMPTY = new RenderingFilter() {
        @Override
        public boolean isExternal(MethodReference method) {
            return false;
        }

        @Override
        public boolean isShared(MethodReference method) {
            return false;
        }

        @Override
        public boolean isExternal(String className) {
            return false;
        }

        @Override
        public boolean isShared(String className) {
            return false;
        }

        @Override
        public boolean isExternal(FieldReference field) {
            return false;
        }

        @Override
        public boolean isShared(FieldReference field) {
            return false;
        }

        @Override
        public boolean isExternalString(String string) {
            return false;
        }

        @Override
        public boolean isSharedString(String string) {
            return false;
        }
    };
}
