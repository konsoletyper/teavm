package org.teavm.model.resource;

import java.io.IOException;
import java.io.InputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.teavm.common.Mapper;
import org.teavm.model.ClassHolder;
import org.teavm.parsing.Parser;

/**
 *
 * @author konsoletyper
 */
public class ResourceClassHolderMapper implements Mapper<String, ClassHolder> {
    private ResourceReader resourceReader;

    public ResourceClassHolderMapper(ResourceReader resourceReader) {
        this.resourceReader = resourceReader;
    }

    @Override
    public ClassHolder map(String name) {
        ClassNode clsNode = new ClassNode();
        String resourceName = name.replace('.', '/') + ".class";
        if (!resourceReader.hasResource(resourceName)) {
            return null;
        }
        try (InputStream input = resourceReader.openResource(resourceName)) {
            ClassReader reader = new ClassReader(input);
            reader.accept(clsNode, 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Parser.parseClass(clsNode);
    }
}
