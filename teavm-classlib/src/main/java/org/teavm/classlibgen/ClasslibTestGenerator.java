package org.teavm.classlibgen;

import org.teavm.codegen.DefaultAliasProvider;
import org.teavm.codegen.DefaultNamingStrategy;
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.Decompiler;
import org.teavm.javascript.Renderer;
import org.teavm.javascript.ast.ClassNode;
import org.teavm.model.ClassHolder;
import org.teavm.model.resource.ClasspathClassHolderSource;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ClasslibTestGenerator {
    public static void main(String[] args) {
        ClasspathClassHolderSource source = new ClasspathClassHolderSource();
        Decompiler decompiler = new Decompiler(source);
        ClassHolder cls = source.getClassHolder("java.lang.Object");
        ClassNode clsNode = decompiler.decompile(cls);
        DefaultAliasProvider aliasProvider = new DefaultAliasProvider();
        DefaultNamingStrategy naming = new DefaultNamingStrategy(aliasProvider, source);
        SourceWriter writer = new SourceWriter(naming);
        Renderer renderer = new Renderer(writer, source);
        renderer.render(clsNode);
        System.out.println(writer);
    }
}
