package org.teavm.eclipse.debugger;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.debug.core.sourcelookup.containers.FolderSourceContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMSourcePathComputerDelegate implements ISourcePathComputerDelegate {
    @Override
    public ISourceContainer[] computeSourceContainers(ILaunchConfiguration config, IProgressMonitor monitor)
            throws CoreException {
        List<ISourceContainer> sourceContainers = new ArrayList<>();
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
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
        }
        return sourceContainers.toArray(new ISourceContainer[0]);
    }
}
