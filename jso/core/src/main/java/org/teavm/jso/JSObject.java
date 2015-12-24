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
package org.teavm.jso;

/**
 * <p>The base type for all overlay types. Overlay types are Java types that represent JavaScript object,
 * and therefore can be passed to and from JavaScript code.</p>
 *
 * <p>An overlay type is an abstract class or an interface that extends/implements JSObject. An overlay type
 * has following restrictions:</p>
 *
 * <ul>
 *   <li>it must not contain any member fields, however static fields are allowed;</li>
 *   <li>its non-abstract methods must not override methods of a parent type.</li>
 * </ul>
 *
 * <p>To simplify creation of overlay objects, you can use shortcut annotations instead of {@link JSBody}:
 * {@link JSMethod}, {@link JSProperty} and {@link JSIndexer}.</p>
 *
 * <p>Example:</p>
 *
 * <pre>
 * public abstract class Int32Array implements JSObject {
 *     {@literal @}JSBody(params = {}, script = "return this.length;")
 *     public native int getLength();
 *
 *     {@literal @}JSIndexer
 *     public abstract int get(int index);
 *
 *     {@literal @}JSIndexer
 *     public abstract void set(int index, int value);
 *
 *     {@literal @}JSBody(params = "length", script = "return new Int32Array(length);")
 *     public static native ArrayBuffer create(int length);
 *
 *     {@literal @}JSBody(params = "buffer", script = "return new Int32Array(buffer);")
 *     public static native ArrayBuffer create(ArrayBuffer buffer);
 *
 *     {@literal @}JSBody(params = { "buffer", "offset", "length" },
 *             script = "return new Int32Array(buffer, offset, length);")
 *     public static native ArrayBuffer create(ArrayBuffer buffer, int offset, int length);
 * }
 * </pre>
 *
 * @see JSBody
 * @author Alexey Andreev
 */
public interface JSObject {
    @SuppressWarnings("unchecked")
    default <T extends JSObject> T cast() {
        return (T) this;
    }
}
