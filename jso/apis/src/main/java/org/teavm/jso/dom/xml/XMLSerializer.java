/*
 *  Copyright 2020 Falko Bräutigam.
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

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/**
 * The XMLSerializer interface provides the ability to construct an XML string
 * representing a DOM tree.
 */
public abstract class XMLSerializer implements JSObject {

    @JSBody(script = "return new XMLSerializer();")
    public static native XMLSerializer create();
    
    /**
     * Constructs a string representing the specified DOM tree in XML form.
     *
     * @param rootNode The Node to use as the root of the DOM tree or subtree for
     *        which to construct an XML representation. The root node itself must be
     *        either a {@link Node} or {@link Attr} object.
     * @return A DOMString containing the XML representation of the specified DOM
     *         tree.
     * @throws TypeError The specified rootNode is not a compatible node type. The
     *         root node must be either Node or Attr.
     * @throws InvalidStateError The tree could not be successfully serialized,
     *         probably due to issues with the content's compatibility with XML
     *         serialization.
     * @throws SyntaxError A serialization of HTML was requested but could not
     *         succeed due to the content not being well-formed.
     */
    public abstract String serializeToString(Node rootNode);
}
