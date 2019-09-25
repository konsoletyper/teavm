/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.idea.devserver;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import java.util.Arrays;
import java.util.Collection;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.idea.devserver.ui.TeaVMDevServerSettingsEditor;

public class TeaVMDevServerConfiguration extends ModuleBasedConfiguration<RunConfigurationModule> {
    private String mainClass = "";
    private String jdkPath;
    private int port = 9090;
    private String pathToFile = "";
    private String fileName = "classes.js";
    private boolean indicator = true;
    private boolean deobfuscateStack = true;
    private boolean automaticallyReloaded;
    private int maxHeap = 1024;
    private String proxyUrl = "";
    private String proxyPath = "";

    public TeaVMDevServerConfiguration(
            @NotNull RunConfigurationModule configurationModule,
            @NotNull ConfigurationFactory factory) {
        super("TeaVM dev server", configurationModule, factory);
    }

    @Override
    public Collection<Module> getValidModules() {
        return Arrays.asList(ModuleManager.getInstance(getProject()).getModules());
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);

        Element child = element.getChild("teavm");
        if (child != null) {
            XmlSerializer.deserializeInto(this, child);
        }
    }

    @Override
    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);

        Element child = element.getChild("teavm");
        if (child == null) {
            child = new Element("teavm");
            element.addContent(child);
        }
        XmlSerializer.serializeInto(this, child, (accessor, bean) ->
                !accessor.getName().equals("isAllowRunningInParallel"));
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new TeaVMDevServerSettingsEditor(getProject());
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new TeaVMDevServerRunState(environment, this);
    }

    @Property
    @Tag
    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    @Property
    @Tag
    public String getJdkPath() {
        return jdkPath;
    }

    public void setJdkPath(String jdkPath) {
        this.jdkPath = jdkPath;
    }

    @Property
    @Tag
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Property
    @Tag
    public String getPathToFile() {
        return pathToFile;
    }

    public void setPathToFile(String pathToFile) {
        this.pathToFile = pathToFile;
    }

    @Property
    @Tag
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Property
    @Tag
    public boolean isIndicator() {
        return indicator;
    }

    public void setIndicator(boolean indicator) {
        this.indicator = indicator;
    }

    @Property
    @Tag
    public boolean isDeobfuscateStack() {
        return deobfuscateStack;
    }

    public void setDeobfuscateStack(boolean deobfuscateStack) {
        this.deobfuscateStack = deobfuscateStack;
    }

    @Property
    @Tag
    public boolean isAutomaticallyReloaded() {
        return automaticallyReloaded;
    }

    public void setAutomaticallyReloaded(boolean automaticallyReloaded) {
        this.automaticallyReloaded = automaticallyReloaded;
    }

    @Property
    @Tag
    public int getMaxHeap() {
        return maxHeap;
    }

    public void setMaxHeap(int maxHeap) {
        this.maxHeap = maxHeap;
    }

    @Property
    @Tag
    public String getProxyUrl() {
        return proxyUrl;
    }

    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    @Property
    @Tag
    public String getProxyPath() {
        return proxyPath;
    }

    public void setProxyPath(String proxyPath) {
        this.proxyPath = proxyPath;
    }
}
