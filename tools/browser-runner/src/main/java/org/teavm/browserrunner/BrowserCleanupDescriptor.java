/*
 *  Copyright 2025 Alexey Andreev.
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

public class BrowserCleanupDescriptor {
    private final String testPath;
    private final boolean module;
    private final Collection<String> additionalFiles;

    public BrowserCleanupDescriptor(String testPath, boolean module, Collection<String> additionalFiles) {
        this.testPath = testPath;
        this.module = module;
        this.additionalFiles = additionalFiles;
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
}
