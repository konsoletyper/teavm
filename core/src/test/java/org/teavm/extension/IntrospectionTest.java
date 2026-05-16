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
package org.teavm.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectField;
import org.teavm.extension.introspect.IntrospectGenericArray;
import org.teavm.extension.introspect.IntrospectParameterizedType;
import org.teavm.extension.introspect.IntrospectProjection;
import org.teavm.extension.introspect.IntrospectTypeVariable;
import org.teavm.extension.introspect.IntrospectWorld;
import org.teavm.extension.introspect.Introspection;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ReferenceCache;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.parsing.ClasspathResourceProvider;

public class IntrospectionTest {
    private IntrospectWorld world;

    public IntrospectionTest() {
        var classLoader = IntrospectionTest.class.getClassLoader();
        var resourceProvider = new ClasspathResourceProvider(classLoader);
        var classHolderSource = new ClasspathClassHolderSource(resourceProvider, new ReferenceCache());
        world = new Introspection(new ClassHierarchy(classHolderSource), classLoader);
    }

    @Test
    public void classRead() {
        var a = world.findClass(A.class.getName());
        assertEquals(A.class.getName(), a.name());
        assertEquals("A", a.simpleName());
        assertFalse(a.isInterface());
    }

    @Test
    public void fieldRead() {
        var a = world.findClass(A.class.getName());
        var f = a.field("f");
        assertEquals("f", f.name());
        assertEquals(world.findClass(int.class), f.type());
    }

    @Test
    public void primitiveClass() {
        var intClass = world.findClass(int.class);
        assertTrue(intClass.isPrimitive());
        assertEquals("int", intClass.name());
        assertEquals("int", intClass.simpleName());
        assertFalse(world.findClass(A.class.getName()).isPrimitive());
    }

    @Test
    public void allPrimitiveTypes() {
        assertEquals("boolean", world.findClass(boolean.class).name());
        assertEquals("byte", world.findClass(byte.class).name());
        assertEquals("short", world.findClass(short.class).name());
        assertEquals("char", world.findClass(char.class).name());
        assertEquals("int", world.findClass(int.class).name());
        assertEquals("long", world.findClass(long.class).name());
        assertEquals("float", world.findClass(float.class).name());
        assertEquals("double", world.findClass(double.class).name());
    }

    @Test
    public void arrayClass() {
        var intArrayClass = world.findClass(int[].class);
        assertTrue(intArrayClass.isArray());
        assertFalse(intArrayClass.isPrimitive());
        assertEquals("int", intArrayClass.componentType().name());
        assertEquals("int[]", intArrayClass.simpleName());
    }

    @Test
    public void objectArrayClass() {
        var strArrayClass = world.findClass(String[].class);
        assertTrue(strArrayClass.isArray());
        assertEquals(String.class.getName(), strArrayClass.componentType().name());
        assertEquals("String[]", strArrayClass.simpleName());
    }

    @Test
    public void interfaceClass() {
        var i = world.findClass(I.class.getName());
        assertTrue(i.isInterface());
        assertFalse(i.isPrimitive());
        assertFalse(i.isArray());
        assertFalse(i.isEnum());
        assertFalse(i.isAnnotation());
    }

    @Test
    public void annotationClass() {
        var ann = world.findClass(Ann.class.getName());
        assertTrue(ann.isAnnotation());
        assertTrue(ann.isInterface());
        assertFalse(ann.isEnum());
    }

    @Test
    public void enumClass() {
        var dir = world.findClass(Direction.class.getName());
        assertTrue(dir.isEnum());
        assertFalse(dir.isInterface());
        assertFalse(dir.isPrimitive());
    }

    @Test
    public void enumConstants() {
        var dir = world.findClass(Direction.class.getName());
        var constants = dir.enumConstants();
        assertEquals(4, constants.size());
        var names = constants.stream()
                .map(IntrospectField::name)
                .sorted()
                .collect(Collectors.toList());
        assertEquals(List.of("EAST", "NORTH", "SOUTH", "WEST"), names);
    }

