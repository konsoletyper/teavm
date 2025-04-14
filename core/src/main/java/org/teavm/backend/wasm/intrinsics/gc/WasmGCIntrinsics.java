/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.intrinsics.gc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.generate.gc.methods.WasmGCIntrinsicProvider;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.runtime.StringInternPool;
import org.teavm.backend.wasm.runtime.gc.WasmGCResources;
import org.teavm.common.ServiceRepository;
import org.teavm.interop.Address;
import org.teavm.interop.Structure;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.runtime.heap.Heap;

public class WasmGCIntrinsics implements WasmGCIntrinsicProvider {
    private Map<MethodReference, IntrinsicContainer> intrinsics = new HashMap<>();
    private List<WasmGCIntrinsicFactory> factories;
    private ClassReaderSource classes;
    private ServiceRepository services;

    public WasmGCIntrinsics(ClassReaderSource classes, ServiceRepository services,
            List<WasmGCIntrinsicFactory> factories, Map<MethodReference, WasmGCIntrinsic> customIntrinsics) {
        this.classes = classes;
        this.services = services;
        this.factories = List.copyOf(factories);
        fillWasmRuntime();
        fillObject();
        fillClass();
        fillClassSupport();
        fillSystem();
        fillLongAndInteger();
        fillFloat();
        fillDouble();
        fillArray();
        fillString();
        fillResources();
        fillHeap();
        fillAddress();
        fillStructure();
        for (var entry : customIntrinsics.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }

    private void fillWasmRuntime() {
        var intrinsic = new WasmRuntimeIntrinsic();
        for (var cls : List.of(int.class, long.class, float.class, double.class)) {
            add(new MethodReference(WasmRuntime.class, "lt", cls, cls, boolean.class), intrinsic);
            add(new MethodReference(WasmRuntime.class, "gt", cls, cls, boolean.class), intrinsic);
        }
        for (var cls : List.of(int.class, long.class)) {
            add(new MethodReference(WasmRuntime.class, "ltu", cls, cls, boolean.class), intrinsic);
            add(new MethodReference(WasmRuntime.class, "gtu", cls, cls, boolean.class), intrinsic);
        }
        for (var cls : List.of(float.class, double.class)) {
            add(new MethodReference(WasmRuntime.class, "min", cls, cls, cls), intrinsic);
            add(new MethodReference(WasmRuntime.class, "max", cls, cls, cls), intrinsic);
        }
    }

    private void fillObject() {
        var intrinsic = new ObjectIntrinsic();
        add(new MethodReference(Object.class, "getClass", Class.class), intrinsic);
        add(new MethodReference(Object.class, "cloneObject", Object.class), intrinsic);
        add(new MethodReference(Object.class.getName(), "getMonitor",
                ValueType.object("java.lang.Object$Monitor")), intrinsic);
        add(new MethodReference(Object.class.getName(), "setMonitor",
                ValueType.object("java.lang.Object$Monitor"), ValueType.VOID), intrinsic);
        add(new MethodReference(Object.class.getName(), "wasmGCIdentity", ValueType.INTEGER), intrinsic);
        add(new MethodReference(Object.class.getName(), "setWasmGCIdentity", ValueType.INTEGER,
                ValueType.VOID), intrinsic);
    }

    private void fillClass() {
        var intrinsic = new ClassIntrinsic();
        add(new MethodReference(Class.class, "getComponentType", Class.class), intrinsic);
        add(new MethodReference(Class.class, "getWasmGCFlags", int.class), intrinsic);
        add(new MethodReference(Class.class, "getNameImpl", String.class), intrinsic);
        add(new MethodReference(Class.class, "setNameImpl", String.class, void.class), intrinsic);
        add(new MethodReference(Class.class, "getEnclosingClass", Class.class), intrinsic);
        add(new MethodReference(Class.class, "getDeclaringClass", Class.class), intrinsic);
        add(new MethodReference(Class.class, "getSuperclass", Class.class), intrinsic);
        add(new MethodReference(Class.class, "getSimpleNameCache", Class.class, String.class), intrinsic);
        add(new MethodReference(Class.class, "setSimpleNameCache", Class.class, String.class, void.class), intrinsic);
        add(new MethodReference(Class.class, "getCanonicalNameCache", String.class), intrinsic);
        add(new MethodReference(Class.class, "setCanonicalNameCache", String.class, void.class), intrinsic);
        add(new MethodReference(Class.class, "getDeclaredAnnotationsImpl", Annotation[].class), intrinsic);
        add(new MethodReference(Class.class, "getInterfacesImpl", Class[].class), intrinsic);
        add(new MethodReference(Class.class, "previous", Class.class), intrinsic);
        add(new MethodReference(Class.class, "last", Class.class), intrinsic);
        add(new MethodReference(Class.class, "initializeImpl", void.class), intrinsic);
        add(new MethodReference("java.lang.Class", "getDeclaredFieldsImpl",
                ValueType.object("org.teavm.classlib.impl.reflection.FieldInfoList")), intrinsic);
        add(new MethodReference("java.lang.Class", "getDeclaredMethodsImpl",
                ValueType.object("org.teavm.classlib.impl.reflection.MethodInfoList")), intrinsic);
    }

    private void fillClassSupport() {
        var intrinsic = new ClassSupportIntrinsic();
        add(new MethodReference("org.teavm.classlib.impl.reflection.ClassSupport",
                "getEnumConstants", ValueType.object("java.lang.Class"),
                ValueType.arrayOf(ValueType.object("java.lang.Enum"))), intrinsic);
    }

    private void fillSystem() {
        var arrayCopyIntrinsic = new SystemArrayCopyIntrinsic();
        add(new MethodReference(System.class, "arraycopy", Object.class, int.class, Object.class,
                int.class, int.class, void.class), arrayCopyIntrinsic);
        add(new MethodReference(System.class, "doArrayCopy", Object.class, int.class, Object.class,
                int.class, int.class, void.class), arrayCopyIntrinsic);
        add(new MethodReference(System.class, "currentTimeMillis", long.class), new SystemIntrinsic());
    }

    private void fillLongAndInteger() {
        fillIntNum(int.class, Integer.class, WasmIntType.INT32);
        fillIntNum(long.class, Long.class, WasmIntType.INT64);
    }

    private void fillIntNum(Class<?> javaClass, Class<?> wrapperClass, WasmIntType wasmType) {
        var intrinsic = new IntNumIntrinsic(javaClass, wasmType);
        add(new MethodReference(wrapperClass, "divideUnsigned", javaClass, javaClass, javaClass), intrinsic);
        add(new MethodReference(wrapperClass, "remainderUnsigned", javaClass, javaClass, javaClass), intrinsic);
        add(new MethodReference(wrapperClass, "compareUnsigned", javaClass, javaClass, int.class), intrinsic);
        add(new MethodReference(wrapperClass, "numberOfLeadingZeros", javaClass, int.class), intrinsic);
        add(new MethodReference(wrapperClass, "numberOfTrailingZeros", javaClass, int.class), intrinsic);
        add(new MethodReference(wrapperClass, "bitCount", javaClass, int.class), intrinsic);
    }

    private void fillFloat() {
        var intrinsic = new FloatIntrinsic();
        add(new MethodReference(Float.class, "getNaN", float.class), intrinsic);
        add(new MethodReference(Float.class, "isNaN", float.class, boolean.class), intrinsic);
        add(new MethodReference(Float.class, "isInfinite", float.class, boolean.class), intrinsic);
        add(new MethodReference(Float.class, "isFinite", float.class, boolean.class), intrinsic);
        add(new MethodReference(Float.class, "floatToRawIntBits", float.class, int.class), intrinsic);
        add(new MethodReference(Float.class, "intBitsToFloat", int.class, float.class), intrinsic);
    }

    private void fillDouble() {
        var intrinsic = new DoubleIntrinsic();
        add(new MethodReference(Double.class, "getNaN", double.class), intrinsic);
        add(new MethodReference(Double.class, "isNaN", double.class, boolean.class), intrinsic);
        add(new MethodReference(Double.class, "isInfinite", double.class, boolean.class), intrinsic);
        add(new MethodReference(Double.class, "isFinite", double.class, boolean.class), intrinsic);
        add(new MethodReference(Double.class, "doubleToRawLongBits", double.class, long.class), intrinsic);
        add(new MethodReference(Double.class, "longBitsToDouble", long.class, double.class), intrinsic);
    }

    private void fillArray() {
        var intrinsic = new ArrayIntrinsic();
        add(new MethodReference(Array.class, "getLength", Object.class, int.class), intrinsic);
        add(new MethodReference(Array.class, "getImpl", Object.class, int.class, Object.class), intrinsic);
    }

    private void fillString() {
        var intrinsic = new StringInternPoolIntrinsic();
        var className = StringInternPool.class.getName() + "$Entry";
        add(new MethodReference(className, "getValue", ValueType.parse(String.class)), intrinsic);
        add(new MethodReference(className, "setValue", ValueType.parse(String.class), ValueType.VOID), intrinsic);
    }

    private void fillResources() {
        var intrinsic = new WasmGCResourcesIntrinsic();
        add(new MethodReference(WasmGCResources.class, "readSingleByte", int.class, int.class), intrinsic);
    }

    private void fillHeap() {
        var intrinsic = new HeapIntrinsic();
        add(new MethodReference(Heap.class, "grow", int.class, int.class), intrinsic);
    }

    private void fillAddress() {
        var intrinsic = new AddressIntrinsic();
        add(new MethodReference(Address.class, "add", int.class, Address.class), intrinsic);
        add(new MethodReference(Address.class, "add", long.class, Address.class), intrinsic);
        add(new MethodReference(Address.class, "toLong", long.class), intrinsic);
        add(new MethodReference(Address.class, "toInt", int.class), intrinsic);
        add(new MethodReference(Address.class, "fromLong", long.class, Address.class), intrinsic);
        add(new MethodReference(Address.class, "fromInt", int.class, Address.class), intrinsic);
        add(new MethodReference(Address.class, "isLessThan", Address.class, boolean.class), intrinsic);
        add(new MethodReference(Address.class, "toStructure", Structure.class), intrinsic);
        add(new MethodReference(Address.class, "getByte", byte.class), intrinsic);
        add(new MethodReference(Address.class, "putByte", byte.class, void.class), intrinsic);
        add(new MethodReference(Address.class, "getShort", short.class), intrinsic);
        add(new MethodReference(Address.class, "putShort", short.class, void.class), intrinsic);
        add(new MethodReference(Address.class, "getChar", char.class), intrinsic);
        add(new MethodReference(Address.class, "putChar", char.class, void.class), intrinsic);
        add(new MethodReference(Address.class, "getInt", int.class), intrinsic);
        add(new MethodReference(Address.class, "putInt", int.class, void.class), intrinsic);
        add(new MethodReference(Address.class, "getLong", long.class), intrinsic);
        add(new MethodReference(Address.class, "putLong", long.class, void.class), intrinsic);
        add(new MethodReference(Address.class, "getFloat", float.class), intrinsic);
        add(new MethodReference(Address.class, "putFloat", float.class, void.class), intrinsic);
        add(new MethodReference(Address.class, "getDouble", double.class), intrinsic);
        add(new MethodReference(Address.class, "putDouble", double.class, void.class), intrinsic);
        add(new MethodReference(Address.class, "getAddress", Address.class), intrinsic);
        add(new MethodReference(Address.class, "putAddress", Address.class, void.class), intrinsic);
        add(new MethodReference(Address.class, "align", Address.class, int.class, Address.class), intrinsic);
        add(new MethodReference(Address.class, "sizeOf", int.class), intrinsic);
        add(new MethodReference(Address.class, "add", Class.class, int.class, Address.class), intrinsic);
        add(new MethodReference(Address.class, "fillZero", Address.class, int.class, void.class), intrinsic);
        add(new MethodReference(Address.class, "fill", Address.class, byte.class, int.class, void.class), intrinsic);
        add(new MethodReference(Address.class, "moveMemoryBlock", Address.class, Address.class, int.class, void.class),
                intrinsic);
    }

    private void fillStructure() {
        var intrinsic = new StructureIntrinsic();
        add(new MethodReference(Structure.class, "cast", Structure.class), intrinsic);
        add(new MethodReference(Structure.class, "toAddress", Address.class), intrinsic);
        add(new MethodReference(Structure.class, "sizeOf", Class.class, int.class), intrinsic);
        add(new MethodReference(Structure.class, "add", Class.class, Structure.class, int.class, Structure.class),
                intrinsic);
    }

    private void add(MethodReference methodRef, WasmGCIntrinsic intrinsic) {
        intrinsics.put(methodRef, new IntrinsicContainer(intrinsic));
    }

    @Override
    public WasmGCIntrinsic get(MethodReference method) {
        var result = intrinsics.get(method);
        if (result == null) {
            WasmGCIntrinsic intrinsic = null;
            for (var factory : factories) {
                intrinsic = factory.createIntrinsic(method, factoryContext);
                if (intrinsic != null) {
                    break;
                }
            }
            intrinsics.put(method, new IntrinsicContainer(intrinsic));
            result = new IntrinsicContainer(intrinsic);
        }
        return result.intrinsic;
    }

    static class IntrinsicContainer {
        final WasmGCIntrinsic intrinsic;

        IntrinsicContainer(WasmGCIntrinsic intrinsic) {
            this.intrinsic = intrinsic;
        }
    }

    private final WasmGCIntrinsicFactoryContext factoryContext = new WasmGCIntrinsicFactoryContext() {
        @Override
        public ClassReaderSource classes() {
            return classes;
        }

        @Override
        public ServiceRepository services() {
            return services;
        }
    };
}
