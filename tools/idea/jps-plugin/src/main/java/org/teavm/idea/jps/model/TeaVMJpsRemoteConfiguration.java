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
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.ex.JpsElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.module.JpsModule;

public class TeaVMJpsRemoteConfiguration extends JpsElementBase<TeaVMJpsRemoteConfiguration> {
    private static final JpsElementChildRole<TeaVMJpsRemoteConfiguration> ROLE = JpsElementChildRoleBase.create(
            "TeaVM remote configuration");
    private int port;

    public static TeaVMJpsRemoteConfiguration get(JpsProject project) {
        return project.getContainer().getChild(ROLE);
    }

    public void setTo(JpsModule project) {
        project.getContainer().setChild(ROLE, this);
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @NotNull
    @Override
    public TeaVMJpsRemoteConfiguration createCopy() {
        TeaVMJpsRemoteConfiguration copy = new TeaVMJpsRemoteConfiguration();
        copy.port = port;
        return copy;
    }

    @Override
    public void applyChanges(@NotNull TeaVMJpsRemoteConfiguration modified) {
        port = modified.port;
    }
}
