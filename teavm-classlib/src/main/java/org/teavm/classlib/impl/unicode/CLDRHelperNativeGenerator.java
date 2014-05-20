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
package org.teavm.classlib.impl.unicode;

import java.io.IOException;
import org.teavm.codegen.SourceWriter;
import org.teavm.dependency.DependencyChecker;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class CLDRHelperNativeGenerator implements Generator, DependencyPlugin {
    @Override
    public void methodAchieved(DependencyChecker checker, MethodDependency method) {
        switch (method.getMethod().getName()) {
            case "getLikelySubtagsImpl":
                method.getResult().propagate("java.lang.String");
                break;
        }
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "getLikelySubtagsImpl":
                writer.append("var data = ").appendClass("java.util.Locale").append(".$CLDR.likelySubtags[$rt_ustr(")
                        .append(context.getParameterName(1)).append(")];").softNewLine();
                writer.append("return data ? $rt_str(data) : null;").softNewLine();
                break;
        }
    }
}
