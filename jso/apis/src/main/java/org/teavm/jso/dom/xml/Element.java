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
package org.teavm.jso.dom.xml;

import org.teavm.jso.JSProperty;

public interface Element extends Node {
    String getAttribute(String name);

    void setAttribute(String name, String value);

    void removeAttribute(String name);

    Attr getAttributeNode(String name);

    Attr setAttributeNode(Attr newAttr);

    Attr removeAttributeNode(Attr oldAttr);

    NodeList<? extends Element> getElementsByTagName(String name);

    String getAttributeNS(String namespaceURI, String localName);

    void setAttributeNS(String namespaceURI, String qualifiedName, String value);

    void removeAttributeNS(String namespaceURI, String localName);

    Attr getAttributeNodeNS(String namespaceURI, String localName);

    Attr setAttributeNodeNS(Attr newAttr);

    NodeList<? extends Element> getElementsByTagNameNS(String namespaceURI, String localName);

    boolean hasAttribute(String name);

    boolean hasAttributeNS(String namespaceURI, String localName);

    Element querySelector(String selectors);

    NodeList<? extends Element> querySelectorAll(String selectors);
    
    @JSProperty
    String getId();

    @JSProperty
    void setId(String id);

    @JSProperty
    String getTagName();
}
