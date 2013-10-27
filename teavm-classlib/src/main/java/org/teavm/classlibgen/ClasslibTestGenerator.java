package org.teavm.classlibgen;

import org.teavm.codegen.DefaultAliasProvider;
import org.teavm.codegen.DefaultNamingStrategy;
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.MethodDecompiler;
import org.teavm.javascript.Optimizer;
import org.teavm.javascript.Renderer;
import org.teavm.javascript.ast.RenderableMethod;
import org.teavm.model.ClassHolder;
import org.teavm.model.MethodHolder;
import org.teavm.model.resource.ClasspathClassHolderSource;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ClasslibTestGenerator {
    public static void main(String[] args) {
        ClasspathClassHolderSource source = new ClasspathClassHolderSource();
        MethodDecompiler decompiler = new MethodDecompiler(source);
        DefaultAliasProvider aliasProvider = new DefaultAliasProvider();
        DefaultNamingStrategy naming = new DefaultNamingStrategy(aliasProvider, source);
        SourceWriter writer = new SourceWriter(naming);
        Renderer renderer = new Renderer(writer, source);
        Optimizer optimizer = new Optimizer();
        ClassHolder cls = source.getClassHolder("java.lang.Object");
        for (MethodHolder method : cls.getMethods()) {
            RenderableMethod renderableMethod = decompiler.decompile(method);
            optimizer.optimize(renderableMethod);
            renderer.render(renderableMethod);
        }
        System.out.println(writer);
    }
}
