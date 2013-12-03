package org.teavm.classlib.java.lang;

import org.teavm.javascript.ni.GeneratedBy;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TClass<T> extends TObject {
    String name;
    boolean primitive;
    boolean array;
    private TClass<?> componentType;
    private boolean componentTypeDirty = true;

    static TClass<?> createNew() {
        return new TClass<>();
    }

    @GeneratedBy(ClassNativeGenerator.class)
    public native boolean isInstance(TObject obj);

    public String getName() {
        return name;
    }

    public boolean isPrimitive() {
        return primitive;
    }

    public boolean isArray() {
        return array;
    }

    public TClass<?> getComponentType() {
        if (componentTypeDirty) {
            componentType = getComponentType0();
            componentTypeDirty = false;
        }
        return componentType;
    }

    @GeneratedBy(ClassNativeGenerator.class)
    private native TClass<?> getComponentType0();
}
