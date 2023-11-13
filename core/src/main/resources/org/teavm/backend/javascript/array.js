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
"use strict";

let $rt_createArray = (cls, sz) => {
    let data = new teavm_globals.Array(sz);
    data.fill(null);
    return new ($rt_arraycls(cls))(data);
}
let $rt_wrapArray = (cls, data) => new ($rt_arraycls(cls))(data);
let $rt_createUnfilledArray = (cls, sz) => new ($rt_arraycls(cls))(new teavm_globals.Array(sz));
let $rt_createLongArray;
let $rt_createLongArrayFromData;
if (typeof teavm_globals.BigInt64Array !== 'function') {
    $rt_createLongArray = sz => {
        let data = new teavm_globals.Array(sz);
        let arr = new $rt_longArrayCls(data);
        data.fill(Long_ZERO);
        return arr;
    }
    $rt_createLongArrayFromData = init => new $rt_longArrayCls(init);
} else {
    $rt_createLongArray = sz => new $rt_longArrayCls(new teavm_globals.BigInt64Array(sz));
    $rt_createLongArrayFromData = data => {
        let buffer = new teavm_globals.BigInt64Array(data.length);
        buffer.set(data);
        return new $rt_longArrayCls(buffer);
    }
}

let $rt_createCharArray = sz => new $rt_charArrayCls(new teavm_globals.Uint16Array(sz));
let $rt_createCharArrayFromData = data => {
    let buffer = new teavm_globals.Uint16Array(data.length);
    buffer.set(data);
    return new $rt_charArrayCls(buffer);
}
let $rt_createByteArray = sz => new $rt_byteArrayCls(new teavm_globals.Int8Array(sz));
let $rt_createByteArrayFromData = data => {
    let buffer = new teavm_globals.Int8Array(data.length);
    buffer.set(data);
    return new $rt_byteArrayCls(buffer);
}
let $rt_createShortArray = sz => new $rt_shortArrayCls(new teavm_globals.Int16Array(sz));
let $rt_createShortArrayFromData = data => {
    let buffer = new teavm_globals.Int16Array(data.length);
    buffer.set(data);
    return new $rt_shortArrayCls(buffer);
}
let $rt_createIntArray = sz => new $rt_intArrayCls(new teavm_globals.Int32Array(sz));
let $rt_createIntArrayFromData = data => {
    let buffer = new teavm_globals.Int32Array(data.length);
    buffer.set(data);
    return new $rt_intArrayCls(buffer);
}
let $rt_createBooleanArray = sz => new $rt_booleanArrayCls(new teavm_globals.Int8Array(sz));
let $rt_createBooleanArrayFromData = data => {
    let buffer = new teavm_globals.Int8Array(data.length);
    buffer.set(data);
    return new $rt_booleanArrayCls(buffer);
}

let $rt_createFloatArray = sz => new $rt_floatArrayCls(new teavm_globals.Float32Array(sz));
let $rt_createFloatArrayFromData = data => {
    let buffer = new teavm_globals.Float32Array(data.length);
    buffer.set(data);
    return new $rt_floatArrayCls(buffer);
}
let $rt_createDoubleArray = sz => new $rt_doubleArrayCls(new teavm_globals.Float64Array(sz));
let $rt_createDoubleArrayFromData = data => {
    let buffer = new teavm_globals.Float64Array(data.length);
    buffer.set(data);
    return new $rt_doubleArrayCls(buffer);
}

