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

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.util.Set;
import org.teavm.runtime.reflect.ModifiersInfo;

/**
 * Represents flags for classes and class members.
 * @see ElementHolder
 * @see AccessLevel
 * @author Alexey Andreev
 */
public enum ElementModifier {
    ABSTRACT,
    INTERFACE,
    FINAL,
    ENUM,
    ANNOTATION,
    SYNTHETIC,
    BRIDGE,
    DEPRECATED,
    NATIVE,
    STATIC,
    STRICT,
    SYNCHRONIZED,
    TRANSIENT,
    VARARGS,
    VOLATILE,
    RECORD;

    public static int pack(Set<ElementModifier> elementModifiers) {
        ElementModifier[] knownModifiers = ElementModifier.values();
        int value = 0;
        int bit = 1;
        for (int i = 0; i < knownModifiers.length; ++i) {
            ElementModifier modifier = knownModifiers[i];
            if (elementModifiers.contains(modifier)) {
                value |= bit;
            }
            bit <<= 1;
        }
        return value;
    }

    public static int encodeModifiers(ClassReader cls) {
        var modifiers = asModifiersInfo(cls.readModifiers(), cls.getLevel());
        if (cls.hasModifier(ElementModifier.ANNOTATION)) {
            var retention = cls.getAnnotations().get(Retention.class.getName());
            if (retention != null && retention.getValue("value").getEnumValue().getFieldName().equals("RUNTIME")) {
                if (cls.getAnnotations().get(Inherited.class.getName()) != null) {
                    modifiers |= ModifiersInfo.INHERITED_ANNOTATION;
                }
            }
        }
        return modifiers;
    }

    public static int asModifiersInfo(Set<ElementModifier> elementModifiers, AccessLevel level) {
        var modifiers = 0;
        switch (level) {
            case PACKAGE_PRIVATE:
                break;
            case PRIVATE:
                modifiers |= ModifiersInfo.PRIVATE;
                break;
            case PROTECTED:
                modifiers |= ModifiersInfo.PROTECTED;
                break;
            case PUBLIC:
                modifiers |= ModifiersInfo.PUBLIC;
                break;
        }
        if (elementModifiers.contains(ElementModifier.STATIC)) {
            modifiers |= ModifiersInfo.STATIC;
        }
        if (elementModifiers.contains(ElementModifier.BRIDGE)) {
            modifiers |= ModifiersInfo.BRIDGE;
        }
        if (elementModifiers.contains(ElementModifier.FINAL)) {
            modifiers |= ModifiersInfo.FINAL;
        }
        if (elementModifiers.contains(ElementModifier.SYNCHRONIZED)) {
            modifiers |= ModifiersInfo.SYNCHRONIZED;
        }
        if (elementModifiers.contains(ElementModifier.VOLATILE)) {
            modifiers |= ModifiersInfo.VOLATILE;
        }
        if (elementModifiers.contains(ElementModifier.TRANSIENT)) {
            modifiers |= ModifiersInfo.TRANSIENT;
        }
        if (elementModifiers.contains(ElementModifier.NATIVE)) {
            modifiers |= ModifiersInfo.NATIVE;
        }
        if (elementModifiers.contains(ElementModifier.INTERFACE)) {
            modifiers |= ModifiersInfo.INTERFACE;
        }
        if (elementModifiers.contains(ElementModifier.ABSTRACT)) {
            modifiers |= ModifiersInfo.ABSTRACT;
        }
        if (elementModifiers.contains(ElementModifier.STRICT)) {
            modifiers |= ModifiersInfo.STRICT;
        }
        if (elementModifiers.contains(ElementModifier.VARARGS)) {
            modifiers |= ModifiersInfo.VARARGS;
        }
        if (elementModifiers.contains(ElementModifier.ANNOTATION)) {
            modifiers |= ModifiersInfo.ANNOTATION;
        }
        if (elementModifiers.contains(ElementModifier.SYNTHETIC)) {
            modifiers |= ModifiersInfo.SYNTHETIC;
        }
        if (elementModifiers.contains(ElementModifier.ENUM)) {
            modifiers |= ModifiersInfo.ENUM;
        }
        return modifiers;
    }
}
