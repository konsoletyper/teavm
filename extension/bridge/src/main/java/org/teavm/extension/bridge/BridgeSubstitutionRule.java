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
package org.teavm.extension.bridge;

import org.teavm.jso.JSObject;

/**
 * Single rule, contributed by {@link org.teavm.extension.spi.substitution.SubstitutionPolicy} via
 * {@link org.teavm.extension.spi.substitution.SubstitutionSink#selectClasses(java.util.function.Predicate)}.
 * Host replays these rules to its own sink. String properties are {@code null} when not set.
 */
public interface BridgeSubstitutionRule extends JSObject {
    /**
     * Tests the predicate of the rule against the given class name.
     */
    boolean matches(String className);

    String simpleNamePrefix();

    String simpleNameSuffix();

    String packagePrefix();

    String packageSuffix();

    /**
     * Package that is replaced by {@link #packagePrefix()}, see
     * {@link org.teavm.extension.spi.substitution.ClassSubstitutionPolicy#replacePackage(String, String)}.
     */
    String replacedPackage();

    /**
     * See {@link org.teavm.extension.spi.substitution.ClassSubstitutionPolicy#dontFallbackWhenNoSubstitution()}.
     */
    boolean dontFallback();
}