    @Test
    public void enumConstantIsEnumConstant() {
        var dir = world.findClass(Direction.class.getName());
        var northField = dir.declaredField("NORTH");
        assertTrue(northField.isEnumConstant());
        var regularField = world.findClass(A.class.getName()).field("f");
        assertFalse(regularField.isEnumConstant());
    }

    @Test
    public void nonEnumClassHasNoEnumConstants() {
        var a = world.findClass(A.class.getName());
        assertTrue(a.enumConstants().isEmpty());
    }

    @Test
    public void superclassRead() {
        var child = world.findClass(Child.class.getName());
        var base = world.findClass(Base.class.getName());
        assertEquals(base.name(), child.superclass().name());
    }

    @Test
    public void superclassOfObjectIsNull() {
        var obj = world.findClass(Object.class.getName());
        assertNull(obj.superclass());
    }

    @Test
    public void interfacesRead() {
        var child = world.findClass(Child.class.getName());
        assertEquals(1, child.interfaces().size());
        assertEquals(I.class.getName(), child.interfaces().get(0).name());
    }

    @Test
    public void classWithNoInterfacesHasEmptyList() {
        var base = world.findClass(Base.class.getName());
        assertTrue(base.interfaces().isEmpty());
    }

    @Test
    public void isAssignableFromSubclass() {
        var child = world.findClass(Child.class.getName());
        var base = world.findClass(Base.class.getName());
        assertTrue(base.isAssignableFrom(child));
        assertFalse(child.isAssignableFrom(base));
    }

    @Test
    public void isAssignableFromInterface() {
        var child = world.findClass(Child.class.getName());
        var iface = world.findClass(I.class.getName());
        assertTrue(iface.isAssignableFrom(child));
        assertFalse(child.isAssignableFrom(iface));
    }

    @Test
    public void isAssignableFromSelf() {
        var base = world.findClass(Base.class.getName());
        assertTrue(base.isAssignableFrom(base));
    }

    @Test
    public void isAssignableFromJavaClass() {
        var child = world.findClass(Child.class.getName());
        assertTrue(child.isAssignableFrom(Child.class));
        assertFalse(child.isAssignableFrom(Base.class));
    }

    @Test
    public void findClassUnknownReturnsNull() {
        assertNull(world.findClass("com.example.DoesNotExist"));
    }

    @Test
    public void classModifiersAbstract() {
        var base = world.findClass(Base.class.getName());
        assertTrue(Modifier.isAbstract(base.modifiers()));
        assertTrue(Modifier.isPublic(base.modifiers()));
    }

    @Test
    public void classModifiersFinal() {
        var fin = world.findClass(FinalClass.class.getName());
        assertTrue(Modifier.isFinal(fin.modifiers()));
        assertFalse(Modifier.isAbstract(fin.modifiers()));
    }

    @Test
    public void classModifiersInterface() {
        var iface = world.findClass(I.class.getName());
        assertTrue(Modifier.isInterface(iface.modifiers()));
    }

    @Test
    public void declaredFieldsRead() {
        var base = world.findClass(Base.class.getName());
        var fieldNames = base.declaredFields().stream()
                .map(IntrospectField::name)
                .sorted()
                .collect(Collectors.toList());
        assertEquals(List.of("x", "y"), fieldNames);
    }

    @Test
    public void fieldsOnlyPublicInherited() {
        var child = world.findClass(Child.class.getName());
        var names = child.fields().stream()
                .map(IntrospectField::name)
                .collect(Collectors.toList());
        assertTrue(names.contains("x"));
        assertTrue(names.contains("z"));
        assertFalse(names.contains("y"));
    }

    @Test
    public void declaredFieldMissingReturnsNull() {
        var base = world.findClass(Base.class.getName());
        assertNull(base.declaredField("nonExistentField"));
    }

    @Test
    public void fieldMissingReturnsNull() {
        var base = world.findClass(Base.class.getName());
        assertNull(base.field("y"));
    }

