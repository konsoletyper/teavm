/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.jso.impl.wasmgc;

import static org.teavm.jso.impl.JSMethods.JS_OBJECT;
import static org.teavm.jso.impl.JSMethods.WASM_GC_JS_RUNTIME_CLASS;
import static org.teavm.jso.impl.wasmgc.WasmGCJSConstants.STRING_TO_JS;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.teavm.backend.javascript.rendering.AstWriter;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmExternConversion;
import org.teavm.backend.wasm.model.expression.WasmExternConversionType;
import org.teavm.backend.wasm.model.expression.WasmFunctionReference;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmThrow;
import org.teavm.jso.JSClass;
import org.teavm.jso.impl.AliasCollector;
import org.teavm.jso.impl.JSBodyAstEmitter;
import org.teavm.jso.impl.JSBodyBloatedEmitter;
import org.teavm.jso.impl.JSBodyEmitter;
import org.teavm.jso.impl.JSMarshallable;
import org.teavm.jso.impl.JSVararg;
import org.teavm.model.ClassReader;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class WasmGCJsoCommonGenerator {
    private WasmGCJSFunctions jsFunctions;
    private boolean initialized;
    private List<Consumer<WasmFunction>> initializerParts = new ArrayList<>();
    private boolean rethrowExported;
    private Map<String, WasmGlobal> stringsConstants = new HashMap<>();

    private WasmFunction createClassFunction;
    private WasmFunction defineFunctionFunction;
    private WasmFunction defineMethodFunction;
    private WasmFunction defineStaticMethodFunction;
    private WasmFunction definePropertyFunction;
    private WasmFunction defineStaticPropertyFunction;
    private WasmFunction exportClassFunction;
    private WasmFunction javaObjectToJSFunction;
    private WasmGlobal defaultWrapperClass;
    private Map<String, WasmGlobal> definedClasses = new HashMap<>();
    private Map<ImportDecl, WasmGlobal> importGlobals = new HashMap<>();

    WasmGCJsoCommonGenerator(WasmGCJSFunctions jsFunctions) {
        this.jsFunctions = jsFunctions;
    }

    private void initialize(WasmGCJsoContext context) {
        if (initialized) {
            return;
        }
        initialized = true;
        context.addToInitializer(this::writeToInitializer);
        exportRethrowException(context);
    }

    private void writeToInitializer(WasmFunction function) {
        for (var part : initializerParts) {
            part.accept(function);
        }
    }

    private void addInitializerPart(WasmGCJsoContext context, Consumer<WasmFunction> part) {
        initialize(context);
        initializerParts.add(part);
    }

    WasmGlobal addJSBody(WasmGCJsoContext context, JSBodyEmitter emitter, boolean inlined) {
        initialize(context);
        var paramCount = emitter.method().parameterCount();
        if (!emitter.isStatic()) {
            paramCount++;
        }
        var imports = emitter.imports();
        paramCount += imports.length;

        var global = new WasmGlobal(context.names().suggestForMethod(emitter.method()),
                WasmType.Reference.EXTERN, new WasmNullConstant(WasmType.Reference.EXTERN));
        context.module().globals.add(global);
        var body = "";
        if (emitter instanceof JSBodyBloatedEmitter) {
            body = ((JSBodyBloatedEmitter) emitter).script;
        } else if (emitter instanceof JSBodyAstEmitter) {
            var writer = new WasmGCJSBodyWriter();
            if (inlined) {
                writer.sb.append("return ");
            }
            var astEmitter = (JSBodyAstEmitter) emitter;
            var astWriter = new AstWriter(writer, name -> (w, prec) -> w.append(name));
            if (!emitter.isStatic()) {
                astWriter.declareNameEmitter("this", (w, prec) -> w.append("__this__"));
            }
            astWriter.print(astEmitter.ast);
            if (inlined) {
                writer.sb.append(";");
            }
            body = writer.sb.toString();
        } else {
            throw new IllegalArgumentException();
        }

        var constructor = new WasmCall(jsFunctions.getFunctionConstructor(context, paramCount));
        var paramNames = new ArrayList<String>();
        for (var importDecl : imports) {
            paramNames.add(importDecl.alias);
        }
        if (!emitter.isStatic()) {
            paramNames.add("__this__");
        }
        paramNames.addAll(List.of(emitter.parameterNames()));
        for (var parameter : paramNames) {
            var paramName = new WasmGetGlobal(context.strings().getStringConstant(parameter).global);
            constructor.getArguments().add(stringToJs(context, paramName));
        }
        var functionBody = new WasmGetGlobal(context.strings().getStringConstant(body).global);
        constructor.getArguments().add(stringToJs(context, functionBody));
        WasmExpression value = constructor;
        if (imports.length > 0) {
            var bind = new WasmCall(jsFunctions.getBind(context, imports.length));
            bind.getArguments().add(value);
            for (var importDecl : imports) {
                var importGlobal = getImportGlobal(context, importDecl.fromModule, "__self__");
                bind.getArguments().add(new WasmGetGlobal(importGlobal));
            }
            value = bind;
        }
        var result = value;
        initializerParts.add(initializer -> initializer.getBody().add(new WasmSetGlobal(global, result)));

        return global;
    }

    WasmGlobal getImportGlobal(WasmGCJsoContext context, String module, String id) {
        return importGlobals.computeIfAbsent(new ImportDecl(module, id), m -> {
            var name = context.names().topLevel(WasmGCNameProvider.sanitize("teavm.js@imports:" + module + "#" + id));
            var global = new WasmGlobal(name, WasmType.Reference.EXTERN,
                    new WasmNullConstant(WasmType.Reference.EXTERN));
            global.setImmutable(true);
            context.module().globals.add(global);
            global.setImportModule(module);
            global.setImportName(id);
            return global;
        });
    }

    private WasmFunction stringToJsFunction(WasmGCJsoContext context) {
        return context.functions().forStaticMethod(STRING_TO_JS);
    }

    WasmExpression stringToJs(WasmGCJsoContext context, WasmExpression str) {
        return new WasmCall(stringToJsFunction(context), str);
    }

    private void exportRethrowException(WasmGCJsoContext context) {
        if (rethrowExported) {
            return;
        }
        rethrowExported = true;
        var fn = context.functions().forStaticMethod(new MethodReference(WASM_GC_JS_RUNTIME_CLASS, "wrapException",
                JS_OBJECT, ValueType.object("java.lang.Throwable")));
        fn.setExportName("teavm.js.wrapException");

        fn = context.functions().forStaticMethod(new MethodReference(WASM_GC_JS_RUNTIME_CLASS, "extractException",
                ValueType.object("java.lang.Throwable"), JS_OBJECT));
        fn.setExportName("teavm.js.extractException");

        createThrowExceptionFunction(context);
    }

    private void createThrowExceptionFunction(WasmGCJsoContext context) {
        var fn = new WasmFunction(context.functionTypes().of(null, WasmType.Reference.EXTERN));
        fn.setName(context.names().topLevel("teavm@throwException"));
        fn.setExportName("teavm.js.throwException");
        context.module().functions.add(fn);

        var exceptionLocal = new WasmLocal(WasmType.Reference.EXTERN);
        fn.add(exceptionLocal);

        var asAny = new WasmExternConversion(WasmExternConversionType.EXTERN_TO_ANY, new WasmGetLocal(exceptionLocal));
        var throwableType = (WasmType.Reference) context.typeMapper().mapType(ValueType.parse(Throwable.class));
        var asThrowable = new WasmCast(asAny, throwableType);
        var throwExpr = new WasmThrow(context.exceptionTag());
        throwExpr.getArguments().add(asThrowable);
        fn.getBody().add(throwExpr);
    }

    WasmExpression jsStringConstant(WasmGCJsoContext context, String str) {
        var global = stringsConstants.computeIfAbsent(str, s -> {
            var javaGlobal = context.strings().getStringConstant(s).global;
            var function = context.functions().forStaticMethod(STRING_TO_JS);
            var index = stringsConstants.size();
            var brief = str.length() > 16 ? str.substring(0, 16) : str;
            var name = context.names().topLevel("teavm.js.strings<" + index + ">:"
                    + WasmGCNameProvider.sanitize(brief));
            var jsGlobal = new WasmGlobal(name, WasmType.Reference.EXTERN,
                    new WasmNullConstant(WasmType.Reference.EXTERN));
            context.module().globals.add(jsGlobal);
            addInitializerPart(context, initializer -> {
                var call = new WasmCall(function, new WasmGetGlobal(javaGlobal));
                initializer.getBody().add(new WasmSetGlobal(jsGlobal, call));
            });
            return jsGlobal;
        });
        return new WasmGetGlobal(global);
    }

    WasmGlobal getDefaultWrapperClass(WasmGCJsoContext context) {
        if (defaultWrapperClass == null) {
            var name = context.names().topLevel("teavm.js@defaultWrapperClass");
            defaultWrapperClass = new WasmGlobal(name, WasmType.Reference.EXTERN,
                    new WasmNullConstant(WasmType.Reference.EXTERN));
            context.module().globals.add(defaultWrapperClass);
            addInitializerPart(context, initializer -> {
                var createClass = new WasmCall(createClassFunction(context),
                        new WasmNullConstant(WasmType.Reference.EXTERN),
                        new WasmNullConstant(WasmType.Reference.EXTERN),
                        new WasmNullConstant(WasmType.Reference.FUNC));
                initializer.getBody().add(new WasmSetGlobal(defaultWrapperClass, createClass));
            });
        }
        return defaultWrapperClass;
    }

    WasmGlobal getDefinedClass(WasmGCJsoContext context, String className) {
        var result = definedClasses.get(className);
        if (result == null) {
            result = defineClass(context, className);
            definedClasses.put(className, result);
        }
        return result;
    }

    private WasmGlobal defineClass(WasmGCJsoContext context, String className) {
        var name = context.names().topLevel(context.names().suggestForClass(className + "@js"));
        var global = new WasmGlobal(name, WasmType.Reference.EXTERN, new WasmNullConstant(WasmType.Reference.EXTERN));
        context.module().globals.add(global);

        var cls = context.classes().get(className);
        var expressions = new ArrayList<WasmExpression>();
        var isModule = context.entryPoint().equals(className);

        var members = AliasCollector.collectMembers(cls, AliasCollector::isInstanceMember);
        defineMethods(context, members, cls, global, expressions);
        defineProperties(context, members, cls, global, expressions);

        var staticMembers = AliasCollector.collectMembers(cls, AliasCollector::isStaticMember);

        var simpleName = className.substring(className.lastIndexOf('.') + 1);
        var javaClassName = context.strings().getStringConstant(simpleName);
        var jsClassName = stringToJs(context, new WasmGetGlobal(javaClassName.global));

        var exportedParent = parentExportedClass(context, cls.getParent());
        var jsExportedParent = exportedParent != null
                ? new WasmGetGlobal(getDefinedClass(context, exportedParent))
                : new WasmNullConstant(WasmType.Reference.EXTERN);

        var needsExport = !className.equals(context.entryPoint())
                && (!staticMembers.methods.isEmpty() || !staticMembers.properties.isEmpty());
        WasmExpression constructor;
        if (members.constructor != null) {
            var function = context.functions().forStaticMethod(members.constructor);
            function.setReferenced(true);
            constructor = new WasmFunctionReference(function);
            needsExport = true;
        } else {
            constructor = new WasmNullConstant(WasmType.Reference.FUNC);
        }
        var createClass = new WasmCall(createClassFunction(context), jsClassName, jsExportedParent, constructor);
        expressions.add(0, new WasmSetGlobal(global, createClass));
        var globalForStatic = global;
        if (needsExport) {
            globalForStatic = exportClass(context, cls, global, expressions);
        }

        defineStaticMethods(context, staticMembers, cls, globalForStatic, expressions, isModule);
        defineStaticProperties(context, staticMembers, cls, globalForStatic, expressions);

        context.addToInitializer(f -> f.getBody().addAll(expressions));
        return global;
    }

    private void defineMethods(WasmGCJsoContext context, AliasCollector.Members members, ClassReader cls,
            WasmGlobal global, List<WasmExpression> expressions) {
        for (var aliasEntry : members.methods.entrySet()) {
            if (!aliasEntry.getValue().getClassName().equals(cls.getName())) {
                continue;
            }
            var fn = context.functions().forStaticMethod(aliasEntry.getValue());
            var methodReader = context.classes().getMethod(aliasEntry.getValue());
            fn.setReferenced(true);
            var methodName = context.strings().getStringConstant(aliasEntry.getKey());
            var jsMethodName = stringToJs(context, new WasmGetGlobal(methodName.global));
            var defineMethod = new WasmCall(defineMethodFunction(context), new WasmGetGlobal(global),
                    jsMethodName, new WasmFunctionReference(fn), varargValue(methodReader));
            expressions.add(defineMethod);
        }
    }

    private void defineProperties(WasmGCJsoContext context, AliasCollector.Members members, ClassReader cls,
            WasmGlobal global, List<WasmExpression> expressions) {
        for (var aliasEntry : members.properties.entrySet()) {
            var property = aliasEntry.getValue();
            if (!property.getter.getClassName().equals(cls.getName())) {
                continue;
            }
            var getter = context.functions().forStaticMethod(property.getter);
            getter.setReferenced(true);
            WasmFunction setter = null;
            if (property.setter != null) {
                setter = context.functions().forStaticMethod(property.setter);
                setter.setReferenced(true);
            }
            var setterRef = setter != null
                    ? new WasmFunctionReference(setter)
                    : new WasmNullConstant(WasmType.Reference.FUNC);
            var methodName = context.strings().getStringConstant(aliasEntry.getKey());
            var jsMethodName = stringToJs(context, new WasmGetGlobal(methodName.global));
            var defineProperty = new WasmCall(definePropertyFunction(context), new WasmGetGlobal(global),
                    jsMethodName, new WasmFunctionReference(getter), setterRef);
            expressions.add(defineProperty);
        }
    }

    private void defineStaticMethods(WasmGCJsoContext context, AliasCollector.Members members, ClassReader cls,
            WasmGlobal global, List<WasmExpression> expressions, boolean isModule) {
        for (var aliasEntry : members.methods.entrySet()) {
            if (!aliasEntry.getValue().getClassName().equals(cls.getName())) {
                continue;
            }
            var fn = context.functions().forStaticMethod(aliasEntry.getValue());
            fn.setReferenced(true);
            var methodReader = context.classes().getMethod(aliasEntry.getValue());
            if (isModule) {
                var globalName = context.names().topLevel("teavm.js.export.function@" + aliasEntry.getKey());
                var functionGlobal = new WasmGlobal(globalName, WasmType.Reference.EXTERN,
                        new WasmNullConstant(WasmType.Reference.EXTERN));
                functionGlobal.setExportName(aliasEntry.getKey());
                context.module().globals.add(functionGlobal);
                fn.setReferenced(true);
                var exportedFn = new WasmCall(defineFunctionFunction(context), new WasmFunctionReference(fn),
                        varargValue(methodReader));
                expressions.add(new WasmSetGlobal(functionGlobal, exportedFn));
            }
            var methodName = context.strings().getStringConstant(aliasEntry.getKey());
            var jsMethodName = stringToJs(context, new WasmGetGlobal(methodName.global));
            var defineMethod = new WasmCall(defineStaticMethodFunction(context), new WasmGetGlobal(global),
                    jsMethodName, new WasmFunctionReference(fn), varargValue(methodReader));
            expressions.add(defineMethod);
        }
    }

    private WasmExpression varargValue(MethodReader methodReader) {
        return new WasmInt32Constant(methodReader.getAnnotations().get(JSVararg.class.getName()) != null ? 1 : 0);
    }

    private void defineStaticProperties(WasmGCJsoContext context, AliasCollector.Members members, ClassReader cls,
            WasmGlobal global, List<WasmExpression> expressions) {
        for (var aliasEntry : members.properties.entrySet()) {
            var property = aliasEntry.getValue();
            if (!property.getter.getClassName().equals(cls.getName())) {
                continue;
            }
            var getter = context.functions().forStaticMethod(property.getter);
            getter.setReferenced(true);
            WasmFunction setter = null;
            if (property.setter != null) {
                setter = context.functions().forStaticMethod(property.setter);
                setter.setReferenced(true);
            }
            var setterRef = setter != null
                    ? new WasmFunctionReference(setter)
                    : new WasmNullConstant(WasmType.Reference.FUNC);
            var methodName = context.strings().getStringConstant(aliasEntry.getKey());
            var jsMethodName = stringToJs(context, new WasmGetGlobal(methodName.global));
            var defineProperty = new WasmCall(defineStaticPropertyFunction(context), new WasmGetGlobal(global),
                    jsMethodName, new WasmFunctionReference(getter), setterRef);
            expressions.add(defineProperty);
        }
    }

    private WasmGlobal exportClass(WasmGCJsoContext context, ClassReader cls, WasmGlobal global,
            List<WasmExpression> expressions) {
        var exportName = getClassAliasName(cls);
        var globalName = context.names().topLevel("teavm.js.export.class@" + exportName);
        var exportGlobal = new WasmGlobal(globalName, WasmType.Reference.EXTERN,
                new WasmNullConstant(WasmType.Reference.EXTERN));
        exportGlobal.setExportName(exportName);
        context.module().globals.add(exportGlobal);

        var exported = new WasmCall(exportClassFunction(context), new WasmGetGlobal(global));
        expressions.add(new WasmSetGlobal(exportGlobal, exported));

        return exportGlobal;
    }

    private String parentExportedClass(WasmGCJsoContext context, String className) {
        while (className != null) {
            var cls = context.classes().get(className);
            if (cls == null) {
                return null;
            }
            if (cls.getInterfaces().contains(JSMarshallable.class.getName())) {
                return className;
            }
            className = cls.getParent();
        }
        return null;
    }

    private WasmFunction createClassFunction(WasmGCJsoContext context) {
        if (createClassFunction == null) {
            createClassFunction = new WasmFunction(context.functionTypes().of(WasmType.Reference.EXTERN,
                    WasmType.Reference.EXTERN, WasmType.Reference.EXTERN, WasmType.Reference.FUNC));
            createClassFunction.setName(context.names().suggestForClass("teavm.jso@createClass"));
            createClassFunction.setImportName("createClass");
            createClassFunction.setImportModule("teavmJso");
            context.module().functions.add(createClassFunction);
        }
        return createClassFunction;
    }

    private WasmFunction defineFunctionFunction(WasmGCJsoContext context) {
        if (defineFunctionFunction == null) {
            defineFunctionFunction = new WasmFunction(context.functionTypes().of(WasmType.Reference.EXTERN,
                    WasmType.Reference.FUNC, WasmType.INT32));
            defineFunctionFunction.setName(context.names().suggestForClass("teavm.jso@defineFunction"));
            defineFunctionFunction.setImportName("defineFunction");
            defineFunctionFunction.setImportModule("teavmJso");
            context.module().functions.add(defineFunctionFunction);
        }
        return defineFunctionFunction;
    }

    private WasmFunction defineMethodFunction(WasmGCJsoContext context) {
        if (defineMethodFunction == null) {
            defineMethodFunction = new WasmFunction(context.functionTypes().of(null,
                    WasmType.Reference.EXTERN, WasmType.Reference.EXTERN, WasmType.Reference.FUNC,
                    WasmType.INT32));
            defineMethodFunction.setName(context.names().suggestForClass("teavm.jso@defineMethod"));
            defineMethodFunction.setImportName("defineMethod");
            defineMethodFunction.setImportModule("teavmJso");
            context.module().functions.add(defineMethodFunction);
        }
        return defineMethodFunction;
    }

    private WasmFunction defineStaticMethodFunction(WasmGCJsoContext context) {
        if (defineStaticMethodFunction == null) {
            defineStaticMethodFunction = new WasmFunction(context.functionTypes().of(null,
                    WasmType.Reference.EXTERN, WasmType.Reference.EXTERN, WasmType.Reference.FUNC,
                    WasmType.INT32));
            defineStaticMethodFunction.setName(context.names().suggestForClass("teavm.jso@defineStaticMethod"));
            defineStaticMethodFunction.setImportName("defineStaticMethod");
            defineStaticMethodFunction.setImportModule("teavmJso");
            context.module().functions.add(defineStaticMethodFunction);
        }
        return defineStaticMethodFunction;
    }

    private WasmFunction definePropertyFunction(WasmGCJsoContext context) {
        if (definePropertyFunction == null) {
            definePropertyFunction = new WasmFunction(context.functionTypes().of(null,
                    WasmType.Reference.EXTERN, WasmType.Reference.EXTERN, WasmType.Reference.FUNC,
                    WasmType.Reference.FUNC));
            definePropertyFunction.setName(context.names().suggestForClass("teavm.jso@defineProperty"));
            definePropertyFunction.setImportName("defineProperty");
            definePropertyFunction.setImportModule("teavmJso");
            context.module().functions.add(definePropertyFunction);
        }
        return definePropertyFunction;
    }

    private WasmFunction defineStaticPropertyFunction(WasmGCJsoContext context) {
        if (defineStaticPropertyFunction == null) {
            defineStaticPropertyFunction = new WasmFunction(context.functionTypes().of(null,
                    WasmType.Reference.EXTERN, WasmType.Reference.EXTERN, WasmType.Reference.FUNC,
                    WasmType.Reference.FUNC));
            defineStaticPropertyFunction.setName(context.names().suggestForClass("teavm.jso@defineStaticProperty"));
            defineStaticPropertyFunction.setImportName("defineStaticProperty");
            defineStaticPropertyFunction.setImportModule("teavmJso");
            context.module().functions.add(defineStaticPropertyFunction);
        }
        return defineStaticPropertyFunction;
    }

    private WasmFunction exportClassFunction(WasmGCJsoContext context) {
        if (exportClassFunction == null) {
            exportClassFunction = new WasmFunction(context.functionTypes().of(WasmType.Reference.EXTERN,
                    WasmType.Reference.EXTERN));
            exportClassFunction.setName(context.names().suggestForClass("teavm.jso@exportClass"));
            exportClassFunction.setImportName("exportClass");
            exportClassFunction.setImportModule("teavmJso");
            context.module().functions.add(exportClassFunction);
        }
        return exportClassFunction;
    }

    WasmFunction javaObjectToJSFunction(WasmGCJsoContext context) {
        if (javaObjectToJSFunction == null) {
            javaObjectToJSFunction = new WasmFunction(context.functionTypes().of(WasmType.Reference.EXTERN,
                    context.typeMapper().mapType(ValueType.parse(Object.class)), WasmType.Reference.EXTERN));
            javaObjectToJSFunction.setName(context.names().topLevel("teavm.jso@javaObjectToJS"));
            javaObjectToJSFunction.setImportName("javaObjectToJS");
            javaObjectToJSFunction.setImportModule("teavmJso");
            context.module().functions.add(javaObjectToJSFunction);
        }
        return javaObjectToJSFunction;
    }

    private String getClassAliasName(ClassReader cls) {
        var name = cls.getSimpleName();
        if (name == null) {
            name = cls.getName().substring(cls.getName().lastIndexOf('.') + 1);
        }
        var jsExport = cls.getAnnotations().get(JSClass.class.getName());
        if (jsExport != null) {
            var nameValue = jsExport.getValue("name");
            if (nameValue != null) {
                var nameValueString = nameValue.getString();
                if (!nameValueString.isEmpty()) {
                    name = nameValueString;
                }
            }
        }
        return name;
    }

    private static class ImportDecl {
        final String module;
        final String name;

        ImportDecl(String module, String name) {
            this.module = module;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ImportDecl)) {
                return false;
            }
            var that = (ImportDecl) o;
            return Objects.equals(module, that.module) && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(module, name);
        }
    }
}
