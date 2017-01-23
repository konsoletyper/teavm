/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.java.util;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TMath;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.jso.JSBody;

/**
 *
 * @author Alexey Andreev
 */
public class TRandom extends TObject implements TSerializable {
    public TRandom() {
    }

    public TRandom(@SuppressWarnings("unused") long seed) {
    }

    public void setSeed(@SuppressWarnings("unused") long seed) {
    }

    protected int next(int bits) {
        return (int) (random() * (1L << TMath.min(32, bits)));
    }

    public void nextBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte) next(8);
        }
    }

    public int nextInt() {
        return next(32);
    }

    public int nextInt(int n) {
        return (int) (random() * n);
    }

    public long nextLong() {
        return ((long) nextInt() << 32) | nextInt();
    }

    public boolean nextBoolean() {
        return nextInt() % 2 == 0;
    }

    public float nextFloat() {
        return (float) random();
    }

    public double nextDouble() {
        return random();
    }

    @JSBody(script = "return Math.random();")
    private static native double random();
}
