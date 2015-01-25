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
package org.teavm.samples.storage;

import org.teavm.dom.browser.Storage;
import org.teavm.dom.browser.Window;
import org.teavm.dom.events.Event;
import org.teavm.dom.events.EventListener;
import org.teavm.dom.html.HTMLButtonElement;
import org.teavm.dom.html.HTMLDocument;
import org.teavm.dom.html.HTMLElement;
import org.teavm.dom.html.HTMLInputElement;
import org.teavm.jso.JS;

public final class Application {

    private static Window window = (Window)JS.getGlobal();
    private static HTMLDocument document = window.getDocument();
    private static Storage storage = window.getSessionStorage();

    private Application() {
    }

    public static void main(String[] args) {
        if (storage == null) {
            window.alert("storage is not supported.");
        }
        
        HTMLButtonElement saveButton = (HTMLButtonElement)document.getElementById("save-button");
        saveButton.addEventListener("click", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                String key = ((HTMLInputElement)document.getElementById("key")).getValue();
                String value = ((HTMLInputElement)document.getElementById("value")).getValue();
                
                if (key != null && key.length() > 0 && value != null && value.length() > 0) {
                    storage.setItem(key, value);
                    draw();
                }
            }
        });
        HTMLButtonElement deleteButton = (HTMLButtonElement)document.getElementById("delete-button");
        deleteButton.addEventListener("click", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                String key = ((HTMLInputElement)document.getElementById("key")).getValue();
                
                if (key != null && key.length() > 0) {
                    storage.removeItem(key);
                    draw();
                }
            }
        });
        HTMLButtonElement deleteAllButton = (HTMLButtonElement)document.getElementById("delete-all-button");
        deleteAllButton.addEventListener("click", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                storage.clear();
                draw();
            }
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
            
            HTMLElement tdKey = document.createElement("td");
            tdKey.appendChild(document.createTextNode(key));
            
            HTMLElement tdValue = document.createElement("td");
            tdValue.appendChild(document.createTextNode(value));
            
            HTMLElement tr = document.createElement("tr");
            tr.appendChild(tdKey);
            tr.appendChild(tdValue);
            
            tbody.appendChild(tr);
        }
    }
}
