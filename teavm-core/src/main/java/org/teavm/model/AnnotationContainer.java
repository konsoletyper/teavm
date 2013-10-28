package org.teavm.model;

import java.util.Map;
import java.util.HashMap;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class AnnotationContainer {
    private Map<String, AnnotationHolder> annotations = new HashMap<>();

    public void add(AnnotationHolder annotation) {
        if (annotations.containsKey(annotation.getType())) {
            throw new IllegalArgumentException("Annotation of type " + annotation.getType() + " is already there");
        }
        annotations.put(annotation.getType(), annotation);
    }

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
}
