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
package org.teavm.tooling.transformation;

import java.nio.charset.StandardCharsets;
import org.teavm.extension.spi.transformation.ResourceTransformer;
import org.teavm.extension.spi.transformation.ResourceTransformerEnvironment;

/**
 * Replaces the UTF-8 constant {@code ORIGINAL_MARKER} in the class file of {@link ResourceTransformerTestApplication}
 * with a value of the same length, taken from {@link #REPLACEMENT_PROPERTY}. Keeping the length allows patching
 * the constant pool in place without a bytecode library.
 */
public class TestResourceTransformer implements ResourceTransformer {
    public static final String REPLACEMENT_PROPERTY = "teavm.test.transformer.replacement";
    static final String TARGET = ResourceTransformerTestApplication.class.getName().replace('.', '/') + ".class";

    @Override
    public byte[] transform(ResourceTransformerEnvironment environment, String name, byte[] resource) {
        if (!name.equals(TARGET)) {
            return resource;
        }
        var replacementString = environment.property(REPLACEMENT_PROPERTY);
        if (replacementString == null) {
            return resource;
        }
        var original = ResourceTransformerTestApplication.MARKER.getBytes(StandardCharsets.UTF_8);
        var replacement = replacementString.getBytes(StandardCharsets.UTF_8);
        if (original.length != replacement.length) {
            throw new IllegalArgumentException("Replacement must have the same length as original marker");
        }
        var result = resource.clone();
        var index = indexOf(result, original);
        if (index < 0) {
            throw new IllegalStateException("Marker not found in " + name);
        }
        System.arraycopy(replacement, 0, result, index, replacement.length);
        return result;
    }

    private static int indexOf(byte[] data, byte[] pattern) {
        outer:
        for (var i = 0; i <= data.length - pattern.length; ++i) {
            for (var j = 0; j < pattern.length; ++j) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
