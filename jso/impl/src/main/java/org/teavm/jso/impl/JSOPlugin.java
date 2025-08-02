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
package org.teavm.jso.impl;

import static org.teavm.jso.impl.JSMethods.JS_EXCEPTIONS_CLASS;
import static org.teavm.jso.impl.JSMethods.JS_OBJECT;
import static org.teavm.jso.impl.JSMethods.JS_WRAPPER;
import static org.teavm.jso.impl.JSMethods.JS_WRAPPER_CLASS;
import static org.teavm.jso.impl.JSMethods.OBJECT;
import org.teavm.backend.javascript.TeaVMJavaScriptHost;
import org.teavm.backend.wasm.gc.TeaVMWasmGCHost;
import org.teavm.interop.PlatformMarker;
import org.teavm.jso.impl.wasmgc.WasmGCJso;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.platform.plugin.PlatformPlugin;
import org.teavm.vm.TeaVMPluginUtil;
import org.teavm.vm.spi.After;
import org.teavm.vm.spi.ClassFilter;
import org.teavm.vm.spi.ClassFilterContext;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

@After(PlatformPlugin.class)
public class JSOPlugin implements TeaVMPlugin {
    @Override
    public void install(TeaVMHost host) {
        var jsHost = host.getExtension(TeaVMJavaScriptHost.class);
        var wasmGCHost = host.getExtension(TeaVMWasmGCHost.class);
        if (jsHost == null && wasmGCHost == null) {
            return;
        }

        JSBodyRepository repository = new JSBodyRepository();
        host.registerService(JSBodyRepository.class, repository);
        var classTransformer = new JSObjectClassTransformer(repository);
        host.add(classTransformer);
        JSDependencyListener dependencyListener = new JSDependencyListener(repository);
        host.add(dependencyListener);
        host.add(new JSExceptionsDependencyListener());
        host.addClassFilter(new ClassFilter() {
            JSTypeHelper helper;

            @Override
            public boolean accept(ClassFilterContext context, ValueType type) {
                if (!(type instanceof ValueType.Object)) {
                    return true;
                }
                var className = ((ValueType.Object) type).getClassName();
                if (helper == null) {
                    helper = new JSTypeHelper(context.classes());
                }
                if (helper.isJavaScriptClass(className) && !helper.isJavaScriptImplementation(className)) {
                    return false;
                }
                return true;
            }
        });

        var wrapperDependency = new JSWrapperDependency();
        host.add(wrapperDependency);

        if (!isBootstrap()) {
            TeaVMPluginUtil.handleNatives(host, JS.class);
        }

        if (jsHost != null) {
            installForJS(jsHost);
        }

        if (wasmGCHost != null) {
            classTransformer.setClassFilter(n -> !n.startsWith("java."));
            classTransformer.forWasmGC();
            WasmGCJso.install(host, wasmGCHost, repository);
        }
    }

    private void installForJS(TeaVMJavaScriptHost jsHost) {
        var aliasRenderer = new JSAliasRenderer();
        jsHost.add(aliasRenderer);
        jsHost.addGeneratorProvider(new GeneratorAnnotationInstaller<>(new JSBodyGenerator(),
                DynamicGenerator.class.getName()));
        jsHost.addInjectorProvider(new GeneratorAnnotationInstaller<>(new JSBodyGenerator(),
                DynamicInjector.class.getName()));
        jsHost.addVirtualMethods(aliasRenderer);
        jsHost.addForcedFunctionMethods(new JSExportedMethodAsFunction());

        var exceptionsGenerator = new JSExceptionsGenerator();
        jsHost.add(new MethodReference(JS_EXCEPTIONS_CLASS, "getJavaException", JS_OBJECT,
                ValueType.object("java.lang.Throwable")), exceptionsGenerator);
        jsHost.add(new MethodReference(JS_EXCEPTIONS_CLASS, "getJSException", ValueType.object("java.lang.Throwable"), 
                JS_OBJECT), exceptionsGenerator);

        var wrapperGenerator = new JSWrapperGenerator();
        jsHost.add(new MethodReference(JS_WRAPPER_CLASS, "directJavaToJs", OBJECT, JS_OBJECT), wrapperGenerator);
        jsHost.add(new MethodReference(JS_WRAPPER_CLASS, "directJsToJava", JS_OBJECT, OBJECT), wrapperGenerator);
        jsHost.add(new MethodReference(JS_WRAPPER_CLASS, "dependencyJavaToJs", OBJECT, JS_OBJECT), wrapperGenerator);
        jsHost.add(new MethodReference(JS_WRAPPER_CLASS, "dependencyJsToJava", JS_OBJECT, OBJECT), wrapperGenerator);
        jsHost.add(new MethodReference(JS_WRAPPER_CLASS, "marshallJavaToJs", OBJECT, JS_OBJECT), wrapperGenerator);
        jsHost.add(new MethodReference(JS_WRAPPER_CLASS, "unmarshallJavaFromJs", JS_OBJECT, OBJECT), wrapperGenerator);
        jsHost.add(new MethodReference(JS_WRAPPER_CLASS, "isJava", OBJECT, ValueType.BOOLEAN), wrapperGenerator);
        jsHost.add(new MethodReference(JS_WRAPPER_CLASS, "isJava", JS_OBJECT, ValueType.BOOLEAN), wrapperGenerator);
        jsHost.add(new MethodReference(JS_WRAPPER_CLASS, "wrapperToJs", JS_WRAPPER, JS_OBJECT), wrapperGenerator);
        jsHost.add(new MethodReference(JS_WRAPPER_CLASS, "jsToWrapper", JS_OBJECT, JS_WRAPPER), wrapperGenerator);
    }

    @PlatformMarker
    private static boolean isBootstrap() {
        return false;
    }
}
