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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

public abstract class TListResourceBundle extends TResourceBundle {
    HashMap<String, Object> table;

    public TListResourceBundle() {
        super();
    }

    protected abstract Object[][] getContents();

    @Override
    public Enumeration<String> getKeys() {
        initializeTable();
        if (parent != null) {
            return new Enumeration<String>() {
                Iterator<String> local = table.keySet().iterator();

                Enumeration<String> pEnum = parent.getKeys();

                String nextElement;

                private boolean findNext() {
                    if (nextElement != null) {
                        return true;
                    }
                    while (pEnum.hasMoreElements()) {
                        String next = pEnum.nextElement();
                        if (!table.containsKey(next)) {
                            nextElement = next;
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public boolean hasMoreElements() {
                    if (local.hasNext()) {
                        return true;
                    }
                    return findNext();
                }

                @Override
                public String nextElement() {
                    if (local.hasNext()) {
                        return local.next();
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
        } else {
            return new Enumeration<String>() {
                Iterator<String> it = table.keySet().iterator();

                @Override
                public boolean hasMoreElements() {
                    return it.hasNext();
                }

                @Override
                public String nextElement() {
                    return it.next();
                }
            };
        }
    }

    @Override
    public final Object handleGetObject(String key) {
        initializeTable();
        if (key == null) {
            throw new NullPointerException();
        }
        return table.get(key);
    }

    private synchronized void initializeTable() {
        if (table == null) {
            Object[][] contents = getContents();
            table = new HashMap<>(contents.length / 3 * 4 + 3);
            for (Object[] content : contents) {
                if (content[0] == null || content[1] == null) {
                    throw new NullPointerException();
                }
                table.put((String) content[0], content[1]);
            }
        }
    }
}
