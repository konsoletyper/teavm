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

function init(target, queue) {
    let supported = typeof teavm_globals.WeakRef !== 'undefined';
    let value = supported && target !== null ? new teavm_globals.WeakRef(target) : target;
    this[teavm_javaField("java.lang.ref.WeakReference", "value")] = value;
    if (queue !== null && supported && target !== null) {
        let registry = queue[teavm_javaField("java.lang.ref.ReferenceQueue", "registry")];
        if (registry !== null) {
            registry.register(target, { ref: new teavm_globals.WeakRef(this) }, value);
            registry.register(this, { token: value }, value);
            this[teavm_javaField("java.lang.ref.WeakReference", "registry")] = registry;
        }
    }
}
function get() {
    let value = this[teavm_javaField("java.lang.ref.WeakReference", "value")];
    if (typeof teavm_globals.WeakRef !== 'undefined') {
        if (value === null) {
            return null;
        }
        let result = value.deref();
        return typeof result !== 'undefined' ? result : null;
    }
    return value;
}
function clear() {
    let registry = this[teavm_javaField("java.lang.ref.WeakReference", "registry")];
    if (registry !== null) {
        registry.unregister(this[teavm_javaField("java.lang.ref.WeakReference", "value")]);
        this[teavm_javaField("java.lang.ref.WeakReference", "registry")] = null;
    }
    this[teavm_javaField("java.lang.ref.WeakReference", "value")] = null;
}