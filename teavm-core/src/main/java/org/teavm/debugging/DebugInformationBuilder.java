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

import java.util.*;
import org.teavm.codegen.LocationProvider;
import org.teavm.common.IntegerArray;
import org.teavm.common.RecordArray;
import org.teavm.common.RecordArrayBuilder;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class DebugInformationBuilder implements DebugInformationEmitter {
    private LocationProvider locationProvider;
    private DebugInformation debugInformation;
    private MappedList files = new MappedList();
    private MappedList classes = new MappedList();
    private MappedList fields = new MappedList();
    private MappedList methods = new MappedList();
    private MappedList variableNames = new MappedList();
    private List<Long> exactMethods = new ArrayList<>();
    private Map<Long, Integer> exactMethodMap = new HashMap<>();
    private RecordArrayBuilder fileMapping = new RecordArrayBuilder(3, 0);
    private RecordArrayBuilder lineMapping = new RecordArrayBuilder(3, 0);
    private RecordArrayBuilder classMapping = new RecordArrayBuilder(3, 0);
    private RecordArrayBuilder methodMapping = new RecordArrayBuilder(3, 0);
    private RecordArrayBuilder callSiteMapping = new RecordArrayBuilder(3, 1);
    private Map<Integer, RecordArrayBuilder> variableMappings = new HashMap<>();
    private MethodDescriptor currentMethod;
    private String currentClass;
    private String currentFileName;
    private int currentClassMetadata = -1;
    private List<ClassMetadata> classesMetadata = new ArrayList<>();
    private List<CFG> cfgs = new ArrayList<>();
    private int currentLine;

    public LocationProvider getLocationProvider() {
        return locationProvider;
    }

    @Override
    public void setLocationProvider(LocationProvider locationProvider) {
        this.locationProvider = locationProvider;
    }

    @Override
    public void emitLocation(String fileName, int line) {
        debugInformation = null;
        int fileIndex = files.index(fileName);
        if (!Objects.equals(currentFileName, fileName)) {
            add(fileMapping, fileIndex);
            currentFileName = fileName;
        }
        if (currentLine != line) {
            add(lineMapping, line);
            currentLine = line;
        }
    }

    private  RecordArrayBuilder.Record add(RecordArrayBuilder builder) {
        if (builder.size() > 1) {
            RecordArrayBuilder.Record lastRecord = builder.get(builder.size() - 1);
            if (lastRecord.get(0) == locationProvider.getLine() && lastRecord.get(1) == locationProvider.getColumn()) {
                return lastRecord;
            }
        }
        RecordArrayBuilder.Record record = builder.add();
        record.set(0, locationProvider.getLine());
        record.set(1, locationProvider.getColumn());
        return record;
    }

    private  RecordArrayBuilder.Record add(RecordArrayBuilder builder, int value) {
        RecordArrayBuilder.Record record = add(builder);
        record.set(2, value);
        return record;
    }

    @Override
    public void emitClass(String className) {
        debugInformation = null;
        int classIndex = classes.index(className);
        if (!Objects.equals(className, currentClass)) {
            add(classMapping, classIndex);
            currentClass = className;
        }
    }

    @Override
    public void emitMethod(MethodDescriptor method) {
        debugInformation = null;
        int methodIndex = methods.index(method != null ? method.toString() : null);
        if (!Objects.equals(method, currentMethod)) {
            add(methodMapping, methodIndex);
            currentMethod = method;
        }
        if (currentClass != null) {
            int classIndex = classes.index(currentClass);
            long fullIndex = ((long)classIndex << 32) | methodIndex;
            if (!exactMethodMap.containsKey(fullIndex)) {
                exactMethodMap.put(fullIndex, exactMethods.size());
                exactMethods.add(fullIndex);
            }
        }
    }

    @Override
    public void emitVariable(String[] sourceNames, String generatedName) {
        int[] sourceIndexes = new int[sourceNames.length];
        for (int i = 0; i < sourceIndexes.length; ++i) {
            sourceIndexes[i] = variableNames.index(sourceNames[i]);
        }
        Arrays.sort(sourceIndexes);
        int generatedIndex = variableNames.index(generatedName);
        RecordArrayBuilder mapping = variableMappings.get(generatedIndex);
        if (mapping == null) {
            mapping = new RecordArrayBuilder(2, 1);
            variableMappings.put(generatedIndex, mapping);
        }

        RecordArrayBuilder.Record record = add(mapping);
        RecordArrayBuilder.RecordSubArray array = record.getArray(0);
        for (int sourceIndex : sourceIndexes) {
            array.add(sourceIndex);
        }
    }

    @Override
    public DeferredCallSite emitCallSite() {
        final RecordArrayBuilder.Record record = add(callSiteMapping, DebuggerCallSite.NONE);
        DeferredCallSite callSite = new DeferredCallSite() {
            @Override
            public void setVirtualMethod(MethodReference method) {
                record.set(2, DebuggerCallSite.VIRTUAL);
                RecordArrayBuilder.RecordSubArray array = record.getArray(0);
                array.clear();
                array.add(getExactMethodIndex(method));
            }
            @Override
            public void setStaticMethod(MethodReference method) {
                record.set(2, DebuggerCallSite.STATIC);
                RecordArrayBuilder.RecordSubArray array = record.getArray(0);
                array.clear();
                array.add(getExactMethodIndex(method));
            }
            @Override
            public void clean() {
                record.set(2, DebuggerCallSite.NONE);
            }
            private int getExactMethodIndex(MethodReference method) {
                int methodIndex = methods.index(method.getDescriptor().toString());
                int classIndex = classes.index(method.getClassName());
                long fullIndex = ((long)classIndex << 32) | methodIndex;
                Integer exactMethodIndex = exactMethodMap.get(fullIndex);
                if (exactMethodIndex == null) {
                    exactMethodIndex = exactMethods.size();
                    exactMethodMap.put(fullIndex, exactMethodIndex);
                    exactMethods.add(fullIndex);
                }
                return exactMethodIndex;
            }
        };
        return callSite;
    }

    @Override
    public void addClass(String className, String parentName) {
        int classIndex = classes.index(className);
        int parentIndex = classes.index(parentName);
        while (classIndex >= classesMetadata.size()) {
            classesMetadata.add(new ClassMetadata());
        }
        currentClassMetadata = classIndex;
        classesMetadata.get(currentClassMetadata).parentIndex = parentIndex;
    }

    @Override
    public void addField(String fieldName, String jsName) {
        ClassMetadata metadata = classesMetadata.get(currentClassMetadata);
        int fieldIndex = fields.index(fieldName);
        int jsIndex = fields.index(jsName);
        metadata.fieldMap.put(jsIndex, fieldIndex);
    }

    @Override
    public void addSuccessors(SourceLocation location, SourceLocation[] successors) {
        int fileIndex = files.index(location.getFileName());
        if (cfgs.size() <= fileIndex) {
            cfgs.addAll(Collections.<CFG>nCopies(fileIndex - cfgs.size() + 1, null));
        }
        CFG cfg = cfgs.get(fileIndex);
        if (cfg == null) {
            cfg = new CFG();
            cfgs.set(fileIndex, cfg);
        }
        for (SourceLocation succ : successors) {
            if (succ == null) {
                cfg.add(location.getLine(), -1, fileIndex);
            } else {
                cfg.add(location.getLine(), succ.getLine(), files.index(succ.getFileName()));
            }
        }
    }

    private RecordArrayBuilder compress(RecordArrayBuilder builder) {
        int lastValue = 0;
        RecordArrayBuilder compressed = new RecordArrayBuilder(builder.getRecordSize(), builder.getArraysPerRecord());
        for (int i = 0; i < builder.size(); ++i) {
            RecordArrayBuilder.Record record = builder.get(i);
            if (i == 0 || lastValue != record.get(2)) {
                RecordArrayBuilder.Record compressedRecord = compressed.add();
                for (int j = 0; j < builder.getRecordSize(); ++j) {
                    compressedRecord.set(j, record.get(j));
                }
            }
        }
        return compressed;
    }

    private void compressAndSortArrays(RecordArrayBuilder builder) {
        for (int i = 0; i < builder.size(); ++i) {
            RecordArrayBuilder.Record record = builder.get(i);
            for (int j = 0; j < builder.getArraysPerRecord(); ++j) {
                RecordArrayBuilder.RecordSubArray array = record.getArray(j);
                int[] data = array.getData();
                Arrays.sort(data);
                array.clear();
                if (data.length > 0) {
                    int last = data[0];
                    array.add(last);
                    for (int k = 1; k < data.length; ++k) {
                        if (data[k] != last) {
                            last = data[k];
                            array.add(last);
                        }
                    }
                }
            }
        }
    }

    public DebugInformation getDebugInformation() {
        if (debugInformation == null) {
            debugInformation = new DebugInformation();

            debugInformation.fileNames = files.getItems();
            debugInformation.classNames = classes.getItems();
            debugInformation.fields = fields.getItems();
            debugInformation.methods = methods.getItems();
            debugInformation.variableNames = variableNames.getItems();
            debugInformation.exactMethods = new long[exactMethods.size()];
            for (int i = 0; i < exactMethods.size(); ++i) {
                debugInformation.exactMethods[i] = exactMethods.get(i);
            }
            debugInformation.exactMethodMap = new HashMap<>(exactMethodMap);

            debugInformation.fileMapping = compress(fileMapping).build();
            debugInformation.lineMapping = compress(lineMapping).build();
            debugInformation.classMapping = compress(classMapping).build();
            debugInformation.methodMapping = compress(methodMapping).build();
            debugInformation.callSiteMapping = callSiteMapping.build();
            debugInformation.variableMappings = new RecordArray[variableNames.list.size()];
            for (int var : variableMappings.keySet()) {
                RecordArrayBuilder mapping = variableMappings.get(var);
                compressAndSortArrays(mapping);
                debugInformation.variableMappings[var] = mapping.build();
            }

            List<DebugInformation.ClassMetadata> builtMetadata = new ArrayList<>(classes.list.size());
            for (int i = 0; i < classes.list.size(); ++i) {
                if (i >= classesMetadata.size()) {
                    builtMetadata.add(new DebugInformation.ClassMetadata());
                } else {
                    ClassMetadata origMetadata = classesMetadata.get(i);
                    DebugInformation.ClassMetadata mappedMetadata = new DebugInformation.ClassMetadata();
                    mappedMetadata.fieldMap.putAll(origMetadata.fieldMap);
                    mappedMetadata.parentId = origMetadata.parentIndex >= 0 ? origMetadata.parentIndex : null;
                    builtMetadata.add(mappedMetadata);
                }
            }
            debugInformation.classesMetadata = builtMetadata;

            DebugInformation.CFG[] cfgs = new DebugInformation.CFG[files.list.size()];
            for (int i = 0; i < this.cfgs.size(); ++i) {
                if (this.cfgs.get(i) != null) {
                    cfgs[i] = this.cfgs.get(i).build();
                }
            }
            debugInformation.controlFlowGraphs = cfgs;
            debugInformation.rebuild();
        }
        return debugInformation;
    }

    static class MappedList {
        private List<String> list = new ArrayList<>();
        private Map<String, Integer> map = new HashMap<>();

        public int index(String item) {
            if (item == null) {
                return -1;
            }
            Integer index = map.get(item);
            if (index == null) {
                index = list.size();
                list.add(item);
                map.put(item, index);
            }
            return index;
        }

        public String[] getItems() {
            return list.toArray(new String[list.size()]);
        }

        public Map<String, Integer> getIndexes() {
            return new HashMap<>(map);
        }
    }

    static class ClassMetadata {
        int parentIndex;
        Map<Integer, Integer> fieldMap = new HashMap<>();
    }

    static class CFG {
        IntegerArray start = new IntegerArray(1);
        IntegerArray next = new IntegerArray(1);
        IntegerArray lines = new IntegerArray(1);
        IntegerArray files = new IntegerArray(1);

        public void add(int line, int succLine, int succFile) {
            while (start.size() <= line) {
                start.add(-1);
            }
            int ptr = start.get(line);
            start.set(line, lines.size());
            next.add(ptr);
            lines.add(succLine);
            files.add(succFile);
        }

        public DebugInformation.CFG build() {
            int[] offsets = new int[start.size() + 1];
            IntegerArray linesData = new IntegerArray(1);
            IntegerArray filesData = new IntegerArray(1);
            for (int i = 0; i < start.size(); ++i) {
                IntegerArray linesChunk = new IntegerArray(1);
                IntegerArray filesChunk = new IntegerArray(1);
                int ptr = start.get(i);
                while (ptr >= 0) {
                    linesChunk.add(lines.get(ptr));
                    filesChunk.add(files.get(ptr));
                    ptr = next.get(ptr);
                }
                long[] pairs = new long[linesChunk.size()];
                for (int j = 0; j < pairs.length; ++j) {
                    pairs[j] = (((long)filesChunk.get(j)) << 32) | linesChunk.get(j);
                }
                Arrays.sort(pairs);
                int distinctSize = 0;
                for (int j = 0; j < pairs.length; ++j) {
                    long pair = pairs[j];
                    if (distinctSize == 0 || pair != pairs[distinctSize - 1]) {
                        pairs[distinctSize++] = pair;
                        filesData.add((int)(pair >>> 32));
                        linesData.add((int)pair);
                    }
                }
                offsets[i + 1] = linesData.size();
            }
            DebugInformation.CFG cfg = new DebugInformation.CFG();
            cfg.offsets = offsets;
            cfg.lines = linesData.getAll();
            cfg.files = filesData.getAll();
            return cfg;
        }
    }

}