let $rt_arraycls = cls => {
    let result = cls.$array;
    if (result === null) {
        function JavaArray(data) {
            $rt_objcls().call(this);
            this.data = data;
        }
        JavaArray.prototype = teavm_globals.Object.create($rt_objcls().prototype);
        JavaArray.prototype.type = cls;
        JavaArray.prototype.constructor = JavaArray;
        JavaArray.prototype.toString = function() {
            let str = "[";
            for (let i = 0; i < this.data.length; ++i) {
                if (i > 0) {
                    str += ", ";
                }
                str += this.data[i].toString();
            }
            str += "]";
            return str;
        };
        JavaArray.prototype[teavm_javaVirtualMethod('clone()Ljava/lang/Object;')] = function() {
            let dataCopy;
            if ('slice' in this.data) {
                dataCopy = this.data.slice();
            } else {
                dataCopy = new this.data.constructor(this.data.length);
                for (let i = 0; i < dataCopy.length; ++i) {
                    dataCopy[i] = this.data[i];
                }
            }
            return new ($rt_arraycls(this.type))(dataCopy);
        };
        let name = "[" + cls.$meta.binaryName;
        JavaArray.$meta = {
            item: cls,
            supertypes: [$rt_objcls()],
            primitive: false,
            superclass: $rt_objcls(),
            name: name,
            binaryName: name,
            enum: false,
            simpleName: null,
            declaringClass: null,
            enclosingClass: null
        };
        JavaArray.classObject = null;
        JavaArray.$array = null;

        result = JavaArray;
        cls.$array = JavaArray;
    }
    return result;
}
let $rt_createMultiArray = (cls, dimensions) => {
    let first = 0;
    for (let i = dimensions.length - 1; i >= 0; i = (i - 1) | 0) {
        if (dimensions[i] === 0) {
            first = i;
            break;
        }
    }
    if (first > 0) {
        for (let i = 0; i < first; i = (i + 1) | 0) {
            cls = $rt_arraycls(cls);
        }
        if (first === dimensions.length - 1) {
            return $rt_createArray(cls, dimensions[first]);
        }
    }
    let arrays = new teavm_globals.Array($rt_primitiveArrayCount(dimensions, first));
    let firstDim = dimensions[first] | 0;
    for (let i = 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createArray(cls, firstDim);
    }
    return $rt_createMultiArrayImpl(cls, arrays, dimensions, first);
}
let $rt_createByteMultiArray = dimensions => {
    let arrays = new teavm_globals.Array($rt_primitiveArrayCount(dimensions, 0));
    if (arrays.length === 0) {
        return $rt_createMultiArray($rt_bytecls, dimensions);
    }
    let firstDim = dimensions[0] | 0;
    for (let i = 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createByteArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_bytecls, arrays, dimensions);
}
let $rt_createCharMultiArray = dimensions => {
    let arrays = new teavm_globals.Array($rt_primitiveArrayCount(dimensions, 0));
    if (arrays.length === 0) {
        return $rt_createMultiArray($rt_charcls, dimensions);
    }
    let firstDim = dimensions[0] | 0;
    for (let i = 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createCharArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_charcls, arrays, dimensions, 0);
}
let $rt_createBooleanMultiArray = dimensions => {
    let arrays = new teavm_globals.Array($rt_primitiveArrayCount(dimensions, 0));
    if (arrays.length === 0) {
        return $rt_createMultiArray($rt_booleancls, dimensions);
    }
    let firstDim = dimensions[0] | 0;
    for (let i = 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createBooleanArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_booleancls, arrays, dimensions, 0);
}
let $rt_createShortMultiArray = dimensions => {
    let arrays = new teavm_globals.Array($rt_primitiveArrayCount(dimensions, 0));
    if (arrays.length === 0) {
        return $rt_createMultiArray($rt_shortcls, dimensions);
    }
    let firstDim = dimensions[0] | 0;
    for (let i = 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createShortArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_shortcls, arrays, dimensions, 0);
}
let $rt_createIntMultiArray = dimensions => {
    let arrays = new teavm_globals.Array($rt_primitiveArrayCount(dimensions, 0));
    if (arrays.length === 0) {
        return $rt_createMultiArray($rt_intcls, dimensions);
    }
    let firstDim = dimensions[0] | 0;
    for (let i = 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createIntArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_intcls, arrays, dimensions, 0);
}
let $rt_createLongMultiArray = dimensions => {
    let arrays = new teavm_globals.Array($rt_primitiveArrayCount(dimensions, 0));
    if (arrays.length === 0) {
        return $rt_createMultiArray($rt_longcls, dimensions);
    }
    let firstDim = dimensions[0] | 0;
    for (let i = 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createLongArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_longcls, arrays, dimensions, 0);
}
let $rt_createFloatMultiArray = dimensions => {
    let arrays = new teavm_globals.Array($rt_primitiveArrayCount(dimensions, 0));
    if (arrays.length === 0) {
        return $rt_createMultiArray($rt_floatcls, dimensions);
    }
    let firstDim = dimensions[0] | 0;
    for (let i = 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createFloatArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_floatcls, arrays, dimensions, 0);
}
let $rt_createDoubleMultiArray = dimensions => {
    let arrays = new teavm_globals.Array($rt_primitiveArrayCount(dimensions, 0));
    if (arrays.length === 0) {
        return $rt_createMultiArray($rt_doublecls, dimensions);
    }
    let firstDim = dimensions[0] | 0;
    for (let i = 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createDoubleArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_doublecls, arrays, dimensions, 0);
}
let $rt_primitiveArrayCount = (dimensions, start) => {
    let val = dimensions[start + 1] | 0;
    for (let i = start + 2; i < dimensions.length; i = (i + 1) | 0) {
        val = (val * (dimensions[i] | 0)) | 0;
        if (val === 0) {
            break;
        }
    }
    return val;
}
let $rt_createMultiArrayImpl = (cls, arrays, dimensions, start) => {
    let limit = arrays.length;
    for (let i = (start + 1) | 0; i < dimensions.length; i = (i + 1) | 0) {
        cls = $rt_arraycls(cls);
        let dim = dimensions[i];
        let index = 0;
        let packedIndex = 0;
        while (index < limit) {
            let arr = $rt_createUnfilledArray(cls, dim);
            for (let j = 0; j < dim; j = (j + 1) | 0) {
                arr.data[j] = arrays[index];
                index = (index + 1) | 0;
            }
            arrays[packedIndex] = arr;
            packedIndex = (packedIndex + 1) | 0;
        }
        limit = packedIndex;
    }
    return arrays[0];
}
