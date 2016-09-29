/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.backend.wasm;

import org.teavm.interop.Address;
import org.teavm.interop.Import;
import org.teavm.interop.StaticInit;
import org.teavm.interop.Unmanaged;

@StaticInit
@Unmanaged
public final class WasmRuntime {
    public static Address stack = initStack();

    private WasmRuntime() {
    }

    private static native Address initStack();

    public static int compare(int a, int b) {
        return gt(a, b) ? 1 : lt(a, b) ? -1 : 0;
    }

    public static int compare(long a, long b) {
        return gt(a, b) ? 1 : lt(a, b) ? -1 : 0;
    }

    public static int compare(float a, float b) {
        return gt(a, b) ? 1 : lt(a, b) ? -1 : 0;
    }

    public static int compare(double a, double b) {
        return gt(a, b) ? 1 : lt(a, b) ? -1 : 0;
    }

    public static float remainder(float a, float b) {
        return a - (float) (int) (a / b) * b;
    }

    public static double remainder(double a, double b) {
        return a - (double) (long) (a / b) * b;
    }

    private static native boolean lt(int a, int b);

    private static native boolean gt(int a, int b);

    private static native boolean lt(long a, long b);

    private static native boolean gt(long a, long b);

    private static native boolean lt(float a, float b);

    private static native boolean gt(float a, float b);

    private static native boolean lt(double a, double b);

    private static native boolean gt(double a, double b);

    public static Address align(Address address, int alignment) {
        int value = address.toInt();
        if (value == 0) {
            return address;
        }
        value = ((value - 1) / alignment + 1) * alignment;
        return Address.fromInt(value);
    }

    @Import(name = "print", module = "spectest")
    public static native void print(int a);

    public static void fillZero(Address address, int count) {
        int start = address.toInt();

        int alignedStart = start >>> 2 << 2;
        address = Address.fromInt(alignedStart);
        switch (start - alignedStart) {
            case 0:
                address.putInt(0);
                break;
            case 1:
                address.add(1).putByte((byte) 0);
                address.add(2).putShort((short) 0);
                break;
            case 2:
                address.add(2).putShort((short) 0);
                break;
            case 3:
                address.add(3).putByte((byte) 0);
                break;
        }

        int end = start + count;
        int alignedEnd = end >>> 2 << 2;
        address = Address.fromInt(alignedEnd);
        switch (end - alignedEnd) {
            case 0:
                break;
            case 1:
                address.putByte((byte) 0);
                break;
            case 2:
                address.putShort((short) 0);
                break;
            case 3:
                address.putShort((short) 0);
                address.add(2).putByte((byte) 0);
                break;
        }

        for (address = Address.fromInt(alignedStart + 4); address.toInt() < alignedEnd; address = address.add(4)) {
            address.putInt(0);
        }
    }

