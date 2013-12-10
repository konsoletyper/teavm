package org.teavm.codegen;


/**
 *
 * @author Alexey Andreev
 */
public class SourceWriterBuilder {
    private NamingStrategy naming;
    private boolean minified;

    public SourceWriterBuilder(NamingStrategy naming) {
        this.naming = naming;
    }

    public boolean isMinified() {
        return minified;
    }

    public void setMinified(boolean minified) {
        this.minified = minified;
    }

    public SourceWriter build() {
        SourceWriter writer = new SourceWriter(naming);
        writer.setMinified(minified);
        return writer;
    }
}
