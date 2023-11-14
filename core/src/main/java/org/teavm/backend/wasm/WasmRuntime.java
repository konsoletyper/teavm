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

import org.teavm.backend.wasm.runtime.WasmSupport;
import org.teavm.interop.Address;
import org.teavm.interop.StaticInit;
import org.teavm.interop.Unmanaged;
import org.teavm.runtime.RuntimeObject;

@StaticInit
public final class WasmRuntime {
    private WasmRuntime() {
    }

    @Unmanaged
    public static int compare(int a, int b) {
        return gt(a, b) ? 1 : lt(a, b) ? -1 : 0;
    }

    @Unmanaged
    public static int compareUnsigned(int a, int b) {
        return gtu(a, b) ? 1 : ltu(a, b) ? -1 : 0;
    }

    @Unmanaged
    public static int compareUnsigned(long a, long b) {
        return gtu(a, b) ? 1 : ltu(a, b) ? -1 : 0;
    }

    @Unmanaged
    public static int compare(long a, long b) {
        return gt(a, b) ? 1 : lt(a, b) ? -1 : 0;
    }

    @Unmanaged
    public static int compare(float a, float b) {
        return gt(a, b) ? 1 : lt(a, b) ? -1 : 0;
    }

    @Unmanaged
    public static int compare(double a, double b) {
        return gt(a, b) ? 1 : lt(a, b) ? -1 : 0;
    }

    @Unmanaged
    public static native float min(float a, float b);

    @Unmanaged
    public static native double min(double a, double b);

    @Unmanaged
    public static native float max(float a, float b);

    @Unmanaged
    public static native double max(double a, double b);

    @Unmanaged
    public static float remainder(float a, float b) {
        return a - (float) (int) (a / b) * b;
    }

    @Unmanaged
    public static double remainder(double a, double b) {
        return a - (double) (long) (a / b) * b;
    }

    @Unmanaged
    private static native boolean lt(int a, int b);

    @Unmanaged
    private static native boolean gt(int a, int b);

    @Unmanaged
    private static native boolean ltu(int a, int b);

    @Unmanaged
    private static native boolean gtu(int a, int b);

    @Unmanaged
    private static native boolean lt(long a, long b);

    @Unmanaged
    private static native boolean gt(long a, long b);

    @Unmanaged
    private static native boolean ltu(long a, long b);

    @Unmanaged
    private static native boolean gtu(long a, long b);

    @Unmanaged
    private static native boolean lt(float a, float b);

    @Unmanaged
    private static native boolean gt(float a, float b);

    @Unmanaged
    private static native boolean lt(double a, double b);

    @Unmanaged
    private static native boolean gt(double a, double b);

    @Unmanaged
    public static Address align(Address address, int alignment) {
        int value = address.toInt();
        if (value == 0) {
            return address;
        }
        value = ((value - 1) / alignment + 1) * alignment;
        return Address.fromInt(value);
    }

    @Unmanaged
    public static int align(int value, int alignment) {
        if (value == 0) {
            return value;
        }
        value = ((value - 1) / alignment + 1) * alignment;
        return value;
    }

    @Unmanaged
    public static void print(int a) {
        WasmSupport.print(a);
    }

    @Unmanaged
    public static void printString(String s) {
        WasmSupport.printString(s);
    }

    @Unmanaged
    public static void printInt(int i) {
        WasmSupport.printInt(i);
    }

    @Unmanaged
    public static void printOutOfMemory() {
        WasmSupport.printOutOfMemory();
    }

    @Unmanaged
    public static void fillZero(Address address, int count) {
        fill(address, (byte) 0, count);
    }

    @Unmanaged
    public static void fill(Address address, byte value, int count) {
    }

    @Unmanaged
    public static Address allocStack(int size) {
        Address stack = WasmHeap.stack;
        Address result = stack.add(4);
        stack = result.add((size << 2) + 4);
        stack.putInt(size);
        WasmHeap.stack = stack;
        return result;
    }

    @Unmanaged
    public static Address getStackTop() {
        return WasmHeap.stack != WasmHeap.stackAddress ? WasmHeap.stack : null;
    }

    @Unmanaged
    public static Address getNextStackFrame(Address stackFrame) {
        int size = stackFrame.getInt() + 2;
        Address result = stackFrame.add(-size * 4);
        if (result == WasmHeap.stackAddress) {
            result = null;
        }
        return result;
    }

    @Unmanaged
    public static int getStackRootCount(Address stackFrame) {
        return stackFrame.getInt();
    }