    @Test
    public void fieldModifiers() {
        var annotated = world.findClass(Annotated.class.getName());
        var publicField = annotated.declaredField("publicField");
        assertTrue(Modifier.isPublic(publicField.modifiers()));
        assertFalse(Modifier.isPrivate(publicField.modifiers()));
        var privateField = annotated.declaredField("privateField");
        assertTrue(Modifier.isPrivate(privateField.modifiers()));
        assertFalse(Modifier.isPublic(privateField.modifiers()));
        var constant = annotated.declaredField("CONSTANT");
        assertTrue(Modifier.isStatic(constant.modifiers()));
        assertTrue(Modifier.isFinal(constant.modifiers()));
    }

    @Test
    public void fieldDeclaringClass() {
        var base = world.findClass(Base.class.getName());
        var xField = base.declaredField("x");
        assertEquals(base.name(), xField.declaringClass().name());
    }

    @Test
    public void fieldType() {
        var base = world.findClass(Base.class.getName());
        assertEquals("int", base.declaredField("x").type().name());
        assertEquals(String.class.getName(), base.declaredField("y").type().name());
    }

    @Test
    public void declaredMethodsRead() {
        var base = world.findClass(Base.class.getName());
        var methodNames = base.declaredMethods().stream()
                .filter(m -> !m.isConstructor())
                .map(m -> m.name())
                .sorted()
                .collect(Collectors.toList());
        assertTrue(methodNames.contains("doubled"));
        assertTrue(methodNames.contains("helper"));
    }

    @Test
    public void methodsIncludesInherited() {
        var child = world.findClass(Child.class.getName());
        var names = child.methods().stream()
                .map(m -> m.name())
                .collect(Collectors.toList());
        assertTrue(names.contains("doubled"));
        assertTrue(names.contains("helper"));
        assertTrue(names.contains("greet"));
    }

    @Test
    public void constructorDetection() {
        var base = world.findClass(Base.class.getName());
        var constructor = base.declaredMethods().stream()
                .filter(m -> m.isConstructor())
                .findFirst()
                .orElse(null);
        assertNotNull(constructor);
    }

    @Test
    public void regularMethodIsNotConstructor() {
        var base = world.findClass(Base.class.getName());
        var method = base.declaredJavaMethod("doubled", int.class);
        assertNotNull(method);
        assertFalse(method.isConstructor());
    }

    @Test
    public void methodReturnType() {
        var base = world.findClass(Base.class.getName());
        var method = base.declaredJavaMethod("doubled", int.class);
        assertNotNull(method);
        assertEquals("int", method.returnType().name());
    }

    @Test
    public void methodReturnTypeVoid() {
        var base = world.findClass(Base.class.getName());
        var method = base.declaredJavaMethod("helper");
        assertNotNull(method);
        assertEquals("void", method.returnType().name());
    }

    @Test
    public void methodParameters() {
        var base = world.findClass(Base.class.getName());
        var method = base.declaredJavaMethod("doubled", int.class);
        assertEquals(1, method.parameters().size());
        assertEquals("int", method.parameters().get(0).type().name());
    }

    @Test
    public void methodNoParameters() {
        var base = world.findClass(Base.class.getName());
        var method = base.declaredJavaMethod("helper");
        assertNotNull(method);
        assertTrue(method.parameters().isEmpty());
    }

    @Test
    public void methodWithMultipleParams() {
        var annotated = world.findClass(Annotated.class.getName());
        var method = annotated.declaredJavaMethod("twoParams", int.class, String.class);
        assertNotNull(method);
        assertEquals(2, method.parameters().size());
        assertEquals("int", method.parameters().get(0).type().name());
        assertEquals(String.class.getName(), method.parameters().get(1).type().name());
    }

    @Test
    public void methodInheritedLookup() {
        var child = world.findClass(Child.class.getName());
        var method = child.javaMethod("helper");
        assertNotNull(method);
        assertEquals(Base.class.getName(), method.declaringClass().name());
    }

