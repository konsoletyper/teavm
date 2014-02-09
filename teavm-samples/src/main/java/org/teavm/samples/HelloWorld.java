/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.samples;

import org.teavm.dom.browser.Window;
import org.teavm.dom.core.Document;
import org.teavm.dom.core.Element;
import org.teavm.dom.events.Event;
import org.teavm.dom.events.EventListener;
import org.teavm.dom.events.EventTarget;
import org.teavm.javascript.ni.JS;


/**
 *
 * @author Alexey Andreev
 */
public class HelloWorld {
    private static Window window;
    private static Document document;
    private static Element body;

    public static void main(String[] args) {
        window = (Window)JS.getGlobal();
        document = window.getDocument();
        body = document.getDocumentElement().getElementsByTagName("body").item(0);
        createButton();
    }

    private static void createButton() {
        Element elem = document.createElement("div");
        body.appendChild(elem);
        final Element button = document.createElement("button");
        button.appendChild(document.createTextNode("Click me!"));
        elem.appendChild(button);
        ((EventTarget)button).addEventListener("click", new EventListener() {
            @Override public void handleEvent(Event evt) {
                button.getParentNode().removeChild(button);
                printHelloWorld();
            }
        }, false);
    }

    private static void printHelloWorld() {
        println("Hello, world!");
        println("Here is the Fibonacci sequence:");
        long a = 0;
        long b = 1;
        for (int i = 0; i < 70; ++i) {
            println(a);
            long c = a + b;
            a = b;
            b = c;
        }
        println("And so on...");
    }

    private static void println(Object obj) {
        Element elem = document.createElement("div");
        elem.appendChild(document.createTextNode(String.valueOf(obj)));
        body.appendChild(elem);
    }

    private static void println(long val) {
        Element elem = document.createElement("div");
        elem.appendChild(document.createTextNode(String.valueOf(val)));
        body.appendChild(elem);
    }
}
