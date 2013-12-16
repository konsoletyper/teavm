package org.teavm.javascript;

import org.teavm.dependency.MethodGraph;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class JavascriptEntryPoint {
    private String publicName;
    MethodReference reference;
    private MethodGraph graph;

    JavascriptEntryPoint(String publicName, MethodReference reference, MethodGraph graph) {
        this.publicName = publicName;
        this.reference = reference;
        this.graph = graph;
    }

    String getPublicName() {
        return publicName;
    }

    public JavascriptEntryPoint withValue(int argument, String type) {
        if (argument > reference.parameterCount()) {
            throw new IllegalArgumentException("Illegal argument #" + argument + " of " + reference.parameterCount());
        }
        graph.getVariableNode(argument).propagate(type);
        return this;
    }
}
