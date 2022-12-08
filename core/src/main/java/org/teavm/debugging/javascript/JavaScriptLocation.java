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
package org.teavm.debugging.javascript;

import java.util.Objects;

public class JavaScriptLocation {
    private JavaScriptScript script;
    private int line;
    private int column;

    public JavaScriptLocation(JavaScriptScript script, int line, int column) {
        this.script = script;
        this.line = line;
        this.column = column;
    }

    public JavaScriptScript getScript() {
        return script;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof JavaScriptLocation)) {
            return false;
        }
        JavaScriptLocation other = (JavaScriptLocation) obj;
        return Objects.equals(other.script, script) && other.line == line && other.column == column;
    }

    @Override
    public int hashCode() {
        return (31 + column) * ((31 + line) * 31 + Objects.hashCode(script));
    }

    @Override
    public String toString() {
        return script + ":(" + line + ";" + column + ")";
    }
}
