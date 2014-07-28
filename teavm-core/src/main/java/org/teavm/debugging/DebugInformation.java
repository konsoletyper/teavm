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
package org.teavm.debugging;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import org.teavm.common.IntegerArray;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class DebugInformation {
    String[] fileNames;
    Map<String, Integer> fileNameMap;
    String[] classNames;
    Map<String, Integer> classNameMap;
    String[] methods;
    Map<String, Integer> methodMap;
    FileDescription[] fileDescriptions;
    Mapping fileMapping;
    Mapping classMapping;
    Mapping methodMapping;
    Mapping lineMapping;

    public String[] getCoveredSourceFiles() {
        return fileNames.clone();
    }

    public Collection<GeneratedLocation> getGeneratedLocations(String fileName, int line) {
        Integer fileIndex = fileNameMap.get(fileName);
        if (fileIndex == null) {
            return Collections.emptyList();
        }
        FileDescription description = fileIndex >= 0 ? fileDescriptions[fileIndex] : null;
        if (description == null) {
            return Collections.emptyList();
        }
        if (line > description.generatedLocationStart.length) {
            return Collections.emptyList();
        }
        int start = description.generatedLocationStart[line];
        int end = description.generatedLocationStart[line + 1];
        GeneratedLocation[] resultArray = new GeneratedLocation[(end - start) / 2];
        for (int i = 0; i < resultArray.length; ++i) {
            resultArray[i] = new GeneratedLocation(description.generatedLocationData[i * 2],
                    description.generatedLocationData[i * 2 + 1]);
        }
        return Arrays.asList(resultArray);
    }

    public Collection<GeneratedLocation> getGeneratedLocations(SourceLocation sourceLocation) {
        return getGeneratedLocations(sourceLocation.getFileName(), sourceLocation.getLine());
    }

    public SourceLocation getSourceLocation(int line, int column) {
        return getSourceLocation(new GeneratedLocation(line, column));
    }

    public SourceLocation getSourceLocation(GeneratedLocation generatedLocation) {
        String fileName = componentByKey(fileMapping, fileNames, generatedLocation);
        int lineNumberIndex = indexByKey(lineMapping, generatedLocation);
        int lineNumber = lineNumberIndex >= 0 ? lineMapping.values[lineNumberIndex] : -1;
        return new SourceLocation(fileName, lineNumber);
    }

    public MethodReference getMethodAt(GeneratedLocation generatedLocation) {
        String className = componentByKey(classMapping, classNames, generatedLocation);
        if (className == null) {
            return null;
        }
        String method = componentByKey(methodMapping, methods, generatedLocation);
        if (method == null) {
            return null;
        }
        return new MethodReference(className, MethodDescriptor.parse(method));
    }

    public MethodReference getMethodAt(int line, int column) {
        return getMethodAt(new GeneratedLocation(line, column));
    }

    private <T> T componentByKey(Mapping mapping, T[] values, GeneratedLocation location) {
        int keyIndex = indexByKey(mapping, location);
        int valueIndex = keyIndex >= 0 ? mapping.values[keyIndex] : -1;
        return valueIndex >= 0 ? values[valueIndex] : null;
    }

    private int indexByKey(Mapping mapping, GeneratedLocation location) {
        int index = Collections.binarySearch(mapping.keyList(), location);
        return index >= 0 ? index : -index - 2;
    }

    public void write(OutputStream output) throws IOException {
        DebugInformationWriter writer = new DebugInformationWriter(new DataOutputStream(output));
        writer.write(this);
    }

    public static DebugInformation read(InputStream input) throws IOException {
        DebugInformationReader reader = new DebugInformationReader(input);
        return reader.read();
    }

    void rebuildMaps() {
        fileNameMap = mapArray(fileNames);
        classNameMap = mapArray(classNames);
        methodMap = mapArray(methods);
    }

    private Map<String, Integer> mapArray(String[] array) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < array.length; ++i) {
            map.put(array[i], i);
        }
        return map;
    }

    void rebuildFileDescriptions() {
        FileDescriptionBuilder builder = new FileDescriptionBuilder(fileNames.length);
        int fileIndex = 0;
        int lineIndex = 0;
        int currentFile = -1;
        int currentLine = -1;
        while (fileIndex < fileMapping.size() && lineIndex < lineMapping.size()) {
            GeneratedLocation fileLoc = fileMapping.key(fileIndex);
            GeneratedLocation lineLoc = lineMapping.key(lineIndex);
            int cmp = fileLoc.compareTo(lineLoc);
            if (cmp < 0) {
                currentFile = fileMapping.values[fileIndex++];
            } else if (cmp > 0){
                currentLine = lineMapping.values[lineIndex++];
            } else {
                currentFile = fileMapping.values[fileIndex++];
                currentLine = lineMapping.values[lineIndex++];
            }
            builder.emit(fileLoc.getLine(), fileLoc.getColumn(), currentFile, currentLine);
        }
        while (fileIndex < fileMapping.size()) {
            builder.emit(fileMapping.lines[fileIndex], fileMapping.columns[fileIndex],
                    fileMapping.values[fileIndex], currentLine);
            ++fileIndex;
        }
        while (lineIndex < lineMapping.size()) {
            builder.emit(lineMapping.lines[lineIndex], lineMapping.columns[lineIndex], currentFile,
                    lineMapping.values[lineIndex]);
            ++lineIndex;
        }
        fileDescriptions = builder.build();
    }

    static class FileDescriptionBuilder {
        FileDescriptionProto[] files;

        public FileDescriptionBuilder(int size) {
            files = new FileDescriptionProto[size];
            for (int i = 0; i < size; ++i) {
                files[i] = new FileDescriptionProto();
            }
        }

        void emit(int line, int column, int fileIndex, int sourceLine) {
            if (sourceLine == -1 || fileIndex == -1) {
                return;
            }
            FileDescriptionProto proto = files[fileIndex];
            proto.addLocation(sourceLine, line, column);
        }

        public FileDescription[] build() {
            FileDescription[] descriptions = new FileDescription[files.length];
            for (int i = 0; i < files.length; ++i) {
                descriptions[i] = files[i].build();
            }
            return descriptions;
        }
    }

    static class FileDescriptionProto {
        IntegerArray generatedLocationData = new IntegerArray(1);
        IntegerArray generatedLocationPointers = new IntegerArray(1);
        IntegerArray generatedLocationStart = new IntegerArray(1);
        IntegerArray generatedLocationSize = new IntegerArray(1);

        public void addLocation(int sourceLine, int line, int column) {
            ensureLine(sourceLine);
            generatedLocationSize.set(sourceLine, generatedLocationSize.get(sourceLine) + 1);
            int slot = generatedLocationStart.get(sourceLine);
            slot = addData(slot, column);
            slot = addData(slot, line);
            generatedLocationStart.set(sourceLine, slot);
        }

        int addData(int slot, int value) {
            int result = generatedLocationData.size();
            generatedLocationData.add(value);
            generatedLocationPointers.add(slot);
            return result;
        }

        void ensureLine(int sourceLine) {
            while (sourceLine >= generatedLocationSize.size()) {
                generatedLocationSize.add(0);
                generatedLocationStart.add(-1);
            }
        }

        FileDescription build() {
            FileDescription description = new FileDescription();
            description.generatedLocationData = new int[generatedLocationData.size()];
            description.generatedLocationStart = new int[generatedLocationStart.size()];
            int current = 0;
            for (int i = 0; i < generatedLocationStart.size(); ++i) {
                description.generatedLocationStart[i] = current;
                current += generatedLocationSize.get(i) * 2;
                int j = current;
                int ptr = generatedLocationStart.get(i);
                while (ptr >= 0) {
                    description.generatedLocationData[--j] = generatedLocationData.get(ptr);
                    ptr = generatedLocationPointers.get(ptr);
                }
            }
            return description;
        }
    }

    static class FileDescription {
        int[] generatedLocationData;
        int[] generatedLocationStart;
    }

    static class Mapping {
        int[] lines;
        int[] columns;
        int[] values;

        public Mapping(int[] lines, int[] columns, int[] values) {
            this.lines = lines;
            this.columns = columns;
            this.values = values;
        }

        public LocationList keyList() {
            return new LocationList(lines, columns);
        }

        public int size() {
            return lines.length;
        }

        public GeneratedLocation key(int index) {
            return new GeneratedLocation(lines[index], columns[index]);
        }
    }

    static class LocationList extends AbstractList<GeneratedLocation> {
        private int[] lines;
        private int[] columns;

        public LocationList(int[] lines, int[] columns) {
            this.lines = lines;
            this.columns = columns;
        }

        @Override
        public GeneratedLocation get(int index) {
            return new GeneratedLocation(lines[index], columns[index]);
        }

        @Override
        public int size() {
            return lines.length;
        }
    }
}
