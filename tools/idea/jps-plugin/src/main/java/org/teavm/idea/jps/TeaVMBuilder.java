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
package org.teavm.idea.jps;

import static org.teavm.idea.jps.remote.TeaVMBuilderAssistant.REMOTE_PORT;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.BuildOutputConsumer;
import org.jetbrains.jps.builders.BuildRootDescriptor;
import org.jetbrains.jps.builders.DirtyFilesHolder;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.TargetBuilder;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.incremental.messages.CompilerMessage;
import org.teavm.idea.jps.remote.TeaVMBuilderAssistant;
import org.teavm.tooling.builder.BuildException;
import org.teavm.tooling.builder.BuildStrategy;
import org.teavm.tooling.builder.InProcessBuildStrategy;
import org.teavm.tooling.builder.RemoteBuildStrategy;
import org.teavm.tooling.daemon.RemoteBuildService;

public class TeaVMBuilder extends TargetBuilder<BuildRootDescriptor, TeaVMBuildTarget> {
    private TeaVMBuilderAssistant assistant;
    private RemoteBuildService buildService;

    public TeaVMBuilder() {
        super(Collections.singletonList(TeaVMBuildTargetType.INSTANCE));

        String portString = System.getProperty(REMOTE_PORT);
        if (portString != null) {
            try {
                Registry registry = LocateRegistry.getRegistry(Integer.parseInt(portString));
                assistant = (TeaVMBuilderAssistant) registry.lookup(TeaVMBuilderAssistant.ID);
            } catch (NumberFormatException | RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }

        String daemonPortString = System.getProperty(RemoteBuildService.REMOTE_PORT);
        if (daemonPortString != null) {
            try {
                Registry registry = LocateRegistry.getRegistry(Integer.parseInt(daemonPortString));
                buildService = (RemoteBuildService) registry.lookup(RemoteBuildService.ID);
            } catch (NumberFormatException | RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void build(@NotNull TeaVMBuildTarget target,
            @NotNull DirtyFilesHolder<BuildRootDescriptor, TeaVMBuildTarget> holder,
            @NotNull BuildOutputConsumer outputConsumer, @NotNull CompileContext context) {
        if (assistant == null) {
            context.processMessage(new CompilerMessage("TeaVM", BuildMessage.Kind.WARNING,
                    "No TeaVM builder assistant available. Diagnostic messages will be less informative"));
        }

        try {
            BuildStrategy buildStrategy = buildService != null
                    ? new RemoteBuildStrategy(buildService)
                    : createInProcessBuilder();
            TeaVMBuild build = new TeaVMBuild(context, assistant, buildStrategy, outputConsumer, buildService != null);

            build.perform(target.getModule(), target);
        } catch (BuildException e) {
            context.processMessage(new CompilerMessage("TeaVM", e.getCause()));
        } catch (Exception e) {
            context.processMessage(new CompilerMessage("TeaVM", e));
        }
    }

    private BuildStrategy createInProcessBuilder() {
        return new InProcessBuildStrategy((urls, innerClassLoader) -> {
            RenamingClassLoader loader = new RenamingClassLoader(urls, innerClassLoader);
            loader.rename("org/objectweb/asm/", "org/teavm/asm/");
            return loader;
        });
    }

    @NotNull
    @Override
    public String getPresentableName() {
        return "TeaVM builder";
    }
}