    @Test
    public void declaredMethodMissingReturnsNull() {
        var base = world.findClass(Base.class.getName());
        assertNull(base.declaredJavaMethod("nonExistentMethod"));
    }

    @Test
    public void methodModifiersAbstract() {
        var base = world.findClass(Base.class.getName());
        var doubled = base.declaredJavaMethod("doubled", int.class);
        assertTrue(Modifier.isAbstract(doubled.modifiers()));
        assertTrue(Modifier.isPublic(doubled.modifiers()));
    }

    @Test
    public void methodModifiersConcrete() {
        var base = world.findClass(Base.class.getName());
        var helper = base.declaredJavaMethod("helper");
        assertFalse(Modifier.isAbstract(helper.modifiers()));
        assertTrue(Modifier.isPublic(helper.modifiers()));
    }

    @Test
    public void methodDeclaringClass() {
        var base = world.findClass(Base.class.getName());
        var doubled = base.declaredJavaMethod("doubled", int.class);
        assertEquals(base.name(), doubled.declaringClass().name());
    }

    @Test
    public void classAnnotation() {
        var annotated = world.findClass(Annotated.class.getName());
        assertTrue(annotated.hasAnnotation(Ann.class));
        var ann = annotated.annotation(Ann.class);
        assertEquals("class-level", ann.value("str"));
        assertEquals(42, ann.value("count"));
    }

    @Test
    public void fieldAnnotation() {
        var annotated = world.findClass(Annotated.class.getName());
        var field = annotated.declaredField("publicField");
        assertTrue(field.hasAnnotation(Ann.class));
        var ann = field.annotation(Ann.class);
        assertEquals("field-ann", ann.value("str"));
    }

    @Test
    public void methodAnnotation() {
        var annotated = world.findClass(Annotated.class.getName());
        var method = annotated.declaredJavaMethod("annotatedMethod");
        assertNotNull(method);
        assertTrue(method.hasAnnotation(Ann.class));
        var ann = method.annotation(Ann.class);
        assertEquals("method-ann", ann.value("str"));
        assertEquals(7, ann.value("count"));
    }

    @Test
    public void unannotatedClassHasNoAnnotation() {
        var a = world.findClass(A.class.getName());
        assertFalse(a.hasAnnotation(Ann.class));
        assertNull(a.annotation(Ann.class));
    }

    @Test
    public void allAnnotations() {
        var annotated = world.findClass(Annotated.class.getName());
        assertEquals(1, annotated.allAnnotations().size());
    }

    @Test
    public void annotationDefaultValues() {
        var cls = world.findClass(AnnotatedWithDefaults.class.getName());
        var ann = cls.annotation(Ann.class);
        assertNotNull(ann);
        assertEquals("", ann.value("str"));
        assertEquals(0, ann.value("count"));
    }

    @Test
    public void annotationTypeCheck() {
        var annotated = world.findClass(Annotated.class.getName());
        var ann = annotated.annotation(Ann.class);
        assertEquals(Ann.class.getName(), ann.type().name());
    }

    @Test
    public void parameterAnnotation() {
        var annotated = world.findClass(Annotated.class.getName());
        var method = annotated.declaredJavaMethod("annotatedMethod");
        assertNotNull(method);
        assertEquals(0, method.parameters().size());
        var methodWithParamAnn = annotated.declaredJavaMethod("methodWithParamAnnotation", String.class);
        assertNotNull(methodWithParamAnn);
        assertEquals(1, methodWithParamAnn.parameters().size());
        assertTrue(methodWithParamAnn.parameters().get(0).hasAnnotation(Ann.class));
        var paramAnn = methodWithParamAnn.parameters().get(0).annotation(Ann.class);
        assertEquals("param-ann", paramAnn.value("str"));
    }

    // ===== Generics =====

    @Test
    public void classWithNoTypeParameters() {
        var a = world.findClass(A.class.getName());
        assertTrue(a.typeParameters().isEmpty());
    }

