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
                sb.append(", called by ").append(getFullMethodName(stack.getMethod()));
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
