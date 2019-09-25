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
package org.teavm.debugging.information;

import org.teavm.common.RecordArray;

public class SourceLocationIterator {
    private DebugInformation debugInformation;
    private int lineIndex;
    private int fileIndex;
    private GeneratedLocation location;
    private int fileId = -1;
    private int line = -1;
    private boolean endReached;

    SourceLocationIterator(DebugInformation debugInformation) {
        this.debugInformation = debugInformation;
        if (!isEndReached()) {
            read();
        }
    }

    public boolean isEndReached() {
        return endReached;
    }

    private void read() {
        if (fileIndex < debugInformation.fileMapping.size() && lineIndex < debugInformation.lineMapping.size()) {
            RecordArray.Record fileRecord = debugInformation.fileMapping.get(fileIndex);
            RecordArray.Record lineRecord = debugInformation.lineMapping.get(lineIndex);
            GeneratedLocation fileLoc = DebugInformation.key(fileRecord);
            GeneratedLocation lineLoc = DebugInformation.key(lineRecord);
            int cmp = fileLoc.compareTo(lineLoc);
            if (cmp < 0) {
                nextFileRecord();
            } else if (cmp > 0) {
                nextLineRecord();
            } else {
                nextFileRecord();
                nextLineRecord();
            }
        } else if (fileIndex < debugInformation.fileMapping.size()) {
            nextFileRecord();
        } else if (lineIndex < debugInformation.lineMapping.size()) {
            nextLineRecord();
        } else if (endReached) {
            throw new IllegalStateException("End already reached");
        } else {
            endReached = true;
        }
    }

    private void nextFileRecord() {
        RecordArray.Record record = debugInformation.fileMapping.get(fileIndex++);
        location = DebugInformation.key(record);
        fileId = record.get(2);
    }

    private void nextLineRecord() {
        RecordArray.Record record = debugInformation.lineMapping.get(lineIndex++);
        location = DebugInformation.key(record);
        line = record.get(2);
    }

    public void next() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        read();
    }

    public GeneratedLocation getLocation() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        return location;
    }

    public int getFileNameId() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        return fileId;
    }

    public String getFileName() {
        int fileId = getFileNameId();
        return fileId >= 0 ? debugInformation.getFileName(fileId) : null;
    }

    public int getLine() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        return line;
    }
}