    @Test
    public void classTypeParameters() {
        var box = world.findClass(Box.class.getName());
        assertEquals(1, box.typeParameters().size());
        assertEquals("T", box.typeParameters().get(0).name());
    }

    @Test
    public void typeVariableWithNoBound() {
        var box = world.findClass(Box.class.getName());
        var t = box.typeParameters().get(0);
        assertTrue(t.superTypes().isEmpty());
    }

    @Test
    public void typeVariableWithBound() {
        var box = world.findClass(BoundedBox.class.getName());
        var t = box.typeParameters().get(0);
        assertEquals(1, t.superTypes().size());
        var bound = t.superTypes().get(0);
        assertTrue(bound instanceof IntrospectParameterizedType);
        var paramType = (IntrospectParameterizedType) bound;
        assertEquals(Comparable.class.getName(), paramType.rawType().name());
    }

    @Test
    public void multipleTypeParameters() {
        var pair = world.findClass(Pair.class.getName());
        assertEquals(2, pair.typeParameters().size());
        var names = pair.typeParameters().stream()
                .map(IntrospectTypeVariable::name)
                .collect(Collectors.toList());
        assertEquals(List.of("A", "B"), names);
    }

    @Test
    public void fieldGenericTypeRaw() {
        var a = world.findClass(A.class.getName());
        var f = a.field("f");
        assertTrue(f.genericType() instanceof IntrospectClass);
        assertEquals("int", ((IntrospectClass<?>) f.genericType()).name());
    }

    @Test
    public void fieldGenericTypeVariable() {
        var box = world.findClass(Box.class.getName());
        var gtype = box.declaredField("value").genericType();
        assertTrue(gtype instanceof IntrospectTypeVariable);
        assertEquals("T", ((IntrospectTypeVariable) gtype).name());
    }

    @Test
    public void fieldGenericTypeParameterized() {
        var box = world.findClass(Box.class.getName());
        var gtype = box.declaredField("items").genericType();
        assertTrue(gtype instanceof IntrospectParameterizedType);
        var paramType = (IntrospectParameterizedType) gtype;
        assertEquals(List.class.getName(), paramType.rawType().name());
    }

    @Test
    public void parameterizedTypeArguments() {
        var box = world.findClass(Box.class.getName());
        var paramType = (IntrospectParameterizedType) box.declaredField("items").genericType();
        assertEquals(1, paramType.typeArguments().size());
        var arg = paramType.typeArguments().get(0);
        assertEquals(IntrospectProjection.EXACT, arg.projection());
        assertTrue(arg.type() instanceof IntrospectTypeVariable);
        assertEquals("T", ((IntrospectTypeVariable) arg.type()).name());
    }

    @Test
    public void wildcardExtendsArgument() {
        var box = world.findClass(Box.class.getName());
        var paramType = (IntrospectParameterizedType) box.declaredField("covariantItems").genericType();
        var arg = paramType.typeArguments().get(0);
        assertEquals(IntrospectProjection.EXTENDS, arg.projection());
        assertTrue(arg.type() instanceof IntrospectTypeVariable);
    }

    @Test
    public void wildcardSuperArgument() {
        var box = world.findClass(Box.class.getName());
        var paramType = (IntrospectParameterizedType) box.declaredField("contravariantItems").genericType();
        var arg = paramType.typeArguments().get(0);
        assertEquals(IntrospectProjection.SUPER, arg.projection());
        assertTrue(arg.type() instanceof IntrospectTypeVariable);
    }

    @Test
    public void wildcardUnboundedArgument() {
        var box = world.findClass(Box.class.getName());
        var paramType = (IntrospectParameterizedType) box.declaredField("wildcardItems").genericType();
        var arg = paramType.typeArguments().get(0);
        assertEquals(IntrospectProjection.EXTENDS, arg.projection());
        assertEquals(world.findClass(Object.class), arg.type());
    }

    @Test
    public void parameterizedTypeOwnerTypeNull() {
        var box = world.findClass(Box.class.getName());
        var paramType = (IntrospectParameterizedType) box.declaredField("items").genericType();
        assertNull(paramType.ownerType());
    }

