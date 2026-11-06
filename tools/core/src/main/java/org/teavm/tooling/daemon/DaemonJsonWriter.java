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
package org.teavm.tooling.daemon;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.teavm.common.JsonUtil;
import org.teavm.tooling.TeaVMToolLog;
import org.teavm.tooling.builder.RenderedProblem;
import org.teavm.vm.TeaVMPhase;

class DaemonJsonWriter implements TeaVMToolLog {
    private final PrintWriter writer = new PrintWriter(System.out, false, StandardCharsets.UTF_8);

    @Override
    public synchronized void info(String text) {
        writeMessage("info", text, null);
    }

    @Override
    public synchronized void debug(String text) {
        writeMessage("debug", text, null);
    }

    @Override
    public synchronized void warning(String text) {
        writeMessage("warning", text, null);
    }

    @Override
    public synchronized void error(String text) {
        writeMessage("error", text, null);
    }

    @Override
    public synchronized void info(String text, Throwable e) {
        writeMessage("info", text, e);
    }

    @Override
    public synchronized void debug(String text, Throwable e) {
        writeMessage("debug", text, e);
    }

    @Override
    public synchronized void warning(String text, Throwable e) {
        writeMessage("warning", text, e);
    }

    @Override
    public synchronized void error(String text, Throwable e) {
        writeMessage("error", text, e);
    }

    private void writeMessage(String level, String message, Throwable throwable) {
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

    synchronized void phaseStarted(TeaVMPhase phase, int count) {
        writer.append("{\"type\":\"phase-started\",\"phase\":\"").append(phase.name())
                .append("\",\"count\":").append(String.valueOf(count)).append("}");
        writer.println();
        writer.flush();
    }

    synchronized void progressReached(int progress) {
        writer.append("{\"type\":\"progress\",\"progress\":").append(String.valueOf(progress)).append("}");
        writer.println();
        writer.flush();
    }

    synchronized void complete(List<RenderedProblem> problems) {
        try {
            writer.append("{\"type\":\"complete\",\"problems\":[");
            for (var i = 0; i < problems.size(); ++i) {
                if (i > 0) {
                    writer.append(",");
                }
                var problem = problems.get(i);
                writer.append("{\"severity\":");
                switch (problem.getSeverity()) {
                    case ERROR:
                        writer.append("\"error\"");
                        break;
                    case WARNING:
                        writer.append("\"warning\"");
                        break;
                }
                writer.append(",\"message\":\"");
                JsonUtil.writeEscapedString(writer, problem.getText());
                writer.append("\",\"stackTrace\":\"");
                JsonUtil.writeEscapedString(writer, problem.getStackTrace());
                writer.append("\"}");
            }
            writer.append("]}");
            writer.println();
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized void error(Throwable e) {
        try {
            writer.append("{\"type\":\"error\",\"message\":\"");
            var buffer = new StringWriter();
            e.printStackTrace(new PrintWriter(buffer));
            JsonUtil.writeEscapedString(writer, buffer.toString());
            writer.append("\"}");
            writer.println();
            writer.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
