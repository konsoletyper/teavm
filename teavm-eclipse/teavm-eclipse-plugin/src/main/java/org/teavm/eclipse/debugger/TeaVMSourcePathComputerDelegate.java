/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.eclipse.debugger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMSourcePathComputerDelegate implements ISourcePathComputerDelegate {
    @Override
    public ISourceContainer[] computeSourceContainers(ILaunchConfiguration config, IProgressMonitor monitor)
            throws CoreException {
        List<ISourceContainer> sourceContainers = new ArrayList<>();
        /*IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject project : projects) {
            if (!project.isOpen()) {
                continue;
            }
            if (project.hasNature(JavaCore.NATURE_ID)) {
                IJavaProject javaProject = JavaCore.create(project);
                for (IPackageFragmentRoot fragmentRoot : javaProject.getAllPackageFragmentRoots()) {
                    if (fragmentRoot.getResource() instanceof IFolder) {
                        sourceContainers.add(new FolderSourceContainer((IFolder)fragmentRoot.getResource(), true));
                    }
                }
            }
        }*/
        IRuntimeClasspathEntry[] entries = JavaRuntime.computeUnresolvedSourceLookupPath(config);
        IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveSourceLookupPath(entries, config);
        sourceContainers.addAll(Arrays.asList(JavaRuntime.getSourceContainers(resolved)));
        return sourceContainers.toArray(new ISourceContainer[sourceContainers.size()]);
    }
}