    public static void moveMemoryBlock(Address source, Address target, int count) {
        if (count < 8) {
            slowMemoryMove(source, target, count);
        }
        int diff = source.toInt() - target.toInt();
        if (diff == 0) {
            return;
        }
        if ((diff & 3) != 0) {
            slowMemoryMove(source, target, count);
            return;
        }

        Address alignedSourceStart = Address.fromInt(source.toInt() >>> 2 << 2);
        Address alignedTargetStart = Address.fromInt(target.toInt() >>> 2 << 2);

        Address alignedSourceEnd = Address.fromInt((source.toInt() + count) >>> 2 << 2);
        Address alignedTargetEnd = Address.fromInt((target.toInt() + count) >>> 2 << 2);

        if (source.toInt() > target.toInt()) {
            switch (source.toInt() - alignedSourceStart.toInt()) {
                case 0:
                    alignedTargetStart.putInt(alignedSourceStart.getInt());
                    break;
                case 1:
                    alignedTargetStart.add(1).putByte(alignedSourceStart.add(1).getByte());
                    alignedTargetStart.add(2).putShort(alignedSourceStart.add(2).getShort());
                    break;
                case 2:
                    alignedTargetStart.add(2).putShort(alignedSourceStart.add(2).getShort());
                    break;
                case 3:
                    alignedTargetStart.add(3).putByte(alignedSourceStart.add(3).getByte());
                    break;
            }

            alignedSourceStart = alignedSourceStart.add(4);
            alignedTargetStart = alignedTargetStart.add(4);

            while (alignedSourceStart.toInt() < alignedSourceEnd.toInt()) {
                alignedTargetStart.putInt(alignedSourceStart.getInt());
                alignedSourceStart = alignedSourceStart.add(4);
                alignedTargetStart = alignedTargetStart.add(4);
            }

            switch (source.toInt() + count - alignedSourceEnd.toInt()) {
                case 0:
                    break;
                case 1:
                    alignedTargetEnd.putByte(alignedSourceEnd.getByte());
                    break;
                case 2:
                    alignedTargetEnd.putShort(alignedSourceEnd.getShort());
                    break;
                case 3:
                    alignedTargetEnd.putShort(alignedSourceEnd.getShort());
                    alignedTargetEnd.add(2).putByte(alignedSourceEnd.add(2).getByte());
                    break;
            }
        } else {
            switch (source.toInt() + count - alignedSourceEnd.toInt()) {
                case 0:
                    break;
                case 1:
                    alignedTargetEnd.putByte(alignedSourceEnd.getByte());
                    break;
                case 2:
                    alignedTargetEnd.putShort(alignedSourceEnd.getShort());
                    break;
                case 3:
                    alignedTargetEnd.add(2).putByte(alignedSourceEnd.add(2).getByte());
                    alignedTargetEnd.putShort(alignedSourceEnd.getShort());
                    break;
            }

            while (alignedSourceEnd.toInt() > alignedSourceStart.toInt()) {
                alignedTargetEnd.putInt(alignedSourceEnd.getInt());
                alignedSourceEnd = alignedSourceEnd.add(-4);
                alignedTargetEnd = alignedTargetEnd.add(-4);
            }

            switch (source.toInt() - alignedSourceStart.toInt()) {
                case 0:
                    alignedTargetStart.putInt(alignedSourceStart.getInt());
                    break;
                case 1:
                    alignedTargetStart.add(2).putShort(alignedSourceStart.add(2).getShort());
                    alignedTargetStart.add(1).putByte(alignedSourceStart.add(1).getByte());
                    break;
                case 2:
                    alignedTargetStart.add(2).putShort(alignedSourceStart.add(2).getShort());
                    break;
                case 3:
                    alignedTargetStart.add(3).putByte(alignedSourceStart.add(3).getByte());
                    break;
            }
        }
    }

    private static void slowMemoryMove(Address source, Address target, int count) {
        if (source.toInt() > target.toInt()) {
            while (count-- > 0) {
                target.putByte(source.getByte());
                target = target.add(1);
                source = source.add(1);
            }
        } else {
            source.add(count);
            target.add(count);
            while (count-- > 0) {
                target = target.add(-1);
                source = source.add(-1);
                target.putByte(source.getByte());
            }
        }
    }

    public static Address allocStack(int size) {
        Address result = stack.add(4);
        stack = result.add((size << 2) + 4);
        stack.putInt(size);
        return result;
    }

    public static Address getStackTop() {
        return stack != initStack() ? stack : null;
    }

    public static Address getNextStackFrame(Address stackFrame) {
        int size = stackFrame.getInt() + 2;
        Address result = stackFrame.add(-size * 4);
        if (result == initStack()) {
            result = null;
        }
        return result;
    }

    public static int getStackRootCount(Address stackFrame) {
        return stackFrame.getInt();
    }

    public static Address getStackRootPointer(Address stackFrame) {
        int size = stackFrame.getInt();
        return stackFrame.add(-size * 4);
    }

    private static Address getExceptionHandlerPtr(Address stackFrame) {
        int size = stackFrame.getInt();
        return stackFrame.add(-size * 4 - 4);
    }

    public static int getCallSiteId(Address stackFrame) {
        return getExceptionHandlerPtr(stackFrame).getInt();
    }

    public static void setExceptionHandlerId(Address stackFrame, int id) {
        getExceptionHandlerPtr(stackFrame).putInt(id);
    }
}
