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
package org.teavm.vm;

import static org.junit.Assert.assertEquals;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
public class VirtualCallTest {
    @Test
    public void implementInBaseMethodWithDefault() {
        SubclassWithInheritedImplementation o = new SubclassWithInheritedImplementation();
        assertEquals(1, o.x);
        assertEquals(2, new SubclassWithInheritedDefaultImplementation().foo());
    }

    static class BaseClassWithImplementation {
        public int foo() {
            return 1;
        }
    }

    interface BaseInterfaceWithDefault {
        default int foo() {
            return 2;
        }
    }

    static class IntermediateClassInheritingImplementation extends BaseClassWithImplementation {
    }

    static class SubclassWithInheritedImplementation extends IntermediateClassInheritingImplementation
            implements BaseInterfaceWithDefault {
        int x;

        SubclassWithInheritedImplementation() {
            x = foo();
        }
    }

    static class SubclassWithInheritedDefaultImplementation implements BaseInterfaceWithDefault {
    }
    
    @Test
    public void defaultMethodsSupported() {
        WithDefaultMethod[] instances = { new WithDefaultMethodDerivedA(), new WithDefaultMethodDerivedB(),
                new WithDefaultMethodDerivedC() };
        StringBuilder sb = new StringBuilder();
        for (var instance : instances) {
            sb.append(instance.foo() + "," + instance.bar() + ";");
        }

        assertEquals("default,A;default,B;overridden,C;", sb.toString());
    }

    interface WithDefaultMethod {
        default String foo() {
            return "default";
        }

        String bar();
    }

    static class WithDefaultMethodDerivedA implements WithDefaultMethod {
        @Override
        public String bar() {
            return "A";
        }
    }

    static class WithDefaultMethodDerivedB implements WithDefaultMethod {
        @Override
        public String bar() {
            return "B";
        }
    }

    static class WithDefaultMethodDerivedC implements WithDefaultMethod {
        @Override
        public String foo() {
            return "overridden";
        }

        @Override
        public String bar() {
            return "C";
        }
    }

    @Test
    @SkipPlatform({ TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI})
    public void indirectDefaultMethod() {
        StringBuilder sb = new StringBuilder();
        for (FirstPath o : new FirstPath[] { new PathJoint(), new FirstPathOptimizationPrevention() }) {
            sb.append(o.foo()).append(";");
        }
        assertEquals("SecondPath.foo;FirstPath.foo;", sb.toString());
    }

    @Test
    @SkipPlatform({TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI})
    public void indirectDefaultMethodSubclass() {
        StringBuilder sb = new StringBuilder();
        for (FirstPath o : new FirstPath[] { new PathJointSubclass(), new FirstPathOptimizationPrevention() }) {
            sb.append(o.foo()).append(";");
        }
        assertEquals("SecondPath.foo;FirstPath.foo;", sb.toString());
    }

    interface FirstPath {
        default String foo() {
            return "FirstPath.foo";
        }
    }

    interface SecondPath extends FirstPath {
        @Override
        default String foo() {
            return "SecondPath.foo";
        }
    }

    class PathJoint implements FirstPath, SecondPath {
    }

    class PathJointSubclass extends PathJoint implements FirstPath {
    }

    class FirstPathOptimizationPrevention implements FirstPath {
        // Used to ensure that the implementation of FirstPath.foo() is not optimized away by TeaVM.
    }
    
    @Test
    public void virtualCallWithPrivateMethods() {
        assertEquals("ap", callA(new B()));
    }

    @Test
    public void virtualTableCase1() {
        interface I {
            String f();
        }
        interface J extends I {
            String g();
        }
        class A {
        }
        class C extends A implements J {
            @Override
            public String f() {
                return "C.f";
            }
            @Override
            public String g() {
                return "C.g";
            }
        }
        class D implements I {
            @Override
            public String f() {
                return "D.f";
            }
        }

        var list = List.<I>of(new C(), new D());
        var sb = new StringBuilder();
        for (var item : list) {
            sb.append(item.f()).append(";");
        }

        assertEquals("C.f;D.f;", sb.toString());
    }

    private static String callA(A a) {
        return a.a();
    }

    static class A {
        String a() {
            return "a" + p();
        }

        private String p() {
            return "p";
        }
    }

    static class B extends A {
        private String p() {
            return "q";
        }
    }
}
