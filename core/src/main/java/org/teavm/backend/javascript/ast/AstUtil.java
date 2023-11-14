/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.backend.javascript.ast;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ast.AstNode;
import org.teavm.backend.javascript.rendering.JSParser;

public final class AstUtil {
    private AstUtil() {
    }

    public static AstNode parse(String string) {
        var env = new CompilerEnvirons();
        env.setRecoverFromErrors(true);
        env.setLanguageVersion(Context.VERSION_1_8);
        env.setIdeMode(true);
        var factory = new JSParser(env);

        return factory.parse(string, null, 0);
    }

    public static AstNode parseFromResources(ClassLoader classLoader, String path) {
        try (var input = classLoader.getResourceAsStream(path)) {
            return parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
