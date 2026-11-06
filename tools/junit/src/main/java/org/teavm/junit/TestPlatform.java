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
package org.teavm.junit;

/**
 * Identifiers of built-in test platforms, to use with {@link SkipPlatform} and {@link OnlyPlatform}.
 * Platform identifiers are plain strings, so that additional platforms, contributed via
 * {@link org.teavm.junit.spi.TestPlatformProvider}, can define their own identifiers
 * (see {@link org.teavm.junit.spi.ExternalTestPlatform#getName()}).
 */
public final class TestPlatform {
    public static final String JAVASCRIPT = "JAVASCRIPT";
    @Deprecated
    public static final String WEBASSEMBLY = "WEBASSEMBLY";
    @Deprecated
    public static final String WASI = "WASI";
    public static final String WEBASSEMBLY_GC = "WEBASSEMBLY_GC";
    public static final String C = "C";

    private TestPlatform() {
    }
}
