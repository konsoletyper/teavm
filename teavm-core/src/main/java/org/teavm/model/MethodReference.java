package org.teavm.model;

/**
 *
 * @author konsoletyper
 */
public class MethodReference {
    private String className;
    private MethodDescriptor descriptor;

    public MethodReference(String className, MethodDescriptor descriptor) {
        this.className = className;
        this.descriptor = descriptor;
    }

    public String getClassName() {
        return className;
    }

    public MethodDescriptor getDescriptor() {
        return descriptor;
    }

    public int parameterCount() {
        return descriptor.getParameterTypes().length;
    }

    @Override
    public int hashCode() {
        return className.hashCode() ^ descriptor.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return false;
        }
        if (!(obj instanceof MethodReference)) {
            return false;
        }
        MethodReference other = (MethodReference)obj;
        return className.equals(other.className) && descriptor.equals(other.descriptor);
    }

    @Override
    public String toString() {
        return className + "." + descriptor;
    }
}
