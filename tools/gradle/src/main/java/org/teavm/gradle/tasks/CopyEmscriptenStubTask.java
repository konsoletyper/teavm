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
package org.teavm.gradle.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public abstract class CopyEmscriptenStubTask extends DefaultTask {
    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @TaskAction
    public void copyStub() throws IOException {
        var resourceName = "org/teavm/backend/wasm/emscripten-stub.c";
        var classLoader = CopyEmscriptenStubTask.class.getClassLoader();
        var output = getOutputFile().get().getAsFile();
        output.getParentFile().mkdirs();
        try (var input = classLoader.getResourceAsStream(resourceName)) {
            Files.copy(input, output.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
