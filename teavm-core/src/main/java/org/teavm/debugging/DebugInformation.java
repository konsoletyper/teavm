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
    String[] fields;
    Map<String, Integer> fieldMap;
    String[] methods;
    Map<String, Integer> methodMap;
    String[] variableNames;
    Map<String, Integer> variableNameMap;
    FileDescription[] fileDescriptions;
    Mapping fileMapping;
    Mapping classMapping;
    Mapping methodMapping;
    Mapping lineMapping;
    MultiMapping[] variableMappings;
    CFG[] controlFlowGraphs;
    List<ClassMetadata> classesMetadata;

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
        if (line >= description.generatedLocationStart.length - 1) {
            return Collections.emptyList();
        }
        int start = description.generatedLocationStart[line];
        int end = description.generatedLocationStart[line + 1];
        GeneratedLocation[] resultArray = new GeneratedLocation[(end - start) / 2];
        for (int i = 0; i < resultArray.length; ++i) {
            int genLine = description.generatedLocationData[start++];
            int genColumn = description.generatedLocationData[start++];
            resultArray[i] = new GeneratedLocation(genLine, genColumn);
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

    public String[] getVariableMeaningAt(int line, int column, String variable) {
        return getVariableMeaningAt(new GeneratedLocation(line, column), variable);
    }

    public String[] getVariableMeaningAt(GeneratedLocation location, String variable) {
        Integer varIndex = variableNameMap.get(variable);
        if (varIndex == null) {
            return new String[0];
        }
        MultiMapping mapping = variableMappings[varIndex];
        if (mapping == null) {
            return new String[0];
        }
        return componentByKey(mapping, variableNames, location);
    }

    public SourceLocation[] getFollowingLines(SourceLocation location) {
        Integer fileIndex = fileNameMap.get(location.getFileName());
        if (fileIndex == null) {
            return null;
        }
        CFG cfg = controlFlowGraphs[fileIndex];
        if (cfg == null) {
            return null;
        }
        int start = cfg.offsets[location.getLine()];
        int end = cfg.offsets[location.getLine() + 1];
        if (end - start == 1 && cfg.offsets[start] == -1) {
            return new SourceLocation[0];
        } else if (start == end) {
            return null;
        }
        SourceLocation[] result = new SourceLocation[end - start];
        for (int i = 0; i < result.length; ++i) {
            int line = cfg.lines[i + start];
            if (line >= 0) {
                result[i] = new SourceLocation(fileNames[cfg.files[i + start]], line);
            } else {
                result[i] = null;
            }
        }
        return result;
    }

    public String getFieldMeaning(String className, String jsName) {
        Integer classIndex = classNameMap.get(className);
        if (classIndex == null) {
            return null;
        }
        Integer jsIndex = fieldMap.get(jsName);
        if (jsIndex == null) {
            return null;
        }
        while (classIndex != null) {
            ClassMetadata cls = classesMetadata.get(classIndex);
            Integer fieldIndex = cls.fieldMap.get(jsIndex);
            if (fieldIndex != null) {
                return fields[fieldIndex];
            }
            classIndex = cls.parentId;
        }
        return null;
    }

    private <T> T componentByKey(Mapping mapping, T[] values, GeneratedLocation location) {
        int keyIndex = indexByKey(mapping, location);
        int valueIndex = keyIndex >= 0 ? mapping.values[keyIndex] : -1;
        return valueIndex >= 0 ? values[valueIndex] : null;
    }

    private String[] componentByKey(MultiMapping mapping, String[] values, GeneratedLocation location) {
        int keyIndex = indexByKey(mapping, location);
        if (keyIndex < 0) {
            return new String[0];
        }
        int start = mapping.offsets[keyIndex];
        int end = mapping.offsets[keyIndex + 1];
        String[] result = new String[end - start];
        for (int i = 0; i < result.length; ++i) {
            result[i] = values[mapping.data[i + start]];
        }
        return result;
    }

    private int indexByKey(Mapping mapping, GeneratedLocation location) {
        int index = Collections.binarySearch(mapping.keyList(), location);
        return index >= 0 ? index : -index - 2;
    }

    private int indexByKey(MultiMapping mapping, GeneratedLocation location) {
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
        fieldMap = mapArray(fields);
        methodMap = mapArray(methods);
        variableNameMap = mapArray(variableNames);
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
        int lastFileIndex = -1;
        int lastSourceLine = -1;

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
            if (lastFileIndex == fileIndex && lastSourceLine == sourceLine) {
                return;
            }
            lastFileIndex = fileIndex;
            lastSourceLine = sourceLine;
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
            slot = addData(slot, line);
            slot = addData(slot, column);
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

    static class MultiMapping {
        int[] lines;
        int[] columns;
        int[] offsets;
        int[] data;

        public MultiMapping(int[] lines, int[] columns, int[] offsets, int[] data) {
            this.lines = lines;
            this.columns = columns;
            this.offsets = offsets;
            this.data = data;
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

    static class ClassMetadata {
        Integer parentId;
        Map<Integer, Integer> fieldMap = new HashMap<>();
    }

    static class CFG {
        int[] lines;
        int[] files;
        int[] offsets;
    }
}
