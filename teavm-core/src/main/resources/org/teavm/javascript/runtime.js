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
$rt_lastObjectId = 0;
$rt_nextId = function() {
    return $rt_lastObjectId++;
}
$rt_compare = function(a, b) {
    return a > b ? 1 : a < b ? -1 : 0;
}
$rt_isInstance = function(obj, cls) {
    return obj != null && obj.constructor.$meta && $rt_isAssignable(obj.constructor, cls);
}
$rt_isAssignable = function(from, to) {
    if (from === to) {
        return true;
    }
    var supertypes = from.$meta.supertypes;
    for (var i = 0; i < supertypes.length; i = (i + 1) | 0) {
        if ($rt_isAssignable(supertypes[i], to)) {
            return true;
        }
    }
    return false;
}
$rt_createArray = function(cls, sz) {
    var data = new Array(sz);
    var arr = new ($rt_arraycls(cls))(data);
    for (var i = 0; i < sz; i = (i + 1) | 0) {
        data[i] = null;
    }
    return arr;
}
$rt_wrapArray = function(cls, data) {
    var arr = new ($rt_arraycls(cls))(data);
    return arr;
}
$rt_createUnfilledArray = function(cls, sz) {
    return new ($rt_arraycls(cls))(new Array(sz));
}
$rt_createLongArray = function(sz) {
    var data = new Array(sz);
    var arr = new ($rt_arraycls($rt_longcls()))(data);
    for (var i = 0; i < sz; i = (i + 1) | 0) {
        data[i] = Long.ZERO;
    }
    return arr;
}
if (false) {
    $rt_createNumericArray = function(cls, nativeArray) {
        return new ($rt_arraycls(cls))(nativeArray);
    }
    $rt_createByteArray = function(sz) {
        return $rt_createNumericArray($rt_bytecls(), new Int8Array(new ArrayBuffer(sz)), 0);
    };
    $rt_createShortArray = function(sz) {
        return $rt_createNumericArray($rt_shortcls(), new Int16Array(new ArrayBuffer(sz << 1)), 0);
    };
    $rt_createIntArray = function(sz) {
        return $rt_createNumericArray($rt_intcls(), new Int32Array(new ArrayBuffer(sz << 2)), 0);
    };
    $rt_createBooleanArray = function(sz) {
        return $rt_createNumericArray($rt_booleancls(), new Int8Array(new ArrayBuffer(sz)), 0);
    };
    $rt_createFloatArray = function(sz) {
        return $rt_createNumericArray($rt_floatcls(), new Float32Array(new ArrayBuffer(sz << 2)), 0);
    };
    $rt_createDoubleArray = function(sz) {
        return $rt_createNumericArray($rt_doublecls(), new Float64Array(new ArrayBuffer(sz << 3)), 0);
    };
    $rt_createCharArray = function(sz) {
        return $rt_createNumericArray($rt_charcls(), new Uint16Array(new ArrayBuffer(sz << 1)), 0);
    };
} else {
    $rt_createNumericArray = function(cls, sz) {
        var data = new Array(sz);
        var arr = new ($rt_arraycls(cls))(data);
        for (var i = 0; i < sz; i = (i + 1) | 0) {
            data[i] = 0;
        }
        return arr;
    }
    $rt_createByteArray = function(sz) { return $rt_createNumericArray($rt_bytecls(), sz); }
    $rt_createShortArray = function(sz) { return $rt_createNumericArray($rt_shortcls(), sz); }
    $rt_createIntArray = function(sz) { return $rt_createNumericArray($rt_intcls(), sz); }
    $rt_createBooleanArray = function(sz) { return $rt_createNumericArray($rt_booleancls(), sz); }
    $rt_createFloatArray = function(sz) { return $rt_createNumericArray($rt_floatcls(), sz); }
    $rt_createDoubleArray = function(sz) { return $rt_createNumericArray($rt_doublecls(), sz); }
    $rt_createCharArray = function(sz) { return $rt_createNumericArray($rt_charcls(), sz); }
}
$rt_arraycls = function(cls) {
    if (cls.$array == undefined) {
        var arraycls = function(data) {
            this.data = data;
            this.$id = $rt_nextId();
        };
        arraycls.prototype = new ($rt_objcls())();
        arraycls.prototype.constructor = arraycls;
        arraycls.$meta = { item : cls, supertypes : [$rt_objcls()], primitive : false, superclass : $rt_objcls() };
        cls.$array = arraycls;
    }
    return cls.$array;
}
$rt_createcls = function() {
    return {
        $meta : {
            supertypes : []
        }
    };
}
$rt_booleanclsCache = null;
$rt_booleancls = function() {
    if ($rt_booleanclsCache == null) {
        $rt_booleanclsCache = $rt_createcls();
        $rt_booleanclsCache.primitive = true;
        $rt_booleanclsCache.name = "boolean";
    }
    return $rt_booleanclsCache;
}
$rt_charclsCache = null;
$rt_charcls = function() {
    if ($rt_charclsCache == null) {
        $rt_charclsCache = $rt_createcls();
        $rt_charclsCache.primitive = true;
        $rt_charclsCache.name = "char";
    }
    return $rt_charclsCache;
}
$rt_byteclsCache = null;
$rt_bytecls = function() {
    if ($rt_byteclsCache == null) {
        $rt_byteclsCache = $rt_createcls();
        $rt_byteclsCache.primitive = true;
        $rt_byteclsCache.name = "byte";
    }
    return $rt_byteclsCache;
}
$rt_shortclsCache = null;
$rt_shortcls = function() {
    if ($rt_shortclsCache == null) {
        $rt_shortclsCache = $rt_createcls();
        $rt_shortclsCache.primitive = true;
        $rt_shortclsCache.name = "short";
    }
    return $rt_shortclsCache;
}
$rt_intclsCache = null;
$rt_intcls = function() {
    if ($rt_intclsCache === null) {
        $rt_intclsCache = $rt_createcls();
        $rt_intclsCache.primitive = true;
        $rt_intclsCache.name = "int";
    }
    return $rt_intclsCache;
}
$rt_longclsCache = null;
$rt_longcls = function() {
    if ($rt_longclsCache === null) {
        $rt_longclsCache = $rt_createcls();
        $rt_longclsCache.primitive = true;
        $rt_longclsCache.name = "long";
    }
    return $rt_longclsCache;
}
$rt_floatclsCache = null;
$rt_floatcls = function() {
    if ($rt_floatclsCache === null) {
        $rt_floatclsCache = $rt_createcls();
        $rt_floatclsCache.primitive = true;
        $rt_floatclsCache.name = "float";
    }
    return $rt_floatclsCache;
}
$rt_doubleclsCache = null;
$rt_doublecls = function() {
    if ($rt_doubleclsCache === null) {
        $rt_doubleclsCache = $rt_createcls();
        $rt_doubleclsCache.primitive = true;
        $rt_doubleclsCache.name = "double";
    }
    return $rt_doubleclsCache;
}
$rt_voidclsCache = null;
$rt_voidcls = function() {
    if ($rt_voidclsCache === null) {
        $rt_voidclsCache = $rt_createcls();
        $rt_voidclsCache.primitive = true;
        $rt_voidclsCache.name = "void";
    }
    return $rt_voidclsCache;
}
$rt_equals = function(a, b) {
    if (a === b) {
        return true;
    }
    if (a === null || b === null) {
        return false;
    }
    if (typeof(a) == 'object') {
        return a.equals(b);
    } else {
        return false;
    }
}
$rt_clinit = function(cls) {
    if (cls.$clinit) {
        var f = cls.$clinit;
        delete cls.$clinit;
        f();
    }
    return cls;
}
$rt_init = function(cls, constructor, args) {
    var obj = new cls();
    cls.prototype[constructor].apply(obj, args);
    return obj;
}
$rt_throw = function(ex) {
    var err = ex.$jsException;
    if (!err) {
        var err = new Error("Java exception thrown");
        err.$javaException = ex;
        ex.$jsException = err;
    }
    throw err;
}
$rt_byteToInt = function(value) {
    return value > 0xFF ? value | 0xFFFFFF00 : value;
}
$rt_shortToInt = function(value) {
    return value > 0xFFFF ? value | 0xFFFF0000 : value;
}
$rt_createMultiArray = function(cls, dimensions) {
    var arrays = new Array($rt_primitiveArrayCount(dimensions));
    var firstDim = dimensions[0] | 0;
    for (var i = 0 | 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createArray(cls, firstDim);
    }
    return $rt_createMultiArrayImpl(cls, arrays, dimensions);
}
$rt_createByteMultiArray = function(dimensions) {
    var arrays = new Array($rt_primitiveArrayCount(dimensions));
    var firstDim = dimensions[0] | 0;
    for (var i = 0 | 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createByteArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_bytecls(), arrays, dimensions);
}
$rt_createBooleanMultiArray = function(dimensions) {
    var arrays = new Array($rt_primitiveArrayCount(dimensions));
    var firstDim = dimensions[0] | 0;
    for (var i = 0 | 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createBooleanArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_booleancls(), arrays, dimensions);
}
$rt_createShortMultiArray = function(dimensions) {
    var arrays = new Array($rt_primitiveArrayCount(dimensions));
    var firstDim = dimensions[0] | 0;
    for (var i = 0 | 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createShortArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_shortcls(), arrays, dimensions);
}
$rt_createIntMultiArray = function(dimensions) {
    var arrays = new Array($rt_primitiveArrayCount(dimensions));
    var firstDim = dimensions[0] | 0;
    for (var i = 0 | 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createIntArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_intcls(), arrays, dimensions);
}
$rt_createLongMultiArray = function(dimensions) {
    var arrays = new Array($rt_primitiveArrayCount(dimensions));
    var firstDim = dimensions[0] | 0;
    for (var i = 0 | 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createLongArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_longcls(), arrays, dimensions);
}
$rt_createFloatMultiArray = function(dimensions) {
    var arrays = new Array($rt_primitiveArrayCount(dimensions));
    var firstDim = dimensions[0] | 0;
    for (var i = 0 | 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createFloatArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_floatcls(), arrays, dimensions);
}
$rt_createDoubleMultiArray = function(dimensions) {
    var arrays = new Array($rt_primitiveArrayCount(dimensions));
    var firstDim = dimensions[0] | 0;
    for (var i = 0 | 0; i < arrays.length; i = (i + 1) | 0) {
        arrays[i] = $rt_createDoubleArray(firstDim);
    }
    return $rt_createMultiArrayImpl($rt_doublecls(), arrays, dimensions);
}
$rt_primitiveArrayCount = function(dimensions) {
    var val = dimensions[1] | 0;
    for (var i = 2 | 0; i < dimensions.length; i = (i + 1) | 0) {
        val = (val * (dimensions[i] | 0)) | 0;
    }
    return val;
}
$rt_createMultiArrayImpl = function(cls, arrays, dimensions) {
    var limit = arrays.length;
    for (var i = 1 | 0; i < dimensions.length; i = (i + 1) | 0) {
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
$rt_assertNotNaN = function(value) {
    if (typeof value == 'number' && isNaN(value)) {
        throw "NaN";
    }
    return value;
}
$rt_methodStubs = function(clinit, names) {
    for (var i = 0; i < names.length; i = (i + 1) | 0) {
        window[names[i]] = (function(name) {
            return function() {
                clinit();
                return window[name].apply(window, arguments);
            }
        })(names[i]);
    }
}
$rt_stdoutBuffer = "";
$rt_putStdout = function(ch) {
    if (ch === 0xA) {
        if (console) {
            console.info($rt_stdoutBuffer);
        }
        $rt_stdoutBuffer = "";
    } else {
        $rt_stdoutBuffer += String.fromCharCode(ch);
    }
}
$rt_stderrBuffer = "";
$rt_putStderr = function(ch) {
    if (ch === 0xA) {
        if (console) {
            console.info($rt_stderrBuffer);
        }
        $rt_stderrBuffer = "";
    } else {
        $rt_stderrBuffer += String.fromCharCode(ch);
    }
}
function $rt_declClass(cls, data) {
    cls.name = data.name;
    cls.$meta = {};
    cls.$meta.superclass = data.superclass;
    cls.$meta.supertypes = data.interfaces ? data.interfaces.slice() : [];
    if (data.superclass) {
        cls.$meta.supertypes.push(data.superclass);
        cls.prototype = new data.superclass();
    } else {
        cls.prototype = new Object();
    }
    cls.$meta.name = data.name;
    cls.$meta.enum = data.enum;
    cls.prototype.constructor = cls;
    cls.$clinit = data.clinit;
}
function $rt_virtualMethods(cls) {
    for (var i = 1; i < arguments.length; i += 2) {
        var name = arguments[i];
        var func = arguments[i + 1];
        if (typeof name == 'string') {
            cls.prototype[name] = func;
        } else {
            for (var j = 0; j < name.length; ++j) {
                cls.prototype[name[j]] = func;
            }
        }
    }
}

Long = function(lo, hi) {
    this.lo = lo | 0;
    this.hi = hi | 0;
}
Long_ZERO = new Long(0, 0);
Long_fromInt = function(val) {
    return val >= 0 ? new Long(val, 0) : new Long(val, -1);
}
Long_fromNumber = function(val) {
    return new Long(val | 0, (val / 0x100000000) | 0);
}
Long_toNumber = function(val) {
    return val.lo + 0x100000000 * val.hi;
}
Long_add = function(a, b) {
    var a_lolo = a.lo & 0xFFFF;
    var a_lohi = a.lo >>> 16;
    var a_hilo = a.hi & 0xFFFF;
    var a_hihi = a.hi >>> 16;
    var b_lolo = b.lo & 0xFFFF;
    var b_lohi = b.lo >>> 16;
    var b_hilo = b.hi & 0xFFFF;
    var b_hihi = b.hi >>> 16;

    var lolo = (a_lolo + b_lolo) | 0;
    var lohi = (a_lohi + b_lohi + (lolo >> 16)) | 0;
    var hilo = (a_hilo + b_hilo + (lohi >> 16)) | 0;
    var hihi = (a_hihi + b_hihi + (hilo >> 16)) | 0;
    return new Long((lolo & 0xFFFF) | ((lohi & 0xFFFF) << 16),
            (hilo & 0xFFFF) | ((hihi & 0xFFFF) << 16));
}
Long_inc = function(a) {
    var lo = (a.lo + 1) | 0;
    var hi = a.hi;
    if (lo === 0) {
        hi = (hi + 1) | 0;
    }
    return new Long(lo, hi);
}
Long_dec = function(a) {
    var lo = (a.lo - 1) | 0;
    var hi = a.hi;
    if (lo === -1) {
        hi = (hi - 1) | 0;
    }
    return new Long(lo, hi);
}
Long_neg = function(a) {
    return Long_inc(new Long(a.lo ^ 0xFFFFFFFF, a.hi ^ 0xFFFFFFFF));
}
Long_sub = function(a, b) {
    var a_lolo = a.lo & 0xFFFF;
    var a_lohi = a.lo >>> 16;
    var a_hilo = a.hi & 0xFFFF;
    var a_hihi = a.hi >>> 16;
    var b_lolo = b.lo & 0xFFFF;
    var b_lohi = b.lo >>> 16;
    var b_hilo = b.hi & 0xFFFF;
    var b_hihi = b.hi >>> 16;

    var lolo = (a_lolo - b_lolo) | 0;
    var lohi = (a_lohi - b_lohi + (lolo >> 16)) | 0;
    var hilo = (a_hilo - b_hilo + (lohi >> 16)) | 0;
    var hihi = (a_hihi - b_hihi + (hilo >> 16)) | 0;
    return new Long((lolo & 0xFFFF) | ((lohi & 0xFFFF) << 16),
            (hilo & 0xFFFF) | ((hihi & 0xFFFF) << 16));
}
Long_compare = function(a, b) {
    var r = a.hi - b.hi;
    if (r !== 0) {
        return r;
    }
    var r = (a.lo >>> 1) - (b.lo >>> 1);
    if (r !== 0) {
        return r;
    }
    return (a.lo & 1) - (b.lo & 1);
}
Long_isPositive = function(a) {
    return (a.hi & 0x80000000) === 0;
}
Long_isNegative = function(a) {
    return (a.hi & 0x80000000) !== 0;
}
Long_mul = function(a, b) {
    var a_lolo = a.lo & 0xFFFF;
    var a_lohi = a.lo >>> 16;
    var a_hilo = a.hi & 0xFFFF;
    var a_hihi = a.hi >>> 16;
    var b_lolo = b.lo & 0xFFFF;
    var b_lohi = b.lo >>> 16;
    var b_hilo = b.hi & 0xFFFF;
    var b_hihi = b.hi >>> 16;

    var lolo = (a_lolo * b_lolo) | 0;
    var lohi = (a_lohi * b_lolo + a_lolo * b_lohi + (lolo >> 16)) | 0;
    var hilo = (a_hilo * b_lolo + a_lohi * b_lohi + a_lolo * b_hilo + (lohi >> 16)) | 0;
    var hihi = (a_hihi * b_lolo + a_hilo * b_lohi + a_lohi * b_hilo + a_lolo * b_hihi + (hilo >> 16)) | 0;
    return new Long((lolo & 0xFFFF) | ((lohi & 0xFFFF) << 16), (hilo & 0xFFFF) | ((hihi & 0xFFFF) << 16));
}
Long_div = function(a, b) {
    return Long_divRem(a, b)[0];
}
Long_rem = function(a, b) {
    return Long_divRem(a, b)[1];
}
Long_divRem = function(a, b) {
    var positive = Long_isNegative(a) === Long_isNegative(b);
    if (Long_isNegative(a)) {
        a = Long_neg(a);
    }
    if (Long_isNegative(b)) {
        b = Long_neg(b);
    }
    a = new LongInt(a.lo, a.hi, 0);
    b = new LongInt(b.lo, b.hi, 0);
    var q = LongInt_div(a, b);
    a = new Long(a.lo, a.hi);
    q = new Long(q.lo, q.hi);
    return positive ? [q, a] : [Long_neg(q), Long_neg(a)];
}
Long_shiftLeft16 = function(a) {
    return new Long(a.lo << 16, (a.lo >>> 16) | (a.hi << 16));
}
Long_shiftRight16 = function(a) {
    return new Long((a.lo >>> 16) | (a.hi << 16), a.hi >>> 16);
}
Long_and = function(a, b) {
    return new Long(a.lo & b.lo, a.hi & b.hi);
}
Long_or = function(a, b) {
    return new Long(a.lo | b.lo, a.hi | b.hi);
}
Long_xor = function(a, b) {
    return new Long(a.lo ^ b.lo, a.hi ^ b.hi);
}
Long_shl = function(a, b) {
    if (b < 32) {
        return new Long(a.lo << b, (a.lo >>> (32 - b)) | (a.hi << b));
    } else {
        return new Long(0, a.lo << (b - 32));
    }
}
Long_shr = function(a, b) {
    if (b < 32) {
        return new Long((a.lo >>> b) | (a.hi << (32 - b)), a.hi >> b);
    } else {
        return new Long((a.hi >> (b - 32)), -1);
    }
}
Long_shru = function(a, b) {
    if (b < 32) {
        return new Long((a.lo >>> b) | (a.hi << (32 - b)), a.hi >>> b);
    } else {
        return new Long((a.hi >>> (b - 32)), 0);
    }
}

// Represents a mutable 80-bit unsigned integer
LongInt = function(lo, hi, sup) {
    this.lo = lo;
    this.hi = hi;
    this.sup = sup;
}
LongInt_mul = function(a, b) {
    var a_lolo = ((a.lo & 0xFFFF) * b) | 0;
    var a_lohi = ((a.lo >>> 16) * b) | 0;
    var a_hilo = ((a.hi & 0xFFFF) * b) | 0;
    var a_hihi = ((a.hi >>> 16) * b) | 0;
    var sup = (a.sup * b) | 0;

    a_lohi = (a_lohi + (a_lolo >> 16)) | 0;
    a_hilo = (a_hilo + (a_lohi >> 16)) | 0;
    a_hihi = (a_hihi + (a_hilo >> 16)) | 0;
    sup = (sup + (a_hihi >> 16)) | 0;
    a.lo = (a_lolo & 0xFFFF) | (a_lohi << 16);
    a.hi = (a_hilo & 0xFFFF) | (a_hihi << 16);
    a.sup = sup & 0xFFFF;
}
LongInt_sub = function(a, b) {
    var a_lolo = a.lo & 0xFFFF;
    var a_lohi = a.lo >>> 16;
    var a_hilo = a.hi & 0xFFFF;
    var a_hihi = a.hi >>> 16;
    var b_lolo = b.lo & 0xFFFF;
    var b_lohi = b.lo >>> 16;
    var b_hilo = b.hi & 0xFFFF;
    var b_hihi = b.hi >>> 16;

    a_lolo = (a_lolo - b_lolo) | 0;
    a_lohi = (a_lohi - b_lohi + (a_lolo >> 16)) | 0;
    a_hilo = (a_hilo - b_hilo + (a_lohi >> 16)) | 0;
    a_hihi = (a_hihi - b_hihi + (a_hilo >> 16)) | 0;
    sup = (a.sup - b.sup + (a_hihi >> 16)) | 0;
    a.lo = (a_lolo & 0xFFFF) | ((a_lohi & 0xFFFF) << 16);
    a.hi = (a_hilo & 0xFFFF) | ((a_hihi & 0xFFFF) << 16);
    a.sup = sup;
}
LongInt_add = function(a, b) {
    var a_lolo = a.lo & 0xFFFF;
    var a_lohi = a.lo >>> 16;
    var a_hilo = a.hi & 0xFFFF;
    var a_hihi = a.hi >>> 16;
    var b_lolo = b.lo & 0xFFFF;
    var b_lohi = b.lo >>> 16;
    var b_hilo = b.hi & 0xFFFF;
    var b_hihi = b.hi >>> 16;

    a_lolo = (a_lolo + b_lolo) | 0;
    a_lohi = (a_lohi + b_lohi + (a_lolo >> 16)) | 0;
    a_hilo = (a_hilo + b_hilo + (a_lohi >> 16)) | 0;
    a_hihi = (a_hihi + b_hihi + (a_hilo >> 16)) | 0;
    sup = (a.sup + b.sup + (a_hihi >> 16)) | 0;
    a.lo = (a_lolo & 0xFFFF) | (a_lohi << 16);
    a.hi = (a_hilo & 0xFFFF) | (a_hihi << 16);
    a.sup = sup;
}
LongInt_ucompare = function(a, b) {
    var r = (a.sup - b.sup);
    if (r != 0) {
        return r;
    }
    var r = (a.hi >>> 1) - (b.hi >>> 1);
    if (r != 0) {
        return r;
    }
    var r = (a.hi & 1) - (b.hi & 1);
    if (r != 0) {
        return r;
    }
    var r = (a.lo >>> 1) - (b.lo >>> 1);
    if (r != 0) {
        return r;
    }
    return (a.lo & 1) - (b.lo & 1);
}
LongInt_numOfLeadingZeroBits = function(a) {
    var n = 0;
    var d = 16;
    while (d > 0) {
        if ((a >>> d) !== 0) {
            a >>>= d;
            n = (n + d) | 0;
        }
        d = (d / 2) | 0;
    }
    return 31 - n;
}
LongInt_shl = function(a, b) {
    if (b < 32) {
        a.sup = ((a.hi >>> (32 - b)) | (a.sup << b)) & 0xFFFF;
        a.hi = (a.lo >>> (32 - b)) | (a.hi << b);
        a.lo <<= b;
    } else if (b < 64) {
        a.sup = ((a.lo >>> (64 - b)) | (a.hi << (b - 32))) & 0xFFFF;
        a.hi = a.lo << b;
        a.lo = 0;
    } else {
        a.sup = (a.lo << (b - 64)) & 0xFFFF;
        a.hi = 0;
        a.lo = 0;
    }
}
LongInt_shr = function(a, b) {
    if (b < 32) {
        a.lo = (a.lo >>> b) | (a.hi << (32 - b));
        a.hi = (a.hi >>> b) | (a.sup << (32 - b));
        a.sup >>>= b;
    } else if (b < 64) {
        a.lo = (a.hi >>> (b - 32)) | (a.sup << (64 - b));
        a.hi = a.sup >>> (b - 32);
        a.sup = 0;
    } else {
        a.lo = a.sup >>> (b - 64);
        a.hi = 0;
        a.sup = 0;
    }
}
LongInt_copy = function(a) {
    return new LongInt(a.lo, a.hi, a.sup);
}
LongInt_div = function(a, b) {
    // Normalize divisor
    var bits = b.hi !== 0 ? LongInt_numOfLeadingZeroBits(b.hi) : LongInt_numOfLeadingZeroBits(b.lo) + 32;
    var sz = 1 + ((bits / 16) | 0);
    var dividentBits = bits % 16;
    LongInt_shl(b, bits);
    LongInt_shl(a, dividentBits);
    q = new LongInt(0, 0, 0);
    while (sz-- > 0) {
        LongInt_shl(q, 16);
        // Calculate approximate q
        var digitA = (a.hi >>> 16) + (0x10000 * a.sup);
        var digitB = b.hi >>> 16;
        var digit = (digitA / digitB) | 0;
        var t = LongInt_copy(b);
        LongInt_mul(t, digit);
        // Adjust q either down or up
        if (LongInt_ucompare(t, a) >= 0) {
            while (LongInt_ucompare(t, a) > 0) {
                LongInt_sub(t, b);
                q = (q - 1) | 0;
            }
        } else {
            while (true) {
                var nextT = LongInt_copy(t);
                LongInt_add(nextT, b);
                if (LongInt_ucompare(nextT, a) > 0) {
                    break;
                }
                t = nextT;
                q = (q + 1) | 0;
            }
        }
        LongInt_sub(a, t);
        q.lo |= digit;
        LongInt_shl(a, 16);
    }
    LongInt_shr(a, bits + 16);
    return q;
}