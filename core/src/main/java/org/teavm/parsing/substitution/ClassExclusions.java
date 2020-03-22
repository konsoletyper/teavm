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

public class ClassExclusions implements Function<String, Boolean> {
    private final Map<String, Boolean> classExclusions = new HashMap<>();
    private Boolean isExcluded;
    private Boolean isSubpackagesExcluded;
    private final Map<String, ClassExclusions> subExclusions = new HashMap<>();

    @Override
    public Boolean apply(String className) {
        return isExcluded(className.split("\\.")) == Boolean.TRUE;
    }

    public void setPackageHierarchyExclusion(String[] packageNameSegments, boolean isExcluded) {
        if (packageNameSegments == null || packageNameSegments.length < 1) {
            isSubpackagesExcluded = isExcluded;
            return;
        }
        subExclusions.computeIfAbsent(packageNameSegments[0], packageName -> new ClassExclusions())
                .setPackageHierarchyExclusion(
                        Arrays.copyOfRange(packageNameSegments, 1, packageNameSegments.length), isExcluded);
    }

    public void setPackageExclusion(String[] packageNameSegments, boolean isExcluded) {
        if (packageNameSegments == null || packageNameSegments.length < 1) {
            this.isExcluded = isExcluded;
            return;
        }
        subExclusions.computeIfAbsent(packageNameSegments[0], packageName -> new ClassExclusions())
                .setPackageExclusion(Arrays.copyOfRange(packageNameSegments, 1, packageNameSegments.length),
                        isExcluded);
    }

    public void setClassExclusion(String[] classNameSegments, boolean isExcluded) {
        if (classNameSegments == null || classNameSegments.length < 1) {
            return;
        }
        if (classNameSegments.length == 1) {
            classExclusions.put(classNameSegments[0], isExcluded);
        } else {
            subExclusions.computeIfAbsent(classNameSegments[0], packageName -> new ClassExclusions())
                    .setClassExclusion(Arrays.copyOfRange(classNameSegments, 1, classNameSegments.length),
                            isExcluded);
        }
    }

    private Boolean isExcluded(String[] classNameSegments) {
        if (classNameSegments == null || classNameSegments.length < 1) {
            return isExcluded;
        }
        if (classNameSegments.length == 1) {
            final Boolean isClassExcluded = classExclusions.get(classNameSegments[0]);
            return isClassExcluded != null ? isClassExcluded : isExcluded != null ? isExcluded : isSubpackagesExcluded;
        }
        ClassExclusions subPackageExclusions = subExclusions.get(classNameSegments[0]);
        if (subPackageExclusions != null) {
            Boolean isExcluded = subPackageExclusions
                    .isExcluded(Arrays.copyOfRange(classNameSegments, 1, classNameSegments.length));
            return isExcluded != null ? isExcluded : isSubpackagesExcluded;
        }
        return isSubpackagesExcluded;
    }
}
