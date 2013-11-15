package org.teavm.dependency;

/**
 *
 * @author Alexey Andreev
 */
class DependencyNodeToNodeTransition implements DependencyConsumer {
    private DependencyNode source;
    private DependencyNode destination;

    public DependencyNodeToNodeTransition(DependencyNode source, DependencyNode destination) {
        this.source = source;
        this.destination = destination;
    }

    @Override
    public void consume(String type) {
        if (!destination.hasType(type)) {
            destination.propagate(type);
            if (type.startsWith("[")) {
                source.getArrayItemNode().connect(destination.getArrayItemNode());
                destination.getArrayItemNode().connect(destination.getArrayItemNode());
            }
        }
    }
}
