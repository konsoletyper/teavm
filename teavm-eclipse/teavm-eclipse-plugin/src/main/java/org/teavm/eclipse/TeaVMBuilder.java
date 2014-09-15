package org.teavm.eclipse;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.core.*;
import org.teavm.dependency.*;
import org.teavm.model.InstructionLocation;
import org.teavm.model.MethodReference;
import org.teavm.tooling.RuntimeCopyOperation;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolException;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMBuilder extends IncrementalProjectBuilder {
    private URL[] classPath;
    private IContainer[] sourceContainers;

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
        tool.setProgressListener(new TeaVMEclipseProgressListener(monitor));
        try {
            tool.generate();
            removeMarkers();
            if (tool.getDependencyViolations().hasMissingItems()) {
                putMarkers(tool.getDependencyViolations());
            }
        } catch (TeaVMToolException e) {
            throw new CoreException(TeaVMEclipsePlugin.makeError(e));
        }
        return null;
    }

    private void removeMarkers() throws CoreException {
        getProject().deleteMarkers(TeaVMEclipsePlugin.DEPENDENCY_MARKER_ID, true, IResource.DEPTH_INFINITE);
    }

    private void putMarkers(DependencyViolations violations) throws CoreException {
        for (ClassDependencyInfo dep : violations.getMissingClasses()) {
            putMarker("Missing class " + dep.getClassName(), dep.getStack());
        }
        for (FieldDependencyInfo dep : violations.getMissingFields()) {
            putMarker("Missing field " + dep.getReference().toString(), dep.getStack());
        }
        for (MethodDependencyInfo dep : violations.getMissingMethods()) {
            putMarker("Missing method " + dep.getReference().toString(), dep.getStack());
        }
    }

    private void putMarker(String message, DependencyStack stack) throws CoreException {
        while (stack != DependencyStack.ROOT) {
            putMarker(message, stack.getLocation(), stack.getMethod());
            stack = stack.getCause();
        }
    }

    private void putMarker(String message, InstructionLocation location, MethodReference methodRef)
            throws CoreException {
        IResource resource = null;
        if (location != null) {
            String resourceName = location.getFileName();
            for (IContainer container : sourceContainers) {
                resource = container.findMember(resourceName);
                if (resource != null) {
                    break;
                }
            }
        }
        if (resource == null) {
            String resourceName = methodRef.getClassName().replace('.', '/') + ".java";
            for (IContainer container : sourceContainers) {
                resource = container.findMember(resourceName);
                if (resource != null) {
                    break;
                }
            }
        }
        if (resource != null) {
            IMarker marker = resource.createMarker(TeaVMEclipsePlugin.DEPENDENCY_MARKER_ID);
            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
            marker.setAttribute(IMarker.MESSAGE, message);
            if (location != null) {
                marker.setAttribute(IMarker.LINE_NUMBER, location.getLine());
            } else {
                ICompilationUnit unit = (ICompilationUnit)JavaCore.create(resource);
                IType type = unit.getType(methodRef.getClassName());
                // TODO: find method declaration location and put marker there
            }
        }
    }

    private TeaVMProjectSettings getProjectSettings() {
        return TeaVMEclipsePlugin.getDefault().getSettings(getProject());
    }

    private ClassLoader prepareClassLoader() throws CoreException {
        prepareClassPath();
        return new URLClassLoader(classPath, TeaVMBuilder.class.getClassLoader());
    }

    private void prepareClassPath() throws CoreException {
        classPath = new URL[0];
        sourceContainers = new IContainer[0];
        IProject project = getProject();
        if (!project.hasNature(JavaCore.NATURE_ID)) {
            return;
        }
        IJavaProject javaProject = JavaCore.create(project);
        PathCollector collector = new PathCollector();
        SourcePathCollector srcCollector = new SourcePathCollector();
        IWorkspaceRoot workspaceRoot = project.getWorkspace().getRoot();
        try {
            collector.addPath(workspaceRoot.findMember(javaProject.getOutputLocation()).getLocation());
        } catch (MalformedURLException e) {
            TeaVMEclipsePlugin.logError(e);
        }
        IClasspathEntry[] entries = javaProject.getResolvedClasspath(true);
        for (IClasspathEntry entry : entries) {
            switch (entry.getEntryKind()) {
                case IClasspathEntry.CPE_LIBRARY:
                    try {
                        collector.addPath(entry.getPath());
                    } catch (MalformedURLException e) {
                        TeaVMEclipsePlugin.logError(e);
                    }
                    break;
                case IClasspathEntry.CPE_SOURCE:
                    if (entry.getOutputLocation() != null) {
                        try {
                            collector.addPath(workspaceRoot.findMember(entry.getOutputLocation()).getLocation());
                        } catch (MalformedURLException e) {
                            TeaVMEclipsePlugin.logError(e);
                        }
                    }
                    srcCollector.addContainer((IContainer)workspaceRoot.findMember(entry.getPath()));
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
                            collector.addPath(workspaceRoot.findMember(depJavaProject.getOutputLocation())
                                    .getLocation());
                        } catch (MalformedURLException e) {
                            TeaVMEclipsePlugin.logError(e);
                        }
                    }
                    break;
                }
            }
        }
        classPath = collector.getUrls();
        sourceContainers = srcCollector.getContainers();
    }

    static class PathCollector {
        private Set<URL> urlSet = new HashSet<>();
        private List<URL> urls = new ArrayList<>();

        public void addPath(IPath path) throws MalformedURLException {
            File file = path.toFile();
            if (!file.exists()) {
                return;
            }
            if (file.isDirectory()) {
                file = new File(file.getAbsolutePath() + "/");
            } else {
                file = new File(file.getAbsolutePath());
            }
            URL url = file.toURI().toURL();
            if (urlSet.add(url)) {
                urls.add(url);
            }
        }

        public URL[] getUrls() {
            return urls.toArray(new URL[urls.size()]);
        }
    }

    static class SourcePathCollector {
        private Set<IContainer> containerSet = new HashSet<>();
        private List<IContainer> containers = new ArrayList<>();

        public void addContainer(IContainer container) {
            if (containerSet.add(container)) {
                containers.add(container);
            }
        }

        public IContainer[] getContainers() {
            return containers.toArray(new IContainer[containers.size()]);
        }
    }
}
