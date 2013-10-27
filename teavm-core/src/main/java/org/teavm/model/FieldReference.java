package org.teavm.model;

/**
 *
 * @author konsoletyper
 */
public class FieldReference {
    private String className;
    private String fieldName;

    public FieldReference(String className, String fieldName) {
        this.className = className;
        this.fieldName = fieldName;
    }

    public String getClassName() {
        return className;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public int hashCode() {
        return className.hashCode() ^ fieldName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FieldReference)) {
            return false;
        }
        FieldReference other = (FieldReference)obj;
        return className.equals(other.className) && fieldName.equals(other.fieldName);
    }
}
