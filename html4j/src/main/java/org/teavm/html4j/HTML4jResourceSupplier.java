/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.html4j;

import java.util.HashSet;
import java.util.Set;
import org.teavm.classlib.ResourceSupplier;
import org.teavm.model.ListableClassReaderSource;

/**
 *
 * @author Alexey Andreev
 */
public class HTML4jResourceSupplier implements ResourceSupplier {
    @Override
    public String[] supplyResources(ClassLoader classLoader, ListableClassReaderSource classSource) {
        Set<String> resources = new HashSet<>();
        for (String className : classSource.getClassNames()) {
            final int lastDot = className.lastIndexOf('.');
            if (lastDot == -1) {
                continue;
            }
            String packageName = className.substring(0, lastDot);
            String resourceName = packageName.replace('.', '/') + "/" + "jvm.txt";
            resources.add(resourceName);
        }

        return resources.toArray(new String[0]);
    }
}
