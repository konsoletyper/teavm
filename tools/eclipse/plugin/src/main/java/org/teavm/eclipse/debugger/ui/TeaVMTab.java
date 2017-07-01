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
package org.teavm.eclipse.debugger.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class TeaVMTab extends AbstractLaunchConfigurationTab {
    private Text portField;

    @Override
    public void createControl(Composite container) {
        Composite root = new Composite(container, SWT.NONE);
        setControl(root);
        GridLayout layout = new GridLayout();
        layout.verticalSpacing = 6;
        layout.numColumns = 2;
        layout.horizontalSpacing = 6;
        root.setLayout(layout);

        Label portLabel = new Label(root, SWT.NONE);
        portLabel.setText("&Port");

        portField = new Text(root, SWT.SINGLE | SWT.BORDER);
        portField.addModifyListener(new ModifyListener() {
            @Override public void modifyText(ModifyEvent event) {
                updateLaunchConfigurationDialog();
            }
        });
    }

    @Override
    public String getName() {
        return "TeaVM";
    }

    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        try {
            int attr = configuration.getAttribute("teavm-debugger-port", 2357);
            portField.setText(String.valueOf(attr));
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute("teavm-debugger-port", Integer.parseInt(portField.getText()));
    }

    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute("teavm-debugger-port", 2357);
    }
}
