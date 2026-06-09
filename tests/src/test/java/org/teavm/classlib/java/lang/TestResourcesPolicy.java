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
package org.teavm.classlib.java.lang;

import java.util.Collection;
import org.teavm.extension.Autoregistered;
import org.teavm.extension.spi.resources.DefaultResourcesPolicy;

@Autoregistered
public class TestResourcesPolicy extends DefaultResourcesPolicy {
    @Override
    public String[] supplyResources(Collection<? extends String> availableClassNames) {
        String[] names = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "binary-null", "binary-all-bytes" };
        for (int i = 0; i < names.length; ++i) {
            names[i] = "resources-for-test/" + names[i];
        }
        return names;
    }
}
