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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.ex.JpsElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.module.JpsModule;

public class TeaVMJpsConfiguration extends JpsElementBase<TeaVMJpsConfiguration> {
    private static final JpsElementChildRole<TeaVMJpsConfiguration> ROLE = JpsElementChildRoleBase.create(
            "TeaVM configuration");
    private boolean enabled;
    private String mainClass;
    private String targetDirectory;
    private boolean minifying;
    private boolean sourceMapsFileGenerated = true;
    private boolean sourceFilesCopied = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public boolean isMinifying() {
        return minifying;
    }

    public void setMinifying(boolean minifying) {
        this.minifying = minifying;
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

    public static TeaVMJpsConfiguration get(JpsModule module) {
        return module.getContainer().getChild(ROLE);
    }

    public void setTo(JpsModule module) {
        module.getContainer().setChild(ROLE, this);
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
        enabled = modified.enabled;
        mainClass = modified.mainClass;
        targetDirectory = modified.targetDirectory;
        minifying = modified.minifying;
        sourceMapsFileGenerated = modified.sourceMapsFileGenerated;
        sourceFilesCopied = modified.sourceFilesCopied;
    }
}
