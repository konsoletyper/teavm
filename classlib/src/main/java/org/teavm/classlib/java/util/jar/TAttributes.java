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

package org.teavm.classlib.java.util.jar;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TAttributes implements Cloneable, Map<Object, Object> {
    protected Map<Object, Object> map;

    public static class Name {
        private final byte[] name;
        private int hashCode;

        public static final Name CLASS_PATH = new Name("Class-Path");
        public static final Name MANIFEST_VERSION = new Name("Manifest-Version");
        public static final Name MAIN_CLASS = new Name("Main-Class");
        public static final Name SIGNATURE_VERSION = new Name("Signature-Version");
        public static final Name CONTENT_TYPE = new Name("Content-Type");
        public static final Name SEALED = new Name("Sealed");
        public static final Name IMPLEMENTATION_TITLE = new Name("Implementation-Title");
        public static final Name IMPLEMENTATION_VERSION = new Name("Implementation-Version");
        public static final Name IMPLEMENTATION_VENDOR = new Name("Implementation-Vendor");
        public static final Name SPECIFICATION_TITLE = new Name("Specification-Title");
        public static final Name SPECIFICATION_VERSION = new Name("Specification-Version");
        public static final Name SPECIFICATION_VENDOR = new Name("Specification-Vendor");
        public static final Name EXTENSION_LIST = new Name("Extension-List");
        public static final Name EXTENSION_NAME = new Name("Extension-Name");
        public static final Name EXTENSION_INSTALLATION = new Name("Extension-Installation");
        public static final Name IMPLEMENTATION_VENDOR_ID = new Name("Implementation-Vendor-Id");
        public static final Name IMPLEMENTATION_URL = new Name("Implementation-URL");

        static final Name NAME = new Name("Name");

        public Name(String s) {
            int i = s.length();
            if (i == 0 || i > TManifest.LINE_LENGTH_LIMIT - 2) {
                throw new IllegalArgumentException();
            }

            name = new byte[i];

            for (; --i >= 0;) {
                char ch = s.charAt(i);
                if (!((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
                        || ch == '_' || ch == '-' || (ch >= '0' && ch <= '9'))) {
                    throw new IllegalArgumentException(s);
                }
                name[i] = (byte) ch;
            }
        }

        /**
         * A private constructor for a trusted attribute name.
         */
        Name(byte[] buf) {
            name = buf;
        }

        byte[] getBytes() {
            return name;
        }

        @Override
        public String toString() {
            try {
                return new String(name, "ISO-8859-1"); //$NON-NLS-1$
            } catch (UnsupportedEncodingException iee) {
                throw new InternalError(iee.getLocalizedMessage());
            }
        }

        @Override
        public boolean equals(Object object) {
            if (object == null || object.getClass() != getClass() || object.hashCode() != hashCode()) {
                return false;
            }

            return TJarUtils.asciiEqualsIgnoreCase(name, ((Name) object).name);
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                int hash = 0;
                int multiplier = 1;
                for (int i = name.length - 1; i >= 0; i--) {
                    // 'A' & 0xDF == 'a' & 0xDF, ..., 'Z' & 0xDF == 'z' & 0xDF
                    hash += (name[i] & 0xDF) * multiplier;
                    int shifted = multiplier << 5;
                    multiplier = shifted - multiplier;
                }
                hashCode = hash;
            }
            return hashCode;
        }

    }
    public TAttributes() {
        map = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public TAttributes(TAttributes attrib) {
        map = (Map<Object, Object>) ((HashMap<Object, Object>) attrib.map).clone();
    }

    public TAttributes(int size) {
        map = new HashMap<>(size);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return map.entrySet();
    }

    @Override
    public Object get(Object key) {
        return map.get(key);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Set<Object> keySet() {
        return map.keySet();
    }

    @Override
    @SuppressWarnings("cast")
    // Require cast to force ClassCastException
    public Object put(Object key, Object value) {
        return map.put(key, value);
    }

    @Override
    public void putAll(Map<?, ?> attrib) {
        if (attrib == null || !(attrib instanceof TAttributes)) {
            throw new ClassCastException();
        }
        this.map.putAll(attrib);
    }

    @Override
    public Object remove(Object key) {
        return map.remove(key);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public Collection<Object> values() {
        return map.values();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        TAttributes clone;
        try {
            clone = (TAttributes) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
        clone.map = (Map<Object, Object>) ((HashMap<?, ?>) map).clone();
        return clone;
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TAttributes) {
            return map.equals(((TAttributes) obj).map);
        }
        return false;
    }

    public String getValue(Name name) {
        return (String) map.get(name);
    }

    public String getValue(String name) {
        return (String) map.get(new Name(name));
    }

    public String putValue(String name, String val) {
        return (String) map.put(new Name(name), val);
    }
}
