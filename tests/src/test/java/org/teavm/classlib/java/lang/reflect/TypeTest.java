/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.classlib.java.lang.reflect;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.support.Reflectable;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
@OnlyPlatform({TestPlatform.WEBASSEMBLY_GC, TestPlatform.JAVASCRIPT})
public class TypeTest {
    @Test
    public void classTypeParameters() {
        var s = Arrays.stream(A.class.getTypeParameters())
                .map(TypeVariable::getName)
                .collect(Collectors.joining(", "));
        assertEquals("T", s);
        
        s = Arrays.stream(B.class.getTypeParameters())
                .map(TypeVariable::getName)
                .collect(Collectors.joining(", "));
        assertEquals("Q, W", s);

        s = Arrays.stream(C.class.getTypeParameters())
                .map(TypeVariable::getName)
                .collect(Collectors.joining(", "));
        assertEquals("", s);
        
        assertEquals("T", A.class.getTypeParameters()[0].toString());
    }
    
    @Test
    public void methodTypeParameters() throws Exception {
        var cls = A.class;
        var foo = cls.getMethod("foo");
        assertArrayEquals(new TypeVariable<?>[0], foo.getTypeParameters());
        
        var bar = cls.getMethod("bar", Object.class);
        assertEquals(1, bar.getTypeParameters().length);
        assertEquals("S", bar.getTypeParameters()[0].getName());
    }
    
    @Test
    public void classTypeVariableBounds() {
        TypeVariable<?>[] params = A.class.getTypeParameters();
        assertEquals(1, params[0].getBounds().length);
        assertEquals(Object.class, params[0].getBounds()[0]);
        
        params = B.class.getTypeParameters();
        assertArrayEquals(new Type[] { params[0] }, params[1].getBounds());
        
        params = D.class.getTypeParameters();
        var bounds = params[0].getBounds();
        assertEquals(1, bounds.length);
        assertTrue(bounds[0] instanceof ParameterizedType);
        var pt = (ParameterizedType) bounds[0];
        assertEquals(D.class, pt.getRawType());
        assertEquals(params[0], pt.getActualTypeArguments()[0]);
        
        params = E.class.getTypeParameters();
        bounds = params[0].getBounds();
        assertTrue(bounds[0] instanceof ParameterizedType);
        pt = (ParameterizedType) bounds[0];
        assertEquals(A.class, pt.getRawType());
        assertEquals(Integer.class, pt.getActualTypeArguments()[0]);
        
        bounds = params[1].getBounds();
        assertTrue(bounds[0] instanceof ParameterizedType);
        pt = (ParameterizedType) bounds[0];
        assertEquals(A.class, pt.getRawType());
        assertTrue(pt.getActualTypeArguments()[0] instanceof WildcardType);
        var wildcard = (WildcardType) pt.getActualTypeArguments()[0];
        assertEquals(1, wildcard.getUpperBounds().length);
        assertEquals(Long.class, wildcard.getUpperBounds()[0]);
        assertEquals(0, wildcard.getLowerBounds().length);
        
        bounds = params[2].getBounds();
        assertTrue(bounds[0] instanceof ParameterizedType);
        pt = (ParameterizedType) bounds[0];
        assertEquals(A.class, pt.getRawType());
        assertTrue(pt.getActualTypeArguments()[0] instanceof WildcardType);
        wildcard = (WildcardType) pt.getActualTypeArguments()[0];
        assertEquals(1, wildcard.getUpperBounds().length);
        assertEquals(Object.class, wildcard.getUpperBounds()[0]);
        assertEquals(1, wildcard.getLowerBounds().length);
        assertEquals(Number.class, wildcard.getLowerBounds()[0]);

        bounds = params[3].getBounds();
        assertTrue(bounds[0] instanceof ParameterizedType);
        pt = (ParameterizedType) bounds[0];
        assertEquals(A.class, pt.getRawType());
        assertTrue(pt.getActualTypeArguments()[0] instanceof WildcardType);
        wildcard = (WildcardType) pt.getActualTypeArguments()[0];
        assertEquals(1, wildcard.getUpperBounds().length);
        assertEquals(Object.class, wildcard.getUpperBounds()[0]);
        assertEquals(0, wildcard.getLowerBounds().length);
        
        params = F.class.getTypeParameters();
        bounds = params[1].getBounds();
        assertTrue(bounds[0] instanceof ParameterizedType);
        pt = (ParameterizedType) bounds[0];
        assertTrue(pt.getActualTypeArguments()[0] instanceof GenericArrayType);
        var arrayType = (GenericArrayType) pt.getActualTypeArguments()[0];
        assertEquals(params[0], arrayType.getGenericComponentType());
        
        bounds = params[2].getBounds();
        assertTrue(bounds[0] instanceof ParameterizedType);
        pt = (ParameterizedType) bounds[0];
        assertEquals(int[].class, pt.getActualTypeArguments()[0]);
    }
    
    interface A<T> {
        @Reflectable
        void foo();

        @Reflectable
        <S> void bar(S param); 
    }

    static class B<Q, W extends Q> {
    }
    
    static class C {
    }
    
    static class D<T extends D<T>> {
    }
    
    static class E<T extends A<Integer>, S extends A<? extends Long>, W extends A<? super Number>, Z extends A<?>> {
    } 
    
    static class F<T, S extends A<T[]>, W extends A<int[]>> {
    }
}
