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
package org.teavm.classlib.java.util.logging;

import org.teavm.classlib.java.lang.TInteger;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.TString;
import org.teavm.classlib.java.lang.TStringBuilder;
import org.teavm.classlib.java.lang.TThrowable;
import org.teavm.classlib.java.util.THashMap;
import org.teavm.classlib.java.util.TMap;
import org.teavm.jso.JSBody;

public class TLogger {
    public static final TString GLOBAL_LOGGER_NAME = TString.wrap("global");
    private static TMap<TString, TLogger> loggerCache = new THashMap<>();
    private TString name;
    private TLogger parent;

    TLogger(TString name) {
        this.name = name;
    }

    public static TLogger getLogger(TString name) {
        TLogger logger = loggerCache.get(name);
        if (logger == null) {
            logger = new TLogger(name);
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex >= 0) {
                TString parentName = name.substring(0, dotIndex);
                logger.parent = getLogger(parentName);
            } else if (!name.isEmpty()) {
                logger.parent = getLogger(TString.wrap(""));
            }
            loggerCache.put(name, logger);
        }
        return logger;
    }

    public static TLogger getAnonymousLogger() {
        return new TLogger(null);
    }

    public void log(TLogRecord record) {
        TString message = format(record.getMessage(), record.getParameters());
        if (record.getLevel().intValue() >= TLevel.SEVERE.intValue()) {
            error(message);
        } else if (record.getLevel().intValue() >= TLevel.WARNING.intValue()) {
            warn(message);
        } else {
            info(message);
        }
    }

    private TString format(TString message, Object[] params) {
        if (params == null) {
            return message;
        }
        TStringBuilder sb = new TStringBuilder();
        int index = 0;
        while (index < message.length()) {
            int next = message.indexOf('{', index);
            if (next < 0) {
                break;
            }
            int paramStart = next + 1;
            next = digits(paramStart, message);
            if (next < 0) {
                break;
            }
            if (message.charAt(next) != '}') {
                sb.append(message.substring(index, next));
                index = next;
                continue;
            }
            int paramIndex = TInteger.parseInt(message.substring(paramStart, next));
            if (paramIndex >= params.length) {
                sb.append(message.substring(index, next));
                index = next;
                continue;
            }
            sb.append(TObject.wrap(params[paramIndex]));
            index = next + 1;
        }
        return TString.wrap(sb.toString());
    }

    private static int digits(int from, TString message) {
        while (from < message.length()) {
            int c = message.charAt(from++);
            if (c <= '0' || c >= '9') {
                return from;
            }
        }
        return -1;
    }

    public void log(TLevel level, TString msg, Object[] params) {
        TLogRecord record = new TLogRecord(level, msg);
        record.setParameters(params);
        log(record);
    }

    public void log(TLevel level, TString msg) {
        log(new TLogRecord(level, msg));
    }

    public void log(TLevel level, TString msg, TObject param1) {
        TLogRecord record = new TLogRecord(level, msg);
        record.setParameters(new Object[] { param1 });
        log(record);
    }

    public void log(TLevel level, TString msg, TThrowable thrown) {
        TLogRecord record = new TLogRecord(level, msg);
        record.setThrown(thrown);
        log(record);
    }

    public void severe(TString msg) {
        log(TLevel.SEVERE, msg);
    }

    public void warning(TString msg) {
        log(TLevel.WARNING, msg);
    }

    public void config(TString msg) {
        log(TLevel.CONFIG, msg);
    }

    public void fine(TString msg) {
        log(TLevel.FINE, msg);
    }

    public void finer(TString msg) {
        log(TLevel.FINER, msg);
    }

    public void finest(TString msg) {
        log(TLevel.FINEST, msg);
    }

    public boolean isLoggable(@SuppressWarnings("unused") TLevel level) {
        return true;
    }

    public TString getName() {
        return name;
    }

    public TLogger getParent() {
        return parent;
    }

    public void setParent(TLogger parent) {
        this.parent = parent;
    }

    @JSBody(params = "message", script = ""
            + "if (console) {"
                + "console.info(message);"
            + "}")
    public static native void info(TString message);

    @JSBody(params = "message", script = ""
            + "if (console) {"
                + "console.warn(message);"
            + "}")
    private static native void warn(TString message);

    @JSBody(params = "message", script = ""
            + "if (console) {"
                + "console.error(message);"
            + "}")
    private static native void error(TString message);
}
