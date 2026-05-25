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
package org.teavm.extension.spi.substitution;

import java.util.function.Predicate;
import org.teavm.extension.spi.GlobMatch;

public abstract class SimpleSubstitutionPolicy implements SubstitutionPolicy {
    protected static <T> Predicate<T> not(Predicate<T> predicate) {
        return predicate.negate();
    }

    protected static <T> Predicate<String> named(String name) {
        return n -> n.equals(name);
    }

    protected static Predicate<String> namePattern(String pattern) {
        return name -> GlobMatch.match(pattern, name);
    }

    protected static Predicate<String> inPackage(String pkg) {
        var prefix = pkg + ".";
        return name -> name.startsWith(prefix) && name.indexOf('.', prefix.length()) < 0;
    }

    protected static Predicate<String> inPackage(String pkg, boolean includeSubpackages) {
        if (!includeSubpackages) {
            return inPackage(pkg);
        }
        var prefix = pkg + ".";
        return name -> name.startsWith(prefix);
    }
}
