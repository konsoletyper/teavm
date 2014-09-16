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
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.teavm.dependency.*;
import org.teavm.model.InstructionLocation;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
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
    private IContainer[] classFileContainers;
    private Set<IProject> usedProjects = new HashSet<>();

    @Override
    protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
        if ((kind == AUTO_BUILD || kind == INCREMENTAL_BUILD) && !shouldBuild()) {
            System.out.println("Skipping project " + getProject().getName());
            return null;
        }
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
        tool.setProgressListener(new TeaVMEclipseProgressListener(this, monitor, 10000));
        try {
            monitor.beginTask("Running TeaVM", 10000);
            tool.generate();
            removeMarkers();
            if (tool.getDependencyViolations().hasMissingItems()) {
                putMarkers(tool.getDependencyViolations());
            }
            TeaVMEclipsePlugin.getDefault().setProjectClasses(getProject(), classesToResources(tool.getClasses()));
            if (!monitor.isCanceled()) {
                monitor.done();
            }
        } catch (TeaVMToolException e) {
            throw new CoreException(TeaVMEclipsePlugin.makeError(e));
        } finally {
            sourceContainers = null;
            classFileContainers = null;
            classPath = null;
            usedProjects.clear();
        }
        return !usedProjects.isEmpty() ? usedProjects.toArray(new IProject[0]) : null;
    }

    private Set<String> classesToResources(Collection<String> classNames) {
        Set<String> resourcePaths = new HashSet<>();
        for (String className : classNames) {
            for (IContainer clsContainer : classFileContainers) {
                IResource res = clsContainer.findMember(className.replace('.', '/') + ".class");
                if (res != null) {
                    resourcePaths.add(res.getFullPath().toString());
                    usedProjects.add(res.getProject());
                }
            }
        }
        return resourcePaths;
    }

    private boolean shouldBuild() throws CoreException {
        Set<String> classes = TeaVMEclipsePlugin.getDefault().getProjectClasses(getProject());
        if (classes.isEmpty()) {
            return true;
        }
        for (IProject project : getRelatedProjects()) {
            IResourceDelta delta = getDelta(project);
            if (shouldBuild(classes, delta)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldBuild(Set<String> classes, IResourceDelta delta) {
        if (classes.contains(delta.getResource().getFullPath().toString())) {
            return true;
        }
        for (IResourceDelta child : delta.getAffectedChildren()) {
            if (shouldBuild(classes, child)) {
                return true;
            }
        }
        return false;
    }

    private void removeMarkers() throws CoreException {
        getProject().deleteMarkers(TeaVMEclipsePlugin.DEPENDENCY_MARKER_ID, true, IResource.DEPTH_INFINITE);
    }

    private void putMarkers(DependencyViolations violations) throws CoreException {
        for (ClassDependencyInfo dep : violations.getMissingClasses()) {
            putMarker("Missing class " + getSimpleClassName(dep.getClassName()), dep.getStack());
        }
        for (FieldDependencyInfo dep : violations.getMissingFields()) {
            putMarker("Missing field " + getSimpleClassName(dep.getReference().getClassName()) + "." +
                    dep.getReference().getFieldName(), dep.getStack());
        }
        for (MethodDependencyInfo dep : violations.getMissingMethods()) {
            putMarker("Missing method " + getFullMethodName(dep.getReference()), dep.getStack());
        }
    }

    private void putMarker(String message, DependencyStack stack) throws CoreException {
        StringBuilder sb = new StringBuilder();
        sb.append(message);
        boolean wasPut = false;
        while (stack != DependencyStack.ROOT) {
            wasPut |= putMarker(sb.toString(), stack.getLocation(), stack.getMethod());
            if (stack.getMethod() != null) {
                sb.append(", used by ").append(getFullMethodName(stack.getMethod()));
            }
            stack = stack.getCause();
        }
        if (!wasPut) {
            IMarker marker = getProject().createMarker(TeaVMEclipsePlugin.DEPENDENCY_MARKER_ID);
            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
            marker.setAttribute(IMarker.MESSAGE, message);
        }
    }

    private String getFullMethodName(MethodReference methodRef) {
        StringBuilder sb = new StringBuilder();
        sb.append(getSimpleClassName(methodRef.getClassName())).append('.').append(methodRef.getName()).append('(');
        if (methodRef.getDescriptor().parameterCount() > 0) {
            sb.append(getTypeName(methodRef.getDescriptor().parameterType(0)));
            for (int i = 1; i < methodRef.getDescriptor().parameterCount(); ++i) {
                sb.append(',').append(getTypeName(methodRef.getDescriptor().parameterType(i)));
            }
        }
        sb.append(')');
        return sb.toString();
    }

    private String getTypeName(ValueType type) {
        int arrayDim = 0;
        while (type instanceof ValueType.Array) {
            ValueType.Array array = (ValueType.Array)type;
            type = array.getItemType();
        }
        StringBuilder sb = new StringBuilder();
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive)type).getKind()) {
                case BOOLEAN:
                    sb.append("boolean");
                    break;
                case BYTE:
                    sb.append("byte");
                    break;
                case CHARACTER:
                    sb.append("char");
                    break;
                case SHORT:
                    sb.append("short");
                    break;
                case INTEGER:
                    sb.append("int");
                    break;
                case LONG:
                    sb.append("long");
                    break;
                case FLOAT:
                    sb.append("float");
                    break;
                case DOUBLE:
                    sb.append("double");
                    break;
            }
        } else if (type instanceof ValueType.Object) {
            ValueType.Object cls = (ValueType.Object)type;
            sb.append(getSimpleClassName(cls.getClassName()));
        }
        while (arrayDim-- > 0) {
            sb.append("[]");
        }
        return sb.toString();
    }

    private String getSimpleClassName(String className) {
        int index = className.lastIndexOf('.');
        return className.substring(index + 1);
    }

    private boolean putMarker(String message, InstructionLocation location, MethodReference methodRef)
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
                marker.setAttribute(IMarker.LINE_NUMBER, 1);
            }
            return true;
        } else {
            return false;
        }
    }

    private TeaVMProjectSettings getProjectSettings() {
        return TeaVMEclipsePlugin.getDefault().getSettings(getProject());
    }

    private ClassLoader prepareClassLoader() throws CoreException {
        prepareClassPath();
        return new URLClassLoader(classPath, TeaVMBuilder.class.getClassLoader());
    }

    private Set<IProject> getRelatedProjects() throws CoreException {
        Set<IProject> projects = new HashSet<>();
        Set<IProject> visited = new HashSet<>();
        Queue<IProject> queue = new ArrayDeque<>();
        queue.add(getProject());
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        while (!queue.isEmpty()) {
            IProject project = queue.remove();
            if (!visited.add(project) || !project.hasNature(JavaCore.NATURE_ID)) {
                continue;
            }
            projects.add(project);
            IJavaProject javaProject = JavaCore.create(project);
            for (IClasspathEntry entry : javaProject.getRawClasspath()) {
                if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                    project = (IProject)root.findMember(entry.getPath());
                    queue.add(project);
                }
            }
        }
        return projects;
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
        SourcePathCollector binCollector = new SourcePathCollector();
        IWorkspaceRoot workspaceRoot = project.getWorkspace().getRoot();
        try {
            if (javaProject.getOutputLocation() != null) {
                IContainer container = (IContainer)workspaceRoot.findMember(javaProject.getOutputLocation());
                collector.addPath(container.getLocation());
                binCollector.addContainer(container);
            }
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
                    IContainer srcContainer = (IContainer)workspaceRoot.findMember(entry.getPath());
                    if (srcContainer.getProject() == project) {
                        srcCollector.addContainer(srcContainer);
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
                            IContainer container = (IContainer)workspaceRoot.findMember(
                                    depJavaProject.getOutputLocation());
                            collector.addPath(container.getLocation());
                            binCollector.addContainer(container);
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
        classFileContainers = binCollector.getContainers();
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
