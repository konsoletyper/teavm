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

import java.io.*;
import java.util.*;
import org.teavm.common.IntegerArray;
import org.teavm.common.RecordArray;
import org.teavm.common.RecordArrayBuilder;
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
    long[] exactMethods;
    Map<Long, Integer> exactMethodMap;
    RecordArray[] fileDescriptions;
    RecordArray fileMapping;
    RecordArray classMapping;
    RecordArray methodMapping;
    RecordArray lineMapping;
    RecordArray callSiteMapping;
    MultiMapping[] variableMappings;
    RecordArray[] lineCallSites;
    CFG[] controlFlowGraphs;
    List<ClassMetadata> classesMetadata;
    RecordArray methodEntrances;
    MethodTree methodTree;

    public String[] getFilesNames() {
        return fileNames.clone();
    }

    public String[] getVariableNames() {
        return variableNames.clone();
    }

    public LineNumberIterator iterateOverLineNumbers() {
        return new LineNumberIterator(this);
    }

    public FileNameIterator iterateOverFileNames() {
        return new FileNameIterator(this);
    }

    public String getFileName(int fileNameId) {
        return fileNames[fileNameId];
    }

    public Collection<GeneratedLocation> getGeneratedLocations(String fileName, int line) {
        Integer fileIndex = fileNameMap.get(fileName);
        if (fileIndex == null) {
            return Collections.emptyList();
        }
        RecordArray description = fileIndex >= 0 ? fileDescriptions[fileIndex] : null;
        if (description == null) {
            return Collections.emptyList();
        }
        if (line >= description.size()) {
            return Collections.emptyList();
        }
        int[] data = description.get(line).getArray(0);
        GeneratedLocation[] resultArray = new GeneratedLocation[data.length / 2];
        for (int i = 0; i < resultArray.length; ++i) {
            int genLine = data[i * 2];
            int genColumn = data[i * 2 + 1];
            resultArray[i] = new GeneratedLocation(genLine, genColumn);
        }
        return Arrays.asList(resultArray);
    }

    public Collection<GeneratedLocation> getGeneratedLocations(SourceLocation sourceLocation) {
        return getGeneratedLocations(sourceLocation.getFileName(), sourceLocation.getLine());
    }

    public SourceLocationIterator iterateOverSourceLocations() {
        return new SourceLocationIterator(this);
    }

    public SourceLocation getSourceLocation(int line, int column) {
        return getSourceLocation(new GeneratedLocation(line, column));
    }

    public SourceLocation getSourceLocation(GeneratedLocation generatedLocation) {
        String fileName = componentByKey(fileMapping, fileNames, generatedLocation);
        int lineNumberIndex = indexByKey(lineMapping, generatedLocation);
        int lineNumber = lineNumberIndex >= 0 ? lineMapping.get(lineNumberIndex).get(2) : -1;
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
        if (location.getLine() >= cfg.offsets.length - 1) {
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

    public DebuggerCallSite getCallSite(GeneratedLocation location) {
        int keyIndex = indexByKey(callSiteMapping, location);
        return keyIndex >= 0 ? getCallSite(keyIndex) : null;
    }

    private DebuggerCallSite getCallSite(int index) {
        RecordArray.Record record = callSiteMapping.get(index);
        int type = record.get(0);
        int[] data = record.getArray(0);
        switch (type) {
            case DebuggerCallSite.NONE:
                return null;
            case DebuggerCallSite.STATIC:
                return new DebuggerStaticCallSite(getExactMethod(data[0]));
            case DebuggerCallSite.VIRTUAL:
                return new DebuggerVirtualCallSite(getExactMethod(data[0]), data[1], variableNames[data[1]]);
            default:
                throw new AssertionError("Unrecognized call site type: " + type);
        }
    }

    public DebuggerCallSite getCallSite(int line, int column) {
        return getCallSite(new GeneratedLocation(line, column));
    }

    public GeneratedLocation[] getMethodEntrances(MethodReference methodRef) {
        Integer index = getExactMethodIndex(methodRef);
        if (index == null) {
            return new GeneratedLocation[0];
        }
        return methodEntrances.getEntrances(index);
    }

    private Integer getExactMethodIndex(MethodReference methodRef) {
        Integer classIndex = classNameMap.get(methodRef.getClassName());
        if (classIndex == null) {
            return null;
        }
        Integer methodIndex = methodMap.get(methodRef.getDescriptor().toString());
        if (methodIndex == null) {
            return null;
        }
        return getExactMethodIndex(classIndex, methodIndex);
    }

    public MethodReference getExactMethod(int index) {
        long item = exactMethods[index];
        int classIndex = (int)(item >>> 32);
        int methodIndex = (int)item;
        return new MethodReference(classNames[classIndex], MethodDescriptor.parse(methods[methodIndex]));
    }

    public MethodReference[] getDirectOverridingMethods(MethodReference methodRef) {
        Integer methodIndex = getExactMethodIndex(methodRef);
        if (methodIndex == null) {
            return new MethodReference[0];
        }
        int start = methodTree.offsets[methodIndex];
        int end = methodTree.offsets[methodIndex + 1];
        MethodReference[] result = new MethodReference[end - start];
        for (int i = 0; i < result.length; ++i) {
            result[i] = getExactMethod(methodTree.data[i]);
        }
        return result;
    }

    public MethodReference[] getOverridingMethods(MethodReference methodRef) {
        Set<MethodReference> overridingMethods = new HashSet<>();
        getOverridingMethods(methodRef, overridingMethods);
        return overridingMethods.toArray(new MethodReference[0]);
    }

    private void getOverridingMethods(MethodReference methodRef, Set<MethodReference> overridingMethods) {
        if (overridingMethods.add(methodRef)) {
            for (MethodReference overridingMethod : getDirectOverridingMethods(methodRef)) {
                getOverridingMethods(overridingMethod, overridingMethods);
            }
        }
    }

    public DebuggerCallSite[] getCallSites(SourceLocation location) {
        Integer fileIndex = fileNameMap.get(location.getFileName());
        if (fileIndex == null) {
            return new DebuggerCallSite[0];
        }
        RecordArray mapping = lineCallSites[fileIndex];
        if (location.getLine() >= mapping.size()) {
            return new DebuggerCallSite[0];
        }
        int[] callSiteIds = mapping.get(location.getLine()).getArray(0);
        DebuggerCallSite[] callSites = new DebuggerCallSite[callSiteIds.length];
        for (int i = 0; i < callSiteIds.length; ++i) {
            callSites[i] = getCallSite(callSiteIds[i]);
        }
        return callSites;
    }

    private <T> T componentByKey(RecordArray mapping, T[] values, GeneratedLocation location) {
        int keyIndex = indexByKey(mapping, location);
        int valueIndex = keyIndex >= 0 ? mapping.get(keyIndex).get(2) : -1;
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

    private int indexByKey(RecordArray mapping, GeneratedLocation location) {
        int index = Collections.binarySearch(new LocationList(mapping), location);
        return index >= 0 ? index : -index - 2;
    }

    private int valueByKey(RecordArray mapping, GeneratedLocation location) {
        int index = indexByKey(mapping, location);
        return index >= 0 ? mapping.get(index).get(2) : -1;
    }

    private int indexByKey(MultiMapping mapping, GeneratedLocation location) {
        int index = Collections.binarySearch(mapping.keyList(), location);
        return index >= 0 ? index : -index - 2;
    }

    public void write(OutputStream output) throws IOException {
        DebugInformationWriter writer = new DebugInformationWriter(new DataOutputStream(output));
        writer.write(this);
    }

    public void writeAsSourceMaps(Writer output, String sourceFile) throws IOException {
        new SourceMapsWriter(output).write(sourceFile, this);
    }

    public static DebugInformation read(InputStream input) throws IOException {
        DebugInformationReader reader = new DebugInformationReader(input);
        return reader.read();
    }

    void rebuild() {
        rebuildMaps();
        rebuildFileDescriptions();
        rebuildEntrances();
        rebuildMethodTree();
        rebuildLineCallSites();
    }

    void rebuildMaps() {
        fileNameMap = mapArray(fileNames);
        classNameMap = mapArray(classNames);
        fieldMap = mapArray(fields);
        methodMap = mapArray(methods);
        variableNameMap = mapArray(variableNames);
        exactMethodMap = new HashMap<>();
        for (int i = 0; i < exactMethods.length; ++i) {
            exactMethodMap.put(exactMethods[i], i);
        }
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
            RecordArray.Record fileRec = fileMapping.get(fileIndex);
            RecordArray.Record lineRec = lineMapping.get(lineIndex);
            GeneratedLocation fileLoc = key(fileRec);
            GeneratedLocation lineLoc = key(lineRec);
            int cmp = fileLoc.compareTo(lineLoc);
            if (cmp < 0) {
                currentFile = fileRec.get(2);
                fileIndex++;
            } else if (cmp > 0){
                currentLine = lineRec.get(2);
                lineIndex++;
            } else {
                currentFile = fileRec.get(2);
                currentLine = lineRec.get(2);
                fileIndex++;
                lineIndex++;
            }
            builder.emit(fileLoc.getLine(), fileLoc.getColumn(), currentFile, currentLine);
        }
        while (fileIndex < fileMapping.size()) {
            RecordArray.Record fileRec = fileMapping.get(fileIndex++);
            builder.emit(fileRec.get(0), fileRec.get(1), fileRec.get(2), currentLine);
        }
        while (lineIndex < lineMapping.size()) {
            RecordArray.Record lineRec = lineMapping.get(lineIndex++);
            builder.emit(lineRec.get(0), lineRec.get(1), currentFile, lineRec.get(2));
        }
        fileDescriptions = builder.build();
    }

    void rebuildEntrances() {
        RecordArrayBuilder builder = new RecordArrayBuilder(0, 1);
        for (SourceLocationIterator iter = iterateOverSourceLocations(); !iter.isEndReached(); iter.next()) {
            iter.getLocation();
        }
        methodEntrances = new MethodEntrancesBuilder().build();
    }

    void rebuildMethodTree() {
        long[] exactMethods = this.exactMethods.clone();
        Arrays.sort(exactMethods);
        IntegerArray methods = new IntegerArray(1);
        int lastClass = -1;
        for (int i = 0; i < exactMethods.length; ++i) {
            long exactMethod = exactMethods[i];
            int classIndex = (int)(exactMethod >>> 32);
            if (classIndex != lastClass) {
                if (lastClass >= 0) {
                    ClassMetadata clsData = classesMetadata.get(lastClass);
                    clsData.methods = methods.getAll();
                    methods.clear();
                }
                lastClass = classIndex;
            }
            int methodIndex = (int)exactMethod;
            methods.add(methodIndex);
        }
        if (lastClass >= 0) {
            ClassMetadata clsData = classesMetadata.get(lastClass);
            clsData.methods = methods.getAll();
            Arrays.sort(clsData.methods);
        }

        int[] start = new int[exactMethods.length];
        Arrays.fill(start, -1);
        IntegerArray data = new IntegerArray(1);
        IntegerArray next = new IntegerArray(1);
        for (int i = 0; i < classesMetadata.size(); ++i) {
            ClassMetadata clsData = classesMetadata.get(i);
            if (clsData.parentId == null || clsData.methods == null) {
                continue;
            }
            for (int methodIndex : clsData.methods) {
                ClassMetadata superclsData = classesMetadata.get(clsData.parentId);
                Integer parentId = clsData.parentId;
                while (superclsData != null) {
                    if (Arrays.binarySearch(superclsData.methods, methodIndex) >= 0) {
                        int childMethod = getExactMethodIndex(i, methodIndex);
                        int parentMethod = getExactMethodIndex(parentId, methodIndex);
                        int ptr = start[parentMethod];
                        start[parentMethod] = data.size();
                        data.add(childMethod);
                        next.add(ptr);
                        break;
                    }
                    parentId = superclsData.parentId;
                    superclsData = parentId != null ? classesMetadata.get(parentId) : null;
                }
            }
        }

        MethodTree methodTree = new MethodTree();
        methodTree.offsets = new int[start.length + 1];
        methodTree.data = new int[data.size()];
        int index = 0;
        for (int i = 0; i < start.length; ++i) {
            int ptr = start[i];
            while (ptr != -1) {
                methodTree.data[index++] = data.get(ptr);
                ptr = next.get(ptr);
            }
            methodTree.offsets[i + 1] = index;
        }
        this.methodTree = methodTree;
    }

    private Integer getExactMethodIndex(int classIndex, int methodIndex) {
        long entry = ((long)classIndex << 32) | methodIndex;
        return exactMethodMap.get(entry);
    }

    private void rebuildLineCallSites() {
        lineCallSites = new RecordArray[fileNames.length];
        RecordArrayBuilder[] builders = new RecordArrayBuilder[fileNames.length];
        for (int i = 0; i < lineCallSites.length; ++i) {
            builders[i] = new RecordArrayBuilder(0, 1);
        }
        for (int i = 0; i < callSiteMapping.size(); ++i) {
            RecordArray.Record callSiteRec = callSiteMapping.get(i);
            GeneratedLocation loc = key(callSiteRec);
            int callSiteType = callSiteRec.get(2);
            if (callSiteType != DebuggerCallSite.NONE) {
                int line = valueByKey(lineMapping, loc);
                int fileId = valueByKey(fileMapping, loc);
                if (fileId >= 0 && line >= 0) {
                    RecordArrayBuilder builder = builders[fileId];
                    while (builder.size() <= line) {
                        builder.add();
                    }
                    builder.get(line).getArray(0).add(i);
                }
            }
        }
        for (int i = 0; i < lineCallSites.length; ++i) {
            lineCallSites[i] = builders[i].build();
        }
    }

    class MethodEntrancesBuilder {
        int[] start;
        IntegerArray data;
        IntegerArray next;
        int methodIndex;
        int classIndex;

        public MethodEntrances build() {
            methodIndex = -1;
            classIndex = -1;
            start = new int[exactMethods.length];
            Arrays.fill(start, -1);
            data = new IntegerArray(0);
            next = new IntegerArray(0);
            int methodMappingIndex = 0;
            int classMappingIndex = 0;
            GeneratedLocation previousLocation = new GeneratedLocation(0, 0);
            while (methodMappingIndex < methodMapping.lines.length &&
                    classMappingIndex < classMapping.lines.length) {
                GeneratedLocation methodLoc = new GeneratedLocation(methodMapping.lines[methodMappingIndex],
                        methodMapping.columns[methodMappingIndex]);
                GeneratedLocation classLoc = new GeneratedLocation(classMapping.lines[classMappingIndex],
                        classMapping.columns[classMappingIndex]);
                int cmp = methodLoc.compareTo(classLoc);
                if (cmp < 0) {
                    addMethodEntrance(previousLocation, methodLoc);
                    previousLocation = methodLoc;
                    methodIndex = methodMapping.values[methodMappingIndex++];
                } else if (cmp > 0) {
                    addMethodEntrance(previousLocation, classLoc);
                    previousLocation = classLoc;
                    classIndex = classMapping.values[classMappingIndex++];
                } else {
                    addMethodEntrance(previousLocation, classLoc);
                    previousLocation = classLoc;
                    methodIndex = methodMapping.values[methodMappingIndex++];
                    classIndex = classMapping.values[classMappingIndex++];
                }
            }
            while (methodMappingIndex < methodMapping.lines.length) {
                GeneratedLocation methodLoc = new GeneratedLocation(methodMapping.lines[methodMappingIndex],
                        methodMapping.columns[methodMappingIndex]);
                addMethodEntrance(previousLocation, methodLoc);
                previousLocation = methodLoc;
                methodIndex = methodMapping.values[methodMappingIndex++];
            }
            while (classMappingIndex < classMapping.lines.length) {
                GeneratedLocation classLoc = new GeneratedLocation(classMapping.lines[classMappingIndex],
                        classMapping.columns[classMappingIndex]);
                addMethodEntrance(previousLocation, classLoc);
                previousLocation = classLoc;
                classIndex = classMapping.values[classMappingIndex++];
            }
            return assemble();
        }

        private void addMethodEntrance(GeneratedLocation location, GeneratedLocation limit) {
            if (classIndex < 0 || methodIndex < 0) {
                return;
            }
            long exactMethod = ((long)classIndex << 32) | methodIndex;
            Integer exactMethodIndex = exactMethodMap.get(exactMethod);
            if (exactMethodIndex == null) {
                return;
            }

            int lineIndex = indexByKey(lineMapping, location);
            if (lineIndex < 0) {
                lineIndex = 0;
            }
            int line = -1;
            while (lineIndex < lineMapping.values.length) {
                location = new GeneratedLocation(lineMapping.lines[lineIndex], lineMapping.columns[lineIndex]);
                if (location.compareTo(limit) >= 0) {
                    break;
                }
                if (lineMapping.values[lineIndex] >= 0) {
                    line = lineMapping.values[lineIndex];
                    break;
                }
                ++lineIndex;
            }
            if (line == -1) {
                return;
            }

            int ptr = start[exactMethodIndex];
            start[exactMethodIndex] = data.size();
            next.add(ptr);
            ptr = data.size();
            data.add(location.getColumn());
            next.add(ptr);
            start[exactMethodIndex] = data.size();
            data.add(location.getLine());
        }

        private MethodEntrances assemble() {
            MethodEntrances entrances = new MethodEntrances();
            entrances.offsets = new int[start.length + 1];
            entrances.data = new int[data.size()];
            int index = 0;
            for (int i = 0; i < start.length; ++i) {
                int ptr = start[i];
                while (ptr != -1) {
                    entrances.data[index++] = data.get(ptr);
                    ptr = next.get(ptr);
                }
                entrances.offsets[i + 1] = index;
            }
            return entrances;
        }
    }

    static class FileDescriptionBuilder {
        RecordArrayBuilder[] files;
        int lastFileIndex = -1;
        int lastSourceLine = -1;

        public FileDescriptionBuilder(int size) {
            files = new RecordArrayBuilder[size];
            for (int i = 0; i < size; ++i) {
                files[i] = new RecordArrayBuilder(0, 1);
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
            RecordArrayBuilder proto = files[fileIndex];
            while (proto.size() <= sourceLine) {
                proto.add();
            }
            RecordArrayBuilder.RecordSubArray array = proto.get(sourceLine).getArray(0);
            array.add(line);
            array.add(column);
        }

        public RecordArray[] build() {
            RecordArray[] descriptions = new RecordArray[files.length];
            for (int i = 0; i < files.length; ++i) {
                descriptions[i] = files[i].build();
            }
            return descriptions;
        }
    }

    static GeneratedLocation key(RecordArray.Record record) {
        return new GeneratedLocation(record.get(0), record.get(1));
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
        private RecordArray recordArray;

        public LocationList(RecordArray recordArray) {
            this.recordArray = recordArray;
        }

        @Override
        public GeneratedLocation get(int index) {
            RecordArray.Record record = recordArray.get(index);
            return new GeneratedLocation(record.get(0), record.get(1));
        }

        @Override
        public int size() {
            return recordArray.size();
        }
    }

    static class ClassMetadata {
        Integer parentId;
        Map<Integer, Integer> fieldMap = new HashMap<>();
        int[] methods;
    }

    static class CFG {
        int[] lines;
        int[] files;
        int[] offsets;
    }

    class MethodTree {
        int[] data;
        int[] offsets;

        public MethodReference[] getOverridingMethods(int index) {
            if (index < 0 || index > offsets.length - 1) {
                return new MethodReference[0];
            }
            int start = offsets[index];
            int end = offsets[index + 1];
            MethodReference[] references = new MethodReference[end - start];
            for (int i = 0; i < references.length; ++i) {
                long item = exactMethods[data[start + i]];
                int classIndex = (int)(item >>> 32);
                int methodIndex = (int)item;
                references[i] = new MethodReference(classNames[classIndex],
                        MethodDescriptor.parse(methods[methodIndex]));
            }
            return references;
        }
    }
}
