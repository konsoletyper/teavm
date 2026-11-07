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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables coroutine transformation (i.e. support of async methods like {@code Thread.sleep} or
 * {@code Object.wait}) when compiling tests, see {@link org.teavm.vm.TeaVM#setAsyncEnabled(boolean)}.
 * Without this annotation, compilation of a test that reaches any async method fails.
 *
 * <p>When put on a class, applies to all its test methods. When put on a method and the whole class is compiled
 * into a single module (i.e. the class isn't marked with {@link EachTestCompiledSeparately}), coroutine
 * transformation is enabled for the entire module.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Inherited
public @interface AsyncEnabled {
}
