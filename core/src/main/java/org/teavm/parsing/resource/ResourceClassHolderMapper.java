/*
 *  Copyright 2016 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.parsing.resource;

import java.io.IOException;
import java.io.InputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.teavm.common.Mapper;
import org.teavm.model.ClassHolder;
import org.teavm.model.ReferenceCache;
import org.teavm.parsing.Parser;

public class ResourceClassHolderMapper implements Mapper<String, ClassHolder> {
    private Parser parser = new Parser(new ReferenceCache());
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
        return parser.parseClass(clsNode);
    }
}
