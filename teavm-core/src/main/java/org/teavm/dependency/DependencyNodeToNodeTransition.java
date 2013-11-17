package org.teavm.dependency;

/**
 *
 * @author Alexey Andreev
 */
class DependencyNodeToNodeTransition implements DependencyConsumer {
    private DependencyNode source;
    private DependencyNode destination;
    private DependencyTypeFilter filter;

    public DependencyNodeToNodeTransition(DependencyNode source, DependencyNode destination,
            DependencyTypeFilter filter) {
        this.source = source;
        this.destination = destination;
        this.filter = filter;
    }

    @Override
    public void consume(String type) {
        if (filter != null && !filter.match(type)) {
            return;
        }
        if (!destination.hasType(type)) {
            destination.propagate(type);
            if (type.startsWith("[")) {
                source.getArrayItemNode().connect(destination.getArrayItemNode());
                destination.getArrayItemNode().connect(destination.getArrayItemNode());
            }
        }
    }
}
