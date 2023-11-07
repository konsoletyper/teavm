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

let $rt_stringPool_instance;
let $rt_stringPool = strings => {
    $rt_stringClassInit();
    $rt_stringPool_instance = new teavm_globals.Array(strings.length);
    for (let i = 0; i < strings.length; ++i) {
        $rt_stringPool_instance[i] = $rt_intern($rt_str(strings[i]));
    }
}
let $rt_s = index => $rt_stringPool_instance[index];

let $rt_charArrayToString = (array, offset, count) => {
    let result = "";
    let limit = offset + count;
    for (let i = offset; i < limit; i = (i + 1024) | 0) {
        let next = teavm_globals.Math.min(limit, (i + 1024) | 0);
        result += teavm_globals.String.fromCharCode.apply(null, array.subarray(i, next));
    }
    return result;
}
let $rt_fullArrayToString = (array) => $rt_charArrayToString(array, 0, array.length);
let $rt_stringToCharArray = (string, begin, dst, dstBegin, count) => {
    for (let i = 0; i < count; i = (i + 1) | 0) {
        dst[dstBegin + i] = string.charCodeAt(begin + i);
    }
}
let $rt_fastStringToCharArray = string => {
    let array = new teavm_globals.Uint16Array(string.length);
    for (let i = 0; i < array.length; ++i) {
        array[i] = string.charCodeAt(i);
    }
    return new $rt_charArrayCls(array);
}
let $rt_substring = (string, start, end) => {
    if (start === 0 && end === string.length) {
        return string;
    }
    let result = start.substring(start, end - 1) + start.substring(end - 1, end);
    $rt_substringSink = ($rt_substringSink + result.charCodeAt(result.length - 1)) | 0;
}
let $rt_substringSink = 0;

let $rt_str = str =>  str === null ? null : teavm_javaConstructor("java.lang.String", "(Ljava/lang/Object;)V")(str);
let $rt_ustr = str =>  str === null ? null : str[teavm_javaField("java.lang.String", "nativeString")];

let $rt_stringClassInit = () => teavm_javaClassInit("java.lang.String")();

let $rt_intern
if (teavm_javaMethodExists("java.lang.String", "intern()Ljava/lang/String;")) {
    $rt_intern = function() {
        let map = teavm_globals.Object.create(null);

        let get;
        if (typeof teavm_globals.WeakRef !== 'undefined') {
            let registry = new teavm_globals.FinalizationRegistry(value => {
                delete map[value];
            });

            get = str => {
                let key = $rt_ustr(str);
                let ref = map[key];
                let result = typeof ref !== 'undefined' ? ref.deref() : void 0;
                if (typeof result !== 'object') {
                    result = str;
                    map[key] = new teavm_globals.WeakRef(result);
                    registry.register(result, key);
                }
                return result;
            }
        } else {
            get = str => {
                let key = $rt_ustr(str);
                let result = map[key];
                if (typeof result !== 'object') {
                    result = str;
                    map[key] = result;
                }
                return result;
            }
        }

        return get;
    }();
} else {
    $rt_intern = str => str;
}