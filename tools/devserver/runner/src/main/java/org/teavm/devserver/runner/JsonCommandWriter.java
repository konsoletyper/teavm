/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.devserver.runner;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.teavm.common.JsonUtil;
import org.teavm.devserver.DevServerListener;
import org.teavm.diagnostics.DefaultProblemTextConsumer;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.TeaVMToolLog;
import org.teavm.tooling.builder.BuildResult;

public class JsonCommandWriter implements TeaVMToolLog, DevServerListener {
    private PrintWriter writer = new PrintWriter(System.out, false, StandardCharsets.UTF_8);

    @Override
    public void info(String text) {
        writeMessage("info", text, null);
    }

    @Override
    public void debug(String text) {
        writeMessage("debug", text, null);
    }

    @Override
    public void warning(String text) {
        writeMessage("warning", text, null);
    }

    @Override
    public void error(String text) {
        writeMessage("error", text, null);
    }

    @Override
    public void info(String text, Throwable e) {
        writeMessage("info", text, e);
    }

    @Override
    public void debug(String text, Throwable e) {
        writeMessage("debug", text, e);
    }

    @Override
    public void warning(String text, Throwable e) {
        writeMessage("warning", text, e);
    }

    @Override
    public void error(String text, Throwable e) {
        writeMessage("error", text, e);
    }

    private synchronized void writeMessage(String level, String message, Throwable throwable) {
        try {
            writer.append("{\"type\":\"log\",\"level\":\"").append(level).append("\",\"message\":\"");
            JsonUtil.writeEscapedString(writer, message);
            writer.append("\"");
            if (throwable != null) {
                writer.append(",\"throwable\":\"");
                var throwableBuffer = new StringWriter();
                var throwableWriter = new PrintWriter(throwableBuffer);
                throwable.printStackTrace(throwableWriter);
                JsonUtil.writeEscapedString(writer, throwableBuffer.toString());
                writer.append("\"");
            }
            writer.append("}");
            writer.println();
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void compilationStarted() {
        writer.append("{\"type\":\"compilation-started\"}");
        writer.println();
        writer.flush();
    }

    @Override
    public synchronized void compilationProgress(double progress) {
        writer.append("{\"type\":\"compilation-progress\",\"progress\":").append(String.valueOf(progress))
                .append("}");
        writer.println();
        writer.flush();
    }

    @Override
    public synchronized void compilationComplete(BuildResult result) {
        var consumer = new DefaultProblemTextConsumer();
        try {
            writer.append("{\"type\":\"compilation-complete\"");
            if (result != null && !result.getProblems().getProblems().isEmpty()) {
                writer.append(",\"problems\":[");
                for (var i = 0; i < result.getProblems().getProblems().size(); ++i) {
                    if (i > 0) {
                        writer.append(",");
                    }
                    var problem = result.getProblems().getProblems().get(i);
                    writer.append("{\"severity\":");
                    switch (problem.getSeverity()) {
                        case ERROR:
                            writer.append("\"error\"");
                            break;
                        case WARNING:
                            writer.append("\"warning\"");
                            break;
                    }
                    writer.append(",\"location\":\"");
                    var sb = new StringBuilder();
                    TeaVMProblemRenderer.renderCallStack(result.getCallGraph(), problem.getLocation(), sb);
                    JsonUtil.writeEscapedString(writer, sb.toString());
                    writer.append("\",\"message\":\"");
                    problem.render(consumer);
                    JsonUtil.writeEscapedString(writer, consumer.getText());
                    writer.append("\"}");
                }
                writer.append("]");
            }
            writer.append("}");
            writer.println();
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void compilationCancelled() {
        writer.append("{\"type\":\"compilation-cancelled\"}");
        writer.println();
        writer.flush();
    }
}
