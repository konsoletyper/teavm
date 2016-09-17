/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.model.classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.ListableClassReaderSource;

public class InterfaceToClassMapping {
    private Map<String, String> map = new HashMap<>();

    public InterfaceToClassMapping(ListableClassReaderSource classSource) {
        for (String className : classSource.getClassNames()) {
            ClassReader cls = classSource.get(className);
            if (cls.hasModifier(ElementModifier.INTERFACE)) {
                continue;
            }

            map.put(className, className);
            for (String iface : getInterfaces(classSource, className)) {
                String existing = map.get(iface);
                if (existing == null) {
                    map.put(iface, className);
                } else {
                    map.put(iface, commonSuperClass(classSource, className, existing));
                }
            }
        }
    }

    private static Set<String> getInterfaces(ClassReaderSource classSource, String className) {
        Set<String> interfaces = new HashSet<>();
        getInterfaces(classSource, className, interfaces);
        return interfaces;
    }

    private static void getInterfaces(ClassReaderSource classSource, String className, Set<String> interfaces) {
        if (!interfaces.add(className)) {
            return;
        }
        ClassReader cls = classSource.get(className);
        if (cls == null) {
            return;
        }
        for (String iface : cls.getInterfaces()) {
            getInterfaces(classSource, iface, interfaces);
        }
    }

    private static String commonSuperClass(ClassReaderSource classSource, String a, String b) {
        if (a.equals(b)) {
            return a;
        }

        List<String> firstPath = pathToRoot(classSource, a);
        List<String> secondPath = pathToRoot(classSource, b);
        Collections.reverse(firstPath);
        Collections.reverse(secondPath);
        int min = Math.min(firstPath.size(), secondPath.size());
        for (int i = 1; i < min; ++i) {
            if (!firstPath.get(i).equals(secondPath.get(i))) {
                return firstPath.get(i - 1);
            }
        }

        return firstPath.get(0);
    }

    private static List<String> pathToRoot(ClassReaderSource classSource, String className) {
        List<String> path = new ArrayList<>();
        while (true) {
            path.add(className);
            ClassReader cls = classSource.get(className);
            if (cls == null || cls.getParent() == null) {
                break;
            }
            className = cls.getParent();
        }
        return path;
    }

    public String mapClass(String className) {
        return map.get(className);
    }
}
