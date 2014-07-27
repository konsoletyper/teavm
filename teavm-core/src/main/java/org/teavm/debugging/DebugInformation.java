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
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class DebugInformation {
    String[] fileNames;
    Map<String, Integer> fileNameMap;
    FileDescription[] fileDescriptions;
    // TODO: for less memory consumption replace with two arrays + custom binary search
    GeneratedLocation[] fileNameKeys;
    int[] fileNameValues;
    GeneratedLocation[] lineNumberKeys;
    int[] lineNumberValues;

    public Collection<GeneratedLocation> getGeneratedLocations(String fileName, int line) {
        Integer fileIndex = fileNameMap.get(fileName);
        if (fileIndex == null) {
            return Collections.emptyList();
        }
        FileDescription description = fileIndex >= 0 ? fileDescriptions[fileIndex] : null;
        if (description == null) {
            return null;
        }
        GeneratedLocation[] locations = line < description.generatedLocations.length ?
                description.generatedLocations[line] : null;
        return locations != null ? Arrays.asList(locations) : Collections.<GeneratedLocation>emptyList();
    }

    public Collection<GeneratedLocation> getGeneratedLocations(SourceLocation sourceLocation) {
        return getGeneratedLocations(sourceLocation.getFileName(), sourceLocation.getLine());
    }

    public SourceLocation getSourceLocation(int line, int column) {
        return getSourceLocation(new GeneratedLocation(line, column));
    }

    public SourceLocation getSourceLocation(GeneratedLocation generatedLocation) {
        String fileName = componentByKey(fileNameKeys, fileNameValues, fileNames, generatedLocation);
        int lineNumberIndex = indexByKey(lineNumberKeys, generatedLocation);
        int lineNumber = lineNumberIndex >= 0 ? lineNumberValues[lineNumberIndex] : -1;
        return new SourceLocation(fileName, lineNumber);
    }

    public MethodReference getMethodAt(String fileName, int line) {
        if (line < 0) {
            return null;
        }
        Integer fileIndex = fileNameMap.get(fileName);
        if (fileIndex == null) {
            return null;
        }
        FileDescription description = fileDescriptions[fileIndex];
        if (description == null) {
            return null;
        }
        return line < description.methodMap.length ? description.methodMap[line] : null;
    }

    public MethodReference getMethodAt(SourceLocation sourceLocation) {
        return getMethodAt(sourceLocation.getFileName(), sourceLocation.getLine());
    }

    private <T> T componentByKey(GeneratedLocation[] keys, int[] valueIndexes, T[] values,
            GeneratedLocation location) {
        int keyIndex = indexByKey(keys, location);
        int valueIndex = keyIndex >= 0 ? valueIndexes[keyIndex] : -1;
        return valueIndex >= 0 ? values[valueIndex] : null;
    }

    private int indexByKey(GeneratedLocation[] keys, GeneratedLocation location) {
        int index = Arrays.binarySearch(keys, location);
        return index >= 0 ? index : -index - 2;
    }

    public void write(OutputStream output) throws IOException {
        DebugInformationWriter writer = new DebugInformationWriter(new DataOutputStream(output));
        writer.write(this);
    }

    static class FileDescription {
        GeneratedLocation[][] generatedLocations;
        MethodReference[] methodMap;
    }
}
