/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.metaprogramming;

import java.util.Objects;
import org.teavm.metaprogramming.reflect.ReflectMethod;

public class SourceLocation {
    private ReflectMethod method;
    private String fileName;
    private int lineNumber;

    public SourceLocation(ReflectMethod method, String fileName, int lineNumber) {
        this.method = method;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    public SourceLocation(ReflectMethod method) {
        this(method, null, -1);
    }

    public ReflectMethod getMethod() {
        return method;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SourceLocation)) {
            return false;
        }
        SourceLocation other = (SourceLocation) obj;
        return method == other.method && Objects.equals(fileName, other.fileName) && lineNumber == other.lineNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, fileName, lineNumber);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName());
        sb.append(lineNumber > 0 ? fileName + ":" + lineNumber : fileName);
        return sb.toString();
    }
}
