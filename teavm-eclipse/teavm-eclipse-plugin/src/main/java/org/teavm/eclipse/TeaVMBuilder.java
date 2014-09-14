package org.teavm.eclipse;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.teavm.tooling.RuntimeCopyOperation;
import org.teavm.tooling.TeaVMTool;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMBuilder extends IncrementalProjectBuilder {
    @Override
    protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
        TeaVMProjectSettings projectSettings = getProjectSettings();
        projectSettings.load();
        TeaVMTool tool = new TeaVMTool();
        tool.setClassLoader(prepareClassLoader());
        tool.setDebugInformationGenerated(true);
        tool.setSourceMapsFileGenerated(true);
        String targetDir = projectSettings.getTargetDirectory();
        targetDir = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(targetDir);
        tool.setTargetDirectory(new File(targetDir));
        tool.setRuntime(RuntimeCopyOperation.SEPARATE);
        tool.setMinifying(false);
        tool.setMainClass(projectSettings.getMainClass());

        return null;
    }

    private TeaVMProjectSettings getProjectSettings() {
        return TeaVMEclipsePlugin.getDefault().getSettings(getProject());
    }

    private ClassLoader prepareClassLoader() throws CoreException {
        return new URLClassLoader(prepareClassPath(), Thread.currentThread().getContextClassLoader());
    }

    private URL[] prepareClassPath() throws CoreException {
        IProject project = getProject();
        if (!project.hasNature(JavaCore.NATURE_ID)) {
            return new URL[0];
        }
        IJavaProject javaProject = JavaCore.create(project);
        List<URL> urls = new ArrayList<>();
        try {
            urls.add(javaProject.getOutputLocation().toFile().toURI().toURL());
        } catch (MalformedURLException e) {
            TeaVMEclipsePlugin.logError(e);
        }
        IClasspathEntry[] entries = javaProject.getResolvedClasspath(true);
        for (IClasspathEntry entry : entries) {
            switch (entry.getEntryKind()) {
                case IClasspathEntry.CPE_LIBRARY:
                    try {
                        urls.add(entry.getPath().toFile().toURI().toURL());
                    } catch (MalformedURLException e) {
                        TeaVMEclipsePlugin.logError(e);
                    }
                    break;
                case IClasspathEntry.CPE_SOURCE:
                    if (entry.getOutputLocation() != null) {
                        try {
                            urls.add(entry.getOutputLocation().toFile().toURI().toURL());
                        } catch (MalformedURLException e) {
                            TeaVMEclipsePlugin.logError(e);
                        }
                    }
                    break;
                case IClasspathEntry.CPE_PROJECT: {
                    IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(entry.getPath());
                    IProject depProject = resource.getProject();
                    if (!depProject.hasNature(JavaCore.NATURE_ID)) {
                        break;
                    }
                    IJavaProject depJavaProject = JavaCore.create(depProject);
                    if (depJavaProject.getOutputLocation() != null) {
                        try {
                            urls.add(depJavaProject.getOutputLocation().toFile().toURI().toURL());
                        } catch (MalformedURLException e) {
                            TeaVMEclipsePlugin.logError(e);
                        }
                    }
                    break;
                }
            }
        }
        return urls.toArray(new URL[urls.size()]);
    }
}