    @Test
    public void genericArrayType() {
        var box = world.findClass(Box.class.getName());
        var gtype = box.declaredField("array").genericType();
        assertTrue(gtype instanceof IntrospectGenericArray);
        var arrayType = (IntrospectGenericArray) gtype;
        assertTrue(arrayType.componentType() instanceof IntrospectTypeVariable);
        assertEquals("T", ((IntrospectTypeVariable) arrayType.componentType()).name());
    }

    @Test
    public void parameterGenericType() {
        var gm = world.findClass(GenericMethods.class.getName());
        var method = gm.declaredJavaMethod("consume", List.class);
        assertNotNull(method);
        assertEquals(List.class.getName(), method.parameters().get(0).type().name());
        var gtype = method.parameters().get(0).genericType();
        assertTrue(gtype instanceof IntrospectParameterizedType);
        var paramType = (IntrospectParameterizedType) gtype;
        assertEquals(List.class.getName(), paramType.rawType().name());
        assertEquals(1, paramType.typeArguments().size());
        assertEquals(IntrospectProjection.EXACT, paramType.typeArguments().get(0).projection());
    }

    @Test
    public void typeVariableSameInstanceFromClass() {
        var box = world.findClass(Box.class.getName());
        var t = box.typeParameters().get(0);
        var fieldGt = (IntrospectTypeVariable) box.declaredField("value").genericType();
        assertSame(t, fieldGt);
    }

    @Test
    public void boundedTypeVariableArgumentIsResolved() {
        var box = world.findClass(BoundedBox.class.getName());
        var t = box.typeParameters().get(0);
        var bound = (IntrospectParameterizedType) t.superTypes().get(0);
        var arg = bound.typeArguments().get(0);
        assertSame(t, arg.type());
    }

    @Test
    public void classTypeVariableGenericDeclaration() {
        var box = world.findClass(Box.class.getName());
        var t = box.typeParameters().get(0);
        assertSame(box, t.genericDeclaration());
    }

    @Test
    public void methodWithNoTypeParameters() {
        var gm = world.findClass(GenericMethods.class.getName());
        var consume = gm.declaredJavaMethod("consume", List.class);
        assertNotNull(consume);
        assertTrue(consume.typeParameters().isEmpty());
    }

    @Test
    public void methodTypeParameters() {
        var mtp = world.findClass(MethodTypeParams.class.getName());
        var wrap = mtp.declaredJavaMethod("wrap", Object.class);
        assertNotNull(wrap);
        assertEquals(1, wrap.typeParameters().size());
        assertEquals("E", wrap.typeParameters().get(0).name());
    }

    @Test
    public void methodTypeVariableGenericDeclaration() {
        var mtp = world.findClass(MethodTypeParams.class.getName());
        var wrap = mtp.declaredJavaMethod("wrap", Object.class);
        assertNotNull(wrap);
        var e = wrap.typeParameters().get(0);
        assertSame(wrap, e.genericDeclaration());
    }

    @Test
    public void methodTypeVariableWithBound() {
        var mtp = world.findClass(MethodTypeParams.class.getName());
        var bounded = mtp.declaredJavaMethod("bounded", Comparable.class);
        assertNotNull(bounded);
        var e = bounded.typeParameters().get(0);
        assertEquals(1, e.superTypes().size());
        var bound = (IntrospectParameterizedType) e.superTypes().get(0);
        assertEquals(Comparable.class.getName(), bound.rawType().name());
        assertSame(e, bound.typeArguments().get(0).type());
    }

    @Test
    public void methodTypeVariableInParameterType() {
        var mtp = world.findClass(MethodTypeParams.class.getName());
        var wrap = mtp.declaredJavaMethod("wrap", Object.class);
        assertNotNull(wrap);
        var param = wrap.parameters().get(0);
        var gtype = param.genericType();
        assertTrue(gtype instanceof IntrospectTypeVariable);
        assertEquals("E", ((IntrospectTypeVariable) gtype).name());
    }

