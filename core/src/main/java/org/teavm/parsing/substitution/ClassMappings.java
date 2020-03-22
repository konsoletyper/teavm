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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ClassMappings implements Function<String, List<String>> {
    private final Map<String, List<String>> mappedClassNames = new LinkedHashMap<>();
    private final List<String> mappedPackageNames = new LinkedList<>();
    private final List<String> mappedPackageHierarchyNames = new LinkedList<>();
    private final Map<String, ClassMappings> subMappings = new HashMap<>();

    @Override
    public List<String> apply(String className) {
        List<String> mappings = new LinkedList<>();
        getMappingsInto(className.split("\\."), mappings);
        return mappings;
    }

    public void addPackageHierarchyMappingRule(String[] packageNameSegments, String mappedPackageName) {
        if (packageNameSegments == null || packageNameSegments.length < 1) {
            mappedPackageHierarchyNames.add(mappedPackageName);
        } else {
            subMappings.computeIfAbsent(packageNameSegments[0], packageName -> new ClassMappings())
                    .addPackageHierarchyMappingRule(
                            Arrays.copyOfRange(packageNameSegments, 1, packageNameSegments.length),
                            mappedPackageName);
        }
    }

    public void addPackageMappingRule(String[] packageNameSegments, String mappedPackageName) {
        if (packageNameSegments == null || packageNameSegments.length < 1) {
            mappedPackageNames.add(mappedPackageName);
        } else {
            subMappings.computeIfAbsent(packageNameSegments[0], packageName -> new ClassMappings())
                    .addPackageMappingRule(Arrays.copyOfRange(packageNameSegments, 1, packageNameSegments.length),
                            mappedPackageName);
        }
    }

    public void addClassMappingRule(String[] classNameSegments, String mappedClassName) {
        if (classNameSegments == null || classNameSegments.length < 1) {
            return;
        }
        if (classNameSegments.length == 1) {
            mappedClassNames.computeIfAbsent(classNameSegments[0], simpleClassName -> new LinkedList<>())
                    .add(mappedClassName);
            return;
        }
        subMappings.computeIfAbsent(classNameSegments[0], packageName -> new ClassMappings())
                .addClassMappingRule(Arrays.copyOfRange(classNameSegments, 1, classNameSegments.length),
                        mappedClassName);
    }

    private void getMappingsInto(String[] classNameSegments, List<String> mappings) {
        if (classNameSegments == null || classNameSegments.length < 1) {
            return;
        }
        if (classNameSegments.length == 1) {
            final List<String> classNameMappings = mappedClassNames.get(classNameSegments[0]);
            if (classNameMappings != null) {
                mappings.addAll(classNameMappings);
            }
            if (!mappedPackageNames.isEmpty()) {
                String mappingSuffix = "." + classNameSegments[0];
                for (String packageName : mappedPackageNames) {
                    mappings.add(packageName + mappingSuffix);
                }
            }
        } else {
            ClassMappings classMappings = subMappings.get(classNameSegments[0]);
            if (classMappings != null) {
                classMappings
                        .getMappingsInto(Arrays.copyOfRange(classNameSegments, 1, classNameSegments.length), mappings);
            }
        }
        if (!mappedPackageHierarchyNames.isEmpty()) {
            String mappingSuffix = "." + String.join(".", classNameSegments);
            for (String packageName : mappedPackageHierarchyNames) {
                mappings.add(packageName + mappingSuffix);
            }
        }
    }
}
