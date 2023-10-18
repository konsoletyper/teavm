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
    var map = Object.create(null);

    var get;
    if (typeof WeakRef !== 'undefined') {
        var registry = new FinalizationRegistry(value => {
            delete map[value];
        });

        get = function(str) {
            var key = $rt_ustr(str);
            var ref = map[key];
            var result = typeof ref !== 'undefined' ? ref.deref() : void 0;
            if (typeof result !== 'object') {
                result = str;
                map[key] = new WeakRef(result);
                registry.register(result, key);
            }
            return result;
        }
    } else {
        get = function(str) {
            var key = $rt_ustr(str);
            var result = map[key];
            if (typeof result !== 'object') {
                result = str;
                map[key] = result;
            }
            return result;
        }
    }

    return get;
}();
