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
package org.teavm.classlib.java.text;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.util.TMap;
import org.teavm.classlib.java.util.TSet;

public interface TAttributedCharacterIterator extends TCharacterIterator {

    class Attribute implements TSerializable {
        public static final Attribute INPUT_METHOD_SEGMENT = new Attribute(
                "input_method_segment");

        public static final Attribute LANGUAGE = new Attribute("language");

        public static final Attribute READING = new Attribute("reading");

        private String name;

        protected Attribute(String name) {
            this.name = name;
        }

        @Override
        public final boolean equals(Object object) {
            return this == object;
        }

        protected String getName() {
            return name;
        }

        @Override
        public final int hashCode() {
            return super.hashCode();
        }

        @Override
        public String toString() {
            return getClass().getName() + '(' + getName() + ')';
        }
    }

    /**
     * Returns a set of attributes present in the {@code
     * AttributedCharacterIterator}. An empty set is returned if no attributes
     * were defined.
     *
     * @return a set of attribute keys; may be empty.
     */
    TSet<Attribute> getAllAttributeKeys();

    /**
     * Returns the value stored in the attribute for the current character. If
     * the attribute was not defined then {@code null} is returned.
     *
     * @param attribute the attribute for which the value should be returned.
     * @return the value of the requested attribute for the current character or
     *         {@code null} if it was not defined.
     */
    Object getAttribute(Attribute attribute);

    /**
     * Returns a map of all attributes of the current character. If no
     * attributes were defined for the current character then an empty map is
     * returned.
     *
     * @return a map of all attributes for the current character or an empty
     *         map.
     */
    TMap<Attribute, Object> getAttributes();

    /**
     * Returns the index of the last character in the run having the same
     * attributes as the current character.
     *
     * @return the index of the last character of the current run.
     */
    int getRunLimit();

    /**
     * Returns the index of the last character in the run that has the same
     * attribute value for the given attribute as the current character.
     *
     * @param attribute
     *            the attribute which the run is based on.
     * @return the index of the last character of the current run.
     */
    int getRunLimit(Attribute attribute);

    /**
     * Returns the index of the last character in the run that has the same
     * attribute values for the attributes in the set as the current character.
     *
     * @param attributes
     *            the set of attributes which the run is based on.
     * @return the index of the last character of the current run.
     */
    int getRunLimit(TSet<? extends Attribute> attributes);

    /**
     * Returns the index of the first character in the run that has the same
     * attributes as the current character.
     *
     * @return the index of the last character of the current run.
     */
    int getRunStart();

    /**
     * Returns the index of the first character in the run that has the same
     * attribute value for the given attribute as the current character.
     *
     * @param attribute
     *            the attribute which the run is based on.
     * @return the index of the last character of the current run.
     */
    int getRunStart(Attribute attribute);

    /**
     * Returns the index of the first character in the run that has the same
     * attribute values for the attributes in the set as the current character.
     *
     * @param attributes
     *            the set of attributes which the run is based on.
     * @return the index of the last character of the current run.
     */
    int getRunStart(TSet<? extends Attribute> attributes);
}
