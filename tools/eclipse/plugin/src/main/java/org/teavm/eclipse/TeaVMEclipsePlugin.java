/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.eclipse;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.service.prefs.BackingStoreException;

public class TeaVMEclipsePlugin extends AbstractUIPlugin {
    public static final String ID = "teavm-eclipse-plugin";
    public static final String NATURE_ID = ID + ".nature";
    public static final String BUILDER_ID = ID + ".builder";
    public static final String CLASS_DIALOG_ID = ID + ".dialogs.classSelection";
    public static final String PROBLEM_MARKER_ID = ID + ".problemMarker";
    public static final String PROBLEM_MARKER_PROJECT_ATTRIBUTE = ID + ".problemMarker.project";
    public static final String PROBLEM_MARKER_PROFILE_ATTRIBUTE = ID + ".problemMarker.profile";
    public static final String CONFIG_MARKER_ID = ID + ".configMarker";
    private static TeaVMEclipsePlugin defaultInstance;
    private ConcurrentMap<IProject, TeaVMProjectSettings> settingsMap = new ConcurrentHashMap<>();

    public TeaVMEclipsePlugin() {
        defaultInstance = this;
    }

    public static TeaVMEclipsePlugin getDefault() {
        return defaultInstance;
    }

    public static IStatus makeError(Throwable e) {
        return new Status(IStatus.ERROR, ID, "Error occured", e);
    }

    public static void logError(Throwable e) {
        getDefault().getLog().log(makeError(e));
    }

    public TeaVMProjectSettings getSettings(IProject project) {
        TeaVMProjectSettings settings = settingsMap.get(project);
        if (settings == null) {
            settings = new PreferencesBasedTeaVMProjectSettings(project);
            settingsMap.putIfAbsent(project, settings);
            settings = settingsMap.get(project);
        }
        return settings;
    }

    public IStatus addNature(IRunnableContext runnableContext, final IProject project) {
        try {
            runnableContext.run(false, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        addNature(monitor, project);
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
            return Status.OK_STATUS;
        } catch (InterruptedException e) {
            return makeError(e);
        } catch (InvocationTargetException e) {
            return Status.CANCEL_STATUS;
        }
    }

    public void addNature(IProgressMonitor progressMonitor, IProject project) throws CoreException {
        ProjectScope scope = new ProjectScope(project);
        try {
            IEclipsePreferences prefs = scope.getNode(TeaVMEclipsePlugin.ID);
            prefs.flush();
            settingsMap.put(project, new PreferencesBasedTeaVMProjectSettings(project, prefs));
        } catch (BackingStoreException e) {
            throw new RuntimeException("Error creating preferences", e);
        }
        IProjectDescription projectDescription = project.getDescription();
        String[] natureIds = projectDescription.getNatureIds();
        natureIds = Arrays.copyOf(natureIds, natureIds.length + 1);
        natureIds[natureIds.length - 1] = TeaVMEclipsePlugin.NATURE_ID;
        projectDescription.setNatureIds(natureIds);
        project.setDescription(projectDescription, progressMonitor);
    }

    public IStatus removeNature(IRunnableContext runnableContext, final IProject project) {
        try {
            runnableContext.run(false, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        removeNature(monitor, project);
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
            return Status.OK_STATUS;
        } catch (InterruptedException e) {
            return makeError(e);
        } catch (InvocationTargetException e) {
            return Status.CANCEL_STATUS;
        }
    }

    public void removeNature(IProgressMonitor progressMonitor, IProject project) throws CoreException {
        IProjectDescription projectDescription = project.getDescription();
        String[] natureIds = projectDescription.getNatureIds();
        String[] newNatureIds = new String[natureIds.length - 1];
        for (int i = 0; i < natureIds.length; ++i) {
            if (natureIds[i].equals(TeaVMEclipsePlugin.NATURE_ID)) {
                System.arraycopy(natureIds, 0, newNatureIds, 0, i);
                System.arraycopy(natureIds, i + 1, newNatureIds, i, newNatureIds.length - i);
                projectDescription.setNatureIds(newNatureIds);
                project.setDescription(projectDescription, progressMonitor);
                break;
            }
        }
    }
}
