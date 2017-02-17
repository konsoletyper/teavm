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
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class TeaVMJPSConfigurator extends BuildProcessParametersProvider {
    private TeaVMJPSRemoteService remoteService;

    public TeaVMJPSConfigurator(TeaVMJPSRemoteService remoteService) {
        this.remoteService = remoteService;
    }

    @NotNull
    @Override
    public List<String> getVMArguments() {
        return Collections.singletonList("-D" + REMOTE_PORT + "=" + remoteService.getPort());
    }
}
