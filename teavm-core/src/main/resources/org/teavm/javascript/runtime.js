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
    var arr = new Array(sz);
    arr.$class = $rt_arraycls(cls);
    arr.$id = $rt_lastObjectId++;
    for (var i = 0; i < sz; i = (i + 1) | 0) {
        arr[i] = null;
    }
    return arr;
}
$rt_arraycls = function(cls) {
    if (cls.$array == undefined) {
        cls.$array = {
            $meta : { item : cls },
        };
        if ($rt.objcls) {
            cls.$array.$meta.supertypes = [$rt.objcls()];
        }
    }
    return cls.$array;
}

$rt = {
    createBooleanArray : function(cls, sz) {
        var arr = $rt.createArray(cls, sz);
        for (var i = 0; i < sz; i = (i + 1) | 0) {
            arr[i] = false;
        }
        return arr;
    },
    createNumericArray : function(cls, sz) {
        var arr = $rt.createArray(cls, sz);
        for (var i = 0; i < sz; i = (i + 1) | 0) {
            arr[i] = 0;
        }
        return arr;
    },
    createLongArray : function(sz) {
        var arr = $rt.createArray($rt.longcls(), sz);
        for (var i = 0; i < sz; i = (i + 1) | 0) {
            arr[i] = Long.ZERO;
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
    createcls : function() {
        return {
            $meta : {
                supertypes : []
            }
        };
    },
    booleancls : function() {
        if ($rt.booleanclsCache == null) {
            $rt.booleanclsCache = $rt.createcls();
        }
        return $rt.booleanclsCache;
    },
    charcls : function() {
        if ($rt.charclsCache == null) {
            $rt.charclsCache = $rt.createcls();
        }
        return $rt.charclsCache;
    },
    bytecls : function() {
        if ($rt.byteclsCache == null) {
            $rt.byteclsCache = $rt.createcls();
        }
        return $rt.byteclsCache;
    },
    shortcls : function() {
        if ($rt.shortclsCache == null) {
            $rt.shortclsCache = $rt.createcls();
        }
        return $rt.shortclsCache;
    },
    intcls : function() {
        if ($rt.intclsCache == null) {
            $rt.intclsCache = $rt.createcls();
        }
        return $rt.intclsCache;
    },
    longcls : function() {
        if ($rt.longclsCache == null) {
            $rt.longclsCache = $rt.createcls();
        }
        return $rt.longclsCache;
    },
    floatcls : function() {
        if ($rt.floatclsCache == null) {
            $rt.floatclsCache = $rt.createcls();
        }
        return $rt.floatclsCache;
    },
    doublecls : function() {
        if ($rt.doubleclsCache == null) {
            $rt.doubleclsCache = $rt.createcls();
        }
        return $rt.doubleclsCache;
    },
    voidcls : function() {
        if ($rt.voidclsCache == null) {
            $rt.voidclsCache = $rt.createcls();
        }
        return $rt.voidclsCache;
    },
    equals : function(a, b) {
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
    },
    clinit : function(cls) {
        if (cls.$clinit) {
            var f = cls.$clinit;
            delete cls.$clinit;
            f();
        }
        return cls;
    },
    init : function(cls, constructor, args) {
        var obj = new cls();
        cls.prototype[constructor].apply(obj, args);
        return obj;
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
Long.ZERO = new Long(0, 0);
Long.fromInt = function(val) {
    return new Long(val, 0);
}
Long.fromNumber = function(val) {
    return new Long(val | 0, (val / 0x100000000) | 0);
}
Long.toNumber = function(val) {
    return val.lo + 0x100000000 * val.hi;
}
Long.add = function(a, b) {
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
Long.inc = function(a) {
    var lo = (a.lo + 1) | 0;
    var hi = a.hi;
    if (lo === 0) {
        hi = (hi + 1) | 0;
    }
    return new Long(lo, hi);
}
Long.dec = function(a) {
    var lo = (a.lo - 1) | 0;
    var hi = a.hi;
    if (lo === -1) {
        hi = (hi - 1) | 0;
    }
    return new Long(lo, hi);
}
Long.neg = function(a) {
    return Long.inc(new Long(a.lo ^ 0xFFFFFFFF, a.hi ^ 0xFFFFFFFF));
}
Long.sub = function(a, b) {
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
Long.compare = function(a, b) {
    var r = a.hi - a.hi;
    if (r != 0) {
        return r;
    }
    return a.lo - b.lo;
}
Long.isNegative = function(a) {
    return a.hi < 0;
}
Long.mul = function(a, b) {
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
Long.div = function(a, b) {
    var result = (a.hi * 0x100000000 + a.lo) / (b.hi * 0x100000000 + b.lo);
    return new Long(result | 0, (result / 0x100000000) | 0);
}
Long.rem = function(a, b) {
    var result = (a.hi * 0x100000000 + a.lo) % (b.hi * 0x100000000 + b.lo);
    return new Long(result | 0, (result / 0x100000000) | 0);
}
Long.and = function(a, b) {
    return new Long(a.lo & b.lo, a.hi & b.hi);
}
Long.or = function(a, b) {
    return new Long(a.lo | b.lo, a.hi | b.hi);
}
Long.xor = function(a, b) {
    return new Long(a.lo ^ b.lo, a.hi ^ b.hi);
}
Long.shl = function(a, b) {
    if (b < 32) {
        return new Long(a.lo << b, (a.lo >>> (32 - b)) | (a.hi << b));
    } else {
        return new Long(0, a.lo << (b - 32));
    }
}
Long.shr = function(a, b) {
    if (b < 32) {
        return new Long((a.lo >>> b) | (a.hi << (32 - b)), a.hi >> b);
    } else {
        return new Long((a.hi >> (b - 32)), -1);
    }
}
Long.shru = function(a, b) {
    if (b < 32) {
        return new Long((a.lo >>> b) | (a.hi << (32 - b)), a.hi >>> b);
    } else {
        return new Long((a.hi >>> (b - 32)), 0);
    }
}