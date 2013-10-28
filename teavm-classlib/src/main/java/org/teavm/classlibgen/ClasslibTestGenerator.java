package org.teavm.classlibgen;

import org.teavm.codegen.DefaultAliasProvider;
import org.teavm.codegen.DefaultNamingStrategy;
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.Decompiler;
import org.teavm.model.ClassHolder;
import org.teavm.model.resource.ClasspathClassHolderSource;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ClasslibTestGenerator {
    public static void main(String[] args) {
        ClasspathClassHolderSource source = new ClasspathClassHolderSource();
        DefaultAliasProvider aliasProvider = new DefaultAliasProvider();
        DefaultNamingStrategy naming = new DefaultNamingStrategy(aliasProvider, source);
        SourceWriter writer = new SourceWriter(naming);
        Decompiler decompiler = new Decompiler(source, naming, writer);
        ClassHolder cls = source.getClassHolder("java.lang.Object");
        decompiler.decompile(cls);
        System.out.println(writer);
    }
}
