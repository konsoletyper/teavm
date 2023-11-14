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

function jsFunction(self, method) {
    let name = 'jso$functor$' + method;
    let result = self[name];
    if (typeof result !== 'function') {
        let fn = function() {
            return self[method].apply(self, arguments);
        }
        result = () => fn;
        self[name] = result;
    }
    return result();
}
function jsFunctionAsObject(self, method) {
    if (typeof self !== 'function') return self;
    let result = {};
    result[method] = self;
    return result;
}