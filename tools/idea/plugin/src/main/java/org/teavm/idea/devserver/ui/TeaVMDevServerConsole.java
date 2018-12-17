/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.idea.devserver.ui;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ExecutionConsole;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.NumberFormat;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import org.teavm.idea.devserver.DevServerBuildResult;
import org.teavm.idea.devserver.DevServerManager;
import org.teavm.idea.devserver.DevServerManagerListener;

public class TeaVMDevServerConsole extends JPanel implements ExecutionConsole {
    private ConsoleView underlyingConsole;
    private DevServerManager serverManager;
    private ServerListenerImpl serverListener;
    private JButton rebuildButton = new JButton("Clean and rebuild");
    private JProgressBar progressBar = new JProgressBar(0, 1000);
    private volatile boolean rebuildPending;
    private volatile boolean building;

    public TeaVMDevServerConsole(ConsoleView underlyingConsole) {
        this.underlyingConsole = underlyingConsole;

        setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets.bottom = 5;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        add(underlyingConsole.getComponent(), constraints);

        constraints = new GridBagConstraints();
        constraints.insets.right = 5;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        add(rebuildButton, constraints);

        constraints.insets.right = 0;
        constraints.fill = GridBagConstraints.BOTH;
        add(progressBar, constraints);

        progressBar.setVisible(false);
        progressBar.setStringPainted(true);

        rebuildButton.addActionListener(e -> rebuildProject());
    }

    public ConsoleView getUnderlyingConsole() {
        return underlyingConsole;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public JComponent getPreferredFocusableComponent() {
        return underlyingConsole.getPreferredFocusableComponent();
    }

    @Override
    public void dispose() {
        if (serverListener != null) {
            try {
                UnicastRemoteObject.unexportObject(serverListener, true);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        underlyingConsole.dispose();
    }

    public void setServerManager(DevServerManager serverManager) {
        this.serverManager = serverManager;
        try {
            serverListener = new ServerListenerImpl();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        try {
            serverManager.addListener(serverListener);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private void rebuildProject() {
        if (!building) {
            invalidateAndBuild();
        } else if (!rebuildPending) {
            rebuildPending = true;
            try {
                serverManager.cancelBuild();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void invalidateAndBuild() {
        try {
            serverManager.invalidateCache();
            serverManager.buildProject();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        progressBar.setVisible(false);
        rebuildButton.setEnabled(false);
    }

    class ServerListenerImpl extends UnicastRemoteObject implements DevServerManagerListener {
        private NumberFormat percentFormat = NumberFormat.getPercentInstance();

        ServerListenerImpl() throws RemoteException {
        }

        @Override
        public void compilationStarted() {
            building = true;
            progressBar.setValue(0);
            progressBar.setVisible(true);
        }

        @Override
        public void compilationProgress(double progress) {
            building = true;
            progressBar.setValue((int) (progress * 10));
            progressBar.setString(percentFormat.format(progress / 100));
            progressBar.setVisible(true);
        }

        @Override
        public void compilationComplete(DevServerBuildResult result) {
            progressBar.setVisible(false);
            building = false;
        }

        @Override
        public void compilationCancelled() {
            progressBar.setVisible(false);
            building = false;
            if (rebuildPending) {
                invalidateAndBuild();
            }
        }
    }
}
