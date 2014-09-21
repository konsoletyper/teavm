package org.teavm.eclipse.m2e;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
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
import org.teavm.eclipse.TeaVMRuntimeMode;

/**
 *
 * @author Alexey Andreev
 */
public class TeaVMProjectConfigurator extends AbstractProjectConfigurator {
    private static final String TOOL_ID = "teavm-eclipse-m2e-plugin.tool";
    private static final String TEAVM_ARTIFACT_ID = "teavm-maven-plugin";
    private static final String TEAVM_GROUP_ID = "org.teavm";
    private static final String TEAVM_MAIN_GOAL = "build-javascript";
    private int executionIdGenerator;
    private Set<String> usedExecutionIds = new HashSet<>();
    private IMaven maven;
    private MavenProject mavenProject;
    private IProject project;

    @Override
    public void configure(ProjectConfigurationRequest configurationRequest, IProgressMonitor monitor)
            throws CoreException {
        maven = MavenPlugin.getMaven();
        mavenProject = configurationRequest.getMavenProject();
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
        settings.load();
        try {
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
            if (!hasNature) {
                teaVMPlugin.addNature(new SubProgressMonitor(monitor, 1000), project);
            }
            settings.save();
        } finally {
            monitor.done();
        }
    }

    private void configureProfile(MojoExecution execution, TeaVMProfile profile, IProgressMonitor monitor)
            throws CoreException {
        monitor.beginTask("Configuring profile " + profile.getName(), 110);
        String buildDir = getProjectBuildDirectory();

        String mainClass = maven.getMojoParameterValue(mavenProject, execution, "mainClass", String.class,
                new SubProgressMonitor(monitor, 10));
        profile.setMainClass(mainClass);

        String targetDir = maven.getMojoParameterValue(mavenProject, execution, "targetDirectory", String.class,
                new SubProgressMonitor(monitor, 10));
        profile.setTargetDirectory(targetDir != null ? absolutePathToWorkspacePath(targetDir) :
                buildDir + "/javascript");

        String targetFileName = maven.getMojoParameterValue(mavenProject, execution, "targetFileName", String.class,
                new SubProgressMonitor(monitor, 10));
        profile.setTargetFileName(targetFileName != null ? targetFileName : "classes.js");

        Boolean minifying = maven.getMojoParameterValue(mavenProject, execution, "minifying", Boolean.class,
                new SubProgressMonitor(monitor, 10));
        profile.setMinifying(minifying != null ? minifying : true);

        String runtime = maven.getMojoParameterValue(mavenProject, execution, "runtime", String.class,
                new SubProgressMonitor(monitor, 10));
        profile.setRuntimeMode(runtime != null ? getRuntimeMode(runtime) : TeaVMRuntimeMode.SEPARATE);

        Properties properties = maven.getMojoParameterValue(mavenProject, execution, "properties", Properties.class,
                new SubProgressMonitor(monitor, 10));
        profile.setProperties(properties != null ? properties : new Properties());

        Boolean debug = maven.getMojoParameterValue(mavenProject, execution, "debugInformationGenerated",
                Boolean.class, new SubProgressMonitor(monitor, 10));
        profile.setDebugInformationGenerated(debug != null ? debug : false);

        Boolean sourceMaps = maven.getMojoParameterValue(mavenProject, execution, "sourceMapsGenerated",
                Boolean.class, new SubProgressMonitor(monitor, 10));
        profile.setSourceMapsGenerated(sourceMaps != null ? sourceMaps : false);

        Boolean incremental = maven.getMojoParameterValue(mavenProject, execution, "incremental", Boolean.class,
                new SubProgressMonitor(monitor, 10));
        profile.setIncremental(incremental != null ? incremental : false);

        String cacheDir = maven.getMojoParameterValue(mavenProject, execution, "cacheDirectory", String.class,
                new SubProgressMonitor(monitor, 10));
        profile.setCacheDirectory(cacheDir != null ? absolutePathToWorkspacePath(cacheDir) :
                buildDir + "/teavm-cache");

        String[] transformers = maven.getMojoParameterValue(mavenProject, execution, "transformers", String[].class,
                new SubProgressMonitor(monitor, 10));
        profile.setTransformers(transformers != null ? transformers : new String[0]);

        profile.setClassAliases(readClassAliases(execution));

        monitor.done();
    }

    private Map<String, String> readClassAliases(MojoExecution execution) {
        Map<String, String> aliases = new HashMap<>();
        Xpp3Dom aliasesElem = execution.getConfiguration().getChild("classAliases");
        if (aliasesElem != null) {
            for (Xpp3Dom item : aliasesElem.getChildren()) {
                String className = item.getChild("className").getValue();
                String alias = item.getChild("alias").getValue();
                aliases.put(className, alias);
            }
        }
        return aliases;
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
        try {
            IStringVariableManager varManager = VariablesPlugin.getDefault().getStringVariableManager();
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IContainer[] containers = root.findContainersForLocationURI(new URI("file://" + path));
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
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private TeaVMRuntimeMode getRuntimeMode(String name) {
        switch (name) {
            case "SEPARATE":
                return TeaVMRuntimeMode.SEPARATE;
            case "MERGED":
                return TeaVMRuntimeMode.MERGE;
            case "NONE":
                return TeaVMRuntimeMode.NONE;
            default:
                return TeaVMRuntimeMode.NONE;
        }
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