    @Unmanaged
    public static Address getStackRootPointer(Address stackFrame) {
        int size = stackFrame.getInt();
        return stackFrame.add(-size * 4);
    }

    @Unmanaged
    private static Address getExceptionHandlerPtr(Address stackFrame) {
        int size = stackFrame.getInt();
        return stackFrame.add(-size * 4 - 4);
    }

    @Unmanaged
    public static int getCallSiteId(Address stackFrame) {
        return getExceptionHandlerPtr(stackFrame).getInt();
    }

    @Unmanaged
    public static void setExceptionHandlerId(Address stackFrame, int id) {
        var addr = getExceptionHandlerPtr(stackFrame);
        addr.putInt(addr.getInt() + id + 2);
    }

    @Unmanaged
    public static void setExceptionHandlerSkip(Address stackFrame) {
        var addr = getExceptionHandlerPtr(stackFrame);
        addr.putInt(addr.getInt() + 1);
    }

    @Unmanaged
    public static void setExceptionHandlerRestore(Address stackFrame) {
        var addr = getExceptionHandlerPtr(stackFrame);
        addr.putInt(addr.getInt() - 1);
    }

    @Unmanaged
    private static int hashCode(RuntimeString string) {
        int hashCode = 0;
        int length = string.characters.length;
        Address chars = Address.ofData(string.characters);
        for (int i = 0; i < length; ++i) {
            hashCode = 31 * hashCode + chars.getChar();
            chars = chars.add(2);
        }
        return hashCode;
    }

    @Unmanaged
    private static boolean equals(RuntimeString first, RuntimeString second) {
        if (first.characters.length != second.characters.length) {
            return false;
        }

        Address firstChars = Address.ofData(first.characters);
        Address secondChars = Address.ofData(second.characters);
        int length = first.characters.length;
        for (int i = 0; i < length; ++i) {
            if (firstChars.getChar() != secondChars.getChar()) {
                return false;
            }
            firstChars = firstChars.add(2);
            secondChars = secondChars.add(2);
        }
        return true;
    }

    public static String[] resourceMapKeys(Address map) {
        String[] result = new String[resourceMapSize(map)];
        fillResourceMapKeys(map, result);
        return result;
    }

    @Unmanaged
    private static int resourceMapSize(Address map) {
        int result = 0;
        int sz = map.getInt();
        Address data = contentStart(map);
        for (int i = 0; i < sz; ++i) {
            if (data.getAddress() != null) {
                result++;
            }
            data = data.add(Address.sizeOf() * 2);
        }

        return result;
    }

    @Unmanaged
    private static void fillResourceMapKeys(Address map, String[] target) {
        int sz = map.getInt();
        Address data = contentStart(map);
        Address targetData = Address.ofData(target);
        for (int i = 0; i < sz; ++i) {
            Address entry = data.getAddress();
            if (entry != null) {
                targetData.putAddress(entry);
                targetData = targetData.add(Address.sizeOf());
            }
            data = data.add(Address.sizeOf() * 2);
        }
    }

    @Unmanaged
    private static Address contentStart(Address resource) {
        return resource.add(Address.sizeOf());
    }

    @Unmanaged
    public static Address lookupResource(Address map, String string) {
        RuntimeString runtimeString = Address.ofObject(string).toStructure();
        int hashCode = hashCode(runtimeString);
        int sz = map.getInt();
        Address content = contentStart(map);
        for (int i = 0; i < sz; ++i) {
            int index = (hashCode + i) % sz;
            if (index < 0) {
                index += sz;
            }
            Address entry = content.add(index * Address.sizeOf() * 2);
            Address key = entry.getAddress();
            if (key == null) {
                return null;
            }
            if (equals(key.toStructure(), runtimeString)) {
                return entry;
            }
        }
        return null;
    }

    @Unmanaged
    public static Address lookupResource(Address map, Address key) {
        int sz = map.getInt();
        Address content = contentStart(map);
        var hash = key.toInt();
        for (int i = 0; i < sz; ++i) {
            int index = (hash + i) % sz;
            if (index < 0) {
                index += sz;
            }
            var entry = content.add(index * Address.sizeOf() * 2);
            var entryKey = entry.getAddress();
            if (entryKey == null) {
                return null;
            }
            if (key == entryKey) {
                return entry;
            }
        }
        return null;
    }

    @Unmanaged
    public static native void callFunctionFromTable(int index, RuntimeObject instance);

    static class RuntimeString extends RuntimeObject {
        char[] characters;
    }
}
