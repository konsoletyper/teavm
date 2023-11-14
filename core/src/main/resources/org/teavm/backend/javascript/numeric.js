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

let $rt_compare = (a, b) => a > b ? 1 : a < b ? -1 : a === b ? 0 : 1;
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
