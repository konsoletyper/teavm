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

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.*;

public class TLogRecord extends TObject implements TSerializable {
    private static long sequenceNumberGenerator;
    private TLevel level;
    private TString loggerName;
    private TString message;
    private long millis;
    private Object[] parameters;
    private long sequenceNumber;
    private TString sourceClassName;
    private TString sourceMethodName;
    private long threadID;
    private TThrowable thrown;

    public TLogRecord(TLevel level, TString msg) {
        this.level = level;
        this.message = msg;
        this.millis = TSystem.currentTimeMillis();
        this.sequenceNumber = sequenceNumberGenerator++;
        this.threadID = TThread.currentThread().getId();
    }

    public TLevel getLevel() {
        return level;
    }

    public void setLevel(TLevel level) {
        this.level = level;
    }

    public TString getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(TString loggerName) {
        this.loggerName = loggerName;
    }

    public TString getMessage() {
        return message;
    }

    public void setMessage(TString message) {
        this.message = message;
    }

    public long getMillis() {
        return millis;
    }

    public void setMillis(long millis) {
        this.millis = millis;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public TString getSourceClassName() {
        return sourceClassName;
    }

    public void setSourceClassName(TString sourceClassName) {
        this.sourceClassName = sourceClassName;
    }

    public TString getSourceMethodName() {
        return sourceMethodName;
    }

    public void setSourceMethodName(TString sourceMethodName) {
        this.sourceMethodName = sourceMethodName;
    }

    public long getThreadID() {
        return threadID;
    }

    public void setThreadID(long threadID) {
        this.threadID = threadID;
    }

    public TThrowable getThrown() {
        return thrown;
    }

    public void setThrown(TThrowable thrown) {
        this.thrown = thrown;
    }
}
