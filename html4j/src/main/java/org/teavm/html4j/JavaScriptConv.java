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
package org.teavm.html4j;

import org.teavm.backend.javascript.spi.GeneratedBy;

public final class JavaScriptConv {
    private JavaScriptConv() {
    }

    @GeneratedBy(JavaScriptConvGenerator.class)
    public static native Object toJavaScript(Object obj);

    @GeneratedBy(JavaScriptConvGenerator.class)
    public static native Object fromJavaScript(Object obj, Object cls);
}
