/*
 *  Copyright 2017 Alexey Andreev.
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
import org.jetbrains.jps.model.ex.JpsElementBase;

public class TeaVMJpsWorkspaceConfiguration extends JpsElementBase<TeaVMJpsWorkspaceConfiguration> {
    private boolean daemonEnabled;
    private int daemonMemory = 1024;
    private boolean incremental;

    public boolean isDaemonEnabled() {
        return daemonEnabled;
    }

    public void setDaemonEnabled(boolean daemonEnabled) {
        this.daemonEnabled = daemonEnabled;
    }

    public boolean isIncremental() {
        return incremental;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public int getDaemonMemory() {
        return daemonMemory;
    }

    public void setDaemonMemory(int daemonMemory) {
        this.daemonMemory = daemonMemory;
    }

    @NotNull
    @Override
    public TeaVMJpsWorkspaceConfiguration createCopy() {
        TeaVMJpsWorkspaceConfiguration copy = new TeaVMJpsWorkspaceConfiguration();
        copy.applyChanges(this);
        return copy;
    }

    @Override
    public void applyChanges(@NotNull TeaVMJpsWorkspaceConfiguration configuration) {
        daemonEnabled = configuration.daemonEnabled;
        incremental = configuration.incremental;
        daemonMemory = configuration.daemonMemory;
    }
}
