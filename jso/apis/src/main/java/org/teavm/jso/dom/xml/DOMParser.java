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
import org.teavm.jso.dom.html.HTMLDocument;

/**
 * The DOMParser interface provides the ability to parse XML or HTML source code from
 * a string into a DOM {@link Document}.
 */
public abstract class DOMParser implements JSObject {

    @JSBody(script = "return new DOMParser();")
    public static native DOMParser create();
    
    /**
     * Parses the given string.
     * <p>
     * Note that if the parsing process <b>fails</b>, the DOMParser
     * does not throw an exception, but instead returns an error document:
     * 
     * <pre>
     *   <parsererror xmlns="http://www.mozilla.org/newlayout/xml/parsererror.xml">
     *     (error description) 
     *     <sourcetext>(a snippet of the source XML)</sourcetext>
     *   </parsererror>
     * </pre>
     * 
     * The parsing errors are also reported to the Error Console, with the document
     * URI (see below) as the source of the error.
     *
     * @param s The string to be parsed. It must contain either HTML, xml, xhtml+xml,
     *        or svg document.
     * @param mimeType This string determines a class of the the method's return
     *        value. The possible values are the following: text/html, text/xml,
     *        application/xml, application/xhtml+xml, image/svg+xml
     * @return Newly created {@link Document} or {@link HTMLDocument}
     */
    public abstract Document parseFromString(String s, String mimeType);
    
}
