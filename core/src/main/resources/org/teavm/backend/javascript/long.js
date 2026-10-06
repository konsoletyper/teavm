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

let Long_MAX_NORMAL = 1 << 18;
// Conversions between BigInt and number go through typed arrays that share the same buffer,
// since JS engines handle them much faster than BigInt() and Number() calls.
let Long_create = (lo, hi) => {
    $rt_numberConversionIntArray[0] = lo;
    $rt_numberConversionIntArray[1] = hi;
    return $rt_numberConversionLongArray[0];
}
let Long_fromInt = val => teavm_globals.BigInt(val | 0);
let Long_fromNumber = val => {
    if (teavm_globals.Math.abs(val) < 9223372036854775808) {
        let t = teavm_globals.Math.trunc(val);
        $rt_numberConversionIntArray[0] = t | 0;
        $rt_numberConversionIntArray[1] = teavm_globals.Math.floor(t / 4294967296) | 0;
        return $rt_numberConversionLongArray[0];
    }
    return val !== val ? 0n : val > 0 ? 9223372036854775807n : -9223372036854775808n;
}
let Long_toNumber = val => {
    $rt_numberConversionLongArray[0] = val;
    return $rt_numberConversionIntArray[1] * 4294967296 + ($rt_numberConversionIntArray[0] >>> 0);
}
let Long_hi = val => {
    $rt_numberConversionLongArray[0] = val;
    return $rt_numberConversionIntArray[1];
}
let Long_lo = val => {
    $rt_numberConversionLongArray[0] = val;
    return $rt_numberConversionIntArray[0];
}

let Long_eq = (a, b) => a === b
let Long_ne = (a, b) => a !== b
let Long_gt = (a, b) => a > b
let Long_ge = (a, b) => a >= b
let Long_lt = (a, b) => a < b
let Long_le = (a, b) => a <= b
let Long_add = (a, b) => teavm_globals.BigInt.asIntN(64, a + b);
let Long_inc = a => teavm_globals.BigInt.asIntN(64, a + 1n);
let Long_dec = a => teavm_globals.BigInt.asIntN(64, a - 1n);
let Long_neg = a => teavm_globals.BigInt.asIntN(64, -a);
let Long_sub = (a, b) => teavm_globals.BigInt.asIntN(64, a - b);
let Long_compare = (a, b) => a < b ? -1 : a > b ? 1 : 0;
let Long_ucompare = (a, b) => {
    a = teavm_globals.BigInt.asUintN(64, a);
    b = teavm_globals.BigInt.asUintN(64, b);
    return a < b ? -1 : a > b ? 1 : 0;
}
let Long_mul = (a, b) => teavm_globals.BigInt.asIntN(64, a * b);
let Long_div = (a, b) => teavm_globals.BigInt.asIntN(64, a / b);
let Long_udiv = (a, b) => teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt.asUintN(64, a) /
        teavm_globals.BigInt.asUintN(64, b));
let Long_rem = (a, b) => teavm_globals.BigInt.asIntN(64, a % b);
let Long_urem = (a, b) => teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt.asUintN(64, a) %
        teavm_globals.BigInt.asUintN(64, b));
let Long_and = (a, b) => teavm_globals.BigInt.asIntN(64, a & b);
let Long_or = (a, b) => teavm_globals.BigInt.asIntN(64, a | b);
let Long_xor = (a, b) => teavm_globals.BigInt.asIntN(64, a ^ b);
// JS engines generate efficient code for BigInt shifts wrapped into asIntN(64, ...) only when shift amount
// is a constant, otherwise they fall back to arbitrary precision arithmetic. Multiplication is optimized well,
// so left shift is expressed as multiplication by a power of two. Right shifts are performed on 32-bit halves.
let Long_pows = function() {
    let result = [];
    let pow = 1n;
    for (let i = 0; i < 64; ++i) {
        result.push(teavm_globals.BigInt.asIntN(64, pow));
        pow <<= 1n;
    }
    return result;
}();
let Long_shl = (a, b) => teavm_globals.BigInt.asIntN(64, a * Long_pows[b & 63]);
let Long_shr = (a, b) => {
    b &= 63;
    $rt_numberConversionLongArray[0] = a;
    let lo = $rt_numberConversionIntArray[0];
    let hi = $rt_numberConversionIntArray[1];
    if (b >= 32) {
        lo = hi >> (b - 32);
        hi = hi >> 31;
    } else if (b > 0) {
        lo = (lo >>> b) | (hi << (32 - b));
        hi = hi >> b;
    }
    return Long_create(lo, hi);
}
let Long_shru = (a, b) => {
    b &= 63;
    $rt_numberConversionLongArray[0] = a;
    let lo = $rt_numberConversionIntArray[0];
    let hi = $rt_numberConversionIntArray[1];
    if (b >= 32) {
        lo = hi >>> (b - 32);
        hi = 0;
    } else if (b > 0) {
        lo = (lo >>> b) | (hi << (32 - b));
        hi = hi >>> b;
    }
    return Long_create(lo, hi);
}
let Long_shlConst = (a, b) => teavm_globals.BigInt.asIntN(64, a << b);
let Long_shrConst = (a, b) => teavm_globals.BigInt.asIntN(64, a >> b);
let Long_shruConst = (a, b) => teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt.asUintN(64, a) >> b);
let Long_not = a => teavm_globals.BigInt.asIntN(64, ~a);
