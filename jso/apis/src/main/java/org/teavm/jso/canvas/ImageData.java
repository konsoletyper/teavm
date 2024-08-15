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
package org.teavm.jso.canvas;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

@JSClass
public class ImageData implements JSObject {
    public ImageData(Uint8ClampedArray array, int width) {
    }

    public ImageData(int width, int height) {
    }

    public ImageData(Uint8ClampedArray array, int width, int height) {
    }

    @JSProperty
    public native int getWidth();

    @JSProperty
    public native int getHeight();

    @JSProperty
    public native Uint8ClampedArray getData();

    @JSBody(params = { "array", "width" }, script = "return new ImageData(array, width);")
    @Deprecated
    public static native ImageData create(Uint8ClampedArray array, int width);

    @JSBody(params = { "width", "height" }, script = "return new ImageData(width, height);")
    @Deprecated
    public static native ImageData create(int width, int height);

    @JSBody(params = { "array", "width", "height" }, script = "return new ImageData(array, width, height);")
    @Deprecated
    public static native ImageData create(Uint8ClampedArray array, int width, int height);
}
