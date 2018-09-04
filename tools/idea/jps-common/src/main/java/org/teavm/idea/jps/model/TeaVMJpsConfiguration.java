/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.idea.jps.model;

import com.intellij.util.xmlb.annotations.AbstractCollection;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.ex.JpsElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.module.JpsModule;
import org.teavm.tooling.TeaVMTargetType;

public class TeaVMJpsConfiguration extends JpsElementBase<TeaVMJpsConfiguration> {
    static final JpsElementChildRole<TeaVMJpsConfiguration> JS_ROLE = JpsElementChildRoleBase.create(
            "TeaVM configuration (JS)");

    static final JpsElementChildRole<TeaVMJpsConfiguration> WEBASSEMBLY_ROLE = JpsElementChildRoleBase.create(
            "TeaVM configuration (WebAssembly)");

    @Transient
    private TeaVMTargetType targetType;

    private boolean skipped;
    private String mainClass;
    private String targetDirectory;
    private boolean sourceMapsFileGenerated = true;
    private boolean sourceFilesCopied = true;
    private List<TeaVMProperty> properties = new ArrayList<>();

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public TeaVMTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TeaVMTargetType targetType) {
        this.targetType = targetType;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    public boolean isSourceMapsFileGenerated() {
        return sourceMapsFileGenerated;
    }

    public void setSourceMapsFileGenerated(boolean sourceMapsFileGenerated) {
        this.sourceMapsFileGenerated = sourceMapsFileGenerated;
    }

    public boolean isSourceFilesCopied() {
        return sourceFilesCopied;
    }

    public void setSourceFilesCopied(boolean sourceFilesCopied) {
        this.sourceFilesCopied = sourceFilesCopied;
    }

    @Tag("properties")
    @AbstractCollection(surroundWithTag = false)
    public List<TeaVMProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<TeaVMProperty> properties) {
        this.properties = properties;
    }

    public static List<TeaVMJpsConfiguration> getAll(JpsModule module) {
        List<TeaVMJpsConfiguration> configurations = new ArrayList<>();
        for (JpsElementChildRole<TeaVMJpsConfiguration> role : Arrays.asList(JS_ROLE, WEBASSEMBLY_ROLE)) {
            TeaVMJpsConfiguration configuration = module.getContainer().getChild(role);
            if (configuration != null) {
                configurations.add(configuration);
            }
        }
        return configurations;
    }

    @NotNull
    @Override
    public TeaVMJpsConfiguration createCopy() {
        TeaVMJpsConfiguration copy = new TeaVMJpsConfiguration();
        copy.applyChanges(this);
        return copy;
    }

    @Override
    public void applyChanges(@NotNull TeaVMJpsConfiguration modified) {
        mainClass = modified.mainClass;
        targetDirectory = modified.targetDirectory;
        sourceMapsFileGenerated = modified.sourceMapsFileGenerated;
        sourceFilesCopied = modified.sourceFilesCopied;

        properties.clear();
        properties.addAll(modified.properties.stream()
                .map(property -> property.createCopy())
                .collect(Collectors.toList()));
    }
}
