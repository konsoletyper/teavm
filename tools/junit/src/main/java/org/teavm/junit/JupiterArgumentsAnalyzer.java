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
package org.teavm.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ArgumentsSources;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvFileSources;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.CsvSources;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSources;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.FieldSources;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.MethodSources;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullEnum;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.ValueSources;
import org.teavm.junit.JupiterArgumentsPlan.StaticValue;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

/**
 * Builds a {@link JupiterArgumentsPlan} for a {@code @ParameterizedTest} method by reading its
 * argument source annotations via reflection. Only loaded when a test actually is parameterized,
 * so {@code junit-jupiter-params} doesn't have to be on the classpath otherwise.
 *
 * <p>Conversion of compile-time values to parameter types follows Jupiter's implicit conversion
 * rules: widening primitive conversions, and conversion from {@code String} to primitives,
 * enums, {@code Class}, or to any type having a single-{@code String} static factory method or
 * constructor. Explicit converters ({@code @ConvertWith}), aggregators and custom
 * {@code @ArgumentsSource} providers can't be evaluated ahead of time and are reported as errors.
 */
final class JupiterArgumentsAnalyzer {
    private static final String PARAMS_PACKAGE = "org.junit.jupiter.params.";

    private final Method method;
    private final Class<?> testClass;
    private final Class<?>[] parameterTypes;
    private final List<JupiterArgumentsPlan.Segment> segments = new ArrayList<>();

    private JupiterArgumentsAnalyzer(Method method, Class<?> testClass) {
        this.method = method;
        this.testClass = testClass;
        parameterTypes = method.getParameterTypes();
    }

    static JupiterArgumentsPlan analyze(Method method, Class<?> testClass) {
        var analyzer = new JupiterArgumentsAnalyzer(method, testClass);
        analyzer.checkParameters();
        analyzer.collectSources(method.getAnnotations(), new HashSet<>());
        var testInstance = testClass.getAnnotation(TestInstance.class);
        var sharedInstance = testInstance != null && testInstance.value() == TestInstance.Lifecycle.PER_CLASS;
        return new JupiterArgumentsPlan(analyzer.segments, sharedInstance);
    }

    private void checkParameters() {
        for (var parameter : method.getParameters()) {
            for (var annot : parameter.getAnnotations()) {
                var name = annot.annotationType().getName();
                if (name.startsWith(PARAMS_PACKAGE + "converter.") || name.startsWith(PARAMS_PACKAGE + "aggregator.")) {
                    throw error("@" + annot.annotationType().getSimpleName() + " on parameter "
                            + parameter.getName() + " is not supported");
                }
            }
        }
    }

    private void collectSources(Annotation[] annotations, Set<Class<?>> visited) {
        for (var annot : annotations) {
            collectSource(annot, visited);
        }
    }

    private void collectSource(Annotation annot, Set<Class<?>> visited) {
        if (annot instanceof ValueSource) {
            valueSource((ValueSource) annot);
        } else if (annot instanceof ValueSources) {
            for (var source : ((ValueSources) annot).value()) {
                valueSource(source);
            }
        } else if (annot instanceof CsvSource) {
            csvSource((CsvSource) annot);
        } else if (annot instanceof CsvSources) {
            for (var source : ((CsvSources) annot).value()) {
                csvSource(source);
            }
        } else if (annot instanceof EnumSource) {
            enumSource((EnumSource) annot);
        } else if (annot instanceof EnumSources) {
            for (var source : ((EnumSources) annot).value()) {
                enumSource(source);
            }
        } else if (annot instanceof MethodSource) {
            methodSource((MethodSource) annot);
        } else if (annot instanceof MethodSources) {
            for (var source : ((MethodSources) annot).value()) {
                methodSource(source);
            }
        } else if (annot instanceof FieldSource) {
            fieldSource((FieldSource) annot);
        } else if (annot instanceof FieldSources) {
            for (var source : ((FieldSources) annot).value()) {
                fieldSource(source);
            }
        } else if (annot instanceof NullSource) {
            nullSource();
        } else if (annot instanceof EmptySource) {
            emptySource();
        } else if (annot instanceof NullAndEmptySource) {
            nullSource();
            emptySource();
        } else if (annot instanceof CsvFileSource || annot instanceof CsvFileSources) {
            throw error("@CsvFileSource is not supported");
        } else if (annot instanceof ArgumentsSource || annot instanceof ArgumentsSources) {
            throw error("custom @ArgumentsSource providers are not supported");
        } else {
            // Composed annotations, meta-annotated with argument sources
            var type = annot.annotationType();
            if (!type.getName().startsWith("java.lang.") && visited.add(type)) {
                collectSources(type.getAnnotations(), visited);
            }
        }
    }

