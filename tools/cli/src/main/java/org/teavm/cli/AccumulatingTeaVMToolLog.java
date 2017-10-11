/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.cli;

import java.util.ArrayList;
import java.util.List;
import org.teavm.tooling.TeaVMToolLog;

public class AccumulatingTeaVMToolLog implements TeaVMToolLog {
    private TeaVMToolLog delegatedLog;
    private List<Message> pendingMessages = new ArrayList<>();

    public AccumulatingTeaVMToolLog(TeaVMToolLog delegatedLog) {
        this.delegatedLog = delegatedLog;
    }

    public void flush() {
        for (Message message : pendingMessages) {
            switch (message.kind) {
                case INFO:
                    if (message.throwable != null) {
                        delegatedLog.info(message.text, message.throwable);
                    } else {
                        delegatedLog.info(message.text);
                    }
                    break;
                case DEBUG:
                    if (message.throwable != null) {
                        delegatedLog.debug(message.text, message.throwable);
                    } else {
                        delegatedLog.debug(message.text);
                    }
                    break;
                case WARNING:
                    if (message.throwable != null) {
                        delegatedLog.warning(message.text, message.throwable);
                    } else {
                        delegatedLog.warning(message.text);
                    }
                    break;
                case ERROR:
                    if (message.throwable != null) {
                        delegatedLog.error(message.text, message.throwable);
                    } else {
                        delegatedLog.error(message.text);
                    }
                    break;
            }
        }
        pendingMessages.clear();
    }

    @Override
    public void info(String text) {
        pendingMessages.add(new Message(MessageKind.INFO, text, null));
    }

    @Override
    public void debug(String text) {
        pendingMessages.add(new Message(MessageKind.DEBUG, text, null));
    }

    @Override
    public void warning(String text) {
        pendingMessages.add(new Message(MessageKind.WARNING, text, null));
    }

    @Override
    public void error(String text) {
        pendingMessages.add(new Message(MessageKind.ERROR, text, null));
    }

    @Override
    public void info(String text, Throwable e) {
        pendingMessages.add(new Message(MessageKind.INFO, text, e));
    }

    @Override
    public void debug(String text, Throwable e) {
        pendingMessages.add(new Message(MessageKind.DEBUG, text, e));
    }

    @Override
    public void warning(String text, Throwable e) {
        pendingMessages.add(new Message(MessageKind.WARNING, text, e));
    }

    @Override
    public void error(String text, Throwable e) {
        pendingMessages.add(new Message(MessageKind.ERROR, text, e));
    }

    static class Message {
        MessageKind kind;
        String text;
        Throwable throwable;

        public Message(MessageKind kind, String text, Throwable throwable) {
            this.kind = kind;
            this.text = text;
            this.throwable = throwable;
        }
    }

    enum MessageKind {
        INFO,
        DEBUG,
        WARNING,
        ERROR
    }
}
