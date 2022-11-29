/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.chromerdp;

import org.teavm.common.Promise;
import org.teavm.debugging.javascript.JavaScriptLanguage;
import org.teavm.debugging.javascript.JavaScriptScript;

class ChromeRDPScript implements JavaScriptScript {
    private ChromeRDPDebugger debugger;
    private String id;
    private JavaScriptLanguage language;
    private String url;

    ChromeRDPScript(ChromeRDPDebugger debugger, String id, JavaScriptLanguage language, String url) {
        this.debugger = debugger;
        this.id = id;
        this.language = language;
        this.url = url;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public JavaScriptLanguage getLanguage() {
        return language;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public Promise<String> getSource() {
        return debugger.getScriptSource(id);
    }
}