    private void valueSource(ValueSource source) {
        var values = new ArrayList<Object>();
        addArrayElements(values, source.shorts());
        addArrayElements(values, source.bytes());
        addArrayElements(values, source.ints());
        addArrayElements(values, source.longs());
        addArrayElements(values, source.floats());
        addArrayElements(values, source.doubles());
        addArrayElements(values, source.chars());
        addArrayElements(values, source.booleans());
        addArrayElements(values, source.strings());
        addArrayElements(values, source.classes());
        for (var value : values) {
            addSingleValueRow(convert(value, 0));
        }
    }

    private static void addArrayElements(List<Object> target, Object array) {
        var length = Array.getLength(array);
        for (var i = 0; i < length; ++i) {
            target.add(Array.get(array, i));
        }
    }

    private void csvSource(CsvSource source) {
        var hasValues = source.value().length > 0;
        var hasTextBlock = !source.textBlock().isEmpty();
        if (hasValues == hasTextBlock) {
            throw error("@CsvSource must declare either 'value' or 'textBlock', but not both");
        }
        var delimiterString = source.delimiterString();
        if (source.delimiter() != '\0' && !delimiterString.isEmpty()) {
            throw error("@CsvSource can't declare both 'delimiter' and 'delimiterString'");
        }
        if (delimiterString.isEmpty()) {
            delimiterString = String.valueOf(source.delimiter() != '\0' ? source.delimiter() : ',');
        }
        var parser = new JupiterCsvParser(delimiterString, source.quoteCharacter(), source.emptyValue(),
                Set.of(source.nullValues()), source.ignoreLeadingAndTrailingWhitespace());

        List<List<String>> records;
        try {
            if (hasTextBlock) {
                records = parser.parseTextBlock(source.textBlock());
            } else {
                records = new ArrayList<>();
                for (var line : source.value()) {
                    records.add(parser.parseRecord(line));
                }
            }
        } catch (IllegalArgumentException e) {
            throw error("error parsing @CsvSource: " + e.getMessage());
        }
        if (source.useHeadersInDisplayName() && !records.isEmpty()) {
            records = records.subList(1, records.size());
        }

        for (var record : records) {
            if (record.size() < parameterTypes.length) {
                throw error("@CsvSource record " + record + " provides " + record.size() + " values, but "
                        + parameterTypes.length + " parameters are declared");
            }
            var row = new ArrayList<StaticValue>();
            for (var i = 0; i < parameterTypes.length; ++i) {
                row.add(convert(record.get(i), i));
            }
            segments.add(new JupiterArgumentsPlan.StaticRow(row));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void enumSource(EnumSource source) {
        Class<? extends Enum> enumType = source.value();
        if (enumType == NullEnum.class) {
            if (parameterTypes.length == 0 || !parameterTypes[0].isEnum()) {
                throw error("@EnumSource must declare enum type, since the first parameter is not enum");
            }
            enumType = (Class<? extends Enum>) parameterTypes[0];
        }

        var constants = new ArrayList<Enum<?>>(EnumSet.allOf((Class) enumType));
        var from = optionalStringAttribute(source, "from");
        var to = optionalStringAttribute(source, "to");
        if (!from.isEmpty() || !to.isEmpty()) {
            var fromIndex = from.isEmpty() ? 0 : Enum.valueOf((Class) enumType, from).ordinal();
            var toIndex = to.isEmpty() ? constants.size() - 1 : Enum.valueOf((Class) enumType, to).ordinal();
            if (fromIndex > toIndex) {
                throw error("@EnumSource 'from' must not come after 'to'");
            }
            constants = new ArrayList<>(constants.subList(fromIndex, toIndex + 1));
        }

        var names = Arrays.asList(source.names());
        var mode = source.mode().name();
        switch (mode) {
            case "INCLUDE":
            case "EXCLUDE": {
                var existing = new HashSet<String>();
                for (var constant : constants) {
                    existing.add(constant.name());
                }
                for (var name : names) {
                    if (!existing.contains(name)) {
                        throw error("@EnumSource refers to unknown constant " + name + " of " + enumType.getName());
                    }
                }
                if (mode.equals("INCLUDE")) {
                    if (!names.isEmpty()) {
                        constants.removeIf(c -> !names.contains(c.name()));
                    }
                } else {
                    constants.removeIf(c -> names.contains(c.name()));
                }
                break;
            }
            case "MATCH_ALL":
            case "MATCH_ANY":
            case "MATCH_NONE": {
                var patterns = new ArrayList<Pattern>();
                for (var name : names) {
                    patterns.add(Pattern.compile(name));
                }
                constants.removeIf(c -> {
                    var matched = 0;
                    for (var pattern : patterns) {
                        if (pattern.matcher(c.name()).matches()) {
                            matched++;
                        }
                    }
                    switch (mode) {
                        case "MATCH_ALL":
                            return matched != patterns.size();
                        case "MATCH_ANY":
                            return matched == 0;
                        default:
                            return matched != 0;
                    }
                });
                break;
            }
            default:
                throw error("@EnumSource mode " + mode + " is not supported");
        }

        for (var constant : constants) {
            addSingleValueRow(convert(constant, 0));
        }
    }

    private static String optionalStringAttribute(Annotation annot, String name) {
        try {
            var value = annot.annotationType().getMethod(name).invoke(annot);
            return value instanceof String ? (String) value : "";
        } catch (NoSuchMethodException e) {
            return "";
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void nullSource() {
        requireFirstParameter("@NullSource");
        if (parameterTypes[0].isPrimitive()) {
            throw error("@NullSource can't provide null for parameter of primitive type "
                    + parameterTypes[0].getName());
        }
        addSingleValueRow((pe, type) -> pe.constantNull(type));
    }

    private void emptySource() {
        requireFirstParameter("@EmptySource");
        var type = parameterTypes[0];
        StaticValue value;
        if (type == String.class) {
            value = (pe, target) -> pe.constant("").cast(target);
        } else if (type == List.class || type == Collection.class) {
            value = (pe, target) -> pe.invoke(Collections.class, "emptyList", List.class).cast(target);
        } else if (type == Set.class) {
            value = (pe, target) -> pe.invoke(Collections.class, "emptySet", Set.class).cast(target);
        } else if (type == Map.class) {
            value = (pe, target) -> pe.invoke(Collections.class, "emptyMap", Map.class).cast(target);
        } else if (type == SortedSet.class || type == NavigableSet.class) {
            value = (pe, target) -> pe.construct(TreeSet.class).cast(target);
        } else if (type == SortedMap.class || type == NavigableMap.class) {
            value = (pe, target) -> pe.construct(TreeMap.class).cast(target);
        } else if (type.isArray()) {
            var componentType = ValueType.parse(type.getComponentType());
            value = (pe, target) -> pe.constructArray(componentType, 0).cast(target);
        } else {
            throw error("@EmptySource can't provide empty value for parameter of type " + type.getName());
        }
        addSingleValueRow(value);
    }

    private void requireFirstParameter(String sourceName) {
        if (parameterTypes.length == 0) {
            throw error(sourceName + " requires test method to declare at least one parameter");
        }
    }

    private void addSingleValueRow(StaticValue value) {
        if (parameterTypes.length > 1) {
            throw error("single-value argument source can't provide arguments for " + parameterTypes.length
                    + " parameters");
        }
        segments.add(new JupiterArgumentsPlan.StaticRow(List.of(value)));
    }

    private void methodSource(MethodSource source) {
        var names = source.value();
        if (names.length == 0) {
            names = new String[] { "" };
        }
        for (var name : names) {
            var factory = resolveFactoryMethod(name);
            var isStatic = Modifier.isStatic(factory.getModifiers());
            if (!isStatic && !factory.getDeclaringClass().isAssignableFrom(testClass)) {
                throw error("factory method " + factory + " must be static");
            }
            var reference = new MethodReference(factory.getDeclaringClass().getName(),
                    TeaVMTestExecutionSupport.getDescriptor(factory));
            segments.add(new JupiterArgumentsPlan.FactoryMethod(reference, isStatic,
                    Modifier.isPrivate(factory.getModifiers())));
        }
    }

    private Method resolveFactoryMethod(String name) {
        var owner = testClass;
        var methodName = name.trim();
        var hashIndex = methodName.indexOf('#');
        if (hashIndex >= 0) {
            owner = loadClass(methodName.substring(0, hashIndex));
            methodName = methodName.substring(hashIndex + 1);
        }
        var parenIndex = methodName.indexOf('(');
        if (parenIndex >= 0) {
            if (!methodName.substring(parenIndex).replace(" ", "").equals("()")) {
                throw error("factory method " + name + " with parameters is not supported");
            }
            methodName = methodName.substring(0, parenIndex);
        }
        if (methodName.isEmpty()) {
            methodName = method.getName();
        }

        for (var cls = owner; cls != null; cls = cls.getSuperclass()) {
            for (var candidate : cls.getDeclaredMethods()) {
                if (candidate.getName().equals(methodName) && candidate.getParameterCount() == 0
                        && !candidate.isSynthetic()) {
                    return candidate;
                }
            }
        }
        throw error("could not find parameterless factory method " + methodName + " in " + owner.getName());
    }

    private void fieldSource(FieldSource source) {
        var names = source.value();
        if (names.length == 0) {
            names = new String[] { "" };
        }
        for (var name : names) {
            var field = resolveField(name);
            var isStatic = Modifier.isStatic(field.getModifiers());
            if (!isStatic && !field.getDeclaringClass().isAssignableFrom(testClass)) {
                throw error("field " + field + " must be static");
            }
            var reference = new FieldReference(field.getDeclaringClass().getName(), field.getName());
            segments.add(new JupiterArgumentsPlan.FactoryField(reference, ValueType.parse(field.getType()),
                    isStatic));
        }
    }

    private Field resolveField(String name) {
        var owner = testClass;
        var fieldName = name.trim();
        var hashIndex = fieldName.indexOf('#');
        if (hashIndex >= 0) {
            owner = loadClass(fieldName.substring(0, hashIndex));
            fieldName = fieldName.substring(hashIndex + 1);
        }
        if (fieldName.isEmpty()) {
            fieldName = method.getName();
        }
        for (var cls = owner; cls != null; cls = cls.getSuperclass()) {
            for (var candidate : cls.getDeclaredFields()) {
                if (candidate.getName().equals(fieldName)) {
                    return candidate;
                }
            }
        }
        throw error("could not find field " + fieldName + " in " + owner.getName());
    }

    private Class<?> loadClass(String name) {
        try {
            return Class.forName(name.trim(), false, testClass.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw error("class " + name + " not found");
        }
    }

    private StaticValue convert(Object value, int parameterIndex) {
        var type = parameterTypes[parameterIndex];
        try {
            return convert(value, type);
        } catch (IllegalArgumentException e) {
            throw error("can't convert argument '" + value + "' to type " + type.getName() + " of parameter #"
                    + (parameterIndex + 1) + ": " + e.getMessage());
        }
    }

    private static StaticValue convert(Object value, Class<?> type) {
        if (value == null) {
            if (type.isPrimitive()) {
                throw new IllegalArgumentException("null can't be assigned to primitive type");
            }
            return (pe, target) -> pe.constantNull(target);
        }

        var primitiveType = type.isPrimitive() ? type : unbox(type);
        if (primitiveType != null && (value instanceof String || unbox(value.getClass()) != null)) {
            return primitiveConstant(convertPrimitive(value, primitiveType));
        }

        if (type.isInstance(value)) {
            return constant(value);
        }

        if (value instanceof String string) {
            if (type.isEnum()) {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                var constant = Enum.valueOf((Class) type, string);
                return constant(constant);
            }
            if (type == Class.class) {
                try {
                    return constant(Class.forName(string));
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("class not found");
                }
            }
            return stringFactory(string, type);
        }

        throw new IllegalArgumentException("no implicit conversion from " + value.getClass().getName());
    }

    private static StaticValue constant(Object value) {
        var unboxed = unbox(value.getClass());
        if (unboxed != null) {
            return primitiveConstant(value);
        }
        if (value instanceof String string) {
            return (pe, target) -> pe.constant(string).cast(target);
        }
        if (value instanceof Class<?>) {
            var cls = ValueType.parse((Class<?>) value);
            return (pe, target) -> pe.constant(cls).cast(target);
        }
        if (value instanceof Enum<?> constant) {
            var enumType = constant.getDeclaringClass().getName();
            var field = new FieldReference(enumType, constant.name());
            return (pe, target) -> pe.getField(field, ValueType.object(enumType)).cast(target);
        }
        throw new IllegalArgumentException("value of type " + value.getClass().getName()
                + " can't be embedded into compiled code");
    }

    private static StaticValue primitiveConstant(Object value) {
        return (pe, target) -> {
            if (value instanceof Boolean) {
                return pe.constant((Boolean) value ? 1 : 0).cast(boolean.class).cast(target);
            } else if (value instanceof Character) {
                return pe.constant((int) (Character) value).cast(char.class).cast(target);
            } else if (value instanceof Byte) {
                return pe.constant((int) (Byte) value).cast(byte.class).cast(target);
            } else if (value instanceof Short) {
                return pe.constant((int) (Short) value).cast(short.class).cast(target);
            } else if (value instanceof Integer) {
                return pe.constant((Integer) value).cast(target);
            } else if (value instanceof Long) {
                return pe.constant((Long) value).cast(target);
            } else if (value instanceof Float) {
                return pe.constant((Float) value).cast(target);
            } else {
                return pe.constant((Double) value).cast(target);
            }
        };
    }

    private static StaticValue stringFactory(String value, Class<?> type) {
        Method factory = null;
        var factoryCount = 0;
        for (var candidate : type.getDeclaredMethods()) {
            if (Modifier.isStatic(candidate.getModifiers()) && !Modifier.isPrivate(candidate.getModifiers())
                    && candidate.getReturnType() == type && candidate.getParameterCount() == 1
                    && (candidate.getParameterTypes()[0] == String.class
                        || candidate.getParameterTypes()[0] == CharSequence.class)) {
                factory = candidate;
                factoryCount++;
            }
        }
        if (factoryCount == 1) {
            var reference = new MethodReference(type.getName(), TeaVMTestExecutionSupport.getDescriptor(factory));
            var parameterType = factory.getParameterTypes()[0];
            return (pe, target) -> pe.invoke(reference, pe.constant(value).cast(parameterType)).cast(target);
        }

        Constructor<?> constructor;
        try {
            constructor = type.getDeclaredConstructor(String.class);
        } catch (NoSuchMethodException e) {
            constructor = null;
        }
        if (constructor != null && !Modifier.isPrivate(constructor.getModifiers())
                && !Modifier.isAbstract(type.getModifiers())) {
            return (pe, target) -> pe.construct(type.getName(), pe.constant(value)).cast(target);
        }

        throw new IllegalArgumentException("no implicit conversion from String");
    }

    private static Object convertPrimitive(Object value, Class<?> type) {
        if (value instanceof String string) {
            try {
                if (type == boolean.class) {
                    if (string.equalsIgnoreCase("true")) {
                        return true;
                    } else if (string.equalsIgnoreCase("false")) {
                        return false;
                    }
                    throw new IllegalArgumentException("not a boolean value");
                } else if (type == char.class) {
                    if (string.length() != 1) {
                        throw new IllegalArgumentException("string must have exactly one character");
                    }
                    return string.charAt(0);
                } else if (type == byte.class) {
                    return Byte.decode(string);
                } else if (type == short.class) {
                    return Short.decode(string);
                } else if (type == int.class) {
                    return Integer.decode(string);
                } else if (type == long.class) {
                    return Long.decode(string);
                } else if (type == float.class) {
                    return Float.parseFloat(string);
                } else {
                    return Double.parseDouble(string);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("not a number");
            }
        }

        var sourceType = unbox(value.getClass());
        if (sourceType == type) {
            return value;
        }
        if (sourceType == boolean.class || type == boolean.class || type == char.class) {
            throw new IllegalArgumentException("incompatible types");
        }

        var rank = primitiveRank(sourceType);
        var targetRank = primitiveRank(type);
        if (targetRank <= rank || (sourceType == char.class && type == short.class)) {
            throw new IllegalArgumentException("narrowing conversion is not allowed");
        }
        if (sourceType == byte.class && type == char.class) {
            throw new IllegalArgumentException("incompatible types");
        }

        var number = value instanceof Character ? (Number) (int) (Character) value : (Number) value;
        if (type == short.class) {
            return number.shortValue();
        } else if (type == int.class) {
            return number.intValue();
        } else if (type == long.class) {
            return number.longValue();
        } else if (type == float.class) {
            return number.floatValue();
        } else {
            return number.doubleValue();
        }
    }

    private static int primitiveRank(Class<?> type) {
        if (type == byte.class) {
            return 0;
        } else if (type == short.class || type == char.class) {
            return 1;
        } else if (type == int.class) {
            return 2;
        } else if (type == long.class) {
            return 3;
        } else if (type == float.class) {
            return 4;
        } else {
            return 5;
        }
    }

    private static Class<?> unbox(Class<?> type) {
        if (type == Boolean.class) {
            return boolean.class;
        } else if (type == Character.class) {
            return char.class;
        } else if (type == Byte.class) {
            return byte.class;
        } else if (type == Short.class) {
            return short.class;
        } else if (type == Integer.class) {
            return int.class;
        } else if (type == Long.class) {
            return long.class;
        } else if (type == Float.class) {
            return float.class;
        } else if (type == Double.class) {
            return double.class;
        }
        return null;
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("Parameterized test " + method.getDeclaringClass().getName() + "."
                + method.getName() + ": " + message);
    }
}
