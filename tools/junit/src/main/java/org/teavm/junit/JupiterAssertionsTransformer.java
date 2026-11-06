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

import java.util.Map;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.InvokeInstruction;

/**
 * Replaces calls from JUnit Jupiter assertions to some utility methods with calls to lightweight equivalents
 * in {@link JupiterAssertionsSupport}:
 *
 * <ul>
 *   <li>static initializer of {@code org.junit.platform.commons.util.ReflectionUtils} pulls logging
 *     infrastructure and lots of reflection, which can't be compiled by TeaVM;</li>
 *   <li>static initializer of {@code org.junit.platform.commons.util.StringUtils} compiles regular expressions,
 *     which brings the whole regex engine to every test. Apart from direct calls, {@code StringUtils} is reached
 *     via {@code UnrecoverableExceptions}, {@code ExceptionUtils} and {@code Preconditions};</li>
 *   <li>{@code String.format} brings {@code java.util.Formatter} with its dependencies, while assertions only
 *     use {@code %s} specifiers.</li>
 * </ul>
 *
 * <p>Without these replacements every compiled test becomes several times larger,
 * which makes tests compile much longer.
 */
class JupiterAssertionsTransformer implements ClassHolderTransformer {
    private static final String JUPITER_API_PACKAGE = "org.junit.jupiter.api.";
    private static final String COMMONS_UTIL = "org.junit.platform.commons.util.";
    private static final ValueType OBJECT = ValueType.object("java.lang.Object");
    private static final ValueType STRING = ValueType.object("java.lang.String");
    private static final ValueType OBJECT_ARRAY = ValueType.arrayOf(OBJECT);
    private static final ValueType THROWABLE = ValueType.object("java.lang.Throwable");
    private static final Map<MethodReference, MethodReference> REPLACEMENTS = Map.of(
            new MethodReference(COMMONS_UTIL + "ReflectionUtils", "isArray", OBJECT, ValueType.BOOLEAN),
            replacement("isArray", OBJECT, ValueType.BOOLEAN),
            new MethodReference(COMMONS_UTIL + "StringUtils", "isNotBlank", STRING, ValueType.BOOLEAN),
            replacement("isNotBlank", STRING, ValueType.BOOLEAN),
            new MethodReference(COMMONS_UTIL + "StringUtils", "nullSafeToString", OBJECT, STRING),
            replacement("nullSafeToString", OBJECT, STRING),
            new MethodReference(COMMONS_UTIL + "UnrecoverableExceptions", "rethrowIfUnrecoverable", THROWABLE,
                    ValueType.VOID),
            replacement("rethrowIfUnrecoverable", THROWABLE, ValueType.VOID),
            new MethodReference("java.lang.String", "format", STRING, OBJECT_ARRAY, STRING),
            replacement("format", STRING, OBJECT_ARRAY, STRING)
    );

    private static MethodReference replacement(String name, ValueType... signature) {
        return new MethodReference(JupiterAssertionsSupport.class.getName(), name, signature);
    }

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
                        var replacement = REPLACEMENTS.get(invoke.getMethod());
                        if (replacement != null) {
                            invoke.setMethod(replacement);
                        }
                    }
                }
            }
        }
    }
}
