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
var $rt_seed = 2463534242;
function $rt_nextId() {
    var x = $rt_seed;
    x ^= x << 13;
    x ^= x >>> 17;
    x ^= x << 5;
    $rt_seed = x;
    return x;
}
function $rt_compare(a, b) {
    return a > b ? 1 : a < b ? -1 : a === b ? 0 : 1;
}
function $rt_isInstance(obj, cls) {
    return obj instanceof $rt_objcls() && !!obj.constructor.$meta && $rt_isAssignable(obj.constructor, cls);
}
function $rt_isAssignable(from, to) {
    if (from === to) {
        return true;
    }
    if (to.$meta.item !== null) {
        return from.$meta.item !== null && $rt_isAssignable(from.$meta.item, to.$meta.item);
    }
    var supertypes = from.$meta.supertypes;
    for (var i = 0; i < supertypes.length; i = (i + 1) | 0) {
        if ($rt_isAssignable(supertypes[i], to)) {
            return true;
        }
    }
    return false;
}
function $rt_castToInterface(obj, cls) {
    if (obj !== null && !$rt_isInstance(obj, cls)) {
        $rt_throwCCE();
    }
    return obj;
}
function $rt_castToClass(obj, cls) {
    if (obj !== null && !(obj instanceof cls)) {
        $rt_throwCCE();
    }
    return obj;
}
function $rt_createArray(cls, sz) {
    var data = new teavm_globals.Array(sz);
    data.fill(null);
    return new ($rt_arraycls(cls))(data);
}
function $rt_createArrayFromData(cls, init) {
    return $rt_wrapArray(cls, init);
}
function $rt_wrapArray(cls, data) {
    return new ($rt_arraycls(cls))(data);
}
function $rt_createUnfilledArray(cls, sz) {
    return new ($rt_arraycls(cls))(new teavm_globals.Array(sz));
}
var $rt_createLongArray;
var $rt_createLongArrayFromData;
if (typeof teavm_globals.BigInt64Array !== 'function') {
    $rt_createLongArray = function(sz) {
        var data = new teavm_globals.Array(sz);
        var arr = new $rt_longArrayCls(data);
        data.fill(Long_ZERO);
        return arr;
    }
    $rt_createLongArrayFromData = function(init) {
        return new $rt_longArrayCls(init);
    }
} else {
    $rt_createLongArray = function (sz) {
        return new $rt_longArrayCls(new teavm_globals.BigInt64Array(sz));
    }
    $rt_createLongArrayFromData = function(data) {
        var buffer = new teavm_globals.BigInt64Array(data.length);
        buffer.set(data);
        return new $rt_longArrayCls(buffer);
    }
}

function $rt_createCharArray(sz) {
    return new $rt_charArrayCls(new teavm_globals.Uint16Array(sz));
}
function $rt_createCharArrayFromData(data) {
    var buffer = new teavm_globals.Uint16Array(data.length);
    buffer.set(data);
    return new $rt_charArrayCls(buffer);
}
function $rt_createByteArray(sz) {
    return new $rt_byteArrayCls(new teavm_globals.Int8Array(sz));
}
function $rt_createByteArrayFromData(data) {
    var buffer = new teavm_globals.Int8Array(data.length);
    buffer.set(data);
    return new $rt_byteArrayCls(buffer);
}
function $rt_createShortArray(sz) {
    return new $rt_shortArrayCls(new teavm_globals.Int16Array(sz));
}
function $rt_createShortArrayFromData(data) {
    var buffer = new teavm_globals.Int16Array(data.length);
    buffer.set(data);
    return new $rt_shortArrayCls(buffer);
}
function $rt_createIntArray(sz) {
    return new $rt_intArrayCls(new teavm_globals.Int32Array(sz));
}
function $rt_createIntArrayFromData(data) {
    var buffer = new teavm_globals.Int32Array(data.length);
    buffer.set(data);
    return new $rt_intArrayCls(buffer);
}
function $rt_createBooleanArray(sz) {
    return new $rt_booleanArrayCls(new teavm_globals.Int8Array(sz));
}
function $rt_createBooleanArrayFromData(data) {
    var buffer = new teavm_globals.Int8Array(data.length);
    buffer.set(data);
    return new $rt_booleanArrayCls(buffer);
}

