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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnnotationContainer implements AnnotationContainerReader {
    private Map<String, AnnotationHolder> annotations;
    private List<AnnotationHolder> annotationsList;

    public void add(AnnotationHolder annotation) {
        if (annotations == null) {
            annotations = new LinkedHashMap<>();
        }
        if (annotationsList == null) {
            annotationsList = new ArrayList<>();
        }
        if (annotations.containsKey(annotation.getType())) {
            throw new IllegalArgumentException("Annotation of type " + annotation.getType() + " is already there");
        }
        annotations.put(annotation.getType(), annotation);
        annotationsList.add(annotation);
    }

    @Override
    public AnnotationHolder get(String type) {
        return annotations != null ? annotations.get(type) : null;
    }

    public void remove(AnnotationHolder annotation) {
        AnnotationHolder existingAnnot = get(annotation.getType());
        if (existingAnnot != annotation) {
            throw new IllegalArgumentException("There is no such annotation");
        }
        annotations.remove(annotation.getType(), annotation);
        annotationsList.remove(annotation);
    }

    public void remove(String type) {
        if (annotations != null) {
            annotations.remove(type);
        }
        annotationsList.removeIf(a -> a.getType().equals(type));
    }

    public void removeAll() {
        annotations = null;
        annotationsList = null;
    }

    @Override
    public Iterable<AnnotationHolder> all() {
        return annotationsList != null ? annotationsList : Collections.emptyList();
    }
}
