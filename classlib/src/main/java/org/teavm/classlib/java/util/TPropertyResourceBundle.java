/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teavm.classlib.java.util;

import org.teavm.classlib.java.io.TIOException;
import org.teavm.classlib.java.io.TInputStream;
import org.teavm.classlib.java.io.TReader;

/**
 * <a href="https://raw.githubusercontent.com/apache/harmony/java6/classlib/modules/luni/src/main/java/java/util/PropertyResourceBundle.java">Original file.</a>
 * <p></p>
 * {@code PropertyResourceBundle} loads resources from an {@code TInputStream}. All resources are
 * Strings. The resources must be of the form {@code key=value}, one
 * resource per line (see TProperties).
 *
 * @see TResourceBundle
 * @see TProperties
 * @since 1.1
 */
public class TPropertyResourceBundle extends TResourceBundle {

    TProperties resources;

    /**
     * Constructs a new instance of {@code PropertyResourceBundle} and loads the
     * properties file from the specified {@code TInputStream}.
     *
     * @param stream
     *            the {@code TInputStream}.
     * @throws TIOException
     *             if an error occurs during a read operation on the
     *             {@code TInputStream}.
     */
    public TPropertyResourceBundle(TInputStream stream) throws TIOException {
        if( null == stream ) {
            throw new NullPointerException();
        }
        resources = new TProperties();
        resources.load(stream);
    }

    /**
     * Constructs a new instance of PropertyResourceBundle and loads the
     * properties from the reader.
     *
     * @param reader
     *            the input reader
     * @throws TIOException
     * @since 1.6
     */
    public TPropertyResourceBundle(TReader reader) throws TIOException {
        resources = new TProperties();
        resources.load(reader);
    }

    @SuppressWarnings("unchecked")
    private TEnumeration<String> getLocalKeys() {
        return (TEnumeration<String>) resources.propertyNames();
    }

    /**
     * Answers the names of the resources contained in this
     * PropertyResourceBundle.
     *
     * @return an TEnumeration of the resource names
     */
    @Override
    public TEnumeration<String> getKeys() {
        if (parent == null) {
            return getLocalKeys();
        }
        return new TEnumeration<String>() {
            TEnumeration<String> local = getLocalKeys();

            TEnumeration<String> pEnum = parent.getKeys();

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

            public boolean hasMoreElements() {
                if (local.hasMoreElements()) {
                    return true;
                }
                return findNext();
            }

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

    /**
     * Answers the named resource from this PropertyResourceBundle, or null if
     * the resource is not found.
     *
     * @param key
     *            the name of the resource
     * @return the resource object
     */
    @Override
    public Object handleGetObject(String key) {
        return resources.get(key);
    }
}