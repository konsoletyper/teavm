/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.model;

import java.util.HashMap;
import java.util.Map;

public class AnnotationContainer implements AnnotationContainerReader {
    private Map<String, AnnotationHolder> annotations = new HashMap<>();

    public void add(AnnotationHolder annotation) {
        if (annotations.containsKey(annotation.getType())) {
            throw new IllegalArgumentException("Annotation of type " + annotation.getType() + " is already there");
        }
        annotations.put(annotation.getType(), annotation);
    }

    @Override
    public AnnotationHolder get(String type) {
        return annotations.get(type);
    }

    public void remove(AnnotationHolder annotation) {
        AnnotationHolder existingAnnot = get(annotation.getType());
        if (existingAnnot != annotation) {
            throw new IllegalArgumentException("There is no such annotation");
        }
        annotations.remove(annotation.getType());
    }

    public void remove(String type) {
        annotations.remove(type);
    }

    @Override
    public Iterable<AnnotationHolder> all() {
        return annotations.values();
    }
}
