/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.extension.spi.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.teavm.extension.ExtensionEnvironment;
import org.teavm.extension.introspect.IntrospectAccess;
import org.teavm.extension.introspect.IntrospectAnnotatedElement;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectElement;
import org.teavm.extension.introspect.IntrospectField;
import org.teavm.extension.introspect.IntrospectMember;
import org.teavm.extension.introspect.IntrospectMethod;

public abstract class SimpleReflectionPolicy implements ReflectionPolicy {
    private ExtensionEnvironment env;
    private List<ClassPolicy> policies = new ArrayList<>();

    @Override
    public void initialize(ExtensionEnvironment env) {
        this.env = env;
        setup();
    }

    protected abstract void setup();

    protected final ExtensionEnvironment environment() {
        return env;
    }

    @Override
    public Collection<IntrospectMember> classAccessibleMembers(IntrospectClass<?> cls) {
        var members = new HashSet<IntrospectMember>();
        for (var policy : policies) {
            if (policy.predicate.test(cls)) {
                for (var field : cls.declaredFields()) {
                    if (policy.fieldsPredicates.stream().anyMatch(p -> p.test(field))
                            || policy.membersPredicates.stream().anyMatch(p -> p.test(field))) {
                        members.add(field);
                    }
                }
                for (var method : cls.declaredMethods()) {
                    if (policy.methodsPredicates.stream().anyMatch(p -> p.test(method))
                            || policy.membersPredicates.stream().anyMatch(p -> p.test(method))) {
                        members.add(method);
                    }
                }
            }
        }
        return members;
    }

