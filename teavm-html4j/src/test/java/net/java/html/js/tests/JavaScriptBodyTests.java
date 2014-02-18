/*
 *  Copyright 2014 Alexey Andreev.
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
/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Oracle. Portions Copyright 2013-2014 Oracle. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package net.java.html.js.tests;

import static org.junit.Assert.*;
import java.io.StringReader;
import java.util.Arrays;
import java.util.concurrent.Callable;
import org.apidesign.html.boot.spi.Fn;
import org.junit.Test;

/**
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
public class JavaScriptBodyTests {
    @Test public void sumTwoNumbers() {
        int res = Bodies.sum(5, 3);
        assertEquals(8, res);
    }

    @Test public void accessJsObject() {
        Object o = Bodies.instance(10);
        int ten = Bodies.readX(o);
        assertEquals(10, ten);
    }

    @Test public void callWithNoReturnType() {
        Object o = Bodies.instance(10);
        Bodies.incrementX(o);
        int ten = Bodies.readX(o);
        assertEquals(11, ten);
    }

    @Test public void callbackToRunnable() {
        R run = new R();
        Bodies.callback(run);
        assertEquals(1, run.cnt);
    }

    /*@Test public void typeOfCharacter() {
        String charType = Bodies.typeof('a', false);
        assertEquals("number", charType);
    }*/

    @Test public void typeOfBoolean() {
        String booleanType = Bodies.typeof(true, false);
        assertEquals("boolean", equals(booleanType));
    }

    @Test public void typeOfPrimitiveBoolean() {
        String booleanType = Bodies.typeof(true);
        assertTrue("boolean".equals(booleanType) || "number".equals(booleanType));
    }

    @Test public void typeOfInteger() {
        String intType = Bodies.typeof(1, false);
        assertEquals("number", equals(intType));
    }

    @Test public void typeOfString() {
        String strType = Bodies.typeof("Ahoj", false);
        assertEquals("string", strType);
    }

    /*@Test public void typeOfDouble() {
        String doubleType = Bodies.typeof(0.33, false);
        assertEquals("number", doubleType);
    }*/

    @Test public void typeOfBooleanValueOf() {
        String booleanType = Bodies.typeof(true, true);
        assertEquals("boolean", booleanType);
    }

    @Test public void typeOfIntegerValueOf() {
        String intType = Bodies.typeof(1, true);
        assertEquals("number", intType);
    }

    @Test public void typeOfStringValueOf() {
        String strType = Bodies.typeof("Ahoj", true);
        assertEquals("string", strType);
    }

    /*@Test public void typeOfDoubleValueOf() {
        String doubleType = Bodies.typeof(0.33, true);
        assertEquals("number", doubleType);
    }*/

    @Test public void computeInARunnable() {
        final int[] sum = new int[2];
        class First implements Runnable {
            @Override public void run() {
                sum[0] = Bodies.sum(22, 20);
                sum[1] = Bodies.sum(32, 10);
            }
        }
        Bodies.callback(new First());
        assertEquals(42, sum[0]);
        assertEquals(42, sum[1]);
    }

    /*@Test public void doubleCallbackToRunnable() {
        final R run = new R();
        final R r2 = new R();
        class First implements Runnable {
            @Override public void run() {
                Bodies.callback(run);
                Bodies.callback(r2);
            }
        }
        Bodies.callback(new First());
        assert run.cnt == 1 : "Can call even private implementation classes: " + run.cnt;
        assert r2.cnt == 1 : "Can call even private implementation classes: " + r2.cnt;
    }

    @Test public void identity() {
        Object p = new Object();
        Object r = Bodies.id(p);
        assert r == p : "The object is the same";
    }

    @Test public void encodingString() {
        Object p = "Ji\n\"Hi\"\nHon";
        Object r = Bodies.id(p);
        assert p.equals(r) : "The object is the same: " + p + " != " + r;
    }

    @Test public void encodingBackslashString() {
        Object p = "{\"firstName\":\"/*\\n * Copyright (c) 2013\",\"lastName\":null,\"sex\":\"MALE\",\"address\":{\"street\":null}}";
        Object r = Bodies.id(p);
        assert p.equals(r) : "The object is the same: " + p + " != " + r;
    }

    @Test public void nullIsNull() {
        Object p = null;
        Object r = Bodies.id(p);
        assert r == p : "The null is the same";
    }

    @Test public void callbackWithResult() {
        Callable<Boolean> c = new C();
        Object b = Bodies.callback(c);
        assert b == Boolean.TRUE : "Should return true";
    }

    @Test public void callbackWithParameters() {
        int res = Bodies.sumIndirect(new Sum());
        assert res == 42 : "Expecting 42";
    }

    @Test public void selectFromStringJavaArray() {
        String[] arr = { "Ahoj", "World" };
        Object res = Bodies.select(arr, 1);
        assert "World".equals(res) : "Expecting World, but was: " + res;
    }

    @Test public void selectFromObjectJavaArray() {
        Object[] arr = { new Object(), new Object() };
        Object res = Bodies.select(arr, 1);
        assert arr[1].equals(res) : "Expecting " + arr[1] + ", but was: " + res;
    }

    @Test public void lengthOfJavaArray() {
        String[] arr = { "Ahoj", "World" };
        int res = Bodies.length(arr);
        assert res == 2 : "Expecting 2, but was: " + res;
    }

    @Test public void isJavaArray() {
        String[] arr = { "Ahoj", "World" };
        boolean is = Bodies.isArray(arr);
        assert is: "Expecting it to be an array: " + is;
    }

    @Test public void javaArrayInOutIsCopied() {
        String[] arr = { "Ahoj", "World" };
        Object res = Bodies.id(arr);
        assert res != null : "Non-null is returned";
        assert res instanceof Object[] : "Returned an array: " + res;
        assert !(res instanceof String[]) : "Not returned a string array: " + res;

        Object[] ret = (Object[]) res;
        assert arr.length == ret.length : "Same length: " + ret.length;
        assert arr[0].equals(ret[0]) : "Same first elem";
        assert arr[1].equals(ret[1]) : "Same 2nd elem";
    }

    @Test public void modifyJavaArrayHasNoEffect() {
        String[] arr = { "Ahoj", "World" };
        String value = Bodies.modify(arr, 0, "Hello");
        assert "Hello".equals(value) : "Inside JS the value is changed: " + value;
        assert "Ahoj".equals(arr[0]) : "From a Java point of view it remains: " + arr[0];
    }

    @Test
    public void callbackWithArray() {
        class A implements Callable<String[]> {
            @Override
            public String[] call() throws Exception {
                return new String[] { "Hello" };
            }
        }
        Callable<String[]> a = new A();
        Object b = Bodies.callbackAndPush(a, "World!");
        assert b instanceof Object[] : "Returns an array: " + b;
        Object[] arr = (Object[]) b;
        String str = Arrays.toString(arr);
        assert arr.length == 2 : "Size is two " + str;
        assert "Hello".equals(arr[0]) : "Hello expected: " + arr[0];
        assert "World!".equals(arr[1]) : "World! expected: " + arr[1];
    }

    @Test public void truth() {
        assert Bodies.truth() : "True is true";
    }

    @Test public void factorial2() {
        assert new Factorial().factorial(2) == 2;
    }

    @Test public void factorial3() {
        assert new Factorial().factorial(3) == 6;
    }

    @Test public void factorial4() {
        assert new Factorial().factorial(4) == 24;
    }

    @Test public void factorial5() {
        assert new Factorial().factorial(5) == 120;
    }

    @Test public void factorial6() {
        assert new Factorial().factorial(6) == 720;
    }

    @Test public void sumArray() {
        int r = Bodies.sumArr(new Sum());
        assert r == 6 : "Sum is six: " + r;
    }

    @Test public void callLater() throws Exception{
        final Fn.Presenter p = Fn.activePresenter();
        if (p == null) {
            return;
        }
        p.loadScript(new StringReader(
            "if (typeof window === 'undefined') window = {};"
        ));
        Later l = new Later();
        l.register();
        p.loadScript(new StringReader(
            "window.later();"
        ));
        for (int i = 0; i < 100 && l.call != 42; i++) {
            Thread.sleep(50);
        }
        assert l.call == 42 : "Method was called: " + l.call;
    }*/

    private static class R implements Runnable {
        int cnt;
        private final Thread initThread;

        public R() {
            initThread = Thread.currentThread();
        }

        @Override
        public void run() {
            assert initThread == Thread.currentThread() : "Expecting to run in " + initThread + " but running in " + Thread.currentThread();
            cnt++;
        }
    }

    private static class C implements Callable<Boolean> {
        @Override
        public Boolean call() throws Exception {
            return Boolean.TRUE;
        }
    }
}
