/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.wasm.debug;

import org.teavm.backend.wasm.debug.info.FieldType;
import org.teavm.model.PrimitiveType;

public class DebugClassLayoutBuilder extends DebugSectionBuilder implements DebugClassLayout {
    private DebugClasses classes;
    private DebugStrings strings;
    private int currentIndex;
    private int currentAddress;
    private ClassPhase phase = ClassPhase.NO_CLASS;
    private int lastFieldOffset;

    public DebugClassLayoutBuilder(DebugClasses classes, DebugStrings strings) {
        super(DebugConstants.SECTION_CLASS_LAYOUT);
        this.classes = classes;
        this.strings = strings;
    }

    @Override
    public void startClass(String name, int parent, int globalIndex, int size) {
        blob.writeByte(parent >= 0 ? DebugConstants.CLASS_CLASS : DebugConstants.CLASS_ROOT);
        blob.writeLEB(classes.classPtr(name));
        if (parent >= 0) {
            blob.writeSLEB(currentIndex - parent);
        }
        writeAddress(globalIndex);
        blob.writeLEB(size);
        lastFieldOffset = 0;
        phase = ClassPhase.INSTANCE_FIELDS;
    }

    @Override
    public void instanceField(String name, int index, FieldType type) {
        writeFieldType(type);
        blob.writeLEB(strings.stringPtr(name));
        blob.writeSLEB(index - lastFieldOffset);
        lastFieldOffset = index;
    }

    private void writeFieldType(FieldType type) {
        switch (type) {
            case BOOLEAN:
                blob.writeByte(DebugConstants.FIELD_BOOLEAN);
                break;
            case BYTE:
                blob.writeByte(DebugConstants.FIELD_BYTE);
                break;
            case SHORT:
                blob.writeByte(DebugConstants.FIELD_SHORT);
                break;
            case CHAR:
                blob.writeByte(DebugConstants.FIELD_CHAR);
                break;
            case INT:
                blob.writeByte(DebugConstants.FIELD_INT);
                break;
            case LONG:
                blob.writeByte(DebugConstants.FIELD_LONG);
                break;
            case FLOAT:
                blob.writeByte(DebugConstants.FIELD_FLOAT);
                break;
            case DOUBLE:
                blob.writeByte(DebugConstants.FIELD_DOUBLE);
                break;
            case OBJECT:
                blob.writeByte(DebugConstants.FIELD_OBJECT);
                break;
            case ADDRESS:
                blob.writeByte(DebugConstants.FIELD_ADDRESS);
                break;
            case UNDEFINED:
                blob.writeByte(DebugConstants.FIELD_UNDEFINED);
                break;
        }
    }

    @Override
    public void endClass() {
        if (phase == ClassPhase.INSTANCE_FIELDS) {
            blob.writeByte(DebugConstants.FIELD_END);
        }
        ++currentIndex;
    }

    @Override
    public void writeArray(FieldType itemType, int globalIndex) {
        blob.writeByte(DebugConstants.CLASS_ARRAY);
        writeFieldType(itemType);
        writeAddress(globalIndex);
        ++currentIndex;
    }

    private void writeAddress(int address) {
        blob.writeSLEB(address - currentAddress);
        currentAddress = address;
    }

    private enum ClassPhase {
        NO_CLASS,
        INSTANCE_FIELDS
    }
}
