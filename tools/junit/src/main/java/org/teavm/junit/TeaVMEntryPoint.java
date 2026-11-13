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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Compiles a real program entry point instead of the generated JUnit launcher.
 * Use on an {@code @Test} method of an {@link EachTestCompiledSeparately} class.
 * The method body runs on the JVM only; backend runs launch the named class and
 * verify its output. Each backend run starts with fresh program state.
 * External compiler platforms and asynchronous Wasm GC entry points are not supported.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TeaVMEntryPoint {
    String value();

    String[] args() default {};

    String stdout();
}
