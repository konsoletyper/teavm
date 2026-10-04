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
package org.teavm.perf.jmh;

import org.teavm.perf.runtime.BlackholeSink;

/**
 * TeaVM-friendly replacement of JMH's {@code org.openjdk.jmh.infra.Blackhole}.
 * It's substituted for the original class by {@link org.teavm.perf.JmhSubstitutionPolicy}.
 */
public class TBlackhole {
    public TBlackhole() {
    }

    public TBlackhole(String challengeResponse) {
    }

    public final void consume(Object obj) {
        BlackholeSink.objectSink = obj;
        BlackholeSink.objectCount++;
    }

    public final void consume(byte b) {
        BlackholeSink.intSink += b;
    }

    public final void consume(boolean bool) {
        BlackholeSink.intSink += bool ? 1 : 0;
    }

    public final void consume(char c) {
        BlackholeSink.intSink += c;
    }

    public final void consume(short s) {
        BlackholeSink.intSink += s;
    }

    public final void consume(int i) {
        BlackholeSink.intSink += i;
    }

    public final void consume(long l) {
        BlackholeSink.longSink += l;
    }

    public final void consume(float f) {
        BlackholeSink.doubleSink += f;
    }

    public final void consume(double d) {
        BlackholeSink.doubleSink += d;
    }

    public static void consumeCPU(long tokens) {
        long t = tokens;
        long acc = 0;
        while (t > 0) {
            acc = acc * 0x5DEECE66DL + 0xBL + t;
            t--;
        }
        BlackholeSink.longSink += acc;
    }

    public void evaporate(String challengeResponse) {
    }
}
