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
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class DebugInformation {
    List<String> fileNames = new ArrayList<>();
    Map<String, Integer> fileNameMap = new HashMap<>();
    List<String> classNames = new ArrayList<>();
    Map<String, Integer> classNameMap = new HashMap<>();
    List<MethodDescriptor> methods = new ArrayList<>();
    Map<MethodDescriptor, Integer> methodMap = new HashMap<>();
    List<FileDescription> fileDescriptions = new ArrayList<>();
    GeneratedLocation[] fileNameKeys;
    int[] fileNameValues;
    GeneratedLocation[] classKeys;
    int[] classValues;
    GeneratedLocation[] methodKeys;
    int[] methodValues;
    GeneratedLocation[] lineNumberKeys;
    int[] lineNumberValues;

    public Collection<GeneratedLocation> getGeneratedLocations(String fileName, int lineNumber) {
        Integer fileIndex = fileNameMap.get(fileName);
        if (fileIndex == null) {
            return Collections.emptyList();
        }
        FileDescription description = fileDescriptions.get(lineNumber);
        return lineNumber < description.generatedLocations.length ?
                Arrays.asList(description.generatedLocations[lineNumber]) :
                Collections.<GeneratedLocation>emptyList();
    }

    public SourceLocation getSourceLocation(int line, int column) {
        return getSourceLocation(new GeneratedLocation(line, column));
    }

    public SourceLocation getSourceLocation(GeneratedLocation generatedLocation) {
        String fileName = componentByKey(fileNameKeys, fileNameValues, fileNames, generatedLocation);
        String className = componentByKey(classKeys, classValues, classNames, generatedLocation);
        MethodDescriptor method = componentByKey(methodKeys, methodValues, methods, generatedLocation);
        int lineNumberIndex = indexByKey(lineNumberKeys, generatedLocation);
        int lineNumber = lineNumberIndex >= 0 ? lineNumberValues[lineNumberIndex] : -1;
        return new SourceLocation(fileName, lineNumber, new MethodReference(className, method));
    }

    private <T> T componentByKey(GeneratedLocation[] keys, int[] valueIndexes, List<T> values,
            GeneratedLocation location) {
        int keyIndex = indexByKey(keys, location);
        int valueIndex = keyIndex >= 0 ? valueIndexes[keyIndex] : -1;
        return valueIndex >= 0 ? values.get(valueIndex) : null;
    }

    private int indexByKey(GeneratedLocation[] keys, GeneratedLocation location) {
        int index = Arrays.binarySearch(keys, location);
        return index >= 0 ? index : -index - 2;
    }

    class FileDescription {
        GeneratedLocation[][] generatedLocations;
    }
}
