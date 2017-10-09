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

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface Node extends JSObject {
    short ELEMENT_NODE = 1;
    short ATTRIBUTE_NODE = 2;
    short TEXT_NODE = 3;
    short CDATA_SECTION_NODE = 4;
    short ENTITY_REFERENCE_NODE = 5;
    short ENTITY_NODE = 6;
    short PROCESSING_INSTRUCTION_NODE = 7;
    short COMMENT_NODE = 8;
    short DOCUMENT_NODE = 9;
    short DOCUMENT_TYPE_NODE = 10;
    short DOCUMENT_FRAGMENT_NODE = 11;
    short NOTATION_NODE = 12;

    @JSProperty
    String getNodeName();

    @JSProperty
    String getNodeValue();

    @JSProperty
    void setNodeValue(String value);

    @JSProperty
    short getNodeType();

    @JSProperty
    Node getParentNode();

    @JSProperty
    NodeList<Node> getChildNodes();

    @JSProperty
    Node getFirstChild();

    @JSProperty
    Node getLastChild();

    @JSProperty
    Node getPreviousSibling();

    @JSProperty
    Node getNextSibling();

    @JSProperty
    NamedNodeMap<Attr> getAttributes();

    Node insertBefore(Node newChild, Node refChild);

    Node replaceChild(Node newChild, Node oldChild);

    Node removeChild(Node oldChild);

    Node appendChild(Node newChild);

    boolean hasChildNodes();

    boolean hasChildNodesJS();

    Node cloneNode(boolean deep);

    void normalize();

    boolean isSupported(String feature, String version);

    @JSProperty
    String getNamespaceURI();

    @JSProperty
    String getPrefix();

    @JSProperty
    void setPrefix(String prefix);

    @JSProperty
    String getLocalName();

    boolean hasAttributes();

    @JSProperty
    Document getOwnerDocument();

    default void delete() {
        if (getParentNode() != null) {
            getParentNode().removeChild(this);
        }
    }
}
