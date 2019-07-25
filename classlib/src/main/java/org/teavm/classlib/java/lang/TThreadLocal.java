/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

public class TThreadLocal<T> extends TObject {
    private boolean initialized;
    private T value;

    public TThreadLocal() {
        super();
    }

    protected T initialValue() {
        return null;
    }

    public T get() {
        if (!initialized) {
            value = initialValue();
            initialized = true;
        }
        return value;
    }

    public void set(T value) {
        initialized = true;
        this.value = value;
    }

    public void remove() {
        initialized = false;
        value = null;
    }
}
