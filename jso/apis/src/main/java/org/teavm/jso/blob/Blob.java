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

/**
*
* @author Jan-Felix Wittmann
*/
public abstract class Blob implements JSObject, BlobConvertible {

    @JSProperty
    public abstract int getSize();

    @JSProperty
    public abstract String getType();

    public abstract Blob slice();

    public abstract Blob slice(int start);

    public abstract Blob slice(int start, int end);

    public abstract Blob slice(int start, int end, String contentType);

    @JSBody(params = { "array" }, script = "return new Blob(array);")
    public static native <T extends JSObject & BlobConvertible> Blob create(JSArray<T> array);

    @JSBody(params = { "array", "options" }, script = "return new Blob(array, options);")
    public static native <T extends JSObject & BlobConvertible> Blob create(JSArray<T> array, BlobOptions options);

}
