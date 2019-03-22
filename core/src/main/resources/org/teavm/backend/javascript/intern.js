/*
 *  Copyright 2019 Alexey Andreev.
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

var $rt_intern = function() {
    var table = new Array(100);
    var size = 0;

    function get(str) {
        var hash = $rt_stringHash(str);
        var bucket = getBucket(hash);
        for (var i = 0; i < bucket.length; ++i) {
            if ($rt_stringEquals(bucket[i], str)) {
                return bucket[i];
            }
        }
        bucket.push(str);
        return str;
    }

    function getBucket(hash) {
        while (true) {
            var position = hash % table.length;
            var bucket = table[position];
            if (typeof bucket !== "undefined") {
                return bucket;
            }
            if (++size / table.length > 0.5) {
                rehash();
            } else {
                bucket = [];
                table[position] = bucket;
                return bucket;
            }
        }
    }

    function rehash() {
        var old = table;
        table = new Array(table.length * 2);
        size = 0;
        for (var i = 0; i < old.length; ++i) {
            var bucket = old[i];
            if (typeof bucket !== "undefined") {
                for (var j = 0; j < bucket.length; ++j) {
                    get(bucket[j]);
                }
            }
        }
    }

    return get;
}();
