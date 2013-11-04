package org.teavm.javascript.ast;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ClassNode {
    private String name;
    private String parentName;
    private List<FieldNode> fields = new ArrayList<>();
    private List<MethodNode> methods = new ArrayList<>();
    private List<String> interfaces = new ArrayList<>();

    public ClassNode(String name, String parentName) {
        this.name = name;
        this.parentName = parentName;
    }

    public String getName() {
        return name;
    }

    public String getParentName() {
        return parentName;
    }

    public List<FieldNode> getFields() {
        return fields;
    }

    public List<MethodNode> getMethods() {
        return methods;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }
}
