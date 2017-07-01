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
package org.teavm.eclipse.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.teavm.eclipse.TeaVMEclipsePlugin;
import org.teavm.eclipse.TeaVMProfile;
import org.teavm.eclipse.TeaVMProjectSettings;

public class TeaVMProjectPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {
    private Button natureButton;
    private Table profilesTable;
    private Button addProfileButton;
    private Button removeProfileButton;
    private Button editProfileButton;
    private TeaVMProjectSettings settings;
    private IProject project;

    @Override
    protected Control createContents(Composite parent) {
        project = (IProject)getElement().getAdapter(IProject.class);
        settings = TeaVMEclipsePlugin.getDefault().getSettings(project);

        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 10;
        layout.marginWidth = 10;
        container.setLayout(layout);

        natureButton = new Button(container, SWT.CHECK);
        natureButton.setText("Enable TeaVM");
        natureButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        Control profilesContainer = createProfilesContainer(container);
        profilesContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        try {
            natureButton.setSelection(project.hasNature(TeaVMEclipsePlugin.NATURE_ID));
        } catch (CoreException e) {
            reportStatus(e.getStatus());
        }
        loadProfiles();

        return container;
    }

    private Control createProfilesContainer(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.numColumns = 2;
        layout.verticalSpacing = 3;
        layout.horizontalSpacing = 3;
        container.setLayout(layout);

        Label caption = new Label(container, SWT.NONE);
        caption.setText("Profiles");
        caption.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

        profilesTable = new Table(container, SWT.BORDER | SWT.V_SCROLL | SWT.CHECK);
        profilesTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 4));
        profilesTable.setHeaderVisible(true);
        profilesTable.setLinesVisible(true);
        TableColumn nameColumn = new TableColumn(profilesTable, SWT.LEFT);
        nameColumn.setText("Name");
        nameColumn.setWidth(150);
        TableColumn pathColumn = new TableColumn(profilesTable, SWT.LEFT);
        pathColumn.setText("Target directory");
        pathColumn.setWidth(300);
        TableColumn fileColumn = new TableColumn(profilesTable, SWT.LEFT);
        fileColumn.setText("Target file");
        fileColumn.setWidth(150);
        profilesTable.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                updateTableSelection();
            }
        });

        addProfileButton = new Button(container, SWT.PUSH);
        addProfileButton.setText("Add...");
        addProfileButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        addProfileButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                addProfile();
            }
        });

        editProfileButton = new Button(container, SWT.PUSH);
        editProfileButton.setText("Edit...");
        editProfileButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        editProfileButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                editProfile();
            }
        });

        removeProfileButton = new Button(container, SWT.PUSH);
        removeProfileButton.setText("Remove");
        removeProfileButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        removeProfileButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                deleteProfile();
            }
        });

        return container;
    }

    private void updateTableSelection() {
        if (profilesTable.getSelectionCount() != 1) {
            removeProfileButton.setEnabled(false);
            return;
        }
        TableItem item = profilesTable.getSelection()[0];
        TeaVMProfile profile = (TeaVMProfile)item.getData();
        removeProfileButton.setEnabled(profile.getExternalToolId().isEmpty());
    }

    private void loadProfiles() {
        try {
            settings.load();
        } catch (CoreException e) {
            reportStatus(e.getStatus());
        }
        for (TeaVMProfile profile : settings.getProfiles()) {
            createItemForProfile(profile);
        }
    }

    private TableItem createItemForProfile(TeaVMProfile profile) {
        TableItem item = new TableItem(profilesTable, SWT.NONE);
        item.setData(profile);
        updateItem(item);
        return item;
    }

    private void updateItem(TableItem item) {
        TeaVMProfile profile = (TeaVMProfile)item.getData();
        item.setText(0, profile.getName());
        item.setText(1, profile.getTargetDirectory());
        item.setText(2, profile.getTargetFileName());
        item.setChecked(profile.isEnabled());
    }

    private void storeItem(TableItem item) {
        TeaVMProfile profile = (TeaVMProfile)item.getData();
        profile.setEnabled(item.getChecked());
    }

    private void addProfile() {
        try {
            TeaVMProfile profile = settings.createProfile();
            TableItem item = createItemForProfile(profile);
            storeItem(item);
            TeaVMProfileDialog dialog = new TeaVMProfileDialog(getShell(), settings, profile);
            dialog.setProject(project);
            dialog.open();
            updateItem(item);
        } catch (CoreException e) {
            reportStatus(e.getStatus());
        }
    }

    private void editProfile() {
        if (profilesTable.getSelectionCount() != 1) {
            return;
        }
        try {
            TableItem item = profilesTable.getSelection()[0];
            TeaVMProfile profile = (TeaVMProfile)item.getData();
            storeItem(item);
            TeaVMProfileDialog dialog = new TeaVMProfileDialog(getShell(), settings, profile);
            dialog.setProject(project);
            dialog.open();
            updateItem(item);
        } catch (CoreException e) {
            reportStatus(e.getStatus());
        }
    }

    private void deleteProfile() {
        if (profilesTable.getSelectionCount() != 1) {
            return;
        }
        TableItem item = profilesTable.getSelection()[0];
        TeaVMProfile profile = (TeaVMProfile)item.getData();
        if (!profile.getExternalToolId().isEmpty()) {
            return;
        }
        boolean confirmed = MessageDialog.openConfirm(getShell(), "Deletion confirmation",
                "Are you sure to delete profile " + item.getText(0) + "?");
        if (!confirmed) {
            return;
        }
        settings.deleteProfile((TeaVMProfile)item.getData());
        item.dispose();
    }

    @Override
    public boolean performOk() {
        try {
            updateNature();
            for (int i = 0; i < profilesTable.getItemCount(); ++i) {
                TableItem item = profilesTable.getItem(i);
                storeItem(item);
            }
            settings.save();
        } catch (CoreException e) {
            reportStatus(e.getStatus());
        }
        return super.performOk();
    }

    private void updateNature() throws CoreException {
        if (natureButton.getSelection()) {
            if (!project.hasNature(TeaVMEclipsePlugin.NATURE_ID)) {
                addNature(project);
            }
        } else {
            if (project.hasNature(TeaVMEclipsePlugin.NATURE_ID)) {
                removeNature(project);
            }
        }
    }

    private void addNature(final IProject project) {
        reportStatus(TeaVMEclipsePlugin.getDefault().addNature(PlatformUI.getWorkbench().getProgressService(),
                project));
    }

    private void removeNature(final IProject project) {
        reportStatus(TeaVMEclipsePlugin.getDefault().removeNature(PlatformUI.getWorkbench().getProgressService(),
                project));
    }

    private void reportStatus(IStatus status) {
        if (!status.isOK()) {
            TeaVMEclipsePlugin.getDefault().getLog().log(status);
        }
        if (status.getSeverity() == IStatus.ERROR) {
            ErrorDialog.openError(getShell(), "Error occured", "Error occured", status);
        }
    }
}
