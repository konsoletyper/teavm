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

import java.util.*;
import org.teavm.backend.javascript.codegen.LocationProvider;
import org.teavm.common.RecordArray;
import org.teavm.common.RecordArrayBuilder;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

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
    private RecordArrayBuilder statementStartMapping = new RecordArrayBuilder(2, 0);
    private RecordArrayBuilder fileMapping = new RecordArrayBuilder(3, 0);
    private RecordArrayBuilder lineMapping = new RecordArrayBuilder(3, 0);
    private RecordArrayBuilder classMapping = new RecordArrayBuilder(3, 0);
    private RecordArrayBuilder methodMapping = new RecordArrayBuilder(3, 0);
    private RecordArrayBuilder callSiteMapping = new RecordArrayBuilder(4, 0);
    private Map<Integer, RecordArrayBuilder> variableMappings = new HashMap<>();
    private MethodDescriptor currentMethod;
    private String currentClass;
    private String currentFileName;
    private int currentClassMetadata = -1;
    private List<ClassMetadata> classesMetadata = new ArrayList<>();
    private List<RecordArrayBuilder> cfgs = new ArrayList<>();
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

    private RecordArrayBuilder.Record add(RecordArrayBuilder builder) {
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
            long fullIndex = ((long) classIndex << 32) | methodIndex;
            if (!exactMethodMap.containsKey(fullIndex)) {
                exactMethodMap.put(fullIndex, exactMethods.size());
                exactMethods.add(fullIndex);
            }
        }
    }

    @Override
    public void emitStatementStart() {
        RecordArrayBuilder.Record record = statementStartMapping.add();
        record.set(0, locationProvider.getLine());
        record.set(1, locationProvider.getColumn());
    }

    @Override
    public void emitVariable(String[] sourceNames, String generatedName) {
        int[] sourceIndexes = new int[sourceNames.length];
        for (int i = 0; i < sourceIndexes.length; ++i) {
            sourceIndexes[i] = variableNames.index(sourceNames[i]);
        }
        Arrays.sort(sourceIndexes);
        int generatedIndex = variableNames.index(generatedName);
        RecordArrayBuilder mapping = variableMappings.computeIfAbsent(generatedIndex,
                k -> new RecordArrayBuilder(2, 1));

        RecordArrayBuilder.Record record = add(mapping);
        RecordArrayBuilder.SubArray array = record.getArray(0);
        for (int sourceIndex : sourceIndexes) {
            array.add(sourceIndex);
        }
    }

    @Override
    public DeferredCallSite emitCallSite() {
        final RecordArrayBuilder.Record record = add(callSiteMapping, DebuggerCallSite.NONE);
        return new DeferredCallSite() {
            @Override
            public void setVirtualMethod(MethodReference method) {
                record.set(2, DebuggerCallSite.VIRTUAL);
                record.set(3, getExactMethodIndex(method));
            }
            @Override
            public void setStaticMethod(MethodReference method) {
                record.set(2, DebuggerCallSite.STATIC);
                record.set(3, getExactMethodIndex(method));
            }
            @Override
            public void clean() {
                record.set(2, DebuggerCallSite.NONE);
                record.set(3, 0);
            }
            private int getExactMethodIndex(MethodReference method) {
                int methodIndex = methods.index(method.getDescriptor().toString());
                int classIndex = classes.index(method.getClassName());
                long fullIndex = ((long) classIndex << 32) | methodIndex;
                Integer exactMethodIndex = exactMethodMap.get(fullIndex);
                if (exactMethodIndex == null) {
                    exactMethodIndex = exactMethods.size();
                    exactMethodMap.put(fullIndex, exactMethodIndex);
                    exactMethods.add(fullIndex);
                }
                return exactMethodIndex;
            }
        };
    }

    @Override
    public void addClass(String jsName, String className, String parentName) {
        int classIndex = classes.index(className);
        int parentIndex = classes.index(parentName);
        while (classIndex >= classesMetadata.size()) {
            classesMetadata.add(new ClassMetadata());
        }
        currentClassMetadata = classIndex;

        ClassMetadata metadata = classesMetadata.get(classIndex);
        metadata.parentIndex = parentIndex;
        metadata.jsName = jsName;
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
        while (cfgs.size() <= fileIndex) {
            cfgs.add(new RecordArrayBuilder(1, 1));
        }
        RecordArrayBuilder cfg = cfgs.get(fileIndex);
        while (cfg.size() <= location.getLine()) {
            cfg.add();
        }
        RecordArrayBuilder.Record record = cfg.get(location.getLine());
        if (record.get(0) == 0) {
            record.set(0, 1);
        }
        RecordArrayBuilder.SubArray array = record.getArray(0);
        for (SourceLocation succ : successors) {
            if (succ == null) {
                record.set(0, 2);
            } else {
                array.add(files.index(succ.getFileName()));
                array.add(succ.getLine());
            }
        }
    }

    private RecordArrayBuilder compress(RecordArrayBuilder builder) {
        int lastValue = 0;
        RecordArrayBuilder compressed = new RecordArrayBuilder(builder.getRecordSize(), builder.getArraysPerRecord());
        for (int i = 0; i < builder.size(); ++i) {
            RecordArrayBuilder.Record record = builder.get(i);
            if (i == 0 || lastValue != record.get(2)) {
                lastValue = record.get(2);
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
                RecordArrayBuilder.SubArray array = record.getArray(j);
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

            debugInformation.statementStartMapping = statementStartMapping.build();
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
                DebugInformation.ClassMetadata mappedMetadata = new DebugInformation.ClassMetadata();
                mappedMetadata.id = i;
                if (i < classesMetadata.size()) {
                    ClassMetadata origMetadata = classesMetadata.get(i);
                    mappedMetadata.jsName = origMetadata.jsName;
                    mappedMetadata.fieldMap.putAll(origMetadata.fieldMap);
                    mappedMetadata.parentId = origMetadata.parentIndex >= 0 ? origMetadata.parentIndex : null;
                }
                builtMetadata.add(mappedMetadata);
            }
            debugInformation.classesMetadata = builtMetadata;

            RecordArray[] cfgs = new RecordArray[files.list.size()];
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
        String jsName;
        Map<Integer, Integer> fieldMap = new HashMap<>();
    }
}
