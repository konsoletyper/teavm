/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.idea.debug;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.debugging.CallFrame;
import org.teavm.debugging.Debugger;

public class TeaVMExecutionStack extends XExecutionStack {
    private Debugger innerDebugger;
    private TeaVMStackFrame[] frames;
    private ProjectRootManager rootManager;

    public TeaVMExecutionStack(@NotNull Debugger innerDebugger, @NotNull Project project) {
        super("TeaVM");
        this.innerDebugger = innerDebugger;
        rootManager = ProjectRootManager.getInstance(project);
    }

    @Nullable
    @Override
    public XStackFrame getTopFrame() {
        TeaVMStackFrame[] frames = calculateFrames();
        return frames.length > 0 ? frames[0] : null;
    }

    @NotNull
    private TeaVMStackFrame[] calculateFrames() {
        if (frames == null) {
            CallFrame[] innerCallStack = innerDebugger.getCallStack();
            frames = new TeaVMStackFrame[innerCallStack.length];
            for (int i = 0; i < innerCallStack.length; ++i) {
                frames[i] = new TeaVMStackFrame(this, innerCallStack[i]);
            }
        }
        return frames;
    }

    @Override
    public void computeStackFrames(int firstFrameIndex, XStackFrameContainer container) {
        TeaVMStackFrame[] frames = calculateFrames();
        int index = Math.min(firstFrameIndex, frames.length);
        container.addStackFrames(Arrays.asList(frames).subList(index, frames.length), true);
    }

    @Nullable
    VirtualFile findVirtualFile(@NotNull String partialPath) {
        VirtualFile[] resultHolder = new VirtualFile[1];
        ApplicationManager.getApplication().runReadAction(() -> {
            Stream<VirtualFile> roots = Stream.concat(
                    Arrays.stream(rootManager.getContentSourceRoots()),
                    Arrays.stream(rootManager.orderEntries().getAllSourceRoots()));
            resultHolder[0] = roots
                    .map(sourceRoot -> sourceRoot.findFileByRelativePath(partialPath))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        });
        return resultHolder[0];
    }
}
