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
package org.teavm.samples.hello;

import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

public final class Client {
    private static HTMLDocument document = Window.current().getDocument();
    private static HTMLButtonElement helloButton = document.getElementById("hello-button").cast();
    private static HTMLElement responsePanel = document.getElementById("response-panel");
    private static HTMLElement thinkingPanel = document.getElementById("thinking-panel");

    private Client() {
    }

    public static void main(String[] args) {
        helloButton.listenClick(evt -> sayHello());
    }

    private static void sayHello() {
        helloButton.setDisabled(true);
        thinkingPanel.getStyle().setProperty("display", "");
        var xhr = XMLHttpRequest.create();
        xhr.onComplete(() -> receiveResponse(xhr.getResponseText()));
        xhr.open("GET", "hello");
        xhr.send();
    }

    private static void receiveResponse(String text) {
        var responseElem = document.createElement("div");
        responseElem.appendChild(document.createTextNode(text));
        responsePanel.appendChild(responseElem);
        helloButton.setDisabled(false);
        thinkingPanel.getStyle().setProperty("display", "none");
    }
}
