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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

public abstract class EmscriptenTask extends DefaultTask {
    @InputFiles
    public abstract ConfigurableFileCollection getInputFiles();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract Property<String> getOutputName();

    @Internal
    public abstract Property<String> getEmscriptenDir();

    @Input
    public abstract ListProperty<String> getCompilerArgs();

    @Input
    public abstract ListProperty<String> getExportedFunctions();

    @Inject
    protected abstract ExecOperations getExecOperations();

    @TaskAction
    public void compile() {
        getExecOperations().exec(execSpec -> {
            execSpec.commandLine(new File(getEmscriptenDir().get(), "emcc").getAbsolutePath());
            execSpec.args(
                    "--no-entry",
                    "-s", "MODULARIZE",
                    "-s", "RELOCATABLE",
                    "-s", "EXPORT_ES6=1",
                    "-s", "ALLOW_MEMORY_GROWTH=1",
                    "-s", "STACK_OVERFLOW_CHECK=0",
                    "-s", "MALLOC=none",
                    "-o", getOutputDir().get().file(getOutputName().get()).getAsFile().getAbsoluteFile()
            );
            var exportedFunctions = new ArrayList<>(List.of("_malloc", "_free", "_realloc"));
            exportedFunctions.addAll(getExportedFunctions().get());
            execSpec.args("-s", "EXPORTED_FUNCTIONS=" + String.join(",", exportedFunctions));
            execSpec.args(getCompilerArgs().get());
            execSpec.args(getInputFiles().getFiles());
        }).assertNormalExitValue().rethrowFailure();
    }
}
