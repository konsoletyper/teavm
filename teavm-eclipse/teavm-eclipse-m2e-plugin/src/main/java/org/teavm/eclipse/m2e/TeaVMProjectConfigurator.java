package org.teavm.eclipse.m2e;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.teavm.eclipse.TeaVMEclipsePlugin;
import org.teavm.eclipse.TeaVMProfile;
import org.teavm.eclipse.TeaVMProjectSettings;

/**
 *
 * @author Alexey Andreev
 */
public class TeaVMProjectConfigurator extends AbstractProjectConfigurator {
    private static final String TEAVM_ARTIFACT_ID = "teavm-maven-plugin";
    private static final String TEAVM_GROUP_ID = "org.teavm";
    private static final String TEAVM_MAIN_GOAL = "build-javascript";
    private int executionIdGenerator;
    private Set<String> usedExecutionIds = new HashSet<>();
    private IMaven maven;
    private MavenProject mavenProject;

    @Override
    public void configure(ProjectConfigurationRequest configurationRequest, IProgressMonitor monitor)
            throws CoreException {
        maven = MavenPlugin.getMaven();
        mavenProject = configurationRequest.getMavenProject();
        List<MojoExecution> executions = configurationRequest.getMavenProjectFacade().getMojoExecutions(
                TEAVM_GROUP_ID, TEAVM_ARTIFACT_ID, monitor, TEAVM_MAIN_GOAL);
        TeaVMEclipsePlugin teaVMPlugin = TeaVMEclipsePlugin.getDefault();
        IProject project = configurationRequest.getProject();
        boolean hasNature = project.hasNature(TeaVMEclipsePlugin.NATURE_ID);
        int sz = executions.size();
        if (!hasNature) {
            ++sz;
        }
        monitor.beginTask("Configuring TeaVM builder", sz * 1000);
        TeaVMProjectSettings settings = teaVMPlugin.getSettings(project);
        settings.load();
        try {
            for (MojoExecution execution : executions) {
                if (monitor.isCanceled()) {
                    return;
                }
                String profileId = getIdForProfile(execution);
                TeaVMProfile profile = settings.getProfile(profileId);
                if (profile == null) {
                    profile = settings.createProfile();
                    profile.setName(profileId);
                }
                configureProfile(execution, profile, new SubProgressMonitor(monitor, 1000));
                if (monitor.isCanceled()) {
                    return;
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
        monitor.beginTask("Configuring profile " + profile.getName(), 30);
        String mainClass = maven.getMojoParameterValue(mavenProject, execution, "mainClass", String.class,
                new SubProgressMonitor(monitor, 10));
        profile.setMainClass(mainClass);
        String targetDir = maven.getMojoParameterValue(mavenProject, execution, "targetDirectory", String.class,
                new SubProgressMonitor(monitor, 10));
        profile.setTargetDirectory(targetDir);
        String targetFileName = maven.getMojoParameterValue(mavenProject, execution, "targetFileName", String.class,
                new SubProgressMonitor(monitor, 10));
        profile.setTargetFileName(targetFileName != null ? targetFileName : "classes.js");
        monitor.done();
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
