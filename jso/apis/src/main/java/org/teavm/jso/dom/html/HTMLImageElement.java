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

import org.teavm.jso.JSProperty;
import org.teavm.jso.canvas.CanvasImageSource;

public interface HTMLImageElement extends HTMLElement, CanvasImageSource {
    @JSProperty
    String getAlt();

    @JSProperty
    void setAlt(String alt);

    @JSProperty
    int getWidth();

    @JSProperty
    void setWidth(int width);

    @JSProperty
    int getHeight();

    @JSProperty
    void setHeight(int height);

    @JSProperty
    int getNaturalWidth();

    @JSProperty
    int getNaturalHeight();

    @JSProperty
    String getSrc();

    @JSProperty
    void setSrc(String src);

    @JSProperty
    String getCrossOrigin();

    @JSProperty
    void setCrossOrigin(String crossOrigin);
}
