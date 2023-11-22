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
let Long_divRem;
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

let Long_eq;
let Long_ne;
let Long_gt;
let Long_ge;
let Long_lt;
let Long_le;
let Long_compare;
let Long_ucompare;
let Long_add;
let Long_sub;
let Long_inc;
let Long_dec;
let Long_mul;
let Long_div;
let Long_rem;
let Long_udiv;
let Long_urem;
let Long_neg;
let Long_and;
let Long_or;
let Long_xor;
let Long_shl;
let Long_shr;
let Long_shru;
let Long_not;

if (typeof teavm_globals.BigInt !== 'function') {
    Long_eq = (a, b) => a.hi === b.hi && a.lo === b.lo;

    Long_ne = (a, b) => a.hi !== b.hi || a.lo !== b.lo;

    Long_gt = (a, b) => {
        if (a.hi < b.hi) {
            return false;
        }
        if (a.hi > b.hi) {
            return true;
        }
        let x = a.lo >>> 1;
        let y = b.lo >>> 1;
        if (x !== y) {
            return x > y;
        }
        return (a.lo & 1) > (b.lo & 1);
    }

    Long_ge = (a, b) => {
        if (a.hi < b.hi) {
            return false;
        }
        if (a.hi > b.hi) {
            return true;
        }
        let x = a.lo >>> 1;
        let y = b.lo >>> 1;
        if (x !== y) {
            return x >= y;
        }
        return (a.lo & 1) >= (b.lo & 1);
    }

    Long_lt = (a, b) => {
        if (a.hi > b.hi) {
            return false;
        }
        if (a.hi < b.hi) {
            return true;
        }
        let x = a.lo >>> 1;
        let y = b.lo >>> 1;
        if (x !== y) {
            return x < y;
        }
        return (a.lo & 1) < (b.lo & 1);
    }

    Long_le = (a, b) => {
        if (a.hi > b.hi) {
            return false;
        }
        if (a.hi < b.hi) {
            return true;
        }
        let x = a.lo >>> 1;
        let y = b.lo >>> 1;
        if (x !== y) {
            return x <= y;
        }
        return (a.lo & 1) <= (b.lo & 1);
    }

    Long_add = (a, b) => {
        if (a.hi === (a.lo >> 31) && b.hi === (b.lo >> 31)) {
            return Long_fromNumber(a.lo + b.lo);
        } else if (Math.abs(a.hi) < Long_MAX_NORMAL && Math.abs(b.hi) < Long_MAX_NORMAL) {
            return Long_fromNumber(Long_toNumber(a) + Long_toNumber(b));
        }
        let a_lolo = a.lo & 0xFFFF;
        let a_lohi = a.lo >>> 16;
        let a_hilo = a.hi & 0xFFFF;
        let a_hihi = a.hi >>> 16;
        let b_lolo = b.lo & 0xFFFF;
        let b_lohi = b.lo >>> 16;
        let b_hilo = b.hi & 0xFFFF;
        let b_hihi = b.hi >>> 16;

        let lolo = (a_lolo + b_lolo) | 0;
        let lohi = (a_lohi + b_lohi + (lolo >> 16)) | 0;
        let hilo = (a_hilo + b_hilo + (lohi >> 16)) | 0;
        let hihi = (a_hihi + b_hihi + (hilo >> 16)) | 0;
        return new Long((lolo & 0xFFFF) | ((lohi & 0xFFFF) << 16), (hilo & 0xFFFF) | ((hihi & 0xFFFF) << 16));
    }

    Long_inc = a => {
        let lo = (a.lo + 1) | 0;
        let hi = a.hi;
        if (lo === 0) {
            hi = (hi + 1) | 0;
        }
        return new Long(lo, hi);
    }

    Long_dec = a => {
        let lo = (a.lo - 1) | 0;
        let hi = a.hi;
        if (lo === -1) {
            hi = (hi - 1) | 0;
        }
        return new Long(lo, hi);
    }

    Long_neg = a => Long_inc(new Long(a.lo ^ 0xFFFFFFFF, a.hi ^ 0xFFFFFFFF))

    Long_sub = (a, b) => {
        if (a.hi === (a.lo >> 31) && b.hi === (b.lo >> 31)) {
            return Long_fromNumber(a.lo - b.lo);
        }
        let a_lolo = a.lo & 0xFFFF;
        let a_lohi = a.lo >>> 16;
        let a_hilo = a.hi & 0xFFFF;
        let a_hihi = a.hi >>> 16;
        let b_lolo = b.lo & 0xFFFF;
        let b_lohi = b.lo >>> 16;
        let b_hilo = b.hi & 0xFFFF;
        let b_hihi = b.hi >>> 16;

        let lolo = (a_lolo - b_lolo) | 0;
        let lohi = (a_lohi - b_lohi + (lolo >> 16)) | 0;
        let hilo = (a_hilo - b_hilo + (lohi >> 16)) | 0;
        let hihi = (a_hihi - b_hihi + (hilo >> 16)) | 0;
        return new Long((lolo & 0xFFFF) | ((lohi & 0xFFFF) << 16), (hilo & 0xFFFF) | ((hihi & 0xFFFF) << 16));
    }

    Long_compare = (a, b) => {
        let r = a.hi - b.hi;
        if (r !== 0) {
            return r;
        }
        r = (a.lo >>> 1) - (b.lo >>> 1);
        if (r !== 0) {
            return r;
        }
        return (a.lo & 1) - (b.lo & 1);
    }

    Long_ucompare = (a, b) => {
        let r = $rt_ucmp(a.hi, b.hi);
        if (r !== 0) {
            return r;
        }
        r = (a.lo >>> 1) - (b.lo >>> 1);
        if (r !== 0) {
            return r;
        }
        return (a.lo & 1) - (b.lo & 1);
    }

    Long_mul = (a, b) => {
        let positive = Long_isNegative(a) === Long_isNegative(b);
        if (Long_isNegative(a)) {
            a = Long_neg(a);
        }
        if (Long_isNegative(b)) {
            b = Long_neg(b);
        }
        let a_lolo = a.lo & 0xFFFF;
        let a_lohi = a.lo >>> 16;
        let a_hilo = a.hi & 0xFFFF;
        let a_hihi = a.hi >>> 16;
        let b_lolo = b.lo & 0xFFFF;
        let b_lohi = b.lo >>> 16;
        let b_hilo = b.hi & 0xFFFF;
        let b_hihi = b.hi >>> 16;

        let lolo = 0;
        let lohi = 0;
        let hilo = 0;
        let hihi = 0;
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
        let result = new Long((lolo & 0xFFFF) | (lohi << 16), (hilo & 0xFFFF) | (hihi << 16));
        return positive ? result : Long_neg(result);
    }

    Long_div = (a, b) => {
        if (Math.abs(a.hi) < Long_MAX_NORMAL && Math.abs(b.hi) < Long_MAX_NORMAL) {
            return Long_fromNumber(Long_toNumber(a) / Long_toNumber(b));
        }
        return Long_divRem(a, b)[0];
    }

    Long_udiv = (a, b) => {
        if (a.hi >= 0 && a.hi < Long_MAX_NORMAL && b.hi >= 0 && b.hi < Long_MAX_NORMAL) {
            return Long_fromNumber(Long_toNumber(a) / Long_toNumber(b));
        }
        return Long_udivRem(a, b)[0];
    }

    Long_rem = (a, b) => {
        if (Math.abs(a.hi) < Long_MAX_NORMAL && Math.abs(b.hi) < Long_MAX_NORMAL) {
            return Long_fromNumber(Long_toNumber(a) % Long_toNumber(b));
        }
        return Long_divRem(a, b)[1];
    }

    Long_urem = (a, b) => {
        if (a.hi >= 0 && a.hi < Long_MAX_NORMAL && b.hi >= 0 && b.hi < Long_MAX_NORMAL) {
            return Long_fromNumber(Long_toNumber(a) / Long_toNumber(b));
        }
        return Long_udivRem(a, b)[1];
    }

    Long_divRem = (a, b) => {
        if (b.lo === 0 && b.hi === 0) {
            throw new teavm_globals.Error("Division by zero");
        }
        let positive = Long_isNegative(a) === Long_isNegative(b);
        if (Long_isNegative(a)) {
            a = Long_neg(a);
        }
        if (Long_isNegative(b)) {
            b = Long_neg(b);
        }
        a = new LongInt(a.lo, a.hi, 0);
        b = new LongInt(b.lo, b.hi, 0);
        let q = LongInt_div(a, b);
        a = new Long(a.lo, a.hi);
        q = new Long(q.lo, q.hi);
        return positive ? [q, a] : [Long_neg(q), Long_neg(a)];
    };

    let Long_udivRem = (a, b) => {
        if (b.lo === 0 && b.hi === 0) {
            throw new teavm_globals.Error("Division by zero");
        }
        a = new LongInt(a.lo, a.hi, 0);
        b = new LongInt(b.lo, b.hi, 0);
        let q = LongInt_div(a, b);
        a = new Long(a.lo, a.hi);
        q = new Long(q.lo, q.hi);
        return [q, a];
    };

    Long_and = (a, b) => new Long(a.lo & b.lo, a.hi & b.hi)

    Long_or = (a, b) => new Long(a.lo | b.lo, a.hi | b.hi)

    Long_xor = (a, b) => new Long(a.lo ^ b.lo, a.hi ^ b.hi)

    Long_shl = (a, b) => {
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

    Long_shr = (a, b) => {
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

    Long_shru = (a, b) => {
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

    Long_not = a => new Long(~a.hi, ~a.lo)

    // Represents a mutable 80-bit unsigned integer
    function LongInt(lo, hi, sup) {
        this.lo = lo;
        this.hi = hi;
        this.sup = sup;
    }

    let LongInt_mul = (a, b) => {
        let a_lolo = ((a.lo & 0xFFFF) * b) | 0;
        let a_lohi = ((a.lo >>> 16) * b) | 0;
        let a_hilo = ((a.hi & 0xFFFF) * b) | 0;
        let a_hihi = ((a.hi >>> 16) * b) | 0;
        let sup = (a.sup * b) | 0;

        a_lohi = (a_lohi + (a_lolo >>> 16)) | 0;
        a_hilo = (a_hilo + (a_lohi >>> 16)) | 0;
        a_hihi = (a_hihi + (a_hilo >>> 16)) | 0;
        sup = (sup + (a_hihi >>> 16)) | 0;
        a.lo = (a_lolo & 0xFFFF) | (a_lohi << 16);
        a.hi = (a_hilo & 0xFFFF) | (a_hihi << 16);
        a.sup = sup & 0xFFFF;
    };

    let LongInt_sub = (a, b) => {
        let a_lolo = a.lo & 0xFFFF;
        let a_lohi = a.lo >>> 16;
        let a_hilo = a.hi & 0xFFFF;
        let a_hihi = a.hi >>> 16;
        let b_lolo = b.lo & 0xFFFF;
        let b_lohi = b.lo >>> 16;
        let b_hilo = b.hi & 0xFFFF;
        let b_hihi = b.hi >>> 16;

        a_lolo = (a_lolo - b_lolo) | 0;
        a_lohi = (a_lohi - b_lohi + (a_lolo >> 16)) | 0;
        a_hilo = (a_hilo - b_hilo + (a_lohi >> 16)) | 0;
        a_hihi = (a_hihi - b_hihi + (a_hilo >> 16)) | 0;
        let sup = (a.sup - b.sup + (a_hihi >> 16)) | 0;
        a.lo = (a_lolo & 0xFFFF) | (a_lohi << 16);
        a.hi = (a_hilo & 0xFFFF) | (a_hihi << 16);
        a.sup = sup;
    };

    let LongInt_add = (a, b) => {
        let a_lolo = a.lo & 0xFFFF;
        let a_lohi = a.lo >>> 16;
        let a_hilo = a.hi & 0xFFFF;
        let a_hihi = a.hi >>> 16;
        let b_lolo = b.lo & 0xFFFF;
        let b_lohi = b.lo >>> 16;
        let b_hilo = b.hi & 0xFFFF;
        let b_hihi = b.hi >>> 16;

        a_lolo = (a_lolo + b_lolo) | 0;
        a_lohi = (a_lohi + b_lohi + (a_lolo >> 16)) | 0;
        a_hilo = (a_hilo + b_hilo + (a_lohi >> 16)) | 0;
        a_hihi = (a_hihi + b_hihi + (a_hilo >> 16)) | 0;
        let sup = (a.sup + b.sup + (a_hihi >> 16)) | 0;
        a.lo = (a_lolo & 0xFFFF) | (a_lohi << 16);
        a.hi = (a_hilo & 0xFFFF) | (a_hihi << 16);
        a.sup = sup;
    };

    let LongInt_inc = a => {
        a.lo = (a.lo + 1) | 0;
        if (a.lo === 0) {
            a.hi = (a.hi + 1) | 0;
            if (a.hi === 0) {
                a.sup = (a.sup + 1) & 0xFFFF;
            }
        }
    };

    let LongInt_dec = a => {
        a.lo = (a.lo - 1) | 0;
        if (a.lo === -1) {
            a.hi = (a.hi - 1) | 0;
            if (a.hi === -1) {
                a.sup = (a.sup - 1) & 0xFFFF;
            }
        }
    };

    let LongInt_ucompare = (a, b) => {
        let r = (a.sup - b.sup);
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
    };

    let LongInt_numOfLeadingZeroBits = a => {
        let n = 0;
        let d = 16;
        while (d > 0) {
            if ((a >>> d) !== 0) {
                a >>>= d;
                n = (n + d) | 0;
            }
            d = (d / 2) | 0;
        }
        return 31 - n;
    };

    let LongInt_shl = (a, b) => {
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
    };

    let LongInt_shr = (a, b) => {
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
    };

    let LongInt_copy = a => new LongInt(a.lo, a.hi, a.sup);

    let LongInt_div = (a, b) => {
        // Normalize divisor
        let bits = b.hi !== 0 ? LongInt_numOfLeadingZeroBits(b.hi) : LongInt_numOfLeadingZeroBits(b.lo) + 32;
        let sz = 1 + ((bits / 16) | 0);
        let dividentBits = bits % 16;
        LongInt_shl(b, bits);
        LongInt_shl(a, dividentBits);
        let q = new LongInt(0, 0, 0);
        while (sz-- > 0) {
            LongInt_shl(q, 16);
            // Calculate approximate q
            let digitA = (a.hi >>> 16) + (0x10000 * a.sup);
            let digitB = b.hi >>> 16;
            let digit = (digitA / digitB) | 0;
            let t = LongInt_copy(b);
            LongInt_mul(t, digit);
            // Adjust q either down or up
            if (LongInt_ucompare(t, a) >= 0) {
                while (LongInt_ucompare(t, a) > 0) {
                    LongInt_sub(t, b);
                    --digit;
                }
            } else {
                while (true) {
                    let nextT = LongInt_copy(t);
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
    };
} else {
    Long_eq = (a, b) => a === b

    Long_ne = (a, b) => a !== b

    Long_gt = (a, b) => a > b

    Long_ge = (a, b) => a >= b

    Long_lt = (a, b) => a < b

    Long_le = (a, b) => a <= b

    Long_add = (a, b) => teavm_globals.BigInt.asIntN(64, a + b)

    Long_inc = a => teavm_globals.BigInt.asIntN(64, a + 1)

    Long_dec = a => teavm_globals.BigInt.asIntN(64, a - 1)

    Long_neg = a => teavm_globals.BigInt.asIntN(64, -a)

    Long_sub = (a, b) => teavm_globals.BigInt.asIntN(64, a - b)

    Long_compare = (a, b) => a < b ? -1 : a > b ? 1 : 0
    Long_ucompare = (a, b) => {
        a = teavm_globals.BigInt.asUintN(64, a);
        b = teavm_globals.BigInt.asUintN(64, b);
        return a < b ? -1 : a > b ? 1 : 0;
    }

    Long_mul = (a, b) => teavm_globals.BigInt.asIntN(64, a * b)

    Long_div = (a, b) => teavm_globals.BigInt.asIntN(64, a / b)

    Long_udiv = (a, b) => teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt.asUintN(64, a) /
        teavm_globals.BigInt.asUintN(64, b))

    Long_rem = (a, b) => teavm_globals.BigInt.asIntN(64, a % b)

    Long_urem = (a, b) => teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt.asUintN(64, a) %
        teavm_globals.BigInt.asUintN(64, b))

    Long_and = (a, b) => teavm_globals.BigInt.asIntN(64, a & b)

    Long_or = (a, b) => teavm_globals.BigInt.asIntN(64, a | b)

    Long_xor = (a, b) => teavm_globals.BigInt.asIntN(64, a ^ b)

    Long_shl = (a, b) => teavm_globals.BigInt.asIntN(64, a << teavm_globals.BigInt(b & 63))

    Long_shr = (a, b) => teavm_globals.BigInt.asIntN(64, a >> teavm_globals.BigInt(b & 63))

    Long_shru = (a, b) => teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt.asUintN(64, a) >>
        teavm_globals.BigInt(b & 63))

    Long_not = a => teavm_globals.BigInt.asIntN(64, ~a)
}
