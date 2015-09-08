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

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
public interface WebGLContextAttributes extends JSObject {
    @JSProperty
    boolean isAlpha();

    @JSProperty
    void setAlpha(boolean alpha);

    @JSProperty
    boolean isDepth();

    @JSProperty
    void setDepth(boolean depth);

    @JSProperty
    boolean isScencil();

    @JSProperty
    void setStencil(boolean stencil);

    @JSProperty
    boolean isAntialias();

    @JSProperty
    void setAntialias(boolean antialias);

    @JSProperty
    boolean isPremultipliedAlpha();

    @JSProperty
    void setPremultipliedAlpha(boolean premultipliedAlpha);

    @JSProperty
    boolean isPreserveDrawingBuffer();

    @JSProperty
    void setPreserveDrawingBuffer(boolean preserveDrawingBuffer);
}