    @Override
    public boolean isClassFoundByName(IntrospectClass<?> cls) {
        for (var policy : policies) {
            if (policy.predicate.test(cls) && policy.foundByName) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ProxyListener getProxyInterfaces(ProxyInterfaceConsumer consumer) {
        return cls -> {
            for (var policy : policies) {
                if (policy.predicate.test(cls) && policy.isProxy) {
                    consumer.accept(List.of(cls));
                    break;
                }
            }
        };
    }
    
    protected final ClassPolicy allClasses() {
        return selectClasses(c -> true);   
    }

    protected final ClassPolicy selectClasses(Predicate<? super IntrospectClass<?>> predicate) {
        var policy = new ClassPolicy(predicate);
        policies.add(policy);
        return policy;
    }

    protected final ClassPolicy selectClass(String name) {
        return selectClasses(named(name));
    }

    protected static final Predicate<IntrospectElement> PUBLIC = elem -> elem.access() == IntrospectAccess.PUBLIC;
    protected static final Predicate<IntrospectElement> PROTECTED = elem -> elem.access() == IntrospectAccess.PROTECTED;
    protected static final Predicate<IntrospectElement> PRIVATE = elem -> elem.access() == IntrospectAccess.PRIVATE;
    protected static final Predicate<IntrospectElement> PACKAGE_PRIVATE =
            elem -> elem.access() == IntrospectAccess.PACKAGE_PRIVATE;
    protected static final Predicate<IntrospectElement> ABSTRACT = elem -> Modifier.isAbstract(elem.modifiers());
    protected static final Predicate<IntrospectElement> STATIC = elem -> Modifier.isStatic(elem.modifiers());
    protected static final Predicate<IntrospectElement> FINAL = elem -> Modifier.isFinal(elem.modifiers());
    protected static final Predicate<IntrospectField> TRANSIENT = elem -> Modifier.isTransient(elem.modifiers());
    protected static final Predicate<IntrospectField> VOLATILE = elem -> Modifier.isVolatile(elem.modifiers());
    protected static final Predicate<IntrospectMethod> NATIVE = elem -> Modifier.isNative(elem.modifiers());
    protected static final Predicate<IntrospectMethod> SYNCHRONIZED = elem -> Modifier.isSynchronized(elem.modifiers());
    protected static final Predicate<IntrospectClass<?>> ENUM = IntrospectClass::isEnum;
    protected static final Predicate<IntrospectClass<?>> ANNOTATION = IntrospectClass::isAnnotation;
    protected static final Predicate<IntrospectClass<?>> INTERFACE = IntrospectClass::isInterface;

    protected static Predicate<IntrospectElement> named(String name) {
        return elem -> elem.name().equals(name);
    }

    protected static Predicate<IntrospectElement> namePattern(String pattern) {
        return elem -> globMatch(pattern, elem.name());
    }
    
    protected static Predicate<IntrospectAnnotatedElement> withAnnotation(Class<? extends Annotation> type) {
        return elem -> elem.hasAnnotation(type);
    }

    protected Predicate<IntrospectAnnotatedElement> withAnnotation(String type) {
        var annot = env.findClass(type).<Annotation>asSubclassUnchecked();
        return elem -> elem.hasAnnotation(annot);
    }
    
    protected Predicate<IntrospectClass<?>> extending(Class<?> superclass) {
        var introspectSuperclass = env.findClass(superclass);
        return introspectSuperclass::isAssignableFrom;
    }

    protected Predicate<IntrospectClass<?>> extending(String superclass) {
        var introspectSuperclass = env.findClass(superclass);
        return introspectSuperclass::isAssignableFrom;
    }

    protected Predicate<IntrospectMethod> withSignature(Class<?>... types) {
        var signature = Stream.of(types).map(env::findClass).toArray(IntrospectClass<?>[]::new);
        return elem -> {
            if (signature.length != elem.parameters().size()) {
                return false;
            }
            for (var i = 0; i < signature.length; ++i) {
                if (signature[i] != elem.parameters().get(i).type()) {
                    return false;
                }
            }
            return true;
        };
    }
    
    protected Predicate<IntrospectField> ofType(Class<?> type) {
        var introspectType = env.findClass(type);
        return elem -> elem.type() == introspectType;
    }

    static boolean globMatch(String pattern, String name) {
        return globMatch(pattern, name, 0, 0);
    }

    private static boolean globMatch(String pattern, String name, int patternIndex, int nameIndex) {
        if (patternIndex == pattern.length()) {
            return nameIndex == name.length();
        }
        var starIndex = pattern.indexOf('*', patternIndex);
        if (starIndex < 0) {
            var remainingLen = pattern.length() - patternIndex;
            return nameIndex + remainingLen == name.length()
                    && pattern.regionMatches(patternIndex, name, nameIndex, remainingLen);
        }
        if (!pattern.regionMatches(patternIndex, name, nameIndex, starIndex - patternIndex)) {
            return false;
        }
        nameIndex += starIndex - patternIndex;
        if (starIndex == pattern.length() - 1) {
            return name.indexOf('.', nameIndex) < 0;
        } else if (pattern.charAt(starIndex + 1) == '*') {
            if (starIndex + 2 == pattern.length()) {
                return true;
            }
            var nextChar = pattern.charAt(starIndex + 2);
            var index = nameIndex;
            while (true) {
                var next = name.indexOf(nextChar, index);
                if (next < 0) {
                    return false;
                }
                if (globMatch(pattern, name, starIndex + 3, next + 1)) {
                    return true;
                }
                index = next + 1;
            }
        } else {
            var nextChar = pattern.charAt(starIndex + 1);
            var dotPos = name.indexOf('.', nameIndex);
            var index = nameIndex;
            while (true) {
                var next = name.indexOf(nextChar, index);
                if (next < 0 || (dotPos >= 0 && dotPos < next)) {
                    return false;
                }
                if (globMatch(pattern, name, starIndex + 2, next + 1)) {
                    return true;
                }
                index = next + 1;
            }
        }
    }

    public static class ClassPolicy {
        private Predicate<? super IntrospectClass<?>> predicate;
        private List<Predicate<? super IntrospectMember>> membersPredicates = new ArrayList<>();
        private List<Predicate<? super IntrospectMethod>> methodsPredicates = new ArrayList<>();
        private List<Predicate<? super IntrospectField>> fieldsPredicates = new ArrayList<>();
        private boolean foundByName;
        private boolean isProxy;

        private ClassPolicy(Predicate<? super IntrospectClass<?>> predicate) {
            this.predicate = predicate;
        }

        public ClassPolicy foundByName() {
            foundByName = true;
            return this;
        }
        
        public ClassPolicy proxyable() {
            isProxy = true;
            return this;
        }

        public ClassPolicy reflectableMembers(Predicate<? super IntrospectMember> predicate) {
            membersPredicates.add(predicate);
            return this;
        }

        public ClassPolicy reflectableMethods(Predicate<? super IntrospectMethod> predicate) {
            methodsPredicates.add(predicate);
            return this;
        }

        public ClassPolicy reflectableFields(Predicate<? super IntrospectField> predicate) {
            fieldsPredicates.add(predicate);
            return this;
        }
    }
}
