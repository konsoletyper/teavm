/*
 *  Copyright 2021 konsoletyper.
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
package org.teavm.tooling.deobfuscate.js;

import org.teavm.jso.JSExport;
import org.teavm.jso.JSProperty;

public class Frame {
    private String className;
    private String fileName;
    private String methodName;
    private int lineNumber;

    public Frame(String className, String methodName, String fileName, int lineNumber) {
        this.className = className;
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    @JSExport
    @JSProperty
    public String getClassName() {
        return className;
    }

    @JSExport
    @JSProperty
    public String getFileName() {
        return fileName;
    }

    @JSExport
    @JSProperty
    public String getMethodName() {
        return methodName;
    }

    @JSExport
    @JSProperty
    public int getLineNumber() {
        return lineNumber;
    }
}
