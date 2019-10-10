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
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public class ExactMethodIterator {
    private DebugInformation debugInformation;
    private DebugInformation.Layer layer;
    private GeneratedLocation location;
    private int classIndex;
    private int methodIndex;
    private int classId = -1;
    private int methodId = -1;

    ExactMethodIterator(DebugInformation debugInformation, DebugInformation.Layer layer) {
        this.debugInformation = debugInformation;
        this.layer = layer;
        if (!isEndReached()) {
            read();
        }
    }

    public boolean isEndReached() {
        return methodIndex >= layer.methodMapping.size() && classIndex >= layer.classMapping.size();
    }

    private void read() {
        if (classIndex < layer.classMapping.size()
                && methodIndex < layer.methodMapping.size()) {
            RecordArray.Record classRecord = layer.classMapping.get(classIndex);
            RecordArray.Record methodRecord = layer.methodMapping.get(methodIndex);
            GeneratedLocation classLoc = DebugInformation.key(classRecord);
            GeneratedLocation methodLoc = DebugInformation.key(methodRecord);
            int cmp = classLoc.compareTo(methodLoc);
            if (cmp < 0) {
                nextClassRecord();
            } else if (cmp > 0) {
                nextMethodRecord();
            } else {
                nextClassRecord();
                nextMethodRecord();
            }
        } else if (classIndex < layer.classMapping.size()) {
            nextClassRecord();
        } else if (methodIndex < layer.methodMapping.size()) {
            nextMethodRecord();
        } else {
            throw new IllegalStateException("End already reached");
        }
    }

    private void nextClassRecord() {
        RecordArray.Record record = layer.classMapping.get(classIndex++);
        classId = record.get(2);
        location = DebugInformation.key(record);
    }

    private void nextMethodRecord() {
        RecordArray.Record record = layer.methodMapping.get(methodIndex++);
        methodId = record.get(2);
        location = DebugInformation.key(record);
    }

    public void next() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        read();
    }

    public int getClassNameId() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        return classId;
    }

    public String getClassName() {
        int classId = getClassNameId();
        return classId >= 0 ? debugInformation.getClassName(classId) : null;
    }

    public int getMethodId() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        return methodId;
    }

    public MethodDescriptor getMethod() {
        int methodId = getMethodId();
        return methodId >= 0 ? debugInformation.getMethod(methodId) : null;
    }

    public int getExactMethodId() {
        if (classId < 0 || methodId < 0) {
            return -1;
        }
        return debugInformation.getExactMethodId(classId, methodId);
    }

    public MethodReference getExactMethod() {
        int methodId = getExactMethodId();
        return methodId >= 0 ? debugInformation.getExactMethod(getExactMethodId()) : null;
    }

    public GeneratedLocation getLocation() {
        return location;
    }
}
