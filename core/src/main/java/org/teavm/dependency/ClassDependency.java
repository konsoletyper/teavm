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
package org.teavm.dependency;

import org.teavm.model.CallLocation;
import org.teavm.model.ClassReader;

public class ClassDependency implements ClassDependencyInfo {
    private DependencyAnalyzer analyzer;
    private String className;
    private ClassReader classReader;
    boolean present;
    boolean activated;

    ClassDependency(DependencyAnalyzer analyzer, String className, ClassReader classReader) {
        this.analyzer = analyzer;
        this.className = className;
        this.classReader = classReader;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public boolean isMissing() {
        return classReader == null && !present;
    }

    public ClassReader getClassReader() {
        return classReader;
    }

    public void initClass(CallLocation location) {
        if (!isMissing()) {
            analyzer.initClass(this, location);
        }
    }

    void cleanup() {
        if (classReader != null) {
            present = true;
            classReader = null;
        }
    }
}
