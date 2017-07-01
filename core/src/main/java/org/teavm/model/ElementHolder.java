/*
 *  Copyright 2012 Alexey Andreev.
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
package org.teavm.model;

import java.util.EnumSet;

public abstract class ElementHolder implements ElementReader {
    private EnumSet<ElementModifier> modifiers = EnumSet.noneOf(ElementModifier.class);
    private AnnotationContainer annotations = new AnnotationContainer();
    private AccessLevel level = AccessLevel.PACKAGE_PRIVATE;
    private String name;

    public ElementHolder(String name) {
        this.name = name;
    }

    @Override
    public EnumSet<ElementModifier> readModifiers() {
        return modifiers.clone();
    }

    @Override
    public boolean hasModifier(ElementModifier modifier) {
        return modifiers.contains(modifier);
    }

    public EnumSet<ElementModifier> getModifiers() {
        return modifiers;
    }

    @Override
    public AccessLevel getLevel() {
        return level;
    }

    public void setLevel(AccessLevel level) {
        this.level = level;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AnnotationContainer getAnnotations() {
        return annotations;
    }
}
