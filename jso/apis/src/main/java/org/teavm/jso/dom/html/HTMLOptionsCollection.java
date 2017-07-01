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

import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSProperty;

public interface HTMLOptionsCollection extends HTMLCollection {
    @Override
    HTMLOptionElement item(int index);

    @Override
    HTMLOptionElement namedItem(String name);

    @JSIndexer
    void set(int index, HTMLOptionElement element);

    void add(HTMLOptionElement element, HTMLElement before);

    void add(HTMLOptionElement element, int before);

    void add(HTMLOptionElement element);

    void remove(int index);

    @JSProperty
    int getSelectedIndex();

    @JSProperty
    void setSelectedIndex(int selectedIndex);
}
