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

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class DependencyType implements DependencyAgentType {
    private DependencyChecker dependencyChecker;
    private String name;
    int index;

    public DependencyType(DependencyChecker dependencyChecker, String name, int index) {
        this.dependencyChecker = dependencyChecker;
        this.name = name;
        this.index = index;
    }

    public DependencyChecker getDependencyChecker() {
        return dependencyChecker;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DependencyAgent getDependencyAgent() {
        return dependencyChecker;
    }
}
