/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.runtime;

import java.util.Arrays;
import org.teavm.interop.AsyncCallback;
import org.teavm.interop.StaticInit;
import org.teavm.interop.Unmanaged;

@StaticInit
public class Fiber {
    public static final int STATE_RUNNING = 0;
    public static final int STATE_SUSPENDING = 1;
    public static final int STATE_RESUMING = 2;

    private int[] intValues;
    private int intTop;
    private long[] longValues;
    private int longTop;
    private float[] floatValues;
    private int floatTop;
    private double[] doubleValues;
    private int doubleTop;
    private Object[] objectValues;
    private int objectTop;
    private int state;
    private FiberRunner runner;
    private Object result;
    private Throwable exception;

    private static Fiber current;

    private Fiber(FiberRunner runner) {
        this.runner = runner;
    }

    public void push(int value) {
        if (intValues == null) {
            intValues = new int[4];
        } else if (intTop + 1 == intValues.length) {
            intValues = Arrays.copyOf(intValues, intValues.length * 3 / 2);
        }
        intValues[intTop++] = value;
    }

    public void push(long value) {
        if (longValues == null) {
            longValues = new long[4];
        } else if (longTop + 1 == longValues.length) {
            longValues = Arrays.copyOf(longValues, longValues.length * 3 / 2);
        }
        longValues[longTop++] = value;
    }

    public void push(float value) {
        if (floatValues == null) {
            floatValues = new float[4];
        } else if (floatTop + 1 == floatValues.length) {
            floatValues = Arrays.copyOf(floatValues, floatValues.length * 3 / 2);
        }
        floatValues[floatTop++] = value;
    }

    public void push(double value) {
        if (doubleValues == null) {
            doubleValues = new double[4];
        } else if (doubleTop + 4 == doubleValues.length) {
            doubleValues = Arrays.copyOf(doubleValues, doubleValues.length * 3 / 2);
        }
        doubleValues[doubleTop++] = value;
    }

    public void push(Object value) {
        if (objectValues == null) {
            objectValues = new Object[4];
        } else if (objectTop + 4 == objectValues.length) {
            objectValues = Arrays.copyOf(objectValues, objectValues.length * 3 / 2);
        }
        objectValues[objectTop++] = value;
    }

    @Unmanaged
    public int popInt() {
        return intValues[--intTop];
    }

    @Unmanaged
    public long popLong() {
        return longValues[--longTop];
    }

    @Unmanaged
    public float popFloat() {
        return floatValues[--floatTop];
    }

    @Unmanaged
    public double popDouble() {
        return doubleValues[--doubleTop];
    }

    @Unmanaged
    public Object popObject() {
        Object result = objectValues[--objectTop];
        objectValues[objectTop] = null;
        return result;
    }

    @Unmanaged
    public static Fiber current() {
        return current;
    }

    @Unmanaged
    public boolean isSuspending() {
        return state == STATE_SUSPENDING;
    }

    @Unmanaged
    public boolean isResuming() {
        return state == STATE_RESUMING;
    }

    @Unmanaged
    public static boolean getBoolean(Object v) {
        return v != null ? (Boolean) v : false;
    }

    @Unmanaged
    public static byte getByte(Object v) {
        return v != null ? (Byte) v : 0;
    }

    @Unmanaged
    public static short getShort(Object v) {
        return v != null ? (Short) v : 0;
    }

    @Unmanaged
    public static int getInt(Object v) {
        return v != null ? (Integer) v : 0;
    }

    @Unmanaged
    public static char getChar(Object v) {
        return v != null ? (Character) v : 0;
    }

    @Unmanaged
    public static long getLong(Object v) {
        return v != null ? (Long) v : 0;
    }

    @Unmanaged
    public static float getFloat(Object v) {
        return v != null ? (Float) v : 0;
    }

    @Unmanaged
    public static double getDouble(Object v) {
        return v != null ? (Double) v : 0;
    }

    public static Object suspend(AsyncCall call) throws Throwable {
        Fiber fiber = current();
        Thread javaThread = Thread.currentThread();
        if (fiber.isResuming()) {
            fiber.state = STATE_RUNNING;
            if (fiber.exception != null) {
                throw fiber.exception;
            }
            return fiber.result;
        }

        fiber.state = STATE_SUSPENDING;
        call.run(new AsyncCallback<Object>() {
            @Override
            public void complete(Object result) {
                setCurrentThread(javaThread);
                fiber.result = result;
                fiber.resume();
            }

            @Override
            public void error(Throwable e) {
                setCurrentThread(javaThread);
                fiber.exception = e;
                fiber.resume();
            }
        });
        return null;
    }

    static native void setCurrentThread(Thread thread);

    public static void start(FiberRunner runner) {
        new Fiber(runner).start();
    }

    static void startMain() {
        start(() -> {
            runMain();
            if (!current().isSuspending()) {
                EventQueue.stop();
            }
        });
    }

    static native void runMain();

    private void start() {
        Fiber former = current;
        current = this;
        runner.run();
        current = former;
    }

    private void resume() {
        state = STATE_RESUMING;
        start();
    }

    public interface FiberRunner {
        void run();
    }

    public interface AsyncCall {
        void run(AsyncCallback<?> callback);
    }
}
