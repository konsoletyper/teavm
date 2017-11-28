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

public interface Document extends Node {
    @JSProperty
    DocumentType getDoctype();

    @JSProperty
    DOMImplementation getImplementation();

    @JSProperty
    Element getDocumentElement();

    Element createElement(String tagName);

    DocumentFragment createDocumentFragment();

    Text createTextNode(String data);

    Comment createComment(String data);

    CDATASection createCDATASection(String data);

    ProcessingInstruction createProcessingInstruction(String target, String data);

    Attr createAttribute(String name);

    EntityReference createEntityReference(String name);

    NodeList<Element> getElementsByTagName(String name);

    <T extends Node> T importNode(T importedNode, boolean deep);

    Element createElementNS(String namespaceURI, String qualifiedName);

    Attr createAttributeNS(String namespaceURI, String qualifiedName);

    NodeList<Element> getElementsByTagNameNS(String namespaceURI, String localName);

    Element getElementById(String elementId);

    Element querySelector(String selectors);

    NodeList<? extends Element> querySelectorAll(String selectors);
}
