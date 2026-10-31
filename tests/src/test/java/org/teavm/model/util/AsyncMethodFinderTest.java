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
package org.teavm.model.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.dependency.DependencyTestPatcher;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.MethodReference;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;

public class AsyncMethodFinderTest {
    @Test
    public void interfaceMethodImplementedByInheritedDefaultMethodIsInFamily() {
        var vm = build("callThroughInterfaceSnippet", new AddInterface(InheritsDefault.class, Call.class));
        var finder = find(vm);
        var defaultMethod = new MethodReference(SuspendingDefault.class, "m", int.class, int.class);
        assertTrue(finder.getAsyncMethods().contains(defaultMethod), "the default method must be async");
        assertTrue(finder.getAsyncFamilyMethods().contains(
                new MethodReference(Call.class, "m", int.class, int.class)));
    }

    @SuppressWarnings("unused")
    public static void callThroughInterfaceSnippet() throws InterruptedException {
        // InheritsDefault implements Call only after AddInterface runs. javac does not accept this class shape in
        // source, because a default method inherited through a superclass does not implement Call.m. Separate
        // compilation can still produce it: Call can gain m after InheritsDefault was compiled.
        Call call = (Call) (Object) new InheritsDefault();
        call.m(2);
    }

    @Test
    public void privateInterfaceMethodIsNotInFamily() {
        var vm = build("privateInterfaceMethodSnippet");
        var finder = find(vm);
        var helper = new MethodReference(WithPrivateHelper.class, "helper", int.class, int.class);
        assertTrue(finder.getAsyncMethods().contains(new MethodReference(SuspendingHelperBase.class, "helper",
                int.class, int.class)), "the superclass method with the same descriptor must be async");
        var helperClass = vm.getWrittenClasses().get(WithPrivateHelper.class.getName());
        assertNotNull(helperClass.getMethod(helper.getDescriptor()), "the private method must be compiled");
        assertFalse(finder.getAsyncFamilyMethods().contains(helper));
    }

    @SuppressWarnings("unused")
    public static void privateInterfaceMethodSnippet() throws InterruptedException {
        var instance = new UsesPrivateHelper();
        instance.helper(5);
        instance.callHelper(5);
    }

    private TeaVM build(String methodName, ClassHolderTransformer... transformers) {
        var target = new JavaScriptTarget();
        target.setStrict(true);
        var vm = new TeaVMBuilder(target).build();
        vm.add(new DependencyTestPatcher(getClass().getName(), methodName));
        for (var transformer : transformers) {
            vm.add(transformer);
        }
        vm.installPlugins();
        vm.setEntryPoint(getClass().getName());
        vm.build(fileName -> new ByteArrayOutputStream(), "tmp");
        if (!vm.getProblemProvider().getSevereProblems().isEmpty()) {
            fail("Code compiled with errors: " + vm.getProblemProvider().getSevereProblems());
        }
        return vm;
    }

    private AsyncMethodFinder find(TeaVM vm) {
        var finder = new AsyncMethodFinder(vm.getDependencyInfo().getCallGraph(), vm.getDependencyInfo());
        finder.find(vm.getWrittenClasses());
        return finder;
    }

    static class AddInterface implements ClassHolderTransformer {
        private final String className;
        private final String interfaceName;

        AddInterface(Class<?> cls, Class<?> iface) {
            className = cls.getName();
            interfaceName = iface.getName();
        }

        @Override
        public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
            if (cls.getName().equals(className)) {
                cls.getInterfaces().add(interfaceName);
            }
        }
    }

    interface Call {
        int m(int x) throws InterruptedException;
    }

    interface SuspendingDefault {
        default int m(int x) throws InterruptedException {
            Thread.sleep(5);
            return x * 3;
        }
    }

    static class SuspendingDefaultBase implements SuspendingDefault {
    }

    static final class InheritsDefault extends SuspendingDefaultBase {
    }

    interface WithPrivateHelper {
        private int helper(int x) {
            return x - 1;
        }

        default int callHelper(int x) {
            return helper(x);
        }
    }

    static class SuspendingHelperBase {
        public int helper(int x) throws InterruptedException {
            Thread.sleep(1);
            return x;
        }
    }

    static final class UsesPrivateHelper extends SuspendingHelperBase implements WithPrivateHelper {
    }
}
