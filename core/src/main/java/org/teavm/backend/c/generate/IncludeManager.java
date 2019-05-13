/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.backend.c.generate;

import java.util.HashSet;
import java.util.Set;
import org.teavm.model.ValueType;

public abstract class IncludeManager {
    private String currentFileName;
    private Set<String> includedFiles = new HashSet<>();
    private Set<String> includedClasses = new HashSet<>();
    private Set<ValueType> includedTypes = new HashSet<>();

    public void init(String currentFileName) {
        this.currentFileName = currentFileName;
        includedFiles.clear();
        includedClasses.clear();
        includedTypes.clear();
    }

    public abstract void addInclude(String include);

    public String relativeIncludeString(String fileName) {
        int commonIndex = 0;
        while (true) {
            int next = fileName.indexOf('/', commonIndex);
            if (next < 0 || next > currentFileName.length()
                    || !currentFileName.regionMatches(commonIndex, fileName, commonIndex, next - commonIndex + 1)) {
                break;
            }
            commonIndex = next + 1;
        }

        StringBuilder sb = new StringBuilder("\"");
        int index = commonIndex;
        while (true) {
            int next = currentFileName.indexOf('/', index);
            if (next < 0) {
                break;
            }
            index = next + 1;
            sb.append("../");
        }

        return sb.append(fileName.substring(commonIndex)).append("\"").toString();
    }

    public void includeClass(String className) {
        if (!includedClasses.add(className)) {
            return;
        }
        includePath(ClassGenerator.fileName(className) + ".h");
    }

    public void includeType(ValueType type) {
        if (!includedTypes.add(type)) {
            return;
        }
        includePath(ClassGenerator.fileName(type) + ".h");
    }

    public void includePath(String fileName) {
        if (!includedFiles.add(fileName)) {
            return;
        }
        addInclude(relativeIncludeString(fileName));
    }
}
