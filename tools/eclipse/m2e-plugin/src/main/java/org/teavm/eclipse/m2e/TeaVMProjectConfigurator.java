package org.teavm.eclipse.m2e;

import java.io.File;
import java.util.*;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.teavm.eclipse.TeaVMEclipsePlugin;
import org.teavm.eclipse.TeaVMProfile;
import org.teavm.eclipse.TeaVMProjectSettings;

public class TeaVMProjectConfigurator extends AbstractProjectConfigurator {
    private static final String TOOL_ID = "teavm-eclipse-m2e-plugin.tool";
    private static final String TEAVM_ARTIFACT_ID = "teavm-maven-plugin";
    private static final String TEAVM_GROUP_ID = "org.teavm";
    private static final String TEAVM_MAIN_GOAL = "compile";
    private int executionIdGenerator;
    private Set<String> usedExecutionIds = new HashSet<>();
    private IMaven maven;
    private MavenSession mavenSession;
    private IProject project;

    @Override
    public void configure(ProjectConfigurationRequest configurationRequest, IProgressMonitor monitor)
            throws CoreException {
        maven = MavenPlugin.getMaven();
        mavenSession = configurationRequest.getMavenSession();
        List<MojoExecution> executions = configurationRequest.getMavenProjectFacade().getMojoExecutions(
                TEAVM_GROUP_ID, TEAVM_ARTIFACT_ID, monitor, TEAVM_MAIN_GOAL);
        TeaVMEclipsePlugin teaVMPlugin = TeaVMEclipsePlugin.getDefault();
        project = configurationRequest.getProject();
        boolean hasNature = project.hasNature(TeaVMEclipsePlugin.NATURE_ID);
        int sz = executions.size();
        if (!hasNature) {
            ++sz;
        }
        monitor.beginTask("Configuring TeaVM builder", sz * 1000);
        TeaVMProjectSettings settings = teaVMPlugin.getSettings(project);
        try {
            if (!hasNature) {
                teaVMPlugin.addNature(new SubProgressMonitor(monitor, 1000), project);
            }
            settings.load();
            Set<String> coveredProfiles = new HashSet<>();
            for (MojoExecution execution : executions) {
                if (monitor.isCanceled()) {
                    return;
                }
                String profileId = getIdForProfile(execution);
                coveredProfiles.add(profileId);
                TeaVMProfile profile = settings.getProfile(profileId);
                if (profile == null) {
                    profile = settings.createProfile();
                    profile.setName(profileId);
                }
                profile.setExternalToolId(TOOL_ID);
                configureProfile(execution, profile, new SubProgressMonitor(monitor, 1000));
                if (monitor.isCanceled()) {
                    return;
                }
            }
            for (TeaVMProfile profile : settings.getProfiles()) {
                if (!coveredProfiles.contains(profile.getName()) && profile.getExternalToolId().equals(TOOL_ID)) {
                    settings.deleteProfile(profile);
                }
            }
            settings.save();
        } finally {
            monitor.done();
        }
    }

    private void configureProfile(MojoExecution execution, TeaVMProfile profile, IProgressMonitor monitor)
            throws CoreException {
        monitor.beginTask("Configuring profile " + profile.getName(), 100);
        String buildDir = getProjectBuildDirectory();

        String mainClass = maven.getMojoParameterValue(mavenSession, execution, "mainClass", String.class);
        profile.setMainClass(mainClass);
        monitor.worked(10);

        String targetDir = maven.getMojoParameterValue(mavenSession, execution, "targetDirectory", String.class);
        profile.setTargetDirectory(targetDir != null ? absolutePathToWorkspacePath(targetDir) :
                buildDir + "/javascript");
        monitor.worked(10);

        String targetFileName = maven.getMojoParameterValue(mavenSession, execution, "targetFileName", String.class);
        profile.setTargetFileName(targetFileName != null ? targetFileName : "classes.js");
        monitor.worked(10);

        Properties properties = maven.getMojoParameterValue(mavenSession, execution, "properties", Properties.class);
        profile.setProperties(properties != null ? properties : new Properties());
        monitor.worked(10);

        Boolean debug = maven.getMojoParameterValue(mavenSession, execution, "debugInformationGenerated",
                Boolean.class);
        profile.setDebugInformationGenerated(debug != null ? debug : false);
        monitor.worked(10);

        Boolean sourceMaps = maven.getMojoParameterValue(mavenSession, execution, "sourceMapsGenerated",
                Boolean.class);
        profile.setSourceMapsGenerated(sourceMaps != null ? sourceMaps : false);
        monitor.worked(10);

        Boolean sourceFiles = maven.getMojoParameterValue(mavenSession, execution, "sourceFilesCopied",
                Boolean.class);
        profile.setSourceFilesCopied(sourceFiles != null ? sourceFiles : false);
        monitor.worked(10);

        Boolean incremental = maven.getMojoParameterValue(mavenSession, execution, "incremental", Boolean.class);
        profile.setIncremental(incremental != null ? incremental : false);
        monitor.worked(10);

        String cacheDir = maven.getMojoParameterValue(mavenSession, execution, "cacheDirectory", String.class);
        profile.setCacheDirectory(cacheDir != null ? absolutePathToWorkspacePath(cacheDir) :
                buildDir + "/teavm-cache");
        monitor.worked(10);

        String[] transformers = maven.getMojoParameterValue(mavenSession, execution, "transformers", String[].class);
        profile.setTransformers(transformers != null ? transformers : new String[0]);
        monitor.worked(10);

        profile.setClassesToPreserve(readClassesToPreserve(execution));
        monitor.worked(10);

        monitor.done();
    }

    private Set<? extends String> readClassesToPreserve(MojoExecution execution) {
        Set<String> classes = new HashSet<>();
        Xpp3Dom classesElem = execution.getConfiguration().getChild("classesToPreserve");
        if (classesElem != null) {
            for (Xpp3Dom item : classesElem.getChildren()) {
                classes.add(item.getValue());
            }
        }
        return classes;
    }

    private String getProjectBuildDirectory() throws CoreException {
        IStringVariableManager varManager = VariablesPlugin.getDefault().getStringVariableManager();
        if (!project.hasNature(JavaCore.NATURE_ID)) {
            return varManager.generateVariableExpression("workspace_loc", "/" + project.getName());
        }
        IJavaProject javaProject = JavaCore.create(project);
        String path = javaProject.getOutputLocation().toString();
        return varManager.generateVariableExpression("workspace_loc", path);
    }

    private String absolutePathToWorkspacePath(String path) {
        IStringVariableManager varManager = VariablesPlugin.getDefault().getStringVariableManager();
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IContainer[] containers = root.findContainersForLocationURI(new File(path).toURI());
        if (containers.length == 0) {
            return null;
        }
        IContainer container = containers[0];
        String suffix = "";
        while (!(container instanceof IProject)) {
            suffix = "/" + container.getName() + suffix;
            container = container.getParent();
        }
        path = container.getFullPath().toString();
        return varManager.generateVariableExpression("workspace_loc", path) + suffix;
    }

    private String getIdForProfile(MojoExecution pluginExecution) {
        String executionId = pluginExecution.getExecutionId();
        if (executionId != null && usedExecutionIds.add(executionId)) {
            return executionId;
        }
        String id;
        do {
            id = "maven-" + executionIdGenerator++;
        } while (!usedExecutionIds.add(id));
        return id;
    }
}
