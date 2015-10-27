/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.jso.blob;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSFunction;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSString;

/**
*
* @author Jan-Felix Wittmann
*/
public abstract class Blob implements JSObject, BlobConvertible {

    @JSProperty
    public abstract int getSize();

    @JSProperty
    public abstract String getType();

    public Blob slice() {
        return getSliceImpl().call(this).cast();
    }

    public Blob slice(int start) {
        return getSliceImpl().call(this, JSNumber.valueOf(start)).cast();
    }

    public Blob slice(int start, int end) {
        return getSliceImpl().call(this, JSNumber.valueOf(start), JSNumber.valueOf(end)).cast();
    }

    public Blob slice(int start, int end, String contentType) {
        return getSliceImpl().call(this, JSNumber.valueOf(start), JSNumber.valueOf(end), JSString.valueOf(contentType))
                .cast();
    }

    @JSBody(params = { "array" }, script = "return new Blob(array);")
    public static native <T extends JSObject & BlobConvertible> Blob create(JSArray<T> array);

    @JSBody(params = { "array", "options" }, script = "return new Blob(array, options);")
    public static native <T extends JSObject & BlobConvertible> Blob create(JSArray<T> array, BlobOptions options);

    @JSBody(params = {}, script = "return this.slice || this.mozSlice || this.webkitSlice;")
    private native JSFunction getSliceImpl();
}
