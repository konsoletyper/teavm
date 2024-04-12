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
package org.teavm.jso.dom.html;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.canvas.CanvasImageSource;

public abstract class HTMLCanvasElement extends HTMLElement implements CanvasImageSource {
    @JSProperty
    public abstract int getWidth();

    @JSProperty
    public abstract void setWidth(int width);

    @JSProperty
    public abstract int getHeight();

    @JSProperty
    public abstract void setHeight(int height);

    public abstract JSObject getContext(String contextId);

    public abstract JSObject getContext(String contextId, JSObject attributes);

    public abstract String toDataURL(String type, double quality);

    public abstract String toDataURL(String type);

    public abstract String toDataURL();
}
