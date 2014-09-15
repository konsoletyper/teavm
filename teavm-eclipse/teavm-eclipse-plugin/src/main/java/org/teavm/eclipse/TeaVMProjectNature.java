package org.teavm.eclipse;

import java.util.Arrays;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMProjectNature implements IProjectNature {
    private IProject project;

    private boolean hasBuilder() throws CoreException {
        IProjectDescription description = project.getDescription();
        ICommand[] buildCommands = description.getBuildSpec();
        for (ICommand command : buildCommands) {
            if (command.getBuilderName().equals(TeaVMEclipsePlugin.BUILDER_ID)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void configure() throws CoreException {
        if (!hasBuilder()) {
            IProjectDescription description = project.getDescription();
            ICommand[] buildCommands = description.getBuildSpec();
            buildCommands = Arrays.copyOf(buildCommands, buildCommands.length + 1);
            ICommand teaVMCommand = description.newCommand();
            teaVMCommand.setBuilderName(TeaVMEclipsePlugin.BUILDER_ID);
            buildCommands[buildCommands.length - 1] = teaVMCommand;
            description.setBuildSpec(buildCommands);
            project.setDescription(description, new NullProgressMonitor());
        }
    }

    @Override
    public void deconfigure() throws CoreException {
        if (hasBuilder()) {
            IProjectDescription description = project.getDescription();
            ICommand[] buildCommands = description.getBuildSpec();
            int index = -1;
            for (int i = 0; i < buildCommands.length; ++i) {
                ICommand command = buildCommands[i];
                if (command.getBuilderName().equals(TeaVMEclipsePlugin.BUILDER_ID)) {
                    index = i;
                    break;
                }
            }
            ICommand[] newBuildCommands = new ICommand[buildCommands.length - 1];
            System.arraycopy(buildCommands, 0, newBuildCommands, 0, index);
            System.arraycopy(buildCommands, index + 1, newBuildCommands, index, newBuildCommands.length - index);
            description.setBuildSpec(newBuildCommands);
            project.setDescription(description, new NullProgressMonitor());
        }
    }

    @Override
    public IProject getProject() {
        return project;
    }

    @Override
    public void setProject(IProject project) {
        this.project = project;
    }

    public TeaVMProjectSettings getSettings() {
        return TeaVMEclipsePlugin.getDefault().getSettings(project);
    }
}
