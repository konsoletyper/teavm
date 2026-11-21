/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.browserrunner;

import java.util.Collection;

public class BrowserRunDescriptor {
    private final String name;
    private final String testPath;
    private final boolean module;
    private final Collection<String> additionalFiles;
    private final String argument;
    private final boolean cached;
    private final Collection<String> programArguments;

    public BrowserRunDescriptor(String name, String testPath, boolean module, Collection<String> additionalFiles,
            String argument, boolean cached) {
        this(name, testPath, module, additionalFiles, argument, cached, null);
    }

    public BrowserRunDescriptor(String name, String testPath, boolean module, Collection<String> additionalFiles,
            String argument, boolean cached, Collection<String> programArguments) {
        this.name = name;
        this.testPath = testPath;
        this.module = module;
        this.additionalFiles = additionalFiles;
        this.argument = argument;
        this.cached = cached && programArguments == null;
        this.programArguments = programArguments;
    }

    public String getName() {
        return name;
    }

    public String getTestPath() {
        return testPath;
    }

    public boolean isModule() {
        return module;
    }

    public Collection<String> getAdditionalFiles() {
        return additionalFiles;
    }

    public String getArgument() {
        return argument;
    }

    public boolean isCached() {
        return cached;
    }

    public Collection<String> getProgramArguments() {
        return programArguments;
    }
}
