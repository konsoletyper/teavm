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
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class DebugInformationBuilder implements DebugInformationEmitter {
    private LocationProvider locationProvider;
    private DebugInformation debugInformation;
    private List<String> fileNames = new ArrayList<>();
    private Map<String, Integer> fileNameMap = new HashMap<>();
    private List<Entry> fileNameEntries = new ArrayList<>();
    private List<Entry> lineNumberEntries = new ArrayList<>();
    private MethodReference currentMethod;
    private String currentFileName;
    private int currentLine;
    private List<FileDescriptionProto> fileDescriptions = new ArrayList<>();

    public LocationProvider getLocationProvider() {
        return locationProvider;
    }

    public void setLocationProvider(LocationProvider locationProvider) {
        this.locationProvider = locationProvider;
    }

    private GeneratedLocation getGeneratedLocation() {
        return new GeneratedLocation(locationProvider.getLine(), locationProvider.getColumn());
    }

    @Override
    public void emitLocation(String fileName, int line) {
        Integer fileIndex;
        if (fileName != null) {
            fileIndex = fileNameMap.get(fileName);
            if (fileIndex == null) {
                fileIndex = fileNames.size();
                fileNames.add(fileName);
                fileNameMap.put(fileName, fileIndex);
                fileDescriptions.add(new FileDescriptionProto());
            }
        } else {
            fileIndex = -1;
        }
        if (currentFileName != fileName) {
            fileNameEntries.add(new Entry(getGeneratedLocation(), fileIndex));
        }
        if (currentLine != line) {
            lineNumberEntries.add(new Entry(getGeneratedLocation(), line));
        }
        if (fileName != null && line >= 0 && (currentFileName != fileName || currentLine != line)) {
            FileDescriptionProto fileDesc = fileDescriptions.get(fileIndex);
            fileDesc.setMethod(line, currentMethod);
            fileDesc.addGeneratedLocation(line, getGeneratedLocation());
        }
    }

    @Override
    public void emitMethod(MethodReference method) {
        currentMethod = method;
    }

    public DebugInformation getDebugInformation() {
        if (debugInformation == null) {
            debugInformation = new DebugInformation();

            debugInformation.fileNames = fileNames.toArray(new String[0]);
            debugInformation.fileNameMap = new HashMap<>(fileNameMap);

            debugInformation.fileNameKeys = new GeneratedLocation[fileNameEntries.size()];
            debugInformation.fileNameValues = new int[fileNameEntries.size()];
            int index = 0;
            for (Entry entry : fileNameEntries) {
                debugInformation.fileNameKeys[index] = entry.key;
                debugInformation.fileNameValues[index] = entry.value;
                index++;
            }

            debugInformation.lineNumberKeys = new GeneratedLocation[lineNumberEntries.size()];
            debugInformation.lineNumberValues = new int[lineNumberEntries.size()];
            index = 0;
            for (Entry entry : lineNumberEntries) {
                debugInformation.lineNumberKeys[index] = entry.key;
                debugInformation.lineNumberValues[index] = entry.value;
                index++;
            }

            debugInformation.fileDescriptions = new DebugInformation.FileDescription[fileDescriptions.size()];
            index = 0;
            for (FileDescriptionProto fileDescProto : fileDescriptions) {
                DebugInformation.FileDescription fileDesc = new DebugInformation.FileDescription();
                debugInformation.fileDescriptions[index++] = fileDesc;
                fileDesc.methodMap = fileDescProto.methodMap.toArray(new MethodReference[0]);
                fileDesc.generatedLocations = new GeneratedLocation[fileDescProto.generatedLocations.size()][];
                for (int i = 0; i < fileDescProto.generatedLocations.size(); ++i) {
                    fileDesc.generatedLocations[i] = fileDescProto.generatedLocations.get(index)
                            .toArray(new GeneratedLocation[0]);
                }
            }
        }
        return debugInformation;
    }

    static class FileDescriptionProto {
        List<List<GeneratedLocation>> generatedLocations = new ArrayList<>();
        List<MethodReference> methodMap = new ArrayList<>();

        void addGeneratedLocation(int line, GeneratedLocation location) {
            if (line >= generatedLocations.size()) {
                generatedLocations.addAll(Collections.<List<GeneratedLocation>>nCopies(
                        line - generatedLocations.size() + 1, null));
            }
            List<GeneratedLocation> existingLocations = generatedLocations.get(line);
            existingLocations.add(location);
        }

        void setMethod(int line, MethodReference method) {
            if (line >= methodMap.size()) {
                methodMap.addAll(Collections.<MethodReference>nCopies(line - methodMap.size() + 1, null));
            }
            methodMap.set(line, method);
        }
    }

    static class Entry {
        GeneratedLocation key;
        int value;

        public Entry(GeneratedLocation key, int value) {
            this.key = key;
            this.value = value;
        }
    }
}
