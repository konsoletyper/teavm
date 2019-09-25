/*
 *  Copyright 2019 konsoletyper.
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
package org.teavm.backend.c.generate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public class ClassGenerationContext {
    private GenerationContext context;
    private IncludeManager includes;
    private String currentClassName;
    private CodeWriter methodIdWriter;
    private CodeWriter importWriter;
    private CodeWriter initWriter;
    private Set<MethodReference> importedMethods = new HashSet<>();
    private Map<MethodDescriptor, String> virtualMethodIds = new HashMap<>();

    public ClassGenerationContext(GenerationContext context, IncludeManager includes, CodeWriter prologueWriter,
            CodeWriter initWriter, String currentClassName) {
        this.context = context;
        this.includes = includes;
        importWriter = prologueWriter.fragment();
        methodIdWriter = prologueWriter.fragment();
        this.initWriter = initWriter != null ? initWriter.fragment() : null;
        this.currentClassName = currentClassName;
    }

    public GenerationContext getContext() {
        return context;
    }

    public void importMethod(MethodReference method, boolean isStatic) {
        if (context.isIncremental()) {
            if (importedMethods.add(method)) {
                importWriter.print("extern ");
                CodeGenerator.generateMethodSignature(importWriter, context.getNames(), method, isStatic, false);
                importWriter.println(";");
            }
        } else {
            includes.includeClass(method.getClassName());
        }
    }

    public String getVirtualMethodId(MethodDescriptor methodDesc) {
        return virtualMethodIds.computeIfAbsent(methodDesc, m -> {
            String name = "methodId_" + context.getNames().forClass(currentClassName) + "_"
                    + context.getNames().forVirtualMethod(m);
            methodIdWriter.println("static int32_t " + name + ";");
            initWriter.print(name + " = teavm_vc_getMethodId(u");
            StringPoolGenerator.generateSimpleStringLiteral(initWriter, methodDesc.toString());
            initWriter.println(");");
            return name;
        });
    }
}
