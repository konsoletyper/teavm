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

/**
 *
 * @author Junji Takakura
 */
public interface HTMLVideoElement extends HTMLMediaElement {

    @JSProperty
    int getWidth();

    @JSProperty
    int getHeight();

    @JSProperty
    void setWidth(int width);

    @JSProperty
    void setHeight(int height);

    @JSProperty
    int getVideoWidth();

    @JSProperty
    int getVideoHeight();

    @JSProperty
    String getPoster();

    @JSProperty
    void setPoster(String poster);
}
