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
package org.teavm.perf.runtime;

/**
 * Accumulates everything consumed by blackholes. Values are stored in static fields which are eventually read
 * by {@link BenchmarkEntryPoint} and reported to the host, so the compiler can't prove that computations
 * producing these values are dead.
 */
public final class BlackholeSink {
    public static int intSink;
    public static long longSink;
    public static double doubleSink;
    public static Object objectSink;
    public static int objectCount;

    private BlackholeSink() {
    }

    static long hash() {
        long result = intSink;
        result = result * 31 + longSink;
        result = result * 31 + Double.doubleToLongBits(doubleSink);
        result = result * 31 + objectCount;
        result = result * 31 + (objectSink != null ? 1 : 0);
        return result;
    }
}
