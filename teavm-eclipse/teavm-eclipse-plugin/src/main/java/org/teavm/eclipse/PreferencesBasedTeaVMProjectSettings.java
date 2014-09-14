package org.teavm.eclipse;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.variables.VariablesPlugin;
import org.osgi.service.prefs.BackingStoreException;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class PreferencesBasedTeaVMProjectSettings implements TeaVMProjectSettings {
    public static final String MAIN_CLASS = "mainClass";
    public static final String TARGET_DIRECTORY = "targetDirectory";
    private IEclipsePreferences preferences;
    private String projectName;

    public PreferencesBasedTeaVMProjectSettings(IProject project) {
        ProjectScope scope = new ProjectScope(project);
        preferences = scope.getNode(TeaVMEclipsePlugin.ID);
        projectName = project.getName();
    }

    @Override
    public String getMainClass() {
        return preferences.get(MAIN_CLASS, "");
    }

    @Override
    public void setMainClass(String mainClass) {
        preferences.put(MAIN_CLASS, mainClass);
    }

    @Override
    public String getTargetDirectory() {
        return preferences.get(TARGET_DIRECTORY,  VariablesPlugin.getDefault().getStringVariableManager()
                .generateVariableExpression("workspace_loc", projectName));
    }

    @Override
    public void setTargetDirectory(String targetDirectory) {
        preferences.put(TARGET_DIRECTORY, targetDirectory);
    }

    @Override
    public void save() throws CoreException {
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            throw new CoreException(TeaVMEclipsePlugin.makeError(e));
        }
    }

    @Override
    public void load() throws CoreException {
        try {
            preferences.sync();
        } catch (BackingStoreException e) {
            throw new CoreException(TeaVMEclipsePlugin.makeError(e));
        }
    }
}
