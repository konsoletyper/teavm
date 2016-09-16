/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.html4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.rendering.RenderingManager;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.spi.AbstractRendererListener;

/**
 *
 * @author Jaroslav Tulach
 */
public class ResourcesInterceptor extends AbstractRendererListener {
    private final Set<String> processed = new HashSet<>();
    @Override
    public void begin(RenderingManager manager, BuildTarget buildTarget) throws IOException {
        boolean hasOneResource = false;
        for (String className : manager.getClassSource().getClassNames()) {
            final int lastDot = className.lastIndexOf('.');
            if (lastDot == -1) {
                continue;
            }
            String packageName = className.substring(0, lastDot);
            String resourceName = packageName.replace('.', '/') + "/" + "jvm.txt";
            try (InputStream input = manager.getClassLoader().getResourceAsStream(resourceName)) {
                if (input == null || !processed.add(resourceName)) {
                    continue;
                }
                ByteArrayOutputStream arr = new ByteArrayOutputStream();
                IOUtils.copy(input, arr);
                String base64 = Base64.getEncoder().encodeToString(arr.toByteArray());
                input.close();
                final SourceWriter w = manager.getWriter();
                w.append("// Resource " + resourceName + " included by " + className).newLine();
                w.append("if (!window.teaVMResources) window.teaVMResources = {};").newLine();
                w.append("window.teaVMResources['" + resourceName + "'] = '");
                w.append(base64).append("';").newLine().newLine();
            }
            hasOneResource = true;
        }
        if (hasOneResource) {
            manager.getWriter().append("// TeaVM generated classes").newLine();
        }
    }
}
