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

/**
 *
 * @author Alexey Andreev
 */
public class DebugInformationBuilder implements DebugInformationEmitter {
    private LocationProvider locationProvider;
    private DebugInformation debugInformation;
    private MappedList files = new MappedList();
    private MappedList classes = new MappedList();
    private MappedList methods = new MappedList();
    private Mapping fileMapping = new Mapping();
    private Mapping lineMapping = new Mapping();
    private Mapping classMapping = new Mapping();
    private Mapping methodMapping = new Mapping();
    private MethodDescriptor currentMethod;
    private String currentClass;
    private String currentFileName;
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

    public DebugInformation getDebugInformation() {
        if (debugInformation == null) {
            debugInformation = new DebugInformation();

            debugInformation.fileNames = files.getItems();
            debugInformation.fileNameMap = files.getIndexes();
            debugInformation.classNames = classes.getItems();
            debugInformation.classNameMap = classes.getIndexes();
            debugInformation.methods = methods.getItems();
            debugInformation.methodMap = methods.getIndexes();

            debugInformation.fileMapping = fileMapping.build();
            debugInformation.lineMapping = lineMapping.build();
            debugInformation.classMapping = classMapping.build();
            debugInformation.methodMapping = methodMapping.build();

            debugInformation.rebuildFileDescriptions();
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
}
