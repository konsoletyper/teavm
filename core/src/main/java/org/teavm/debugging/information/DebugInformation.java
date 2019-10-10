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

import java.io.*;
import java.util.*;
import org.teavm.common.IntegerArray;
import org.teavm.common.RecordArray;
import org.teavm.common.RecordArrayBuilder;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ReferenceCache;

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
    Layer[] layers;
    RecordArray callSiteMapping;
    RecordArray statementStartMapping;
    RecordArray[] variableMappings;
    RecordArray[] lineCallSites;
    RecordArray[] controlFlowGraphs;
    List<ClassMetadata> classesMetadata;
    Map<String, ClassMetadata> classMetadataByJsName;
    RecordArray methodEntrances;
    MethodTree methodTree;
    ReferenceCache referenceCache;

    public DebugInformation() {
        this(new ReferenceCache());
    }

    public DebugInformation(ReferenceCache referenceCache) {
        this.referenceCache = referenceCache;
    }

    public String[] getFilesNames() {
        return fileNames.clone();
    }

    public String[] getVariableNames() {
        return variableNames.clone();
    }

    public String getFileName(int fileNameId) {
        return fileNames[fileNameId];
    }

    public String[] getClassNames() {
        return classNames.clone();
    }

    public String getClassName(int classNameId) {
        return classNames[classNameId];
    }

    public MethodDescriptor[] getMethods() {
        MethodDescriptor[] descriptors = new MethodDescriptor[methods.length];
        for (int i = 0; i < descriptors.length; ++i) {
            descriptors[i] = referenceCache.parseDescriptorCached(methods[i]);
        }
        return descriptors;
    }

    public MethodDescriptor getMethod(int methodId) {
        return referenceCache.parseDescriptorCached(methods[methodId]);
    }

    public MethodReference[] getExactMethods() {
        MethodReference[] result = new MethodReference[exactMethods.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = getExactMethod(i);
        }
        return result;
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
        int classIndex = (int) (item >>> 32);
        int methodIndex = (int) item;
        return referenceCache.getCached(classNames[classIndex], referenceCache.parseDescriptorCached(
                methods[methodIndex]));
    }

    public int getExactMethodId(int classNameId, int methodId) {
        long full = ((long) classNameId << 32) | methodId;
        Integer id = exactMethodMap.get(full);
        return id != null ? id : -1;
    }

    public int layerCount() {
        return layers.length;
    }

    public ExactMethodIterator iterateOverExactMethods(int layerIndex) {
        return new ExactMethodIterator(this, layers[layerIndex]);
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

    private LayerSourceLocationIterator iterateOverSourceLocations(int layer) {
        return new LayerSourceLocationIterator(this, layers[layer]);
    }

    public SourceLocation getSourceLocation(int line, int column) {
        return getSourceLocation(new GeneratedLocation(line, column));
    }

    public SourceLocation getSourceLocation(int line, int column, int layerIndex) {
        return getSourceLocation(new GeneratedLocation(line, column), layerIndex);
    }

    public SourceLocation getSourceLocation(GeneratedLocation generatedLocation) {
        return getSourceLocation(generatedLocation, autodetectLayer(generatedLocation));
    }

    public SourceLocation getSourceLocation(GeneratedLocation generatedLocation, int layerIndex) {
        if (layerIndex < 0 || layerIndex >= layers.length) {
            return null;
        }

        Layer layer = layers[layerIndex];
        String fileName = componentByKey(layer.fileMapping, fileNames, generatedLocation);
        int lineNumberIndex = indexByKey(layer.lineMapping, generatedLocation);
        int lineNumber = lineNumberIndex >= 0 ? layer.lineMapping.get(lineNumberIndex).get(2) : -1;
        return new SourceLocation(fileName, lineNumber);
    }

    public MethodReference getMethodAt(GeneratedLocation generatedLocation) {
        return getMethodAt(generatedLocation, autodetectLayer(generatedLocation));
    }

    public MethodReference getMethodAt(GeneratedLocation generatedLocation, int layerIndex) {
        if (layerIndex < 0 || layerIndex >= layers.length) {
            return null;
        }

        Layer layer = layers[layerIndex];

        String className = componentByKey(layer.classMapping, classNames, generatedLocation);
        if (className == null) {
            return null;
        }
        String method = componentByKey(layer.methodMapping, methods, generatedLocation);
        if (method == null) {
            return null;
        }
        return referenceCache.getCached(className, referenceCache.parseDescriptorCached(method));
    }

    private int autodetectLayer(GeneratedLocation generatedLocation) {
        int layerIndex = 0;
        for (int i = 1; i < layers.length; ++i) {
            if (componentByKey(layers[i].classMapping, classNames, generatedLocation) == null) {
                break;
            }
            layerIndex = i;
        }
        return layerIndex;
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
        RecordArray mapping = variableMappings[varIndex];
        if (mapping == null) {
            return new String[0];
        }
        int keyIndex = indexByKey(mapping, location);
        if (keyIndex < 0) {
            return new String[0];
        }
        GeneratedLocation keyLocation = key(mapping.get(keyIndex));
        if (!Objects.equals(getMethodAt(keyLocation), getMethodAt(location))) {
            return new String[0];
        }
        int[] valueIndexes = mapping.get(keyIndex).getArray(0);
        String[] result = new String[valueIndexes.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = valueIndexes[i] >= 0 ? variableNames[valueIndexes[i]] : null;
        }
        return result;
    }

    public SourceLocation[] getFollowingLines(SourceLocation location) {
        Integer fileIndex = fileNameMap.get(location.getFileName());
        if (fileIndex == null) {
            return null;
        }
        RecordArray cfg = controlFlowGraphs[fileIndex];
        if (cfg == null) {
            return null;
        }
        if (location.getLine() >= cfg.size()) {
            return null;
        }
        int type = cfg.get(location.getLine()).get(0);
        if (type == 0) {
            return null;
        }
        int[] data = cfg.get(location.getLine()).getArray(0);
        int length = data.length / 2;
        int size = length;
        if (type == 2) {
            ++size;
        }
        SourceLocation[] result = new SourceLocation[size];
        for (int i = 0; i < length; ++i) {
            result[i] = new SourceLocation(fileNames[data[i * 2]], data[i * 2 + 1]);
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

    public String getClassNameByJsName(String className) {
        ClassMetadata cls = classMetadataByJsName.get(className);
        return cls != null ? classNames[cls.id] : null;
    }

    public DebuggerCallSite getCallSite(GeneratedLocation location) {
        int keyIndex = indexByKey(callSiteMapping, location);
        return keyIndex >= 0 ? getCallSite(keyIndex) : null;
    }

    private DebuggerCallSite getCallSite(int index) {
        RecordArray.Record record = callSiteMapping.get(index);
        int type = record.get(2);
        int method = record.get(3);
        switch (type) {
            case DebuggerCallSite.NONE:
                return null;
            case DebuggerCallSite.STATIC:
                return new DebuggerStaticCallSite(getExactMethod(method));
            case DebuggerCallSite.VIRTUAL:
                return new DebuggerVirtualCallSite(getExactMethod(method));
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
        int[] data = methodEntrances.get(index).getArray(0);
        GeneratedLocation[] entrances = new GeneratedLocation[data.length / 2];
        for (int i = 0; i < entrances.length; ++i) {
            entrances[i] = new GeneratedLocation(data[i * 2], data[i * 2 + 1]);
        }
        return entrances;
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

    public List<GeneratedLocation> getStatementStartLocations() {
        return new LocationList(statementStartMapping);
    }

    public GeneratedLocation getStatementLocation(GeneratedLocation location) {
        int index = indexByKey(statementStartMapping, location);
        if (index < 0) {
            return new GeneratedLocation(0, 0);
        }
        RecordArray.Record record = statementStartMapping.get(index);
        return new GeneratedLocation(record.get(0), record.get(1));
    }

    public GeneratedLocation getNextStatementLocation(GeneratedLocation location) {
        int index = indexByKey(statementStartMapping, location);
        if (index >= statementStartMapping.size()) {
            return new GeneratedLocation(0, 0);
        }
        RecordArray.Record record = statementStartMapping.get(index + 1);
        return new GeneratedLocation(record.get(0), record.get(1));
    }

    private <T> T componentByKey(RecordArray mapping, T[] values, GeneratedLocation location) {
        int keyIndex = indexByKey(mapping, location);
        int valueIndex = keyIndex >= 0 ? mapping.get(keyIndex).get(2) : -1;
        return valueIndex >= 0 ? values[valueIndex] : null;
    }

    private int indexByKey(RecordArray mapping, GeneratedLocation location) {
        int index = binarySearchLocation(mapping, location.getLine(), location.getColumn());
        return index >= 0 ? index : -index - 2;
    }

    private int valueByKey(RecordArray mapping, GeneratedLocation location) {
        int index = indexByKey(mapping, location);
        return index >= 0 ? mapping.get(index).get(2) : -1;
    }

    public void write(OutputStream output) throws IOException {
        DebugInformationWriter writer = new DebugInformationWriter(new DataOutputStream(output));
        writer.write(this);
    }

    public void writeAsSourceMaps(Writer output, String sourceRoot, String sourceFile) throws IOException {
        new SourceMapsWriter(output).write(sourceFile, sourceRoot, this);
    }

    public static DebugInformation read(InputStream input) throws IOException {
        return read(input, new ReferenceCache());
    }

    public static DebugInformation read(InputStream input, ReferenceCache referenceCache) throws IOException {
        DebugInformationReader reader = new DebugInformationReader(input, referenceCache);
        return reader.read();
    }

    void rebuild() {
        rebuildMaps();
        rebuildFileDescriptions();
        rebuildEntrances();
        rebuildMethodTree();
        rebuildLineCallSites();
        rebuildClassMap();
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
        RecordArrayBuilder[] builders = new RecordArrayBuilder[fileNames.length];
        for (int i = 0; i < builders.length; ++i) {
            builders[i] = new RecordArrayBuilder(0, 1);
        }
        for (int layer = 0; layer < layers.length; ++layer) {
            for (LayerSourceLocationIterator iter = iterateOverSourceLocations(layer);
                    !iter.isEndReached(); iter.next()) {
                if (iter.getFileNameId() >= 0 && iter.getLine() >= 0) {
                    RecordArrayBuilder builder = builders[iter.getFileNameId()];
                    while (builder.size() <= iter.getLine()) {
                        builder.add();
                    }
                    GeneratedLocation loc = iter.getLocation();
                    RecordArrayBuilder.SubArray array = builder.get(iter.getLine()).getArray(0);
                    array.add(loc.getLine());
                    array.add(loc.getColumn());
                }
            }
        }
        fileDescriptions = new RecordArray[builders.length];
        for (int i = 0; i < fileDescriptions.length; ++i) {
            fileDescriptions[i] = builders[i].build();
        }
    }

    void rebuildEntrances() {
        RecordArrayBuilder builder = new RecordArrayBuilder(0, 1);
        for (int i = 0; i < exactMethods.length; ++i) {
            builder.add();
        }
        GeneratedLocation prevLocation = new GeneratedLocation(0, 0);
        MethodReference prevMethod = null;
        int prevMethodId = -1;
        RecordArray lineMapping = layers[0].lineMapping;
        for (ExactMethodIterator iter = iterateOverExactMethods(0); !iter.isEndReached(); iter.next()) {
            int id = iter.getExactMethodId();
            if (prevMethod != null) {
                int lineIndex = Math.max(0, indexByKey(lineMapping, prevLocation));
                while (lineIndex < lineMapping.size()) {
                    if (key(lineMapping.get(lineIndex)).compareTo(iter.getLocation()) >= 0) {
                        break;
                    }
                    int line = lineMapping.get(lineIndex).get(2);
                    if (line >= 0) {
                        GeneratedLocation firstLineLoc = key(lineMapping.get(lineIndex));
                        RecordArrayBuilder.SubArray array = builder.get(prevMethodId).getArray(0);
                        array.add(firstLineLoc.getLine());
                        array.add(firstLineLoc.getColumn());
                        break;
                    }
                    ++lineIndex;
                }
            }
            prevMethod = iter.getExactMethod();
            prevMethodId = id;
            prevLocation = iter.getLocation();
        }
        methodEntrances = builder.build();
    }

    void rebuildMethodTree() {
        long[] exactMethods = this.exactMethods.clone();
        Arrays.sort(exactMethods);
        IntegerArray methods = new IntegerArray(1);
        int lastClass = -1;
        for (int i = 0; i < exactMethods.length; ++i) {
            long exactMethod = exactMethods[i];
            int classIndex = (int) (exactMethod >>> 32);
            if (classIndex != lastClass) {
                if (lastClass >= 0) {
                    ClassMetadata clsData = classesMetadata.get(lastClass);
                    clsData.methods = methods.getAll();
                    methods.clear();
                }
                lastClass = classIndex;
            }
            int methodIndex = (int) exactMethod;
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
                    if (superclsData.methods != null && Arrays.binarySearch(superclsData.methods, methodIndex) >= 0) {
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
        long entry = ((long) classIndex << 32) | methodIndex;
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
                int layerIndex = autodetectLayer(loc);
                int line = valueByKey(layers[layerIndex].lineMapping, loc);
                int fileId = valueByKey(layers[layerIndex].fileMapping, loc);
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

    static GeneratedLocation key(RecordArray.Record record) {
        return new GeneratedLocation(record.get(0), record.get(1));
    }

    private void rebuildClassMap() {
        classMetadataByJsName = new HashMap<>();
        for (DebugInformation.ClassMetadata cls : classesMetadata) {
            if (cls.jsName != null) {
                classMetadataByJsName.put(cls.jsName, cls);
            }
        }
    }

    private int binarySearchLocation(RecordArray array, int row, int column) {
        int l = 0;
        int u = array.size() - 1;
        while (true) {
            int i = (l + u) / 2;
            RecordArray.Record e = array.get(i);
            int cmp = Integer.compare(row, e.get(0));
            if (cmp == 0) {
                cmp = Integer.compare(column, e.get(1));
            }
            if (cmp == 0) {
                return i;
            } else if (cmp < 0) {
                u = i - 1;
                if (u < l) {
                    return -i - 1;
                }
            } else {
                l = i + 1;
                if (l > u) {
                    return -i - 2;
                }
            }
        }
    }

    static class LocationList extends AbstractList<GeneratedLocation> implements RandomAccess {
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
        int id;
        Integer parentId;
        Map<Integer, Integer> fieldMap = new HashMap<>();
        int[] methods;
        String jsName;
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
                int classIndex = (int) (item >>> 32);
                int methodIndex = (int) item;
                references[i] = referenceCache.getCached(classNames[classIndex],
                        referenceCache.parseDescriptorCached(methods[methodIndex]));
            }
            return references;
        }
    }

    static class Layer {
        RecordArray fileMapping;
        RecordArray classMapping;
        RecordArray methodMapping;
        RecordArray lineMapping;
    }
}
