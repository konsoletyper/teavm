/*
 *  Copyright 2014 Mohiuddin Ahmed.
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
package com.teavm.samples.logging;

import java.util.Date;
import org.teavm.extras.slf4j.TeaVMLogger;
import org.teavm.extras.slf4j.TeaVMLoggerFactorySubstitution;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

public final class Client {

    public static TeaVMLogger log = (TeaVMLogger) TeaVMLoggerFactorySubstitution.getLogger(Client.class);

    private Client() {

    }

    public static void main(String[] args) {

        log.info("Starting...");

        Window window = Window.current();
        HTMLDocument document = window.getDocument();
        HTMLElement canvas = document.getElementById("canvas");

        log.info("Loaded/located browser DOM...");

        String itemName = "TeaVM generated element";

        for (int i = 0; i < 100; i++) {

            HTMLElement div = document.createElement("div");
            div.appendChild(document.createTextNode(itemName + " : " + new Date()));

            canvas.setInnerHTML(div.getInnerHTML());

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }

            log.debug((100 - i) + " seconds to finish...");
        }

        log.info("Finished.");
    }
}

