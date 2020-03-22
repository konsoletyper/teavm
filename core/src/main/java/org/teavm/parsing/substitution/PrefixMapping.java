/*
 *  Copyright 2020 adam.
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
package org.teavm.parsing.substitution;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PrefixMapping implements Function<String, String> {
    private String packageClassPrefix;
    private String packageHierarchyClassPrefix;
    private final Map<String, PrefixMapping> subMappings = new HashMap<>();

    @Override
    public String apply(String className) {
        final String prefix = getPrefix(className.split("\\."));
        return prefix != null ? new StringBuilder(className).insert(className.lastIndexOf(".") + 1, prefix)
                .toString() : className;
    }

    public String revert(String className) {
        final String prefix = getPrefix(className.split("\\."));
        return prefix != null ? new StringBuilder(className)
                .delete(className.lastIndexOf(".") + 1, className.lastIndexOf(".") + 1 + prefix.length()).toString()
                : className;
    }

    public void setPackageHierarchyClassPrefixRule(String[] packageNameSegments, String classPrefix) {
        if (packageNameSegments == null || packageNameSegments.length < 1) {
            packageHierarchyClassPrefix = classPrefix;
        } else {
            subMappings.computeIfAbsent(packageNameSegments[0], s -> new PrefixMapping())
                    .setPackageHierarchyClassPrefixRule(
                            Arrays.copyOfRange(packageNameSegments, 1, packageNameSegments.length), classPrefix);
        }
    }

    public void setPackageClassPrefixRule(String[] packageNameSegments, String classPrefix) {
        if (packageNameSegments == null || packageNameSegments.length < 1) {
            packageClassPrefix = classPrefix;
        } else {
            subMappings.computeIfAbsent(packageNameSegments[0], s -> new PrefixMapping())
                    .setPackageClassPrefixRule(
                            Arrays.copyOfRange(packageNameSegments, 1, packageNameSegments.length), classPrefix);
        }
    }

    private String getPrefix(String[] classNameSegments) {
        if (classNameSegments == null || classNameSegments.length <= 1) {
            return packageClassPrefix != null ? packageClassPrefix : packageHierarchyClassPrefix;
        }
        final PrefixMapping prefixMapping = subMappings.get(classNameSegments[0]);
        if (prefixMapping != null) {
            final String prefix =
                    prefixMapping.getPrefix(Arrays.copyOfRange(classNameSegments, 1, classNameSegments.length));
            return prefix != null ? prefix : packageHierarchyClassPrefix;
        }
        return packageHierarchyClassPrefix;
    }
}
