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

function init() {
    this[teavm_javaField("java.lang.ref.ReferenceQueue", "inner")] = [];
    if (teavm_javaMethodExists("java.lang.ref.ReferenceQueue", "reportNext(Ljava/lang/ref/Reference;)Z")) {
        this[teavm_javaField("java.lang.ref.ReferenceQueue", "registry")] =
            new (teavm_globals.FinalizationRegistry)(ref => {
                if (!teavm_javaMethod("java.lang.ref.ReferenceQueue",
                        "reportNext(Ljava/lang/ref/Reference;)Z")(this, ref)) {
                    this[teavm_javaField("java.lang.ref.ReferenceQueue", "inner")].push(ref);
                }
            });
    }
}

function poll() {
    var value = this[teavm_javaField("java.lang.ref.ReferenceQueue", "inner")].shift();
    return typeof value !== 'undefined' ? value : null;
}