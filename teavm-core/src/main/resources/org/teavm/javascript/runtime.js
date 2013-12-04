$rt_lastObjectId = 0;
$rt_nextId = function() {
    return $rt_lastObjectId++;
}
$rt_compare = function(a, b) {
    return a > b ? 1 : a < b ? -1 : 0;
}
$rt_isInstance = function(obj, cls) {
    return $rt_isAssignable(obj.$class, cls);
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
    arr.$id = $rt_nextId();
    for (var i = 0; i < sz; i = (i + 1) | 0) {
        arr.data[i] = null;
    }
    return arr;
}
$rt_createNumericArray = function(cls, sz) {
    var arr = $rt_createArray(cls, sz);
    for (var i = 0; i < sz; i = (i + 1) | 0) {
        arr.data[i] = 0;
    }
    return arr;
}
$rt_createLongArray = function(sz) {
    var arr = $rt.createArray($rt_longcls(), sz);
    for (var i = 0; i < sz; i = (i + 1) | 0) {
        arr.data[i] = Long.ZERO;
    }
    return arr;
},
$rt_arraycls = function(cls) {
    if (cls.$array == undefined) {
        var arraycls = function(data) {
            this.data = data;
            this.$class = arraycls;
        };
        arraycls.prototype = new ($rt_objcls())();
        arraycls.$meta = { item : cls, supertypes : [$rt_objcls()], primitive : false };
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
    if ($rt_intclsCache == null) {
        $rt_intclsCache = $rt_createcls();
        $rt_intclsCache.primitive = true;
        $rt_intclsCache.name = "int";
    }
    return $rt_intclsCache;
}
$rt_longclsCache = null;
$rt_longcls = function() {
    if ($rt_longclsCache == null) {
        $rt_longclsCache = $rt_createcls();
        $rt_longclsCache.primitive = true;
        $rt_longclsCache.name = "long";
    }
    return $rt_longclsCache;
}
$rt_floatclsCache = null;
$rt_floatcls = function() {
    if ($rt_floatclsCache == null) {
        $rt_floatclsCache = $rt_createcls();
        $rt_floatclsCache.primitive = true;
        $rt_floatclsCache.name = "float";
    }
    return $rt_floatclsCache;
}
$rt_doubleclsCache = null;
$rt_doublecls = function() {
    if ($rt_doubleclsCache == null) {
        $rt_doubleclsCache = $rt_createcls();
        $rt_doubleclsCache.primitive = true;
        $rt_doubleclsCache.name = "double";
    }
    return $rt_doubleclsCache;
}
$rt_voidclsCache = null;
$rt_voidcls = function() {
    if ($rt_voidclsCache == null) {
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
    var err = new Error("Java exception thrown");
    err.$javaException = ex;
    throw err;
}
$rt_byteToInt = function(value) {
    return value > 0xFF ? value | 0xFFFFFF00 : value;
}
$rt_shortToInt = function(value) {
    return value > 0xFFFF ? value | 0xFFFF0000 : value;
}

$rt = {
    createBooleanArray : function(cls, sz) {
        var arr = $rt.createArray(cls, sz);
        for (var i = 0; i < sz; i = (i + 1) | 0) {
            arr[i] = false;
        }
        return arr;
    },
    createMultiArray : function(cls, dimensions) {
        for (var i = 1; i < dimensions.length; i = (i + 1) | 0) {
            cls = $rt.arraycls(cls);
        }
        return $rt.createMultiArrayImpl(cls, dimensions, 0);
    },
    createMultiArrayImpl : function(cls, dimensions, offset) {
        var result = $rt.createArray(cls, dimensions[offset]);
        offset = (offset + 1) | 0;
        if (offset < dimensions.length) {
            cls = cls.$meta.item;
            for (var i = 0; i < result.length; i = (i + 1) | 0) {
                result[i] = $rt.createMultiArrayImpl(cls, dimensions, offset);
            }
        }
        return result;
    },
    initializeArray : function(cls, initial) {
        var arr = initial.slice();
        arr.$class = $rt.arraycls(cls);
        $rt.setId(arr, $rt.lastObjectId++);
        return arr;
    },
    assertNotNaN : function(value) {
        if (typeof value == 'number' && isNaN(value)) {
            throw "NaN";
        }
        return value;
    }
};

Long = function(lo, hi) {
    this.lo = lo | 0;
    this.hi = hi | 0;
}
Long_ZERO = new Long(0, 0);
Long_fromInt = function(val) {
    return new Long(val, 0);
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
    return Long.inc(new Long(a.lo ^ 0xFFFFFFFF, a.hi ^ 0xFFFFFFFF));
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
    var r = a.hi - a.hi;
    if (r != 0) {
        return r;
    }
    return a.lo - b.lo;
}
Long_isNegative = function(a) {
    return a.hi < 0;
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
    var hihi = (a_hihi * b_lolo + a_hilo * b_lohi + a_lohi * b_hilo + a_lolo * b_hihi +
            (hilo >> 16)) | 0;
    return new Long((lolo & 0xFFFF) | ((lohi & 0xFFFF) << 16),
            (hilo & 0xFFFF) | ((hihi & 0xFFFF) << 16));
}
Long_div = function(a, b) {
    var result = (a.hi * 0x100000000 + a.lo) / (b.hi * 0x100000000 + b.lo);
    return new Long(result | 0, (result / 0x100000000) | 0);
}
Long_rem = function(a, b) {
    var result = (a.hi * 0x100000000 + a.lo) % (b.hi * 0x100000000 + b.lo);
    return new Long(result | 0, (result / 0x100000000) | 0);
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