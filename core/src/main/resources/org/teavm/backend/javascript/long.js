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
let Long_ZERO = teavm_globals.BigInt(0);
let Long_create = (lo, hi) => teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt.asUintN(64, teavm_globals.BigInt(lo))
    | teavm_globals.BigInt.asUintN(64, (teavm_globals.BigInt(hi) << teavm_globals.BigInt(32))));
let Long_fromInt = val => teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt(val | 0));
let Long_fromNumber = val =>  teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt(
    val >= 0 ? teavm_globals.Math.floor(val) : teavm_globals.Math.ceil(val)));
let Long_toNumber = val => teavm_globals.Number(val);
let Long_hi = val => teavm_globals.Number(teavm_globals.BigInt.asIntN(64, val >> teavm_globals.BigInt(32))) | 0;
let Long_lo = val => teavm_globals.Number(teavm_globals.BigInt.asIntN(32, val)) | 0;

let Long_eq = (a, b) => a === b
let Long_ne = (a, b) => a !== b
let Long_gt = (a, b) => a > b
let Long_ge = (a, b) => a >= b
let Long_lt = (a, b) => a < b
let Long_le = (a, b) => a <= b
let Long_add = (a, b) => teavm_globals.BigInt.asIntN(64, a + b);
let Long_inc = a => teavm_globals.BigInt.asIntN(64, a + 1);
let Long_dec = a => teavm_globals.BigInt.asIntN(64, a - 1);
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
let Long_shl = (a, b) => teavm_globals.BigInt.asIntN(64, a << teavm_globals.BigInt(b & 63));
let Long_shr = (a, b) => teavm_globals.BigInt.asIntN(64, a >> teavm_globals.BigInt(b & 63));
let Long_shru = (a, b) => teavm_globals.BigInt.asIntN(64, teavm_globals.BigInt.asUintN(64, a) >>
        teavm_globals.BigInt(b & 63));
let Long_not = a => teavm_globals.BigInt.asIntN(64, ~a);
