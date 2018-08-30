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
package org.teavm.idea;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ServiceManager;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.teavm.idea.daemon.TeaVMBuildDaemon;
import org.teavm.idea.daemon.TeaVMDaemonInfo;
import org.teavm.idea.jps.model.TeaVMJpsWorkspaceConfiguration;

public class TeaVMDaemonComponent implements ApplicationComponent {
    private TeaVMDaemonInfo daemonInfo;
    private boolean incremental;
    private int daemonMemory;

    @Override
    public void initComponent() {
        TeaVMWorkspaceConfigurationStorage configurationStorage = ServiceManager.getService(
                TeaVMWorkspaceConfigurationStorage.class);
        if (configurationStorage != null) {
            TeaVMJpsWorkspaceConfiguration configuration = configurationStorage.getState();
            incremental = configuration.isIncremental();
            daemonMemory = configuration.getDaemonMemory();
            if (configuration.isDaemonEnabled()) {
                startDaemon();
            }
        }
    }

    private TeaVMWorkspaceConfigurationStorage getConfigurationStorage() {
        return ServiceManager.getService(TeaVMWorkspaceConfigurationStorage.class);
    }

    @Override
    public void disposeComponent() {
        if (daemonInfo != null) {
            daemonInfo.getProcess().destroy();
        }
    }

    public boolean isDaemonRunning() {
        return daemonInfo != null;
    }

    public int getDaemonPort() {
        return daemonInfo.getPort();
    }

    public void startDaemon() {
        if (daemonInfo == null) {
            try {
                daemonInfo = TeaVMBuildDaemon.start(incremental, daemonMemory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            updateConfiguration(true);
        }
    }

    public void stopDaemon() {
        if (daemonInfo != null) {
            daemonInfo.getProcess().destroy();
            daemonInfo = null;
            updateConfiguration(false);
        }
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

    public void applyChanges() {
        TeaVMWorkspaceConfigurationStorage configurationStorage = getConfigurationStorage();
        TeaVMJpsWorkspaceConfiguration configuration = configurationStorage.getState();
        configuration.setIncremental(incremental);
        configuration.setDaemonMemory(daemonMemory);
        configurationStorage.loadState(configuration);
    }

    private void updateConfiguration(boolean daemonEnabled) {
        TeaVMWorkspaceConfigurationStorage configurationStorage = getConfigurationStorage();
        TeaVMJpsWorkspaceConfiguration configuration = configurationStorage.getState();
        configuration.setDaemonEnabled(daemonEnabled);
        configurationStorage.loadState(configuration);
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "TeaVM daemon";
    }
}
