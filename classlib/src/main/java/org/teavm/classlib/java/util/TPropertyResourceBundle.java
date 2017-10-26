/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.classlib.java.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

public class TPropertyResourceBundle extends TResourceBundle {
    Properties resources;

    public TPropertyResourceBundle(InputStream stream) throws IOException {
        resources = new Properties();
        resources.load(stream);
    }

    @SuppressWarnings("unchecked")
    private Enumeration<String> getLocalKeys() {
        return (Enumeration<String>) resources.propertyNames();
    }

    @Override
    public Enumeration<String> getKeys() {
        if (parent == null) {
            return getLocalKeys();
        }
        return new Enumeration<String>() {
            Enumeration<String> local = getLocalKeys();

            Enumeration<String> pEnum = parent.getKeys();

            String nextElement;

            private boolean findNext() {
                if (nextElement != null) {
                    return true;
                }
                while (pEnum.hasMoreElements()) {
                    String next = pEnum.nextElement();
                    if (!resources.containsKey(next)) {
                        nextElement = next;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean hasMoreElements() {
                if (local.hasMoreElements()) {
                    return true;
                }
                return findNext();
            }

            @Override
            public String nextElement() {
                if (local.hasMoreElements()) {
                    return local.nextElement();
                }
                if (findNext()) {
                    String result = nextElement;
                    nextElement = null;
                    return result;
                }
                // Cause an exception
                return pEnum.nextElement();
            }
        };
    }

    @Override
    public Object handleGetObject(String key) {
        return resources.get(key);
    }
}
