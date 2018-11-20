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
package org.teavm.idea;

import static org.teavm.idea.jps.remote.TeaVMBuilderAssistant.REMOTE_PORT;
import com.intellij.compiler.server.BuildProcessParametersProvider;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.teavm.tooling.daemon.RemoteBuildService;

public class TeaVMJPSConfigurator extends BuildProcessParametersProvider {
    private TeaVMJPSRemoteService remoteService;
    private TeaVMDaemonComponent daemonComponent;

    public TeaVMJPSConfigurator(TeaVMJPSRemoteService remoteService, TeaVMDaemonComponent daemonComponent) {
        this.remoteService = remoteService;
        this.daemonComponent = daemonComponent;
    }

    @NotNull
    @Override
    public List<String> getVMArguments() {
        List<String> result = new ArrayList<>();
        result.add("-D" + REMOTE_PORT + "=" + remoteService.getPort());
        if (daemonComponent.isDaemonRunning()) {
            result.add("-D" + RemoteBuildService.REMOTE_PORT + "=" + daemonComponent.getDaemonPort());
        }
        return result;
    }
}
