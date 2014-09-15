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
package org.teavm.dependency;

import java.io.IOException;
import java.util.*;

/**
 *
 * @author Alexey Andreev
 */
public class DependencyViolations {
    private final Set<MethodDependencyInfo> missingMethods;
    private final Set<ClassDependencyInfo> missingClasses;
    private final Set<FieldDependencyInfo> missingFields;

    DependencyViolations(Collection<? extends MethodDependencyInfo> missingMethods,
            Collection<? extends ClassDependencyInfo> missingClasses,
            Collection<? extends FieldDependencyInfo> missingFields) {
        this.missingMethods = Collections.unmodifiableSet(new HashSet<>(missingMethods));
        this.missingClasses = Collections.unmodifiableSet(new HashSet<>(missingClasses));
        this.missingFields = Collections.unmodifiableSet(new HashSet<>(missingFields));
    }

    public Set<MethodDependencyInfo> getMissingMethods() {
        return missingMethods;
    }

    public Set<ClassDependencyInfo> getMissingClasses() {
        return missingClasses;
    }

    public Set<FieldDependencyInfo> getMissingFields() {
        return missingFields;
    }

    public boolean hasMissingItems() {
        return !missingMethods.isEmpty() || !missingClasses.isEmpty() || !missingFields.isEmpty();
    }

    public void checkForMissingItems() {
        if (!hasMissingItems()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        try {
            showMissingItems(sb);
        } catch (IOException e) {
            throw new AssertionError("StringBuilder should not throw IOException");
        }
        throw new IllegalStateException(sb.toString());
    }

    public void showMissingItems(Appendable sb) throws IOException {
        List<String> items = new ArrayList<>();
        Map<String, DependencyStack> stackMap = new HashMap<>();
        for (ClassDependencyInfo cls : missingClasses) {
            stackMap.put(cls.getClassName(), cls.getStack());
            items.add(cls.getClassName());
        }
        for (MethodDependencyInfo method : missingMethods) {
            stackMap.put(method.getReference().toString(), method.getStack());
            items.add(method.getReference().toString());
        }
        for (FieldDependencyInfo field : missingFields) {
            stackMap.put(field.getReference().toString(), field.getStack());
            items.add(field.getReference().toString());
        }
        Collections.sort(items);
        sb.append("Can't compile due to the following items missing:\n");
        for (String item : items) {
            sb.append("  ").append(item).append("\n");
            DependencyStack stack = stackMap.get(item);
            if (stack == null) {
                sb.append("    at unknown location\n");
            } else {
                while (stack.getMethod() != null) {
                    sb.append("    at ").append(stack.getMethod().toString());
                    if (stack.getLocation() != null) {
                        sb.append(":").append(String.valueOf(stack.getLocation().getLine()));
                    }
                    sb.append("\n");
                    stack = stack.getCause();
                }
            }
            sb.append('\n');
        }
    }
}