    @Test
    public void methodWithNoExceptions() {
        var base = world.findClass(Base.class.getName());
        var method = base.declaredJavaMethod("helper");
        assertNotNull(method);
        assertTrue(method.exceptionTypes().isEmpty());
    }

    @Test
    public void methodWithSingleException() {
        var throwing = world.findClass(ThrowingMethods.class.getName());
        var method = throwing.declaredJavaMethod("oneException");
        assertNotNull(method);
        assertEquals(1, method.exceptionTypes().size());
        assertEquals(IllegalArgumentException.class.getName(), method.exceptionTypes().get(0).name());
    }

    @Test
    public void methodWithMultipleExceptions() {
        var throwing = world.findClass(ThrowingMethods.class.getName());
        var method = throwing.declaredJavaMethod("twoExceptions");
        assertNotNull(method);
        assertEquals(2, method.exceptionTypes().size());
        var names = method.exceptionTypes().stream()
                .map(IntrospectClass::name)
                .sorted()
                .collect(Collectors.toList());
        assertEquals(List.of(IllegalStateException.class.getName(), NullPointerException.class.getName()), names);
    }

    @Test
    public void checkedExceptionType() {
        var throwing = world.findClass(ThrowingMethods.class.getName());
        var method = throwing.declaredJavaMethod("checkedException");
        assertNotNull(method);
        assertEquals(1, method.exceptionTypes().size());
        assertEquals(Exception.class.getName(), method.exceptionTypes().get(0).name());
    }

    @Test
    public void methodTypeVariableSameInstance() {
        var mtp = world.findClass(MethodTypeParams.class.getName());
        var wrap = mtp.declaredJavaMethod("wrap", Object.class);
        assertNotNull(wrap);
        var e = wrap.typeParameters().get(0);
        var paramGt = (IntrospectTypeVariable) wrap.parameters().get(0).genericType();
        assertSame(e, paramGt);
    }

    // ===== Fixtures =====

    public static class A {
        public int f;
    }

    public interface I {
        String greet();
    }

    public abstract static class Base {
        public int x;
        private String y;

        public abstract int doubled(int n);

        public void helper() {
        }
    }

    public static class Child extends Base implements I {
        public boolean z;

        @Override
        public int doubled(int n) {
            return n * 2;
        }

        @Override
        public String greet() {
            return "hi";
        }
    }

    public static final class FinalClass {
    }

    public enum Direction {
        NORTH, SOUTH, EAST, WEST
    }

    public @interface Ann {
        String str() default "";
        int count() default 0;
    }

    @Ann(str = "class-level", count = 42)
    public static class Annotated {
        @Ann(str = "field-ann")
        public String publicField;

        private int privateField;

        public static final String CONSTANT = "value";

        @Ann(str = "method-ann", count = 7)
        public void annotatedMethod() {
        }

        public void twoParams(int a, String b) {
        }

        public void methodWithParamAnnotation(@Ann(str = "param-ann") String param) {
        }
    }

    @Ann
    public static class AnnotatedWithDefaults {
    }

    public static class Box<T> {
        public T value;
        public T[] array;
        public List<T> items;
        public List<? extends T> covariantItems;
        public List<? super T> contravariantItems;
        public List<?> wildcardItems;
    }

    public static class BoundedBox<T extends Comparable<T>> {
        public T value;
    }

    public static class Pair<A, B> {
        public A first;
        public B second;
    }

    public static class GenericMethods<T> {
        public void consume(List<T> items) {
        }

        public T produce() {
            return null;
        }
    }

    public static class ThrowingMethods {
        public void oneException() throws IllegalArgumentException {
        }

        public void twoExceptions() throws NullPointerException, IllegalStateException {
        }

        public void checkedException() throws Exception {
        }
    }

    public static class MethodTypeParams {
        public <E> List<E> wrap(E item) {
            return null;
        }

        public <E extends Comparable<E>> E bounded(E item) {
            return null;
        }
    }
}
