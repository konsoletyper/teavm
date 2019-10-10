/*
 *  Copyright 2019 Alexey Andreev.
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

class LayerSourceLocationIterator {
    private DebugInformation debugInformation;
    private int lineIndex;
    private int fileIndex;
    private GeneratedLocation location;
    private int fileId = -1;
    private int line = -1;
    private boolean endReached;
    private DebugInformation.Layer layer;

    LayerSourceLocationIterator(DebugInformation debugInformation, DebugInformation.Layer layer) {
        this.debugInformation = debugInformation;
        this.layer = layer;
        read();
    }

    public boolean isEndReached() {
        return endReached;
    }

    private void read() {
        if (fileIndex < layer.fileMapping.size() && lineIndex < layer.lineMapping.size()) {
            RecordArray.Record fileRecord = layer.fileMapping.get(fileIndex);
            RecordArray.Record lineRecord = layer.lineMapping.get(lineIndex);
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
        } else if (fileIndex < layer.fileMapping.size()) {
            nextFileRecord();
        } else if (lineIndex < layer.lineMapping.size()) {
            nextLineRecord();
        } else if (endReached) {
            throw new IllegalStateException("End already reached");
        } else {
            endReached = true;
        }
    }

    private void nextFileRecord() {
        RecordArray.Record record = layer.fileMapping.get(fileIndex++);
        location = DebugInformation.key(record);
        fileId = record.get(2);
    }

    private void nextLineRecord() {
        RecordArray.Record record = layer.lineMapping.get(lineIndex++);
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
