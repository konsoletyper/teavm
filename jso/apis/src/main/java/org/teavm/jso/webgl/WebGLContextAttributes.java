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
package org.teavm.jso.webgl;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public abstract class WebGLContextAttributes implements JSObject {
    @JSProperty
    public abstract boolean isAlpha();

    @JSProperty
    public abstract void setAlpha(boolean alpha);

    @JSProperty
    public abstract boolean isDepth();

    @JSProperty
    public abstract void setDepth(boolean depth);

    @JSProperty
    public abstract boolean isScencil();

    @JSProperty
    public abstract void setStencil(boolean stencil);

    @JSProperty
    public abstract boolean isAntialias();

    @JSProperty
    public abstract void setAntialias(boolean antialias);

    @JSProperty
    public abstract boolean isPremultipliedAlpha();

    @JSProperty
    public abstract void setPremultipliedAlpha(boolean premultipliedAlpha);

    @JSProperty
    public abstract boolean isPreserveDrawingBuffer();

    @JSProperty
    public abstract void setPreserveDrawingBuffer(boolean preserveDrawingBuffer);

    @JSBody(script = "return {};")
    public static native WebGLContextAttributes create();
}