function $rt_createFloatArray(sz) {
    return new $rt_floatArrayCls(new teavm_globals.Float32Array(sz));
}
function $rt_createFloatArrayFromData(data) {
    var buffer = new teavm_globals.Float32Array(data.length);
    buffer.set(data);
    return new $rt_floatArrayCls(buffer);
}
function $rt_createDoubleArray(sz) {
    return new $rt_doubleArrayCls(new teavm_globals.Float64Array(sz));
}
function $rt_createDoubleArrayFromData(data) {
    var buffer = new teavm_globals.Float64Array(data.length);
    buffer.set(data);
    return new $rt_doubleArrayCls(buffer);
}

function $rt_arraycls(cls) {
    var result = cls.$array;
    if (result === null) {
        function JavaArray(data) {
            $rt_objcls().call(this);
            this.data = data;
        }
        JavaArray.prototype = teavm_globals.Object.create($rt_objcls().prototype);
        JavaArray.prototype.type = cls;
        JavaArray.prototype.constructor = JavaArray;
        JavaArray.prototype.toString = function() {
            var str = "[";
            for (var i = 0; i < this.data.length; ++i) {
                if (i > 0) {
                    str += ", ";
                }
                str += this.data[i].toString();
            }
            str += "]";
            return str;
        };
        $rt_setCloneMethod(JavaArray.prototype, function () {
            var dataCopy;
            if ('slice' in this.data) {
                dataCopy = this.data.slice();
            } else {
                dataCopy = new this.data.constructor(this.data.length);
                for (var i = 0; i < dataCopy.length; ++i) {
                    dataCopy[i] = this.data[i];
                }
            }
            return new ($rt_arraycls(this.type))(dataCopy);
        });
        var name = "[" + cls.$meta.binaryName;
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
function $rt_createcls() {
    return {
        $array : null,
        classObject : null,
        $meta: {
            supertypes : [],
            superclass : null
        }
    };
}
function $rt_createPrimitiveCls(name, binaryName) {
    var cls = $rt_createcls();
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
var $rt_booleancls = $rt_createPrimitiveCls("boolean", "Z");
var $rt_charcls = $rt_createPrimitiveCls("char", "C");
var $rt_bytecls = $rt_createPrimitiveCls("byte", "B");
var $rt_shortcls = $rt_createPrimitiveCls("short", "S");
var $rt_intcls = $rt_createPrimitiveCls("int", "I");
var $rt_longcls = $rt_createPrimitiveCls("long", "J");
var $rt_floatcls = $rt_createPrimitiveCls("float", "F");
var $rt_doublecls = $rt_createPrimitiveCls("double", "D");
var $rt_voidcls = $rt_createPrimitiveCls("void", "V");
function $rt_throw(ex) {
    throw $rt_exception(ex);
}
var $rt_javaExceptionProp = teavm_globals.Symbol("javaException")
function $rt_exception(ex) {
    var err = ex.$jsException;
    if (!err) {
        var javaCause = $rt_throwableCause(ex);
        var jsCause = javaCause !== null ? javaCause.$jsException : void 0;
        var cause = typeof jsCause === "object" ? { cause : jsCause } : void 0;
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
function $rt_fillStack(err, ex) {
    if (typeof $rt_decodeStack === "function" && err.stack) {
        var stack = $rt_decodeStack(err.stack);
        var javaStack = $rt_createArray($rt_stecls(), stack.length);
        var elem;
        var noStack = false;
        for (var i = 0; i < stack.length; ++i) {
            var element = stack[i];
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
function $rt_createMultiArray(cls, dimensions) {
    var first = 0;
    for (var i = dimensions.length - 1; i >= 0; i = (i - 1) | 0) {
        if (dimensions[i] === 0) {
            first = i;
            break;
        }
    }
    if (first > 0) {
        for (i = 0; i < first; i = (i + 1) | 0) {
            cls = $rt_arraycls(cls);
        }
        if (first === dimensions.length - 1) {
            return $rt_createArray(cls, dimensions[first]);
        }
    }
    var arrays = new teavm_globals.Array($rt_primitiveArrayCount(dimensions, first));
    var firstDim = dimensions[first] | 0;
    for (i = 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createArray(cls, firstDim);
    }
    return $rt_createMultiArrayImpl(cls, arrays, dimensions, first);
}
function $rt_createByteMultiArray(dimensions) {
    var arrays = new teavm_globals.Array($rt_primitiveArrayCount(dimensions, 0));
    if (arrays.length === 0) {
        return $rt_createMultiArray($rt_bytecls, dimensions);
    }
    var firstDim = dimensions[0] | 0;
    for (var i = 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createByteArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_bytecls, arrays, dimensions);
}
function $rt_createCharMultiArray(dimensions) {
    var arrays = new teavm_globals.Array($rt_primitiveArrayCount(dimensions, 0));
    if (arrays.length === 0) {
        return $rt_createMultiArray($rt_charcls, dimensions);
    }
    var firstDim = dimensions[0] | 0;
    for (var i = 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createCharArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_charcls, arrays, dimensions, 0);
}
function $rt_createBooleanMultiArray(dimensions) {
    var arrays = new teavm_globals.Array($rt_primitiveArrayCount(dimensions, 0));
    if (arrays.length === 0) {
        return $rt_createMultiArray($rt_booleancls, dimensions);
    }
    var firstDim = dimensions[0] | 0;
    for (var i = 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createBooleanArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_booleancls, arrays, dimensions, 0);
}
function $rt_createShortMultiArray(dimensions) {
    var arrays = new teavm_globals.Array($rt_primitiveArrayCount(dimensions, 0));
    if (arrays.length === 0) {
        return $rt_createMultiArray($rt_shortcls, dimensions);
    }
    var firstDim = dimensions[0] | 0;
    for (var i = 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createShortArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_shortcls, arrays, dimensions, 0);
}
function $rt_createIntMultiArray(dimensions) {
    var arrays = new teavm_globals.Array($rt_primitiveArrayCount(dimensions, 0));
    if (arrays.length === 0) {
        return $rt_createMultiArray($rt_intcls, dimensions);
    }
    var firstDim = dimensions[0] | 0;
    for (var i = 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createIntArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_intcls, arrays, dimensions, 0);
}
function $rt_createLongMultiArray(dimensions) {
    var arrays = new teavm_globals.Array($rt_primitiveArrayCount(dimensions, 0));
    if (arrays.length === 0) {
        return $rt_createMultiArray($rt_longcls, dimensions);
    }
    var firstDim = dimensions[0] | 0;
    for (var i = 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createLongArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_longcls, arrays, dimensions, 0);
}
function $rt_createFloatMultiArray(dimensions) {
    var arrays = new teavm_globals.Array($rt_primitiveArrayCount(dimensions, 0));
    if (arrays.length === 0) {
        return $rt_createMultiArray($rt_floatcls, dimensions);
    }
    var firstDim = dimensions[0] | 0;
    for (var i = 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createFloatArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_floatcls, arrays, dimensions, 0);
}
function $rt_createDoubleMultiArray(dimensions) {
    var arrays = new teavm_globals.Array($rt_primitiveArrayCount(dimensions, 0));
    if (arrays.length === 0) {
        return $rt_createMultiArray($rt_doublecls, dimensions);
    }
    var firstDim = dimensions[0] | 0;
    for (var i = 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createDoubleArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_doublecls, arrays, dimensions, 0);
}
function $rt_primitiveArrayCount(dimensions, start) {
    var val = dimensions[start + 1] | 0;
    for (var i = start + 2; i < dimensions.length; i = (i + 1) | 0) {
        val = (val * (dimensions[i] | 0)) | 0;
        if (val === 0) {
            break;
        }
    }
    return val;
}
function $rt_createMultiArrayImpl(cls, arrays, dimensions, start) {
    var limit = arrays.length;
    for (var i = (start + 1) | 0; i < dimensions.length; i = (i + 1) | 0) {
        cls = $rt_arraycls(cls);
        var dim = dimensions[i];
        var index = 0;
        var packedIndex = 0;
        while (index < limit) {
            var arr = $rt_createUnfilledArray(cls, dim);
            for (var j = 0; j < dim; j = (j + 1) | 0) {
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
function $rt_assertNotNaN(value) {
    if (typeof value === 'number' && isNaN(value)) {
        throw "NaN";
    }
    return value;
}
function $rt_createOutputFunction(printFunction) {
    var buffer = "";
    var utf8Buffer = 0;
    var utf8Remaining = 0;

    function putCodePoint(ch) {
        if (ch === 0xA) {
            printFunction(buffer);
            buffer = "";
        } else if (ch < 0x10000) {
            buffer += String.fromCharCode(ch);
        } else {
            ch = (ch - 0x10000) | 0;
            var hi = (ch >> 10) + 0xD800;
            var lo = (ch & 0x3FF) + 0xDC00;
            buffer += String.fromCharCode(hi, lo);
        }
    }

    return function(ch) {
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

var $rt_putStdout = typeof $rt_putStdoutCustom === "function"
    ? $rt_putStdoutCustom
    : typeof console === "object" ? $rt_createOutputFunction(function(msg) { console.info(msg); }) : function() {};
var $rt_putStderr = typeof $rt_putStderrCustom === "function"
    ? $rt_putStderrCustom
    : typeof console === "object" ? $rt_createOutputFunction(function(msg) { console.error(msg); }) : function() {};

var $rt_packageData = null;
function $rt_packages(data) {
    var i = 0;
    var packages = new teavm_globals.Array(data.length);
    for (var j = 0; j < data.length; ++j) {
        var prefixIndex = data[i++];
        var prefix = prefixIndex >= 0 ? packages[prefixIndex] : "";
        packages[j] = prefix + data[i++] + ".";
    }
    $rt_packageData = packages;
}
function $rt_metadata(data) {
    var packages = $rt_packageData;
    var i = 0;
    while (i < data.length) {
        var cls = data[i++];
        cls.$meta = {};
        var m = cls.$meta;
        var className = data[i++];

        m.name = className !== 0 ? className : null;
        if (m.name !== null) {
            var packageIndex = data[i++];
            if (packageIndex >= 0) {
                m.name = packages[packageIndex] + m.name;
            }
        }

        m.binaryName = "L" + m.name + ";";
        var superclass = data[i++];
        m.superclass = superclass !== 0 ? superclass : null;
        m.supertypes = data[i++];
        if (m.superclass) {
            m.supertypes.push(m.superclass);
            cls.prototype = teavm_globals.Object.create(m.superclass.prototype);
        } else {
            cls.prototype = {};
        }
        var flags = data[i++];
        m.enum = (flags & 8) !== 0;
        m.flags = flags;
        m.primitive = false;
        m.item = null;
        cls.prototype.constructor = cls;
        cls.classObject = null;

        m.accessLevel = data[i++];

        var innerClassInfo = data[i++];
        if (innerClassInfo === 0) {
            m.simpleName = null;
            m.declaringClass = null;
            m.enclosingClass = null;
        } else {
            var enclosingClass = innerClassInfo[0];
            m.enclosingClass = enclosingClass !== 0 ? enclosingClass : null;
            var declaringClass = innerClassInfo[1];
            m.declaringClass = declaringClass !== 0 ? declaringClass : null;
            var simpleName = innerClassInfo[2];
            m.simpleName = simpleName !== 0 ? simpleName : null;
        }

        var clinit = data[i++];
        cls.$clinit = clinit !== 0 ? clinit : function() {};

        var virtualMethods = data[i++];
        if (virtualMethods !== 0) {
            for (var j = 0; j < virtualMethods.length; j += 2) {
                var name = virtualMethods[j];
                var func = virtualMethods[j + 1];
                if (typeof name === 'string') {
                    name = [name];
                }
                for (var k = 0; k < name.length; ++k) {
                    cls.prototype[name[k]] = func;
                }
            }
        }

        cls.$array = null;
    }
}
function $rt_wrapFunction0(f) {
    return function() {
        return f(this);
    }
}
function $rt_wrapFunction1(f) {
    return function(p1) {
        return f(this, p1);
    }
}
function $rt_wrapFunction2(f) {
    return function(p1, p2) {
        return f(this, p1, p2);
    }
}
function $rt_wrapFunction3(f) {
    return function(p1, p2, p3) {
        return f(this, p1, p2, p3, p3);
    }
}
function $rt_wrapFunction4(f) {
    return function(p1, p2, p3, p4) {
        return f(this, p1, p2, p3, p4);
    }
}
function $rt_threadStarter(f) {
    return function() {
        var args = teavm_globals.Array.prototype.slice.apply(arguments);
        $rt_startThread(function() {
            f.apply(this, args);
        });
    }
}
function $rt_mainStarter(f) {
    return function(args, callback) {
        if (!args) {
            args = [];
        }
        var javaArgs = $rt_createArray($rt_objcls(), args.length);
        for (var i = 0; i < args.length; ++i) {
            javaArgs.data[i] = $rt_str(args[i]);
        }
        $rt_startThread(function() { f.call(null, javaArgs); }, callback);
    }
}
var $rt_stringPool_instance;
function $rt_stringPool(strings) {
    $rt_stringClassInit();
    $rt_stringPool_instance = new teavm_globals.Array(strings.length);
    for (var i = 0; i < strings.length; ++i) {
        $rt_stringPool_instance[i] = $rt_intern($rt_str(strings[i]));
    }
}
function $rt_s(index) {
    return $rt_stringPool_instance[index];
}
function $rt_eraseClinit(target) {
    return target.$clinit = function() {};
}

var $rt_numberConversionBuffer = new teavm_globals.ArrayBuffer(16);
var $rt_numberConversionView = new teavm_globals.DataView($rt_numberConversionBuffer);
var $rt_numberConversionFloatArray = new teavm_globals.Float32Array($rt_numberConversionBuffer);
var $rt_numberConversionDoubleArray = new teavm_globals.Float64Array($rt_numberConversionBuffer);
var $rt_numberConversionIntArray = new teavm_globals.Int32Array($rt_numberConversionBuffer);

var $rt_doubleToRawLongBits;
var $rt_longBitsToDouble;
if (typeof teavm_globals.BigInt !== 'function') {
    $rt_doubleToRawLongBits = function(n) {
        $rt_numberConversionView.setFloat64(0, n, true);
        return new Long($rt_numberConversionView.getInt32(0, true), $rt_numberConversionView.getInt32(4, true));
    }
    $rt_longBitsToDouble = function(n) {
        $rt_numberConversionView.setInt32(0, n.lo, true);
        $rt_numberConversionView.setInt32(4, n.hi, true);
        return $rt_numberConversionView.getFloat64(0, true);
    }
} else if (typeof teavm_globals.BigInt64Array !== 'function') {
    $rt_doubleToRawLongBits = function(n) {
        $rt_numberConversionView.setFloat64(0, n, true);
        var lo = $rt_numberConversionView.getInt32(0, true);
        var hi = $rt_numberConversionView.getInt32(4, true);
        return teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt.asUintN(32, teavm_globals.BigInt(lo))
            | (teavm_globals.BigInt(hi) << teavm_globals.BigInt(32)));
    }
    $rt_longBitsToDouble = function(n) {
        $rt_numberConversionView.setFloat64(0, n, true);
        var lo = $rt_numberConversionView.getInt32(0, true);
        var hi = $rt_numberConversionView.getInt32(4, true);
        return teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt.asUintN(32, teavm_globals.BigInt(lo))
            | (teavm_globals.BigInt(hi) << teavm_globals.BigInt(32)));
    }
} else {
    var $rt_numberConversionLongArray = new teavm_globals.BigInt64Array($rt_numberConversionBuffer);
    $rt_doubleToRawLongBits = function(n) {
        $rt_numberConversionDoubleArray[0] = n;
        return $rt_numberConversionLongArray[0];
    }
    $rt_longBitsToDouble = function(n) {
        $rt_numberConversionLongArray[0] = n;
        return $rt_numberConversionDoubleArray[0];
    }
}

function $rt_floatToRawIntBits(n) {
    $rt_numberConversionFloatArray[0] = n;
    return $rt_numberConversionIntArray[0];
}
function $rt_intBitsToFloat(n) {
    $rt_numberConversionIntArray[0] = n;
    return $rt_numberConversionFloatArray[0];
}
function $rt_equalDoubles(a, b) {
    if (a !== a) {
        return b !== b;
    }
    $rt_numberConversionDoubleArray[0] = a;
    $rt_numberConversionDoubleArray[1] = b;
    return $rt_numberConversionIntArray[0] === $rt_numberConversionIntArray[2]
            && $rt_numberConversionIntArray[1] === $rt_numberConversionIntArray[3];
}

var JavaError;
if (typeof Reflect === 'object') {
    var defaultMessage = teavm_globals.Symbol("defaultMessage");
    JavaError = function JavaError(message, cause) {
        var self = teavm_globals.Reflect.construct(teavm_globals.Error, [void 0, cause], JavaError);
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
            get: function() {
                try {
                    var javaException = this[$rt_javaExceptionProp];
                    if (typeof javaException === 'object') {
                        var javaMessage = $rt_throwableMessage(javaException);
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

function $rt_javaException(e) {
    return e instanceof teavm_globals.Error && typeof e[$rt_javaExceptionProp] === 'object'
        ? e[$rt_javaExceptionProp]
        : null;
}
function $rt_jsException(e) {
    return typeof e.$jsException === 'object' ? e.$jsException : null;
}
function $rt_wrapException(err) {
    var ex = err[$rt_javaExceptionProp];
    if (!ex) {
        ex = $rt_createException($rt_str("(JavaScript) " + err.toString()));
        err[$rt_javaExceptionProp] = ex;
        ex.$jsException = err;
        $rt_fillStack(err, ex);
    }
    return ex;
}

function $dbg_class(obj) {
    var cls = obj.constructor;
    var arrayDegree = 0;
    while (cls.$meta && cls.$meta.item) {
        ++arrayDegree;
        cls = cls.$meta.item;
    }
    var clsName = "";
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
Long.prototype.__teavm_class__ = function() {
    return "long";
};
function Long_isPositive(a) {
    return (a.hi & 0x80000000) === 0;
}
function Long_isNegative(a) {
    return (a.hi & 0x80000000) !== 0;
}

var Long_MAX_NORMAL = 1 << 18;
var Long_ZERO;
var Long_create;
var Long_fromInt;
var Long_fromNumber;
var Long_toNumber;
var Long_hi;
var Long_lo;
if (typeof teavm_globals.BigInt !== "function") {
    Long.prototype.toString = function() {
        var result = [];
        var n = this;
        var positive = Long_isPositive(n);
        if (!positive) {
            n = Long_neg(n);
        }
        var radix = new Long(10, 0);
        do {
            var divRem = Long_divRem(n, radix);
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
    Long_fromInt = function(val) {
        return new Long(val, (-(val < 0)) | 0);
    }
    Long_fromNumber = function(val) {
        if (val >= 0) {
            return new Long(val | 0, (val / 0x100000000) | 0);
        } else {
            return Long_neg(new Long(-val | 0, (-val / 0x100000000) | 0));
        }
    }
    Long_create = function(lo, hi) {
        return new Long(lo, hi);
    }
    Long_toNumber = function(val) {
        return 0x100000000 * val.hi + (val.lo >>> 0);
    }
    Long_hi = function(val) {
        return val.hi;
    }
    Long_lo = function(val) {
        return val.lo;
    }
} else {
    Long_ZERO = teavm_globals.BigInt(0);
    Long_create = function(lo, hi) {
        return teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt.asUintN(64, teavm_globals.BigInt(lo))
            | teavm_globals.BigInt.asUintN(64, (teavm_globals.BigInt(hi) << teavm_globals.BigInt(32))));
    }
    Long_fromInt = function(val) {
        return teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt(val | 0));
    }
    Long_fromNumber = function(val) {
        return teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt(
            val >= 0 ? teavm_globals.Math.floor(val) : teavm_globals.Math.ceil(val)));
    }
    Long_toNumber = function(val) {
        return teavm_globals.Number(val);
    }
    Long_hi = function(val) {
        return teavm_globals.Number(teavm_globals.BigInt.asIntN(64, val >> teavm_globals.BigInt(32))) | 0;
    }
    Long_lo = function(val) {
        return teavm_globals.Number(teavm_globals.BigInt.asIntN(32, val)) | 0;
    }
}
var $rt_imul = teavm_globals.Math.imul || function(a, b) {
    var ah = (a >>> 16) & 0xFFFF;
    var al = a & 0xFFFF;
    var bh = (b >>> 16) & 0xFFFF;
    var bl = b & 0xFFFF;
    return (al * bl + (((ah * bl + al * bh) << 16) >>> 0)) | 0;
};
var $rt_udiv = function(a, b) {
    return ((a >>> 0) / (b >>> 0)) >>> 0;
};
var $rt_umod = function(a, b) {
    return ((a >>> 0) % (b >>> 0)) >>> 0;
};
var $rt_ucmp = function(a, b) {
    a >>>= 0;
    b >>>= 0;
    return a < b ? -1 : a > b ? 1 : 0;
};
function $rt_checkBounds(index, array) {
    if (index < 0 || index >= array.length) {
        $rt_throwAIOOBE();
    }
    return index;
}
function $rt_checkUpperBound(index, array) {
    if (index >= array.length) {
        $rt_throwAIOOBE();
    }
    return index;
}
function $rt_checkLowerBound(index) {
    if (index < 0) {
        $rt_throwAIOOBE();
    }
    return index;
}
function $rt_classWithoutFields(superclass) {
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
function $rt_charArrayToString(array, offset, count) {
    var result = "";
    var limit = offset + count;
    for (var i = offset; i < limit; i = (i + 1024) | 0) {
        var next = teavm_globals.Math.min(limit, (i + 1024) | 0);
        result += teavm_globals.String.fromCharCode.apply(null, array.subarray(i, next));
    }
    return result;
}
function $rt_fullArrayToString(array) {
    return $rt_charArrayToString(array, 0, array.length);
}
function $rt_stringToCharArray(string, begin, dst, dstBegin, count) {
    for (var i = 0; i < count; i = (i + 1) | 0) {
        dst[dstBegin + i] = string.charCodeAt(begin + i);
    }
}
function $rt_fastStringToCharArray(string) {
    var array = new teavm_globals.Uint16Array(string.length);
    for (var i = 0; i < array.length; ++i) {
        array[i] = string.charCodeAt(i);
    }
    return new $rt_charArrayCls(array);
}
function $rt_substring(string, start, end) {
    if (start === 0 && end === string.length) {
        return string;
    }
    var result = start.substring(start, end - 1) + start.substring(end - 1, end);
    $rt_substringSink = ($rt_substringSink + result.charCodeAt(result.length - 1)) | 0;
}
var $rt_substringSink = 0;

function $rt_setCloneMethod(target, method) {
    target[teavm_javaVirtualMethod('clone()Ljava/lang/Object;')] = method;
}
function $rt_cls(cls) {
    return teavm_javaMethod("java.lang.Class",
        "getClass(Lorg/teavm/platform/PlatformClass;)Ljava/lang/Class;")(cls);
}
function $rt_str(str) {
    if (str === null) {
        return null;
    }
    return teavm_javaConstructor("java.lang.String", "(Ljava/lang/Object;)V")(str);
}
function $rt_ustr(str) {
    return str === null ? null : str[teavm_javaField("java.lang.String", "nativeString")];
}
function $rt_nullCheck(val) {
    if (val === null) {
        $rt_throw(teavm_javaConstructor("java.lang.NullPointerException", "()V")());
    }
    return val;
}
function $rt_stringClassInit() {
    teavm_javaClassInit("java.lang.String")();
}
function $rt_objcls() {
    return teavm_javaClass("java.lang.Object");
}
function $rt_createException(message) {
    return teavm_javaConstructor("java.lang.RuntimeException", "(Ljava/lang/String;)V")(message);
}
function $rt_throwableMessage(t) {
    return teavm_javaMethod("java.lang.Throwable", "getMessage()Ljava/lang/String;")(t);
}
function $rt_throwableCause(t) {
    return teavm_javaMethod("java.lang.Throwable", "getCause()Ljava/lang/Throwable;")(t);
}
function $rt_stecls() {
    if (teavm_javaClassExists("java.lang.StackTraceElement")) {
        return teavm_javaClass("java.lang.StackTraceElement");
    } else {
        return $rt_objcls();
    }
}
function $rt_throwAIOOBE() {
    if (teavm_javaConstructorExists("java.lang.ArrayIndexOutOfBoundsException", "()V")) {
        $rt_throw(teavm_javaConstructor("java.lang.ArrayIndexOutOfBoundsException", "()V")());
    } else {
        $rt_throw($rt_createException($rt_str("")));
    }
}
function $rt_throwCCE() {
    if (teavm_javaConstructorExists("java.lang.ClassCastException", "()V")) {
        $rt_throw(teavm_javaConstructor("java.lang.ClassCastException", "()V")());
    } else {
        $rt_throw($rt_createException($rt_str("")));
    }
}

function $rt_getThread() {
    if (teavm_javaMethodExists("java.lang.Thread", "currentThread()Ljava/lang/Thread;")) {
        return teavm_javaMethod("java.lang.Thread", "currentThread()Ljava/lang/Thread;")();
    }
}
function $rt_setThread(t) {
    if (teavm_javaMethodExists("java.lang.Thread", "setCurrentThread(Ljava/lang/Thread;)V")) {
        return teavm_javaMethod("java.lang.Thread", "setCurrentThread(Ljava/lang/Thread;)V")(t);
    }
}
function $rt_createStackElement(className, methodName, fileName, lineNumber) {
    if (teavm_javaConstructorExists("java.lang.StackTraceElement",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V")) {
        return teavm_javaConstructor("java.lang.StackTraceElement",
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V")(className, methodName, fileName, lineNumber);
    } else {
        return null;
    }
}
function $rt_setStack(e, stack) {
    if (teavm_javaMethodExists("java.lang.Throwable", "setStackTrace([Ljava/lang/StackTraceElement;)V")) {
        teavm_javaMethod("java.lang.Throwable", "setStackTrace([Ljava/lang/StackTraceElement;)V")(e, stack);
    }
}