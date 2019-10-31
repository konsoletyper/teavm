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

import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.java.lang.TInteger;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.TThrowable;
import org.teavm.classlib.java.util.THashMap;
import org.teavm.classlib.java.util.TMap;
import org.teavm.jso.JSBody;

public class TLogger {
    public static final String GLOBAL_LOGGER_NAME = "global";
    private static TMap<String, TLogger> loggerCache = new THashMap<>();
    private String name;
    private TLogger parent;

    TLogger(String name) {
        this.name = name;
    }

    public static TLogger getGlobal() {
        return getLogger(GLOBAL_LOGGER_NAME);
    }

    public static TLogger getLogger(String name) {
        TLogger logger = loggerCache.get(name);
        if (logger == null) {
            logger = new TLogger(name);
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex >= 0) {
                String parentName = name.substring(0, dotIndex);
                logger.parent = getLogger(parentName);
            } else if (!name.isEmpty()) {
                logger.parent = getLogger("");
            }
            loggerCache.put(name, logger);
        }
        return logger;
    }

    public static TLogger getAnonymousLogger() {
        return new TLogger(null);
    }

    public void log(TLogRecord record) {
        String message = format(record.getMessage(), record.getParameters());
        if (PlatformDetector.isLowLevel()) {
            System.out.print("[");
            System.out.print(record.getLevel().getName());
            System.out.print("] ");
            System.out.println(message);
            if (record.getThrown() != null) {
                record.getThrown().printStackTrace(System.out);
            }
        } else {
            if (record.getLevel().intValue() >= TLevel.SEVERE.intValue()) {
                error(message);
            } else if (record.getLevel().intValue() >= TLevel.WARNING.intValue()) {
                warn(message);
            } else {
                infoImpl(message);
            }
        }
    }

    private String format(String message, Object[] params) {
        if (params == null) {
            return message;
        }
        StringBuilder sb = new StringBuilder();
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
                sb.append(message, index, next);
                index = next;
                continue;
            }
            int paramIndex = TInteger.parseInt(message.substring(paramStart, next));
            if (paramIndex >= params.length) {
                sb.append(message, index, next);
                index = next;
                continue;
            }
            sb.append(params[paramIndex]);
            index = next + 1;
        }
        return sb.toString();
    }

    private static int digits(int from, String message) {
        while (from < message.length()) {
            int c = message.charAt(from++);
            if (c <= '0' || c >= '9') {
                return from;
            }
        }
        return -1;
    }

    public void log(TLevel level, String msg, Object[] params) {
        TLogRecord record = new TLogRecord(level, msg);
        record.setParameters(params);
        log(record);
    }

    public void log(TLevel level, String msg) {
        log(new TLogRecord(level, msg));
    }

    public void log(TLevel level, String msg, TObject param1) {
        TLogRecord record = new TLogRecord(level, msg);
        record.setParameters(new Object[] { param1 });
        log(record);
    }

    public void log(TLevel level, String msg, TThrowable thrown) {
        TLogRecord record = new TLogRecord(level, msg);
        record.setThrown(thrown);
        log(record);
    }

    public void severe(String msg) {
        log(TLevel.SEVERE, msg);
    }

    public void warning(String msg) {
        log(TLevel.WARNING, msg);
    }

    public void info(String msg) {
        log(TLevel.INFO, msg);
    }

    public void config(String msg) {
        log(TLevel.CONFIG, msg);
    }

    public void fine(String msg) {
        log(TLevel.FINE, msg);
    }

    public void finer(String msg) {
        log(TLevel.FINER, msg);
    }

    public void finest(String msg) {
        log(TLevel.FINEST, msg);
    }

    public boolean isLoggable(@SuppressWarnings("unused") TLevel level) {
        return true;
    }

    public String getName() {
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
    private static native void infoImpl(String message);

    @JSBody(params = "message", script = ""
            + "if (console) {"
                + "console.warn(message);"
            + "}")
    private static native void warn(String message);

    @JSBody(params = "message", script = ""
            + "if (console) {"
                + "console.error(message);"
            + "}")
    private static native void error(String message);
}
