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
package org.teavm.idea.devserver;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleViewContentType;
import java.io.IOException;
import java.io.OutputStream;
import org.jetbrains.annotations.Nullable;
import org.teavm.idea.DaemonUtil;
import org.teavm.idea.DevServerRunnerListener;
import org.teavm.idea.devserver.ui.TeaVMDevServerConsole;

class TeaVMProcessHandler extends ProcessHandler implements DevServerRunnerListener {
    DevServerConfiguration config;
    private TeaVMDevServerConsole console;
    private DevServerInfo info;

    TeaVMProcessHandler(DevServerConfiguration config, TeaVMDevServerConsole console) {
        this.config = config;
        this.console = console;
    }

    void start() throws IOException {
        info = DevServerRunner.start(DaemonUtil.detectClassPath().toArray(new String[0]), config, this);
        console.setServerManager(info.server);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> info.process.destroy()));
    }

    @Override
    protected void destroyProcessImpl() {
        info.process.destroy();
    }

    @Override
    protected void detachProcessImpl() {
        try {
            info.server.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean detachIsDefault() {
        return true;
    }

    @Nullable
    @Override
    public OutputStream getProcessInput() {
        return null;
    }


    @Override
    public void error(String text) {
        console.getUnderlyingConsole().print(text + System.lineSeparator(), ConsoleViewContentType.ERROR_OUTPUT);
    }

    @Override
    public void info(String text) {
        console.getUnderlyingConsole().print(text + System.lineSeparator(), ConsoleViewContentType.NORMAL_OUTPUT);
    }

    @Override
    public void stopped(int code) {
        console.stop();
        notifyProcessTerminated(code);
    }
}
