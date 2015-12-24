/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.samples.storage;

import org.teavm.jso.browser.Storage;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

/**
*
* @author Junji Takakura
*/
public final class Application {
    private static HTMLDocument document = Window.current().getDocument();
    private static Storage storage = Window.current().getSessionStorage();

    private Application() {
    }

    public static void main(String[] args) {
        if (storage == null) {
            Window.alert("storage is not supported.");
        }

        HTMLButtonElement saveButton = document.getElementById("save-button").cast();
        saveButton.listenClick(e -> {
            String key = document.getElementById("key").<HTMLInputElement>cast().getValue();
            String value = document.getElementById("value").<HTMLInputElement>cast().getValue();

            if (key != null && key.length() > 0 && value != null && value.length() > 0) {
                storage.setItem(key, value);
                draw();
            }
        });
        HTMLButtonElement deleteButton = document.getElementById("delete-button").cast();
        deleteButton.listenClick(e -> {
            String key = document.getElementById("key").<HTMLInputElement>cast().getValue();
            if (key != null && key.length() > 0) {
                storage.removeItem(key);
                draw();
            }
        });
        HTMLButtonElement deleteAllButton = document.getElementById("delete-all-button").cast();
        deleteAllButton.listenClick(e -> {
            storage.clear();
            draw();
        });
        draw();
    }

    private static void draw() {
        HTMLElement tbody = document.getElementById("list");

        while (tbody.getFirstChild() != null) {
            tbody.removeChild(tbody.getFirstChild());
        }

        for (int i = 0; i < storage.getLength(); i++) {
            String key = storage.key(i);
            String value = storage.getItem(key);

            HTMLElement tdKey = document.createElement("td").withText(key);
            HTMLElement tdValue = document.createElement("td").withText(value);
            HTMLElement tr = document.createElement("tr").withChild(tdKey).withChild(tdValue);

            tbody.appendChild(tr);
        }
    }
}
