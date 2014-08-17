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
    private Mapping fileMapping = new Mapping();
    private Mapping lineMapping = new Mapping();
    private Mapping classMapping = new Mapping();
    private Mapping methodMapping = new Mapping();
    private Mapping callSiteMapping = new Mapping();
    private Map<Integer, MultiMapping> variableMappings = new HashMap<>();
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
            fileMapping.add(locationProvider, fileIndex);
            currentFileName = fileName;
        }
        if (currentLine != line) {
            lineMapping.add(locationProvider, line);
            currentLine = line;
        }
    }

    @Override
    public void emitClass(String className) {
        debugInformation = null;
        int classIndex = classes.index(className);
        if (!Objects.equals(className, currentClass)) {
            classMapping.add(locationProvider, classIndex);
            currentClass = className;
        }
    }

    @Override
    public void emitMethod(MethodDescriptor method) {
        debugInformation = null;
        int methodIndex = methods.index(method != null ? method.toString() : null);
        if (!Objects.equals(method, currentMethod)) {
            methodMapping.add(locationProvider, methodIndex);
            currentMethod = method;
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
        MultiMapping mapping = variableMappings.get(generatedIndex);
        if (mapping == null) {
            mapping = new MultiMapping();
            variableMappings.put(generatedIndex, mapping);
        }
        mapping.add(locationProvider, sourceIndexes);
    }

    @Override
    public void emitCallSite(MethodReference method) {
        if (method != null) {
            int methodIndex = methods.index(method.getDescriptor().toString());
            int classIndex = classes.index(method.getClassName());
            long fullIndex = (((long)classIndex << 32) | methodIndex) + 1;
            Integer exactMethodIndex = exactMethodMap.get(fullIndex);
            if (exactMethodIndex == null) {
                exactMethodIndex = exactMethods.size();
                exactMethodMap.put(fullIndex, exactMethodIndex);
                exactMethods.add(fullIndex);
            }
            callSiteMapping.add(locationProvider, exactMethodIndex);
        } else {
            callSiteMapping.add(locationProvider, -1);
        }
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

            debugInformation.fileMapping = fileMapping.build();
            debugInformation.lineMapping = lineMapping.build();
            debugInformation.classMapping = classMapping.build();
            debugInformation.methodMapping = methodMapping.build();
            debugInformation.callSiteMapping = callSiteMapping.build();
            debugInformation.variableMappings = new DebugInformation.MultiMapping[variableNames.list.size()];
            for (int var : variableMappings.keySet()) {
                MultiMapping mapping = variableMappings.get(var);
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

            debugInformation.rebuildFileDescriptions();
            debugInformation.rebuildMaps();
        }
        return debugInformation;
    }

    static class Mapping {
        IntegerArray lines = new IntegerArray(1);
        IntegerArray columns = new IntegerArray(1);
        IntegerArray values = new IntegerArray(1);

        public void add(LocationProvider location, int value) {
            if (lines.size() > 1) {
                int last = lines.size() - 1;
                if (lines.get(last) == location.getLine() && columns.get(last) == location.getColumn()) {
                    values.set(last, value);
                    // TODO: check why this gives an invalid result
                    /*if (values.get(last) == values.get(last - 1)) {
                        values.remove(last);
                        lines.remove(last);
                        columns.remove(last);
                    }*/
                    return;
                }
            }
            lines.add(location.getLine());
            columns.add(location.getColumn());
            values.add(value);
        }

        DebugInformation.Mapping build() {
            return new DebugInformation.Mapping(lines.getAll(), columns.getAll(), values.getAll());
        }
    }

    static class MultiMapping {
        IntegerArray lines = new IntegerArray(1);
        IntegerArray columns = new IntegerArray(1);
        IntegerArray offsets = new IntegerArray(1);
        IntegerArray data = new IntegerArray(1);

        public MultiMapping() {
            offsets.add(0);
        }

        public void add(LocationProvider location, int[] values) {
            if (lines.size() > 1) {
                int last = lines.size() - 1;
                if (lines.get(last) == location.getLine() && columns.get(last) == location.getColumn()) {
                    addToLast(values);
                    return;
                }
            }
            lines.add(location.getLine());
            columns.add(location.getColumn());
            data.addAll(values);
            offsets.add(data.size());
        }

        private void addToLast(int[] values) {
            int start = offsets.get(offsets.size() - 2);
            int end = offsets.get(offsets.size() - 1);
            int[] existing = data.getRange(start, end);
            values = merge(existing, values);
            if (values.length == existing.length) {
                return;
            }
            data.remove(start, end - start);
            data.addAll(values);
            offsets.set(offsets.size() - 1, data.size());
        }

        private int[] merge(int[] a, int[] b) {
            int[] result = new int[a.length + b.length];
            int i = 0;
            int j = 0;
            int k = 0;
            while (i < a.length && j < b.length) {
                int p = a[i];
                int q = b[j];
                if (p == q) {
                    result[k++] = p;
                    ++i;
                    ++j;
                } else if (p < q) {
                    result[k++] = p;
                    ++i;
                } else {
                    result[k++] = q;
                    ++j;
                }
            }
            while (i < a.length) {
                result[k++] = a[i++];
            }
            while (j < b.length) {
                result[k++] = b[j++];
            }
            return k < result.length ? Arrays.copyOf(result, k) : result;
        }

        public DebugInformation.MultiMapping build() {
            return new DebugInformation.MultiMapping(lines.getAll(), columns.getAll(), offsets.getAll(),
                    data.getAll());
        }
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
