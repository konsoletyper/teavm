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

import org.teavm.model.ClassReader;

/**
 *
 * @author Alexey Andreev
 */
public class ClassDependency implements ClassDependencyInfo {
    private DependencyChecker checker;
    private String className;
    private DependencyStack stack;
    private ClassReader classReader;

    ClassDependency(DependencyChecker checker, String className, DependencyStack stack, ClassReader classReader) {
        this.checker = checker;
        this.className = className;
        this.stack = stack;
        this.classReader = classReader;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public boolean isMissing() {
        return classReader == null;
    }

    public ClassReader getClassReader() {
        return classReader;
    }

    @Override
    public DependencyStack getStack() {
        return stack;
    }

    public void initClass(DependencyStack stack) {
        if (!isMissing()) {
            checker.initClass(this, stack);
        }
    }
}
