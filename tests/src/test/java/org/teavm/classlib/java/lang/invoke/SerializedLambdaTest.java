/*
 *  Copyright 2020 adam.
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
package org.teavm.classlib.java.lang.invoke;

import static org.junit.Assert.assertEquals;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class SerializedLambdaTest {
    @Test
    public void serializableLambdaHasWriteReplaceMethod() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        SerializableFunction<Object, String> lambda = Object::toString;
        Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
        writeReplace.setAccessible(true);
        SerializedLambda serializedLambda = (SerializedLambda) writeReplace.invoke(lambda);
        assertEquals("org/teavm/classlib/java/lang/invoke/SerializedLambdaTest", serializedLambda.getCapturingClass());
        assertEquals(0, serializedLambda.getCapturedArgCount());
        assertEquals("org/teavm/classlib/java/lang/invoke/SerializedLambdaTest$SerializableFunction",
                serializedLambda.getFunctionalInterfaceClass());
        assertEquals("apply", serializedLambda.getFunctionalInterfaceMethodName());
        assertEquals("(Ljava/lang/Object;)Ljava/lang/Object;",
                serializedLambda.getFunctionalInterfaceMethodSignature());
        assertEquals("java/lang/Object", serializedLambda.getImplClass());
        assertEquals(5, serializedLambda.getImplMethodKind());
        assertEquals("toString", serializedLambda.getImplMethodName());
        assertEquals("()Ljava/lang/String;", serializedLambda.getImplMethodSignature());
        assertEquals("(Ljava/lang/Object;)Ljava/lang/String;", serializedLambda.getInstantiatedMethodType());
        assertEquals(
                "SerializedLambda[capturingClass=class org.teavm.classlib.java.lang.invoke.SerializedLambdaTest, "
                        + "functionalInterfaceMethod=org/teavm/classlib/java/lang/invoke"
                        + "/SerializedLambdaTest$SerializableFunction.apply:(Ljava/lang/Object;)Ljava/lang/Object;, "
                        + "implementation=invokeVirtual java/lang/Object.toString:()Ljava/lang/String;, "
                        + "instantiatedMethodType=(Ljava/lang/Object;)Ljava/lang/String;, numCaptured=0]",
                serializedLambda.toString());
    }

    @Test
    public void serializableLambdaWriteReplaceCapturesArguments() throws NoSuchMethodException,
            InvocationTargetException,
            IllegalAccessException {
        String captureValue = "captured-value";
        SerializableFunction<Object, String> lambda = o -> captureValue;
        Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
        writeReplace.setAccessible(true);
        SerializedLambda serializedLambda = (SerializedLambda) writeReplace.invoke(lambda);
        assertEquals("org/teavm/classlib/java/lang/invoke/SerializedLambdaTest", serializedLambda.getCapturingClass());
        assertEquals(1, serializedLambda.getCapturedArgCount());
        assertEquals(captureValue, serializedLambda.getCapturedArg(0));
        assertEquals("org/teavm/classlib/java/lang/invoke/SerializedLambdaTest$SerializableFunction",
                serializedLambda.getFunctionalInterfaceClass());
        assertEquals("apply", serializedLambda.getFunctionalInterfaceMethodName());
        assertEquals("(Ljava/lang/Object;)Ljava/lang/Object;",
                serializedLambda.getFunctionalInterfaceMethodSignature());
        assertEquals("org/teavm/classlib/java/lang/invoke/SerializedLambdaTest", serializedLambda.getImplClass());
        assertEquals(6, serializedLambda.getImplMethodKind());
        assertEquals("(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;",
                serializedLambda.getImplMethodSignature());
        assertEquals("(Ljava/lang/Object;)Ljava/lang/String;", serializedLambda.getInstantiatedMethodType());
    }

    private interface SerializableFunction<T, R> extends Function<T, R>, Serializable {
    }
}