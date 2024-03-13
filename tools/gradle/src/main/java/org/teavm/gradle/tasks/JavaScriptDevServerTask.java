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
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.logging.progress.ProgressLoggerFactory;

public abstract class JavaScriptDevServerTask extends DefaultTask {
    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    @Input
    @Optional
    public abstract Property<String> getTargetFileName();

    @Input
    @Optional
    public abstract Property<String> getTargetFilePath();

    @Input
    @Optional
    public abstract MapProperty<String, String> getProperties();

    @Input
    @Optional
    public abstract ListProperty<String> getPreservedClasses();

    @Input
    public abstract Property<String> getMainClass();

    @Input
    @Optional
    public abstract Property<Boolean> getStackDeobfuscated();

    @Input
    @Optional
    public abstract Property<Boolean> getIndicator();

    @Input
    @Optional
    public abstract Property<Integer> getPort();

    @InputFiles
    public abstract ConfigurableFileCollection getSourceFiles();

    @Input
    @Optional
    public abstract Property<Boolean> getAutoReload();

    @Input
    @Optional
    public abstract Property<String> getProxyUrl();

    @Input
    @Optional
    public abstract Property<String> getProxyPath();

    @Input
    @Optional
    public abstract Property<Integer> getProcessMemory();

    @Classpath
    public abstract ConfigurableFileCollection getServerClasspath();

    @Internal
    public abstract Property<Integer> getServerDebugPort();

    @Inject
    protected abstract ProgressLoggerFactory getProgressLoggerFactory();

    @TaskAction
    public void compileInCodeServer() throws IOException {
        var codeServerManager = DevServerManager.instance();
        codeServerManager.cleanup(getProject().getGradle());
        var pm = codeServerManager.getProjectManager(getProject().getPath());

        pm.setClasspath(getClasspath().getFiles());
        pm.setSources(getSourceFiles().getFiles());
        if (getTargetFileName().isPresent()) {
            pm.setTargetFileName(getTargetFileName().get());
        }

        if (getTargetFilePath().isPresent()) {
            pm.setTargetFilePath(getTargetFilePath().get());
        }

        pm.setProperties(getProperties().get());
        pm.setPreservedClasses(getPreservedClasses().get());

        pm.setServerClasspath(getServerClasspath().getFiles());
        pm.setMainClass(getMainClass().get());

        pm.setStackDeobfuscated(!getStackDeobfuscated().isPresent() || getStackDeobfuscated().get());
        pm.setIndicator(getIndicator().isPresent() && getIndicator().get());
        pm.setAutoReload(getAutoReload().isPresent() && getAutoReload().get());

        if (getPort().isPresent()) {
            pm.setPort(getPort().get());
        }
        if (getProxyUrl().isPresent()) {
            pm.setProxyUrl(getProxyUrl().get());
        }
        if (getProxyPath().isPresent()) {
            pm.setProxyPath(getProxyPath().get());
        }

        if (getProcessMemory().isPresent()) {
            pm.setProcessMemory(getProcessMemory().get());
        }
        if (getServerDebugPort().isPresent()) {
            pm.setDebugPort(getServerDebugPort().get());
        }

        var progress = getProgressLoggerFactory().newOperation(getClass());
        progress.start("Compilation", getName());
        pm.runBuild(getLogger(), progress);
        progress.completed();
    }
}
