/*
 *  Copyright 2024 Alexey Andreev.
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

function assertEquals(a, b) {
    if (a == b) {
        return
    }
    if (a instanceof Array && b instanceof Array && a.length === b.length) {
        let allEqual = true;
        for (let i = 0; i < a.length; ++i) {
            if (a[i] != b[i]) {
                allEqual = false;
            }
        }
        if (allEqual) {
            return;
        }
    }
    throw Error(`Assertion failed: ${a} != ${b}`);
}

function assertApproxEquals(a, b) {
    if (Math.abs(a - b) > 0.01) {
        throw Error(`Assertion failed: ${a} != ${b}`);
    }
}