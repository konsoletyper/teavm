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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.dependency.DependencyTestPatcher;
import org.teavm.diagnostics.DefaultProblemTextConsumer;
import org.teavm.model.MethodReference;
import org.teavm.vm.TeaVMBuilder;

/**
 * Checks that the async family does not contain methods that can't dispatch to a suspending implementation.
 * Such extra members don't change program behavior, they only add needless suspension points, so they can't be
 * caught by end-to-end tests.
 */
public class AsyncFamilyPrecisionTest {
    @Test
    public void nonAsyncOverrideOfFamilyMethodIsNotInFamily() {
        var finder = find("overridesSnippet");
        assertTrue(finder.getAsyncMethods().contains(ref(AsyncOverride.class)));
        assertTrue(finder.getAsyncFamilyMethods().contains(ref(Base.class)));
        assertFalse(finder.getAsyncMethods().contains(ref(SyncOverride.class)));
        assertFalse(finder.getAsyncFamilyMethods().contains(ref(SyncOverride.class)),
                "a non-async override can't suspend");
    }

    @SuppressWarnings("unused")
    public static void overridesSnippet() throws InterruptedException {
        Base[] values = { new Base(), new AsyncOverride(), new SyncOverride() };
        for (var value : values) {
            value.m(1);
        }
    }

    @Test
    public void abstractRedeclarationHidesInheritedAsyncImplementation() {
        var finder = find("redeclarationSnippet");
        assertTrue(finder.getAsyncMethods().contains(ref(AsyncImpl.class)));
        assertFalse(finder.getAsyncMethods().contains(ref(SyncLeaf.class)));
        assertFalse(finder.getAsyncFamilyMethods().contains(ref(Callee.class)),
                "every implementation reachable through Callee is SyncLeaf.m");
    }

    @SuppressWarnings("unused")
    public static void redeclarationSnippet() throws InterruptedException {
        new AsyncImpl().m(1);
        Callee callee = new SyncLeaf();
        callee.m(1);
    }

    @Test
    public void privateInterfaceMethodIsNotInFamily() {
        var finder = find("privateInterfaceMethodSnippet");
        assertTrue(finder.getAsyncMethods().contains(ref(AsyncHelperBase.class)));
        assertFalse(finder.getAsyncFamilyMethods().contains(ref(WithPrivateHelper.class)),
                "a private interface method does not take part in virtual dispatch");
    }

    @SuppressWarnings("unused")
    public static void privateInterfaceMethodSnippet() throws InterruptedException {
        var instance = new UsesPrivateHelper();
        instance.m(1);
        instance.callHelper(1);
    }

    @SuppressWarnings("unused")
    public static void waitSnippet() throws InterruptedException {
        var lock = new Object();
        synchronized (lock) {
            lock.wait(1);
        }
        Callee callee = new SyncLeaf();
        callee.m(1);
    }

    @SuppressWarnings("unused")
    public static void classWinsSnippet() {
        new PlainSleepy().m(1);
        Other other = new ClassWins();
        other.m(1);
    }

    @SuppressWarnings("unused")
    public static void moreSpecificDefaultSnippet() {
        new PlainSleepy().m(1);
        Other other = new MoreSpecificWins();
        other.m(1);
    }

    private static MethodReference ref(Class<?> cls) {
        return new MethodReference(cls, "m", int.class, int.class);
    }

    private AsyncMethodFinder find(String methodName) {
        var target = new JavaScriptTarget();
        target.setStrict(true);
        var vm = new TeaVMBuilder(target).build();
        vm.add(new DependencyTestPatcher(getClass().getName(), methodName));
        vm.setAsyncEnabled(true);
        vm.installPlugins();
        vm.setEntryPoint(getClass().getName());
        vm.build(fileName -> new ByteArrayOutputStream(), "tmp");
        if (!vm.getProblemProvider().getSevereProblems().isEmpty()) {
            var text = new DefaultProblemTextConsumer();
            for (var problem : vm.getProblemProvider().getSevereProblems()) {
                problem.render(text);
                text.append("\n");
            }
            fail("Code compiled with errors:\n" + text.getText());
        }
        var finder = new AsyncMethodFinder(vm.getDependencyInfo().getCallGraph(), vm.getDependencyInfo());
        finder.find(vm.getWrittenClasses());
        return finder;
    }

    static class Base {
        public int m(int x) throws InterruptedException {
            return x;
        }
    }

    static class AsyncOverride extends Base {
        @Override
        public int m(int x) throws InterruptedException {
            Thread.sleep(1);
            return x;
        }
    }

    static class SyncOverride extends Base {
        @Override
        public int m(int x) {
            return x + 1;
        }
    }

    interface Callee {
        int m(int x) throws InterruptedException;
    }

    static class AsyncImpl {
        public int m(int x) throws InterruptedException {
            Thread.sleep(1);
            return x;
        }
    }

    abstract static class Redeclares extends AsyncImpl implements Callee {
        @Override
        public abstract int m(int x);
    }

    static final class SyncLeaf extends Redeclares {
        @Override
        public int m(int x) {
            return x + 2;
        }
    }

    interface WithPrivateHelper {
        private int m(int x) {
            return x - 1;
        }

        default int callHelper(int x) {
            return m(x);
        }
    }

    static class AsyncHelperBase {
        public int m(int x) throws InterruptedException {
            Thread.sleep(1);
            return x;
        }
    }

    static final class UsesPrivateHelper extends AsyncHelperBase implements WithPrivateHelper {
    }

    interface Sleepy {
        default int m(int x) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // do nothing
            }
            return x;
        }
    }

    interface Quiet extends Sleepy, Other {
        @Override
        default int m(int x) {
            return x + 3;
        }
    }

    interface Other {
        int m(int x);
    }

    static final class PlainSleepy implements Sleepy {
    }

    static class SyncBase {
        public int m(int x) {
            return x + 4;
        }
    }

    static final class ClassWins extends SyncBase implements Sleepy, Other {
    }

    static final class MoreSpecificWins implements Sleepy, Quiet {
    }
}
