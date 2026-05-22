/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.cli.devserver;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;

class JsonLogger extends LegacyAbstractLogger {
    JsonLogger(String name) {
        this.name = name;
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return null;
    }

    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String msg, Object[] args,
            Throwable throwable) {
        var target = JsonLoggerFactory.getWriter();
        if (target == null) {
            return;
        }
        String formatted = MessageFormatter.basicArrayFormat(msg, args);
        switch (level) {
            case TRACE:
            case DEBUG:
                if (throwable != null) {
                    target.debug(formatted, throwable);
                } else {
                    target.debug(formatted);
                }
                break;
            case INFO:
                if (throwable != null) {
                    target.info(formatted, throwable);
                } else {
                    target.info(formatted);
                }
                break;
            case WARN:
                if (throwable != null) {
                    target.warning(formatted, throwable);
                } else {
                    target.warning(formatted);
                }
                break;
            case ERROR:
                if (throwable != null) {
                    target.error(formatted, throwable);
                } else {
                    target.error(formatted);
                }
                break;
            default:
                break;
        }
    }
}
