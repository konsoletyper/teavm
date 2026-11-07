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

let $rt_checkedIdiv = (a, b) => {
    if (b === 0) {
        $rt_throwAE();
    }
    return (a / b) | 0;
}
let $rt_checkedIrem = (a, b) => {
    if (b === 0) {
        $rt_throwAE();
    }
    return (a % b) | 0;
}
let $rt_checkedLdiv = (a, b) => {
    if (b === 0n) {
        $rt_throwAE();
    }
    return Long_asIntN(64, a / b);
}
let $rt_checkedLrem = (a, b) => {
    if (b === 0n) {
        $rt_throwAE();
    }
    return a % b;
}

let $rt_nullCheck = val => {
    if (val === null) {
        $rt_throw(teavm_javaConstructor("java.lang.NullPointerException", "()V")());
    }
    return val;
}