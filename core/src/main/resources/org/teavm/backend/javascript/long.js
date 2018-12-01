/*
 *  Copyright 2018 Alexey Andreev.
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
function Long_eq(a, b) {
    return a.hi === b.hi && a.lo === b.lo;
}
function Long_ne(a, b) {
    return a.hi !== b.hi || a.lo !== b.lo;
}
function Long_gt(a, b) {
    if (a.hi < b.hi) {
        return false;
    }
    if (a.hi > b.hi) {
        return true;
    }
    var x = a.lo >>> 1;
    var y = b.lo >>> 1;
    if (x !== y) {
        return x > y;
    }
    return (a.lo & 1) > (b.lo & 1);
}
function Long_ge(a, b) {
    if (a.hi < b.hi) {
        return false;
    }
    if (a.hi > b.hi) {
        return true;
    }
    var x = a.lo >>> 1;
    var y = b.lo >>> 1;
    if (x !== y) {
        return x >= y;
    }
    return (a.lo & 1) >= (b.lo & 1);
}
function Long_lt(a, b) {
    if (a.hi > b.hi) {
        return false;
    }
    if (a.hi < b.hi) {
        return true;
    }
    var x = a.lo >>> 1;
    var y = b.lo >>> 1;
    if (x !== y) {
        return x < y;
    }
    return (a.lo & 1) < (b.lo & 1);
}
function Long_le(a, b) {
    if (a.hi > b.hi) {
        return false;
    }
    if (a.hi < b.hi) {
        return true;
    }
    var x = a.lo >>> 1;
    var y = b.lo >>> 1;
    if (x !== y) {
        return x <= y;
    }
    return (a.lo & 1) <= (b.lo & 1);
}

function Long_add(a, b) {
    if (a.hi === (a.lo >> 31) && b.hi === (b.lo >> 31)) {
        return Long_fromNumber(a.lo + b.lo);
    } else if (Math.abs(a.hi) < Long_MAX_NORMAL && Math.abs(b.hi) < Long_MAX_NORMAL) {
        return Long_fromNumber(Long_toNumber(a) + Long_toNumber(b));
    }
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
    return new Long((lolo & 0xFFFF) | ((lohi & 0xFFFF) << 16), (hilo & 0xFFFF) | ((hihi & 0xFFFF) << 16));
}
function Long_inc(a) {
    var lo = (a.lo + 1) | 0;
    var hi = a.hi;
    if (lo === 0) {
        hi = (hi + 1) | 0;
    }
    return new Long(lo, hi);
}
function Long_dec(a) {
    var lo = (a.lo - 1) | 0;
    var hi = a.hi;
    if (lo === -1) {
        hi = (hi - 1) | 0;
    }
    return new Long(lo, hi);
}
function Long_neg(a) {
    return Long_inc(new Long(a.lo ^ 0xFFFFFFFF, a.hi ^ 0xFFFFFFFF));
}
function Long_sub(a, b) {
    if (a.hi === (a.lo >> 31) && b.hi === (b.lo >> 31)) {
        return Long_fromNumber(a.lo - b.lo);
    }
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
    return new Long((lolo & 0xFFFF) | ((lohi & 0xFFFF) << 16), (hilo & 0xFFFF) | ((hihi & 0xFFFF) << 16));
}
function Long_compare(a, b) {
    var r = a.hi - b.hi;
    if (r !== 0) {
        return r;
    }
    r = (a.lo >>> 1) - (b.lo >>> 1);
    if (r !== 0) {
        return r;
    }
    return (a.lo & 1) - (b.lo & 1);
}
function Long_isPositive(a) {
    return (a.hi & 0x80000000) === 0;
}
function Long_isNegative(a) {
    return (a.hi & 0x80000000) !== 0;
}
function Long_mul(a, b) {
    var positive = Long_isNegative(a) === Long_isNegative(b);
    if (Long_isNegative(a)) {
        a = Long_neg(a);
    }
    if (Long_isNegative(b)) {
        b = Long_neg(b);
    }
    var a_lolo = a.lo & 0xFFFF;
    var a_lohi = a.lo >>> 16;
    var a_hilo = a.hi & 0xFFFF;
    var a_hihi = a.hi >>> 16;
    var b_lolo = b.lo & 0xFFFF;
    var b_lohi = b.lo >>> 16;
    var b_hilo = b.hi & 0xFFFF;
    var b_hihi = b.hi >>> 16;

    var lolo = 0;
    var lohi = 0;
    var hilo = 0;
    var hihi = 0;
    lolo = (a_lolo * b_lolo) | 0;
    lohi = lolo >>> 16;
    lohi = ((lohi & 0xFFFF) + a_lohi * b_lolo) | 0;
    hilo = (hilo + (lohi >>> 16)) | 0;
    lohi = ((lohi & 0xFFFF) + a_lolo * b_lohi) | 0;
    hilo = (hilo + (lohi >>> 16)) | 0;
    hihi = hilo >>> 16;
    hilo = ((hilo & 0xFFFF) + a_hilo * b_lolo) | 0;
    hihi = (hihi + (hilo >>> 16)) | 0;
    hilo = ((hilo & 0xFFFF) + a_lohi * b_lohi) | 0;
    hihi = (hihi + (hilo >>> 16)) | 0;
    hilo = ((hilo & 0xFFFF) + a_lolo * b_hilo) | 0;
    hihi = (hihi + (hilo >>> 16)) | 0;
    hihi = (hihi + a_hihi * b_lolo + a_hilo * b_lohi + a_lohi * b_hilo + a_lolo * b_hihi) | 0;
    var result = new Long((lolo & 0xFFFF) | (lohi << 16), (hilo & 0xFFFF) | (hihi << 16));
    return positive ? result : Long_neg(result);
}
function Long_div(a, b) {
    if (Math.abs(a.hi) < Long_MAX_NORMAL && Math.abs(b.hi) < Long_MAX_NORMAL) {
        return Long_fromNumber(Long_toNumber(a) / Long_toNumber(b));
    }
    return Long_divRem(a, b)[0];
}
function Long_udiv(a, b) {
    if (a.hi >= 0 && a.hi < Long_MAX_NORMAL && b.hi >= 0 && b.hi < Long_MAX_NORMAL) {
        return Long_fromNumber(Long_toNumber(a) / Long_toNumber(b));
    }
    return Long_udivRem(a, b)[0];
}
function Long_rem(a, b) {
    if (Math.abs(a.hi) < Long_MAX_NORMAL && Math.abs(b.hi) < Long_MAX_NORMAL) {
        return Long_fromNumber(Long_toNumber(a) % Long_toNumber(b));
    }
    return Long_divRem(a, b)[1];
}
function Long_urem(a, b) {
    if (a.hi >= 0 && a.hi < Long_MAX_NORMAL && b.hi >= 0 && b.hi < Long_MAX_NORMAL) {
        return Long_fromNumber(Long_toNumber(a) / Long_toNumber(b));
    }
    return Long_udivRem(a, b)[1];
}
function Long_divRem(a, b) {
    if (b.lo === 0 && b.hi === 0) {
        throw new Error("Division by zero");
    }
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
function Long_udivRem(a, b) {
    if (b.lo === 0 && b.hi === 0) {
        throw new Error("Division by zero");
    }
    a = new LongInt(a.lo, a.hi, 0);
    b = new LongInt(b.lo, b.hi, 0);
    var q = LongInt_div(a, b);
    a = new Long(a.lo, a.hi);
    q = new Long(q.lo, q.hi);
    return [q, a];
}
function Long_shiftLeft16(a) {
    return new Long(a.lo << 16, (a.lo >>> 16) | (a.hi << 16));
}
function Long_shiftRight16(a) {
    return new Long((a.lo >>> 16) | (a.hi << 16), a.hi >>> 16);
}
function Long_and(a, b) {
    return new Long(a.lo & b.lo, a.hi & b.hi);
}
function Long_or(a, b) {
    return new Long(a.lo | b.lo, a.hi | b.hi);
}
function Long_xor(a, b) {
    return new Long(a.lo ^ b.lo, a.hi ^ b.hi);
}
function Long_shl(a, b) {
    b &= 63;
    if (b === 0) {
        return a;
    } else if (b < 32) {
        return new Long(a.lo << b, (a.lo >>> (32 - b)) | (a.hi << b));
    } else if (b === 32) {
        return new Long(0, a.lo);
    } else {
        return new Long(0, a.lo << (b - 32));
    }
}
function Long_shr(a, b) {
    b &= 63;
    if (b === 0) {
        return a;
    } else if (b < 32) {
        return new Long((a.lo >>> b) | (a.hi << (32 - b)), a.hi >> b);
    } else if (b === 32) {
        return new Long(a.hi, a.hi >> 31);
    } else {
        return new Long((a.hi >> (b - 32)), a.hi >> 31);
    }
}
function Long_shru(a, b) {
    b &= 63;
    if (b === 0) {
        return a;
    } else if (b < 32) {
        return new Long((a.lo >>> b) | (a.hi << (32 - b)), a.hi >>> b);
    } else if (b === 32) {
        return new Long(a.hi, 0);
    } else {
        return new Long((a.hi >>> (b - 32)), 0);
    }
}

// Represents a mutable 80-bit unsigned integer
function LongInt(lo, hi, sup) {
    this.lo = lo;
    this.hi = hi;
    this.sup = sup;
}
function LongInt_mul(a, b) {
    var a_lolo = ((a.lo & 0xFFFF) * b) | 0;
    var a_lohi = ((a.lo >>> 16) * b) | 0;
    var a_hilo = ((a.hi & 0xFFFF) * b) | 0;
    var a_hihi = ((a.hi >>> 16) * b) | 0;
    var sup = (a.sup * b) | 0;

    a_lohi = (a_lohi + (a_lolo >>> 16)) | 0;
    a_hilo = (a_hilo + (a_lohi >>> 16)) | 0;
    a_hihi = (a_hihi + (a_hilo >>> 16)) | 0;
    sup = (sup + (a_hihi >>> 16)) | 0;
    a.lo = (a_lolo & 0xFFFF) | (a_lohi << 16);
    a.hi = (a_hilo & 0xFFFF) | (a_hihi << 16);
    a.sup = sup & 0xFFFF;
}
function LongInt_sub(a, b) {
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
    var sup = (a.sup - b.sup + (a_hihi >> 16)) | 0;
    a.lo = (a_lolo & 0xFFFF) | (a_lohi << 16);
    a.hi = (a_hilo & 0xFFFF) | (a_hihi << 16);
    a.sup = sup;
}
function LongInt_add(a, b) {
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
    var sup = (a.sup + b.sup + (a_hihi >> 16)) | 0;
    a.lo = (a_lolo & 0xFFFF) | (a_lohi << 16);
    a.hi = (a_hilo & 0xFFFF) | (a_hihi << 16);
    a.sup = sup;
}
function LongInt_inc(a) {
    a.lo = (a.lo + 1) | 0;
    if (a.lo === 0) {
        a.hi = (a.hi + 1) | 0;
        if (a.hi === 0) {
            a.sup = (a.sup + 1) & 0xFFFF;
        }
    }
}
function LongInt_dec(a) {
    a.lo = (a.lo - 1) | 0;
    if (a.lo === -1) {
        a.hi = (a.hi - 1) | 0;
        if (a.hi === -1) {
            a.sup = (a.sup - 1) & 0xFFFF;
        }
    }
}
function LongInt_ucompare(a, b) {
    var r = (a.sup - b.sup);
    if (r !== 0) {
        return r;
    }
    r = (a.hi >>> 1) - (b.hi >>> 1);
    if (r !== 0) {
        return r;
    }
    r = (a.hi & 1) - (b.hi & 1);
    if (r !== 0) {
        return r;
    }
    r = (a.lo >>> 1) - (b.lo >>> 1);
    if (r !== 0) {
        return r;
    }
    return (a.lo & 1) - (b.lo & 1);
}
function LongInt_numOfLeadingZeroBits(a) {
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
function LongInt_shl(a, b) {
    if (b === 0) {
        return;
    }
    if (b < 32) {
        a.sup = ((a.hi >>> (32 - b)) | (a.sup << b)) & 0xFFFF;
        a.hi = (a.lo >>> (32 - b)) | (a.hi << b);
        a.lo <<= b;
    } else if (b === 32) {
        a.sup = a.hi & 0xFFFF;
        a.hi = a.lo;
        a.lo = 0;
    } else if (b < 64) {
        a.sup = ((a.lo >>> (64 - b)) | (a.hi << (b - 32))) & 0xFFFF;
        a.hi = a.lo << b;
        a.lo = 0;
    } else if (b === 64) {
        a.sup = a.lo & 0xFFFF;
        a.hi = 0;
        a.lo = 0;
    } else {
        a.sup = (a.lo << (b - 64)) & 0xFFFF;
        a.hi = 0;
        a.lo = 0;
    }
}
function LongInt_shr(a, b) {
    if (b === 0) {
        return;
    }
    if (b === 32) {
        a.lo = a.hi;
        a.hi = a.sup;
        a.sup = 0;
    } else if (b < 32) {
        a.lo = (a.lo >>> b) | (a.hi << (32 - b));
        a.hi = (a.hi >>> b) | (a.sup << (32 - b));
        a.sup >>>= b;
    } else if (b === 64) {
        a.lo = a.sup;
        a.hi = 0;
        a.sup = 0;
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
function LongInt_copy(a) {
    return new LongInt(a.lo, a.hi, a.sup);
}
function LongInt_div(a, b) {
    // Normalize divisor
    var bits = b.hi !== 0 ? LongInt_numOfLeadingZeroBits(b.hi) : LongInt_numOfLeadingZeroBits(b.lo) + 32;
    var sz = 1 + ((bits / 16) | 0);
    var dividentBits = bits % 16;
    LongInt_shl(b, bits);
    LongInt_shl(a, dividentBits);
    var q = new LongInt(0, 0, 0);
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
                --digit;
            }
        } else {
            while (true) {
                var nextT = LongInt_copy(t);
                LongInt_add(nextT, b);
                if (LongInt_ucompare(nextT, a) > 0) {
                    break;
                }
                t = nextT;
                ++digit;
            }
        }
        LongInt_sub(a, t);
        q.lo |= digit;
        LongInt_shl(a, 16);
    }
    LongInt_shr(a, bits + 16);
    return q;
}
