/*
 *  Copyright 2022 Alexey Andreev.
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
import java.io.OutputStream;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.teavm.devserver.client.DevServerClient;
import org.teavm.devserver.client.DevServerListener;
import org.teavm.idea.devserver.ui.TeaVMDevServerConsole;

class TeaVMProcessHandler extends ProcessHandler implements DevServerListener {
    int debugPort;
    private TeaVMDownloader downloader;
    private TeaVMDevServerConsole console;
    private DevServerClient client;

    TeaVMProcessHandler(int debugPort, TeaVMDevServerConsole console, TeaVMDownloader downloader,
            DevServerClient client) {
        this.debugPort = debugPort;
        this.downloader = downloader;
        this.console = console;
        this.client = client;
        client.addListener(this);
    }

    @Override
    public void notifyProcessTerminated(int exitCode) {
        super.notifyProcessTerminated(exitCode);
    }

    @Override
    protected void destroyProcessImpl() {
        downloader.close();
        client.stop();
        notifyProcessTerminated(0);
    }

    @Override
    protected void detachProcessImpl() {
        downloader.close();
        client.stop();
        notifyProcessTerminated(0);
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
    public void onLog(DevServerClient.LogLevel level, String message) {
        var contentType = switch (level) {
            case DEBUG -> ConsoleViewContentType.LOG_DEBUG_OUTPUT;
            case INFO -> ConsoleViewContentType.LOG_INFO_OUTPUT;
            case WARNING -> ConsoleViewContentType.LOG_WARNING_OUTPUT;
            case ERROR -> ConsoleViewContentType.LOG_ERROR_OUTPUT;
        };
        console.getUnderlyingConsole().print(message + System.lineSeparator(), contentType);
    }

    @Override
    public void onComplete(List<DevServerClient.Problem> problems) {
        for (var problem : problems) {
            switch (problem.getSeverity()) {
                case ERROR -> {
                    console.getUnderlyingConsole().print("ERROR:" + problem.getMessage() + System.lineSeparator(),
                            ConsoleViewContentType.ERROR_OUTPUT);
                }
                case WARNING -> {
                    console.getUnderlyingConsole().print("WARNING: " + problem.getMessage() + System.lineSeparator(),
                            ConsoleViewContentType.LOG_WARNING_OUTPUT);
                }
            }
        }
    }

    @Override
    public void onStderr(String line) {
        console.getUnderlyingConsole().print(line + System.lineSeparator(), ConsoleViewContentType.ERROR_OUTPUT);
    }

    @Override
    public void onUnexpectedStop() {
        console.stop();
        notifyProcessTerminated(1);
    }
}
