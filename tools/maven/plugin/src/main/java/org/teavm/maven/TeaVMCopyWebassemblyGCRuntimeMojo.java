/*
 *  Copyright 2024 konsoletyper.
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
package org.teavm.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "copy-webassembly-gc-runtime", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class TeaVMCopyWebassemblyGCRuntimeMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project.build.directory}/javascript")
    private File targetDirectory;

    @Parameter(property = "teavm.wasmGC.runtime.fileName", defaultValue = "classes.wasm-runtime.js")
    private String runtimeFileName;

    @Parameter(property = "teavm.wasmGC.deobfuscator.fileName", defaultValue = "classes.wasm-deobfuscator.wasm")
    private String deobfuscatorFileName;

    @Parameter(property = "teavm.wasmGC.runtime.minified", defaultValue = "true")
    private boolean minified;

    @Parameter(property = "teavm.wasmGC.runtime.modular", defaultValue = "false")
    private boolean modular;

    @Parameter(property = "teavm.wasmGC.runtime.deobfuscator", defaultValue = "false")
    private boolean deobfuscator;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            var name = new StringBuilder("wasm-gc");
            if (modular) {
                name.append("-modular");
            }
            name.append("-runtime");
            if (minified) {
                name.append(".min");
            }
            var resourceName = "org/teavm/backend/wasm/" + name + ".js";
            var classLoader = TeaVMCopyWebassemblyGCRuntimeMojo.class.getClassLoader();
            var output = new File(targetDirectory, runtimeFileName);
            try (var input = classLoader.getResourceAsStream(resourceName)) {
                Files.copy(input, output.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            if (deobfuscator) {
                resourceName = "org/teavm/backend/wasm/deobfuscator.wasm";
                output = new File(targetDirectory, deobfuscatorFileName);
                try (var input = classLoader.getResourceAsStream(resourceName)) {
                    Files.copy(input, output.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to copy webassembly runtime", e);
        }
    }
}
