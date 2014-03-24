package org.teavm.model.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public final class ModelUtils {
    private ModelUtils() {
    }

    public static ClassHolder copyClass(ClassHolder original) {
        ClassHolder copy = new ClassHolder(original.getName());
        copy.setLevel(original.getLevel());
        copy.getModifiers().addAll(original.getModifiers());
        copy.setParent(original.getParent());
        copy.getInterfaces().addAll(original.getInterfaces());
        for (MethodHolder method : original.getMethods()) {
            copy.addMethod(copyMethod(method));
        }
        for (FieldHolder field : original.getFields()) {
            copy.addField(copyField(field));
        }
        copy.setOwnerName(original.getOwnerName());
        copyAnnotations(original.getAnnotations(), copy.getAnnotations());
        return copy;
    }

    public static MethodHolder copyMethod(MethodHolder method) {
        MethodHolder copy = new MethodHolder(method.getDescriptor());
        copy.setLevel(method.getLevel());
        copy.getModifiers().addAll(method.getModifiers());
        copy.setProgram(ProgramUtils.copy(method.getProgram()));
        copyAnnotations(method.getAnnotations(), copy.getAnnotations());
        return copy;
    }

    public static FieldHolder copyField(FieldHolder field) {
        FieldHolder copy = new FieldHolder(field.getName());
        copy.setLevel(field.getLevel());
        copy.getModifiers().addAll(field.getModifiers());
        copy.setType(field.getType());
        copy.setInitialValue(field.getInitialValue());
        copyAnnotations(field.getAnnotations(), copy.getAnnotations());
        return copy;
    }

    private static void copyAnnotations(AnnotationContainer src, AnnotationContainer dst) {
        for (AnnotationHolder annot : src.all()) {
            dst.add(copyAnnotation(annot));
        }
    }

    private static AnnotationHolder copyAnnotation(AnnotationHolder annot) {
        AnnotationHolder copy = new AnnotationHolder(annot.getType());
        for (Map.Entry<String, AnnotationValue> entry : annot.getValues().entrySet()) {
            copy.getValues().put(entry.getKey(), copyAnnotationValue(entry.getValue()));
        }
        return copy;
    }

    private static AnnotationValue copyAnnotationValue(AnnotationValue value) {
        switch (value.getType()) {
            case AnnotationValue.LIST: {
                List<AnnotationValue> listCopy = new ArrayList<>();
                for (AnnotationValue item : value.getList()) {
                    listCopy.add(copyAnnotationValue(item));
                }
                return new AnnotationValue(listCopy);
            }
            case AnnotationValue.ANNOTATION:
                return new AnnotationValue(copyAnnotation(value.getAnnotation()));
            default:
                return value;
        }
    }
}
