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
package org.teavm.junit;

import java.io.File;

class TestRunGroup {
    private File baseDirectory;
    private String fileName;
    private TestPlatform kind;
    private boolean module;

    TestRunGroup(File baseDirectory, String fileName, TestPlatform kind, boolean module) {
        this.baseDirectory = baseDirectory;
        this.fileName = fileName;
        this.kind = kind;
        this.module = module;
    }

    File getBaseDirectory() {
        return baseDirectory;
    }

    String getFileName() {
        return fileName;
    }

    TestPlatform getKind() {
        return kind;
    }

    boolean isModule() {
        return module;
    }
}
