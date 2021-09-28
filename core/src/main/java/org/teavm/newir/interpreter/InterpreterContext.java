/*
 *  Copyright 2021 konsoletyper.
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
package org.teavm.newir.interpreter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.teavm.model.ValueType;
import org.teavm.newir.expr.IrBinaryExpr;

public class InterpreterContext {
    public final int[] iv;
    public final long[] lv;
    public final float[] fv;
    public final double[] dv;
    public final Object[] ov;
    public int ptr;
    public boolean stopped;

    InterpreterContext(
            int intValueCount,
            int longValueCount,
            int floatValueCount,
            int doubleValueCount,
            int objectValueCount
    ) {
        iv = new int[intValueCount];
        lv = new long[longValueCount];
        fv = new float[floatValueCount];
        dv = new double[doubleValueCount];
        ov = new Object[objectValueCount];
    }

    void reset() {
        Arrays.fill(iv, 0);
        Arrays.fill(lv, 0);
        Arrays.fill(fv, 0);
        Arrays.fill(dv, 0);
        Arrays.fill(ov, null);
        ptr = 0;
        stopped = false;
    }

    void emit(int index, int value) {
        iv[index] = value;
        ptr++;
    }

    void emit(int index, long value) {
        lv[index] = value;
        ptr++;
    }

    void emit(int index, float value) {
        fv[index] = value;
        ptr++;
    }

    void emit(int index, double value) {
        dv[index] = value;
        ptr++;
    }

    void emit(int index, boolean value) {
        emit(index, value ? 1 : 0);
    }

    void emit(int index, Object value) {
        ov[index] = value;
        ptr++;
    }

    Object objOrNull(int index) {
        return index >= 0 ? ov[index] : null;
    }
}
