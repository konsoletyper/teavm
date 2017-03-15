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
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.builders.DirtyFilesHolder;
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.incremental.BuilderCategory;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.incremental.ModuleLevelBuilder;
import org.jetbrains.jps.incremental.ProjectBuildException;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.incremental.messages.CompilerMessage;
import org.jetbrains.jps.model.module.JpsModule;
import org.teavm.idea.jps.model.TeaVMBuildStrategy;
import org.teavm.idea.jps.remote.TeaVMBuilderAssistant;
import org.teavm.idea.jps.remote.TeaVMRemoteBuildService;

public class TeaVMBuilder extends ModuleLevelBuilder {
    private TeaVMBuilderAssistant assistant;
    private TeaVMRemoteBuildService buildService;

    public TeaVMBuilder() {
        super(BuilderCategory.CLASS_POST_PROCESSOR);

        String portString = System.getProperty(REMOTE_PORT);
        if (portString != null) {
            try {
                Registry registry = LocateRegistry.getRegistry(Integer.parseInt(portString));
                assistant = (TeaVMBuilderAssistant) registry.lookup(TeaVMBuilderAssistant.ID);
            } catch (NumberFormatException | RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }

        String daemonPortString = System.getProperty(TeaVMRemoteBuildService.REMOTE_PORT);
        if (daemonPortString != null) {
            try {
                Registry registry = LocateRegistry.getRegistry(Integer.parseInt(daemonPortString));
                buildService = (TeaVMRemoteBuildService) registry.lookup(TeaVMRemoteBuildService.ID);
            } catch (NumberFormatException | RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ExitCode build(CompileContext context, ModuleChunk chunk,
            DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget> dirtyFilesHolder,
            OutputConsumer outputConsumer) throws ProjectBuildException, IOException {
        if (assistant == null) {
            context.processMessage(new CompilerMessage("TeaVM", BuildMessage.Kind.WARNING,
                    "No TeaVM builder assistant available. Diagnostic messages will be less informative"));
        }

        boolean doneSomething = false;

        TeaVMBuildStrategy buildStrategy = buildService != null
                ? new RemoteBuildStrategy(context, buildService)
                : new InProcessBuildStrategy(context);
        TeaVMBuild build = new TeaVMBuild(context, assistant, buildStrategy);
        for (JpsModule module : chunk.getModules()) {
            doneSomething |= build.perform(module, chunk.representativeTarget());
            if (context.getCancelStatus().isCanceled()) {
                return ExitCode.ABORT;
            }
        }

        return doneSomething ? ExitCode.OK : ExitCode.NOTHING_DONE;
    }

    @NotNull
    @Override
    public String getPresentableName() {
        return "TeaVM builder";
    }
}
