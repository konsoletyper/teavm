/*
 *  Copyright 2017 Liraz.
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
package org.teavm.dom.html;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLHtmlElement;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class HTMLDocumentTest {

    @Test
    public void getDocumentElementWorks() {
        HTMLHtmlElement documentElement = HTMLDocument.current().getDocumentElement();

        System.out.println(documentElement);
        //assertEquals("html", documentElement.getTagName());
    }

    //@Test
    public void createElementWorks() {
        HTMLElement div = HTMLDocument.current().createElement("div");
        //assertEquals("div", div.getTagName());
    }
}
