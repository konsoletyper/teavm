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
package org.teavm.jso.dom.css;

import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface CSSStyleDeclaration extends JSObject {
    @JSProperty
    String getCssText();

    @JSProperty
    void setCssText(String cssText);

    @JSProperty
    int getLength();

    @JSIndexer
    String item(int index);

    String getPropertyValue(String property);

    String getPropertyPriority(String property);

    void setProperty(String property, String value);

    void setProperty(String property, String value, String priority);

    String removeProperty(String property);
}
