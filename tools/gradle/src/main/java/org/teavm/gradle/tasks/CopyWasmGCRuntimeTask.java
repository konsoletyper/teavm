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
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public abstract class CopyWasmGCRuntimeTask extends DefaultTask {
    public CopyWasmGCRuntimeTask() {
        getModular().convention(false);
        getObfuscated().convention(true);
        getDeobfuscator().convention(false);
    }

    @Input
    public abstract Property<Boolean> getModular();

    @Input
    public abstract Property<Boolean> getObfuscated();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @Input
    public abstract Property<Boolean> getDeobfuscator();

    @OutputFile
    public abstract RegularFileProperty getDeobfuscatorOutputFile();

    @TaskAction
    public void copyRuntime() throws IOException {
        var name = new StringBuilder("wasm-gc");
        if (getModular().get()) {
            name.append("-modular");
        }
        name.append("-runtime");
        if (getObfuscated().get()) {
            name.append(".min");
        }
        var resourceName = "org/teavm/backend/wasm/" + name + ".js";
        var classLoader = CopyWasmGCRuntimeTask.class.getClassLoader();
        var output = getOutputFile().get().getAsFile();
        try (var input = classLoader.getResourceAsStream(resourceName)) {
            Files.copy(input, output.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        if (getDeobfuscator().get()) {
            resourceName = "org/teavm/backend/wasm/deobfuscator.wasm";
            output = getDeobfuscatorOutputFile().get().getAsFile();
            try (var input = classLoader.getResourceAsStream(resourceName)) {
                Files.copy(input, output.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
