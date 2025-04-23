/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.classlib.java.nio;

import java.nio.Buffer;
import org.teavm.interop.Address;
import org.teavm.interop.Import;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.ArrayBufferView;
import org.teavm.jso.typedarrays.BigInt64Array;
import org.teavm.jso.typedarrays.DataView;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Float64Array;
import org.teavm.jso.typedarrays.Int16Array;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.typedarrays.Uint16Array;
import org.teavm.jso.typedarrays.Uint8Array;
import org.teavm.runtime.heap.Heap;

public final class TJSBufferHelper {
    public static class WasmGC {
        private WasmGC() {
        }

        private static final TBufferFinalizationRegistry registry = new TBufferFinalizationRegistry(address -> {
            var addr = Address.fromInt(((JSNumber) address).intValue());
            Heap.release(addr);
        });


        static void register(JSObject object, Address address, JSObject token) {
            registry.register(object, JSNumber.valueOf(address.toInt()), token);
        }

        public static void unregister(JSObject object) {
            registry.unregister(object);
        }

        @Import(module = "teavm", name = "linearMemory")
        static native ArrayBuffer getLinearMemory();
    }

    private TJSBufferHelper() {
    }

    static ArrayBufferView getArrayBufferView(Buffer buffer) {
        if (!(buffer instanceof TArrayBufferViewProvider)) {
            throw new IllegalArgumentException("This buffer is not allocated in linear memory and does not "
                    + "wrap native JS buffer");
        }
        var provider = (TArrayBufferViewProvider) buffer;
        var result = provider.getArrayBufferView();
        return result;
    }

    static Int8Array toInt8Array(ArrayBufferView view) {
        return new Int8Array(view.getBuffer(), view.getByteOffset(), view.getByteLength());
    }

    static Uint8Array toUint8Array(ArrayBufferView view) {
        return new Uint8Array(view.getBuffer(), view.getByteOffset(), view.getByteLength());
    }

    static Int16Array toInt16Array(ArrayBufferView view) {
        return new Int16Array(view.getBuffer(), view.getByteOffset(), view.getByteLength() / 2);
    }

    static Uint16Array toUint16Array(ArrayBufferView view) {
        return new Uint16Array(view.getBuffer(), view.getByteOffset(), view.getByteLength() / 2);
    }

    static Int32Array toInt32Array(ArrayBufferView view) {
        return new Int32Array(view.getBuffer(), view.getByteOffset(), view.getByteLength() / 4);
    }

    static BigInt64Array toBigInt64Array(ArrayBufferView view) {
        return new BigInt64Array(view.getBuffer(), view.getByteOffset(), view.getByteLength() / 8);
    }

    static Float32Array toFloat32Array(ArrayBufferView view) {
        return new Float32Array(view.getBuffer(), view.getByteOffset(), view.getByteLength() / 4);
    }

    static Float64Array toFloat64Array(ArrayBufferView view) {
        return new Float64Array(view.getBuffer(), view.getByteOffset(), view.getByteLength() / 8);
    }

    static DataView toDataView(ArrayBufferView view) {
        return new DataView(view.getBuffer(), view.getByteOffset(), view.getByteLength());
    }
}
