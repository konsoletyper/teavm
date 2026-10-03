/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.junit;

import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.InvokeInstruction;

/**
 * Replaces calls from JUnit Jupiter assertions to {@code org.junit.platform.commons.util.ReflectionUtils}
 * with calls to {@link JupiterAssertionsSupport}. Static initializer of {@code ReflectionUtils} pulls
 * logging infrastructure and lots of reflection, which can't be compiled by TeaVM, while assertions only
 * need a trivial check whether an object is an array.
 */
class JupiterAssertionsTransformer implements ClassHolderTransformer {
    private static final String JUPITER_API_PACKAGE = "org.junit.jupiter.api.";
    private static final ValueType OBJECT = ValueType.object("java.lang.Object");
    private static final MethodReference IS_ARRAY = new MethodReference(
            "org.junit.platform.commons.util.ReflectionUtils", "isArray", OBJECT, ValueType.BOOLEAN);
    private static final MethodReference IS_ARRAY_REPLACEMENT = new MethodReference(
            JupiterAssertionsSupport.class.getName(), "isArray", OBJECT, ValueType.BOOLEAN);

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (!cls.getName().startsWith(JUPITER_API_PACKAGE)) {
            return;
        }
        for (var method : cls.getMethods()) {
            var program = method.getProgram();
            if (program == null) {
                continue;
            }
            for (BasicBlock block : program.getBasicBlocks()) {
                for (var instruction : block) {
                    if (instruction instanceof InvokeInstruction) {
                        var invoke = (InvokeInstruction) instruction;
                        if (invoke.getMethod().equals(IS_ARRAY)) {
                            invoke.setMethod(IS_ARRAY_REPLACEMENT);
                        }
                    }
                }
            }
        }
    }
}
