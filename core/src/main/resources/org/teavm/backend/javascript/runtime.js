/*
 *  Copyright 2013 Alexey Andreev.
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
let $rt_seed = 2463534242;
let $rt_nextId = () => {
    let x = $rt_seed;
    x ^= x << 13;
    x ^= x >>> 17;
    x ^= x << 5;
    $rt_seed = x;
    return x;
}
let $rt_compare = (a, b) => a > b ? 1 : a < b ? -1 : a === b ? 0 : 1;
let $rt_isInstance = (obj, cls) => obj instanceof $rt_objcls() && !!obj.constructor.$meta
        && $rt_isAssignable(obj.constructor, cls);
let $rt_isAssignable = (from, to) => {
    if (from === to) {
        return true;
    }
    if (to.$meta.item !== null) {
        return from.$meta.item !== null && $rt_isAssignable(from.$meta.item, to.$meta.item);
    }
    let supertypes = from.$meta.supertypes;
    for (let i = 0; i < supertypes.length; i = (i + 1) | 0) {
        if ($rt_isAssignable(supertypes[i], to)) {
            return true;
        }
    }
    return false;
}
let $rt_castToInterface = (obj, cls) => {
    if (obj !== null && !$rt_isInstance(obj, cls)) {
        $rt_throwCCE();
    }
    return obj;
}
let $rt_castToClass = (obj, cls) => {
    if (obj !== null && !(obj instanceof cls)) {
        $rt_throwCCE();
    }
    return obj;
}
let $rt_createArray = (cls, sz) => {
    let data = new teavm_globals.Array(sz);
    data.fill(null);
    return new ($rt_arraycls(cls))(data);
}
let $rt_createArrayFromData = (cls, init) => $rt_wrapArray(cls, init);
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
        $rt_setCloneMethod(JavaArray.prototype, function() {
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
        });
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
let $rt_createcls = () => {
    return {
        $array : null,
        classObject : null,
        $meta: {
            supertypes : [],
            superclass : null
        }
    };
}
let $rt_createPrimitiveCls = (name, binaryName) => {
    let cls = $rt_createcls();
    cls.$meta.primitive = true;
    cls.$meta.name = name;
    cls.$meta.binaryName = binaryName;
    cls.$meta.enum = false;
    cls.$meta.item = null;
    cls.$meta.simpleName = null;
    cls.$meta.declaringClass = null;
    cls.$meta.enclosingClass = null;
    return cls;
}
let $rt_booleancls = $rt_createPrimitiveCls("boolean", "Z");
let $rt_charcls = $rt_createPrimitiveCls("char", "C");
let $rt_bytecls = $rt_createPrimitiveCls("byte", "B");
let $rt_shortcls = $rt_createPrimitiveCls("short", "S");
let $rt_intcls = $rt_createPrimitiveCls("int", "I");
let $rt_longcls = $rt_createPrimitiveCls("long", "J");
let $rt_floatcls = $rt_createPrimitiveCls("float", "F");
let $rt_doublecls = $rt_createPrimitiveCls("double", "D");
let $rt_voidcls = $rt_createPrimitiveCls("void", "V");
let $rt_throw = ex => {
    throw $rt_exception(ex)
};
let $rt_javaExceptionProp = teavm_globals.Symbol("javaException")
let $rt_exception = ex => {
    let err = ex.$jsException;
    if (!err) {
        let javaCause = $rt_throwableCause(ex);
        let jsCause = javaCause !== null ? javaCause.$jsException : void 0;
        let cause = typeof jsCause === "object" ? { cause : jsCause } : void 0;
        err = new JavaError("Java exception thrown", cause);
        if (typeof teavm_globals.Error.captureStackTrace === "function") {
            teavm_globals.Error.captureStackTrace(err);
        }
        err[$rt_javaExceptionProp] = ex;
        ex.$jsException = err;
        $rt_fillStack(err, ex);
    }
    return err;
}
let $rt_fillStack = (err, ex) => {
    if (typeof $rt_decodeStack === "function" && err.stack) {
        let stack = $rt_decodeStack(err.stack);
        let javaStack = $rt_createArray($rt_stecls(), stack.length);
        let elem;
        let noStack = false;
        for (let i = 0; i < stack.length; ++i) {
            let element = stack[i];
            elem = $rt_createStackElement($rt_str(element.className),
                $rt_str(element.methodName), $rt_str(element.fileName), element.lineNumber);
            if (elem == null) {
                noStack = true;
                break;
            }
            javaStack.data[i] = elem;
        }
        if (!noStack) {
            $rt_setStack(ex, javaStack);
        }
    }
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
let $rt_assertNotNaN = value => {
    if (typeof value === 'number' && isNaN(value)) {
        throw "NaN";
    }
    return value;
}
let $rt_createOutputFunction = printFunction => {
    let buffer = "";
    let utf8Buffer = 0;
    let utf8Remaining = 0;

    let putCodePoint = ch =>{
        if (ch === 0xA) {
            printFunction(buffer);
            buffer = "";
        } else if (ch < 0x10000) {
            buffer += String.fromCharCode(ch);
        } else {
            ch = (ch - 0x10000) | 0;
            let hi = (ch >> 10) + 0xD800;
            let lo = (ch & 0x3FF) + 0xDC00;
            buffer += String.fromCharCode(hi, lo);
        }
    }

    return ch => {
        if ((ch & 0x80) === 0) {
            putCodePoint(ch);
        } else if ((ch & 0xC0) === 0x80) {
            if (utf8Buffer > 0) {
                utf8Remaining <<= 6;
                utf8Remaining |= ch & 0x3F;
                if (--utf8Buffer === 0) {
                    putCodePoint(utf8Remaining);
                }
            }
        } else if ((ch & 0xE0) === 0xC0) {
            utf8Remaining = ch & 0x1F;
            utf8Buffer = 1;
        } else if ((ch & 0xF0) === 0xE0) {
            utf8Remaining = ch & 0x0F;
            utf8Buffer = 2;
        } else if ((ch & 0xF8) === 0xF0) {
            utf8Remaining = ch & 0x07;
            utf8Buffer = 3;
        }
    };
}

let $rt_putStdout = typeof teavm_globals.$rt_putStdoutCustom === "function"
    ? teavm_globals.$rt_putStdoutCustom
    : typeof console === "object" ? $rt_createOutputFunction(function(msg) { console.info(msg); }) : function() {};
let $rt_putStderr = typeof teavm_globals.$rt_putStderrCustom === "function"
    ? teavm_globals.$rt_putStderrCustom
    : typeof console === "object" ? $rt_createOutputFunction(function(msg) { console.error(msg); }) : function() {};

let $rt_packageData = null;
let $rt_packages = data => {
    let i = 0;
    let packages = new teavm_globals.Array(data.length);
    for (let j = 0; j < data.length; ++j) {
        let prefixIndex = data[i++];
        let prefix = prefixIndex >= 0 ? packages[prefixIndex] : "";
        packages[j] = prefix + data[i++] + ".";
    }
    $rt_packageData = packages;
}
let $rt_metadata = data => {
    let packages = $rt_packageData;
    let i = 0;
    while (i < data.length) {
        let cls = data[i++];
        cls.$meta = {};
        let m = cls.$meta;
        let className = data[i++];

        m.name = className !== 0 ? className : null;
        if (m.name !== null) {
            let packageIndex = data[i++];
            if (packageIndex >= 0) {
                m.name = packages[packageIndex] + m.name;
            }
        }

        m.binaryName = "L" + m.name + ";";
        let superclass = data[i++];
        m.superclass = superclass !== 0 ? superclass : null;
        m.supertypes = data[i++];
        if (m.superclass) {
            m.supertypes.push(m.superclass);
            cls.prototype = teavm_globals.Object.create(m.superclass.prototype);
        } else {
            cls.prototype = {};
        }
        let flags = data[i++];
        m.enum = (flags & 8) !== 0;
        m.flags = flags;
        m.primitive = false;
        m.item = null;
        cls.prototype.constructor = cls;
        cls.classObject = null;

        m.accessLevel = data[i++];

        let innerClassInfo = data[i++];
        if (innerClassInfo === 0) {
            m.simpleName = null;
            m.declaringClass = null;
            m.enclosingClass = null;
        } else {
            let enclosingClass = innerClassInfo[0];
            m.enclosingClass = enclosingClass !== 0 ? enclosingClass : null;
            let declaringClass = innerClassInfo[1];
            m.declaringClass = declaringClass !== 0 ? declaringClass : null;
            let simpleName = innerClassInfo[2];
            m.simpleName = simpleName !== 0 ? simpleName : null;
        }

        let clinit = data[i++];
        cls.$clinit = clinit !== 0 ? clinit : function() {};

        let virtualMethods = data[i++];
        if (virtualMethods !== 0) {
            for (let j = 0; j < virtualMethods.length; j += 2) {
                let name = virtualMethods[j];
                let func = virtualMethods[j + 1];
                if (typeof name === 'string') {
                    name = [name];
                }
                for (let k = 0; k < name.length; ++k) {
                    cls.prototype[name[k]] = func;
                }
            }
        }

        cls.$array = null;
    }
}
let $rt_wrapFunction0 = f => function() {
    return f(this);
}
let $rt_wrapFunction1 = f => function(p1) {
    return f(this, p1);
}
let $rt_wrapFunction2 = f => function (p1, p2) {
    return f(this, p1, p2);
}
let $rt_wrapFunction3 = f => function(p1, p2, p3) {
    return f(this, p1, p2, p3, p3);
}
let $rt_wrapFunction4 = f => function(p1, p2, p3, p4) {
    return f(this, p1, p2, p3, p4);
}
let $rt_threadStarter = f => function() {
    let args = teavm_globals.Array.prototype.slice.apply(arguments);
    $rt_startThread(function() {
        f.apply(this, args);
    });
}
let $rt_mainStarter = f => (args, callback) => {
    if (!args) {
        args = [];
    }
    let javaArgs = $rt_createArray($rt_objcls(), args.length);
    for (let i = 0; i < args.length; ++i) {
        javaArgs.data[i] = $rt_str(args[i]);
    }
    $rt_startThread(() => { f.call(null, javaArgs); }, callback);
}
let $rt_stringPool_instance;
let $rt_stringPool = strings => {
    $rt_stringClassInit();
    $rt_stringPool_instance = new teavm_globals.Array(strings.length);
    for (let i = 0; i < strings.length; ++i) {
        $rt_stringPool_instance[i] = $rt_intern($rt_str(strings[i]));
    }
}
let $rt_s = index => $rt_stringPool_instance[index];
let $rt_eraseClinit = target => target.$clinit = () => {};

let $rt_numberConversionBuffer = new teavm_globals.ArrayBuffer(16);
let $rt_numberConversionView = new teavm_globals.DataView($rt_numberConversionBuffer);
let $rt_numberConversionFloatArray = new teavm_globals.Float32Array($rt_numberConversionBuffer);
let $rt_numberConversionDoubleArray = new teavm_globals.Float64Array($rt_numberConversionBuffer);
let $rt_numberConversionIntArray = new teavm_globals.Int32Array($rt_numberConversionBuffer);

let $rt_doubleToRawLongBits;
let $rt_longBitsToDouble;
if (typeof teavm_globals.BigInt !== 'function') {
    $rt_doubleToRawLongBits = n => {
        $rt_numberConversionView.setFloat64(0, n, true);
        return new Long($rt_numberConversionView.getInt32(0, true), $rt_numberConversionView.getInt32(4, true));
    }
    $rt_longBitsToDouble = n => {
        $rt_numberConversionView.setInt32(0, n.lo, true);
        $rt_numberConversionView.setInt32(4, n.hi, true);
        return $rt_numberConversionView.getFloat64(0, true);
    }
} else if (typeof teavm_globals.BigInt64Array !== 'function') {
    $rt_doubleToRawLongBits = n => {
        $rt_numberConversionView.setFloat64(0, n, true);
        let lo = $rt_numberConversionView.getInt32(0, true);
        let hi = $rt_numberConversionView.getInt32(4, true);
        return teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt.asUintN(32, teavm_globals.BigInt(lo))
            | (teavm_globals.BigInt(hi) << teavm_globals.BigInt(32)));
    }
    $rt_longBitsToDouble = n => {
        $rt_numberConversionView.setFloat64(0, n, true);
        let lo = $rt_numberConversionView.getInt32(0, true);
        let hi = $rt_numberConversionView.getInt32(4, true);
        return teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt.asUintN(32, teavm_globals.BigInt(lo))
            | (teavm_globals.BigInt(hi) << teavm_globals.BigInt(32)));
    }
} else {
    let $rt_numberConversionLongArray = new teavm_globals.BigInt64Array($rt_numberConversionBuffer);
    $rt_doubleToRawLongBits = n => {
        $rt_numberConversionDoubleArray[0] = n;
        return $rt_numberConversionLongArray[0];
    }
    $rt_longBitsToDouble = n => {
        $rt_numberConversionLongArray[0] = n;
        return $rt_numberConversionDoubleArray[0];
    }
}

let $rt_floatToRawIntBits = n => {
    $rt_numberConversionFloatArray[0] = n;
    return $rt_numberConversionIntArray[0];
}
let $rt_intBitsToFloat = n => {
    $rt_numberConversionIntArray[0] = n;
    return $rt_numberConversionFloatArray[0];
}
let $rt_equalDoubles = (a, b) => {
    if (a !== a) {
        return b !== b;
    }
    $rt_numberConversionDoubleArray[0] = a;
    $rt_numberConversionDoubleArray[1] = b;
    return $rt_numberConversionIntArray[0] === $rt_numberConversionIntArray[2]
            && $rt_numberConversionIntArray[1] === $rt_numberConversionIntArray[3];
}

let JavaError;
if (typeof Reflect === 'object') {
    let defaultMessage = teavm_globals.Symbol("defaultMessage");
    JavaError = function JavaError(message, cause) {
        let self = teavm_globals.Reflect.construct(teavm_globals.Error, [void 0, cause], JavaError);
        teavm_globals.Object.setPrototypeOf(self, JavaError.prototype);
        self[defaultMessage] = message;
        return self;
    }
    JavaError.prototype = teavm_globals.Object.create(teavm_globals.Error.prototype, {
        constructor: {
            configurable: true,
            writable: true,
            value: JavaError
        },
        message: {
            get: () => {
                try {
                    let javaException = this[$rt_javaExceptionProp];
                    if (typeof javaException === 'object') {
                        let javaMessage = $rt_throwableMessage(javaException);
                        if (typeof javaMessage === "object") {
                            return javaMessage !== null ? javaMessage.toString() : null;
                        }
                    }
                    return this[defaultMessage];
                } catch (e) {
                    return "Exception occurred trying to extract Java exception message: " + e
                }
            }
        }
    });
} else {
    JavaError = teavm_globals.Error;
}

let $rt_javaException = e => e instanceof teavm_globals.Error && typeof e[$rt_javaExceptionProp] === 'object'
        ? e[$rt_javaExceptionProp]
        : null;

let $rt_jsException = e => typeof e.$jsException === 'object' ? e.$jsException : null;
let $rt_wrapException = err => {
    let ex = err[$rt_javaExceptionProp];
    if (!ex) {
        ex = $rt_createException($rt_str("(JavaScript) " + err.toString()));
        err[$rt_javaExceptionProp] = ex;
        ex.$jsException = err;
        $rt_fillStack(err, ex);
    }
    return ex;
}

let $dbg_class = obj => {
    let cls = obj.constructor;
    let arrayDegree = 0;
    while (cls.$meta && cls.$meta.item) {
        ++arrayDegree;
        cls = cls.$meta.item;
    }
    let clsName = "";
    if (cls === $rt_booleancls) {
        clsName = "boolean";
    } else if (cls === $rt_bytecls) {
        clsName = "byte";
    } else if (cls === $rt_shortcls) {
        clsName = "short";
    } else if (cls === $rt_charcls) {
        clsName = "char";
    } else if (cls === $rt_intcls) {
        clsName = "int";
    } else if (cls === $rt_longcl) {
        clsName = "long";
    } else if (cls === $rt_floatcls) {
        clsName = "float";
    } else if (cls === $rt_doublecls) {
        clsName = "double";
    } else {
        clsName = cls.$meta ? (cls.$meta.name || ("a/" + cls.name)) : "@" + cls.name;
    }
    while (arrayDegree-- > 0) {
        clsName += "[]";
    }
    return clsName;
}

function Long(lo, hi) {
    this.lo = lo | 0;
    this.hi = hi | 0;
}
Long.prototype.__teavm_class__ = () => {
    return "long";
};
let Long_isPositive = a => (a.hi & 0x80000000) === 0;
let Long_isNegative = a => (a.hi & 0x80000000) !== 0;

let Long_MAX_NORMAL = 1 << 18;
let Long_ZERO;
let Long_create;
let Long_fromInt;
let Long_fromNumber;
let Long_toNumber;
let Long_hi;
let Long_lo;
if (typeof teavm_globals.BigInt !== "function") {
    Long.prototype.toString = function() {
        let result = [];
        let n = this;
        let positive = Long_isPositive(n);
        if (!positive) {
            n = Long_neg(n);
        }
        let radix = new Long(10, 0);
        do {
            let divRem = Long_divRem(n, radix);
            result.push(teavm_globals.String.fromCharCode(48 + divRem[1].lo));
            n = divRem[0];
        } while (n.lo !== 0 || n.hi !== 0);
        result = result.reverse().join('');
        return positive ? result : "-" + result;
    };
    Long.prototype.valueOf = function() {
        return Long_toNumber(this);
    };

    Long_ZERO = new Long(0, 0);
    Long_fromInt = val =>new Long(val, (-(val < 0)) | 0);
    Long_fromNumber = val => val >= 0
            ? new Long(val | 0, (val / 0x100000000) | 0)
            : Long_neg(new Long(-val | 0, (-val / 0x100000000) | 0));

    Long_create = (lo, hi) => new Long(lo, hi);
    Long_toNumber = val => 0x100000000 * val.hi + (val.lo >>> 0);
    Long_hi = val => val.hi;
    Long_lo = val => val.lo;
} else {
    Long_ZERO = teavm_globals.BigInt(0);
    Long_create = (lo, hi) => teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt.asUintN(64, teavm_globals.BigInt(lo))
            | teavm_globals.BigInt.asUintN(64, (teavm_globals.BigInt(hi) << teavm_globals.BigInt(32))));
    Long_fromInt = val => teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt(val | 0));
    Long_fromNumber = val =>  teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt(
            val >= 0 ? teavm_globals.Math.floor(val) : teavm_globals.Math.ceil(val)));
    Long_toNumber = val => teavm_globals.Number(val);
    Long_hi = val => teavm_globals.Number(teavm_globals.BigInt.asIntN(64, val >> teavm_globals.BigInt(32))) | 0;
    Long_lo = val => teavm_globals.Number(teavm_globals.BigInt.asIntN(32, val)) | 0;
}
let $rt_imul = teavm_globals.Math.imul || function(a, b) {
    let ah = (a >>> 16) & 0xFFFF;
    let al = a & 0xFFFF;
    let bh = (b >>> 16) & 0xFFFF;
    let bl = b & 0xFFFF;
    return (al * bl + (((ah * bl + al * bh) << 16) >>> 0)) | 0;
};
let $rt_udiv = (a, b) => ((a >>> 0) / (b >>> 0)) >>> 0;
let $rt_umod = (a, b) => ((a >>> 0) % (b >>> 0)) >>> 0;
let $rt_ucmp = (a, b) => {
    a >>>= 0;
    b >>>= 0;
    return a < b ? -1 : a > b ? 1 : 0;
};
let $rt_checkBounds = (index, array) => {
    if (index < 0 || index >= array.length) {
        $rt_throwAIOOBE();
    }
    return index;
}
let $rt_checkUpperBound = (index, array) => {
    if (index >= array.length) {
        $rt_throwAIOOBE();
    }
    return index;
}
let $rt_checkLowerBound = index => {
    if (index < 0) {
        $rt_throwAIOOBE();
    }
    return index;
}
let $rt_classWithoutFields = superclass => {
    if (superclass === 0) {
        return function() {};
    }
    if (superclass === void 0) {
        superclass = $rt_objcls();
    }
    return function() {
        superclass.call(this);
    };
}
let $rt_charArrayToString = (array, offset, count) => {
    let result = "";
    let limit = offset + count;
    for (let i = offset; i < limit; i = (i + 1024) | 0) {
        let next = teavm_globals.Math.min(limit, (i + 1024) | 0);
        result += teavm_globals.String.fromCharCode.apply(null, array.subarray(i, next));
    }
    return result;
}
let $rt_fullArrayToString = (array) => $rt_charArrayToString(array, 0, array.length);
let $rt_stringToCharArray = (string, begin, dst, dstBegin, count) => {
    for (let i = 0; i < count; i = (i + 1) | 0) {
        dst[dstBegin + i] = string.charCodeAt(begin + i);
    }
}
let $rt_fastStringToCharArray = string => {
    let array = new teavm_globals.Uint16Array(string.length);
    for (let i = 0; i < array.length; ++i) {
        array[i] = string.charCodeAt(i);
    }
    return new $rt_charArrayCls(array);
}
let $rt_substring = (string, start, end) => {
    if (start === 0 && end === string.length) {
        return string;
    }
    let result = start.substring(start, end - 1) + start.substring(end - 1, end);
    $rt_substringSink = ($rt_substringSink + result.charCodeAt(result.length - 1)) | 0;
}
let $rt_substringSink = 0;

let $rt_setCloneMethod = (target, method) => target[teavm_javaVirtualMethod('clone()Ljava/lang/Object;')] = method;

let $rt_cls = (cls) => teavm_javaMethod("java.lang.Class",
        "getClass(Lorg/teavm/platform/PlatformClass;)Ljava/lang/Class;")(cls);
let $rt_str = str =>  str === null ? null : teavm_javaConstructor("java.lang.String", "(Ljava/lang/Object;)V")(str);
let $rt_ustr = str =>  str === null ? null : str[teavm_javaField("java.lang.String", "nativeString")];
let $rt_nullCheck = val => {
    if (val === null) {
        $rt_throw(teavm_javaConstructor("java.lang.NullPointerException", "()V")());
    }
    return val;
}
let $rt_stringClassInit = () => teavm_javaClassInit("java.lang.String")();
let $rt_objcls = () => teavm_javaClass("java.lang.Object");
let $rt_createException = message => teavm_javaConstructor("java.lang.RuntimeException",
    "(Ljava/lang/String;)V")(message);
let $rt_throwableMessage = t => teavm_javaMethod("java.lang.Throwable", "getMessage()Ljava/lang/String;")(t);
let $rt_throwableCause = t => teavm_javaMethod("java.lang.Throwable", "getCause()Ljava/lang/Throwable;")(t);
let $rt_stecls = () => teavm_javaClassExists("java.lang.StackTraceElement")
        ? teavm_javaClass("java.lang.StackTraceElement")
        : $rt_objcls();

let $rt_throwAIOOBE = () => teavm_javaConstructorExists("java.lang.ArrayIndexOutOfBoundsException", "()V")
        ? $rt_throw(teavm_javaConstructor("java.lang.ArrayIndexOutOfBoundsException", "()V")())
        : $rt_throw($rt_createException($rt_str("")));

let $rt_throwCCE = () => teavm_javaConstructorExists("java.lang.ClassCastException", "()V")
        ? $rt_throw(teavm_javaConstructor("java.lang.ClassCastException", "()V")())
        : $rt_throw($rt_createException($rt_str("")));

let $rt_getThread = () => {
    if (teavm_javaMethodExists("java.lang.Thread", "currentThread()Ljava/lang/Thread;")) {
        return teavm_javaMethod("java.lang.Thread", "currentThread()Ljava/lang/Thread;")();
    }
}
let $rt_setThread = t => {
    if (teavm_javaMethodExists("java.lang.Thread", "setCurrentThread(Ljava/lang/Thread;)V")) {
        return teavm_javaMethod("java.lang.Thread", "setCurrentThread(Ljava/lang/Thread;)V")(t);
    }
}
let $rt_createStackElement = (className, methodName, fileName, lineNumber) => {
    if (teavm_javaConstructorExists("java.lang.StackTraceElement",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V")) {
        return teavm_javaConstructor("java.lang.StackTraceElement",
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V")(className, methodName, fileName, lineNumber);
    } else {
        return null;
    }
}
let $rt_setStack = (e, stack) => {
    if (teavm_javaMethodExists("java.lang.Throwable", "setStackTrace([Ljava/lang/StackTraceElement;)V")) {
        teavm_javaMethod("java.lang.Throwable", "setStackTrace([Ljava/lang/StackTraceElement;)V")(e, stack);
    }
}