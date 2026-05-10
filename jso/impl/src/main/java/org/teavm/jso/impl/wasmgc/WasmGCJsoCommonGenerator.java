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
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.WasmGCInitializerRegistry;
import org.teavm.backend.wasm.generate.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.classes.WasmGCTypeMapper;
import org.teavm.backend.wasm.generate.strings.WasmGCStringProvider;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmTag;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmExternConversionType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmNullConstant;
import org.teavm.jso.JSClass;
import org.teavm.jso.impl.AliasCollector;
import org.teavm.jso.impl.JSBodyAstEmitter;
import org.teavm.jso.impl.JSBodyBloatedEmitter;
import org.teavm.jso.impl.JSBodyEmitter;
import org.teavm.jso.impl.JSMarshallable;
import org.teavm.jso.impl.JSVararg;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class WasmGCJsoCommonGenerator {
    private WasmGCJSFunctions jsFunctions;
    private WasmFunctionTypes functionTypes;
    private ClassReaderSource classes;
    private BaseWasmFunctionRepository functions;
    private WasmGCTypeMapper typeMapper;
    private WasmGCNameProvider names;
    private WasmGCStringProvider strings;
    private WasmModule module;
    private WasmTag exceptionTag;
    private WasmGCInitializerRegistry initializerRegistry;
    private String entryPoint;

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

    WasmGCJsoCommonGenerator(WasmGCJSFunctions jsFunctions, WasmFunctionTypes functionTypes,
            ClassReaderSource classes, BaseWasmFunctionRepository functions, WasmGCTypeMapper typeMapper,
            WasmGCNameProvider names, WasmGCStringProvider strings, WasmModule module, WasmTag exceptionTag,
            WasmGCInitializerRegistry initializerRegistry, String entryPoint) {
        this.jsFunctions = jsFunctions;
        this.functionTypes = functionTypes;
        this.classes = classes;
        this.functions = functions;
        this.typeMapper = typeMapper;
        this.names = names;
        this.strings = strings;
        this.module = module;
        this.exceptionTag = exceptionTag;
        this.initializerRegistry = initializerRegistry;
        this.entryPoint = entryPoint;
    }

    private void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        initializerRegistry.register(this::writeToInitializer);
        exportRethrowException();
    }

    private void writeToInitializer(WasmFunction function) {
        for (var part : initializerParts) {
            part.accept(function);
        }
    }

    private void addInitializerPart(Consumer<WasmFunction> part) {
        initialize();
        initializerParts.add(part);
    }

    WasmGlobal addJSBody(JSBodyEmitter emitter, boolean inlined) {
        initialize();
        var builder = new WasmInstructionList().builder();
        var paramCount = emitter.method().parameterCount();
        if (!emitter.isStatic()) {
            paramCount++;
        }
        var imports = emitter.imports();
        paramCount += imports.length;

        var global = new WasmGlobal(names.suggestForMethod(emitter.method()), WasmType.EXTERN);
        global.getInitialValue().add(new WasmNullConstant(WasmType.EXTERN));
        module.globals.add(global);
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

        var paramNames = new ArrayList<String>();
        for (var importDecl : imports) {
            paramNames.add(importDecl.alias);
        }
        if (!emitter.isStatic()) {
            paramNames.add("__this__");
        }
        paramNames.addAll(List.of(emitter.parameterNames()));
        for (var parameter : paramNames) {
            builder
                    .getGlobal(strings.getStringConstant(parameter).global)
                    .call(stringToJsFunction());
        }
        builder
                .getGlobal(strings.getStringConstant(body).global)
                .call(stringToJsFunction());
        builder.call(jsFunctions.getFunctionConstructor(paramCount));
        if (imports.length > 0) {
            for (var importDecl : imports) {
                var importGlobal = getImportGlobal(importDecl.fromModule, "__self__");
                builder.getGlobal(importGlobal);
            }
            builder.call(jsFunctions.getBind(imports.length));
        }
        builder.setGlobal(global);
        initializerParts.add(initializer -> initializer.getBody().transferFrom(builder.list));

        return global;
    }

    WasmGlobal getImportGlobal(String module, String id) {
        return importGlobals.computeIfAbsent(new ImportDecl(module, id), m -> {
            var name = names.topLevel(WasmGCNameProvider.sanitize("teavm.js@imports:" + module + "#" + id));
            var global = new WasmGlobal(name, WasmType.EXTERN);
            global.getInitialValue().add(new WasmNullConstant(WasmType.EXTERN));
            global.setImmutable(true);
            this.module.globals.add(global);
            global.setImportModule(module);
            global.setImportName(id);
            return global;
        });
    }

    private WasmFunction stringToJsFunction() {
        var fun = functions.forStaticMethod(STRING_TO_JS);
        if (fun.getExportName() == null) {
            fun.setExportName("teavm.stringToJs");
        }
        return fun;
    }

    private void exportRethrowException() {
        if (rethrowExported) {
            return;
        }
        rethrowExported = true;
        var fn = functions.forStaticMethod(new MethodReference(WASM_GC_JS_RUNTIME_CLASS, "wrapException",
                JS_OBJECT, ValueType.object("java.lang.Throwable")));
        fn.setExportName("teavm.js.wrapException");

        fn = functions.forStaticMethod(new MethodReference(WASM_GC_JS_RUNTIME_CLASS, "extractException",
                ValueType.object("java.lang.Throwable"), JS_OBJECT));
        fn.setExportName("teavm.js.extractException");

        createThrowExceptionFunction();
    }

    private void createThrowExceptionFunction() {
        var fn = new WasmFunction(functionTypes.of(null, WasmType.EXTERN));
        fn.setName(names.topLevel("teavm@throwException"));
        fn.setExportName("teavm.js.throwException");
        module.functions.add(fn);

        var exceptionLocal = new WasmLocal(WasmType.EXTERN);
        fn.add(exceptionLocal);

        var throwableType = (WasmType.Reference) typeMapper.mapType(ValueType.parse(Throwable.class));
        fn.getBody().builder()
                .getLocal(exceptionLocal)
                .externConvert(WasmExternConversionType.EXTERN_TO_ANY)
                .cast(throwableType)
                .throw_(exceptionTag);
    }

    WasmGlobal jsStringConstant(String str) {
        return stringsConstants.computeIfAbsent(str, s -> {
            var javaGlobal = strings.getStringConstant(s).global;
            var function = functions.forStaticMethod(STRING_TO_JS);
            var index = stringsConstants.size();
            var brief = str.length() > 16 ? str.substring(0, 16) : str;
            var name = names.topLevel("teavm.js.strings<" + index + ">:"
                    + WasmGCNameProvider.sanitize(brief));
            var jsGlobal = new WasmGlobal(name, WasmType.EXTERN);
            jsGlobal.getInitialValue().add(new WasmNullConstant(WasmType.EXTERN));
            module.globals.add(jsGlobal);
            addInitializerPart(initializer -> initializer.getBody().builder()
                    .getGlobal(javaGlobal)
                    .call(function)
                    .setGlobal(jsGlobal));
            return jsGlobal;
        });
    }

    WasmGlobal getDefaultWrapperClass() {
        if (defaultWrapperClass == null) {
            var name = names.topLevel("teavm.js@defaultWrapperClass");
            defaultWrapperClass = new WasmGlobal(name, WasmType.EXTERN);
            defaultWrapperClass.getInitialValue().add(new WasmNullConstant(WasmType.EXTERN));
            module.globals.add(defaultWrapperClass);
            addInitializerPart(initializer -> initializer.getBody().builder()
                    .nullConst(WasmType.EXTERN)
                    .nullConst(WasmType.EXTERN)
                    .nullConst(WasmType.FUNC)
                    .call(createClassFunction())
                    .setGlobal(defaultWrapperClass));
        }
        return defaultWrapperClass;
    }

    WasmGlobal getDefinedClass(String className) {
        var result = definedClasses.get(className);
        if (result == null) {
            result = defineClass(className);
            definedClasses.put(className, result);
        }
        return result;
    }

    private WasmGlobal defineClass(String className) {
        var name = names.topLevel(names.suggestForClass(className + "@js"));
        var global = new WasmGlobal(name, WasmType.EXTERN);
        global.getInitialValue().add(new WasmNullConstant(WasmType.EXTERN));
        module.globals.add(global);
        var body = new WasmInstructionList().builder();

        var cls = classes.get(className);
        var isModule = entryPoint.equals(className);
        var members = AliasCollector.collectMembers(cls, AliasCollector::isInstanceMember);

        var staticMembers = AliasCollector.collectMembers(cls, AliasCollector::isStaticMember);

        var simpleName = className.substring(className.lastIndexOf('.') + 1);
        var javaClassName = strings.getStringConstant(simpleName);
        var exportedParent = parentExportedClass(cls.getParent());

        var needsExport = !className.equals(entryPoint)
                && (!staticMembers.methods.isEmpty() || !staticMembers.properties.isEmpty());
        WasmFunction constructorFn = null;
        if (members.constructor != null) {
            constructorFn = functions.forStaticMethod(members.constructor);
            constructorFn.setReferenced(true);
            needsExport = true;
        }

        body.getGlobal(javaClassName.global).call(stringToJsFunction());
        if (exportedParent != null) {
            body.getGlobal(getDefinedClass(exportedParent));
        } else {
            body.nullConst(WasmType.EXTERN);
        }
        if (constructorFn != null) {
            body.funcRef(constructorFn);
        } else {
            body.nullConst(WasmType.FUNC);
        }
        body.call(createClassFunction()).setGlobal(global);

        defineMethods(members, cls, global, body);
        defineProperties(members, cls, global, body);

        var globalForStatic = global;
        if (needsExport) {
            globalForStatic = exportClass(cls, global, body);
        }

        defineStaticMethods(staticMembers, cls, globalForStatic, body, isModule);
        defineStaticProperties(staticMembers, cls, globalForStatic, body);

        initializerRegistry.register(f -> f.getBody().transferFrom(body.list));
        return global;
    }

    private void defineMethods(AliasCollector.Members members, ClassReader cls, WasmGlobal global,
            WasmInstructionBuilder builder) {
        for (var aliasEntry : members.methods.entrySet()) {
            if (!aliasEntry.getValue().getClassName().equals(cls.getName())) {
                continue;
            }
            var fn = functions.forStaticMethod(aliasEntry.getValue());
            var methodReader = classes.getMethod(aliasEntry.getValue());
            fn.setReferenced(true);
            var methodName = strings.getStringConstant(aliasEntry.getKey());
            builder.getGlobal(global)
                    .getGlobal(methodName.global).call(stringToJsFunction())
                    .funcRef(fn)
                    .i32Const(varargValue(methodReader))
                    .call(defineMethodFunction());
        }
    }

    private void defineProperties(AliasCollector.Members members, ClassReader cls, WasmGlobal global,
            WasmInstructionBuilder builder) {
        for (var aliasEntry : members.properties.entrySet()) {
            var property = aliasEntry.getValue();
            if (!property.getter.getClassName().equals(cls.getName())) {
                continue;
            }
            var getter = functions.forStaticMethod(property.getter);
            getter.setReferenced(true);
            WasmFunction setter = null;
            if (property.setter != null) {
                setter = functions.forStaticMethod(property.setter);
                setter.setReferenced(true);
            }
            var methodName = strings.getStringConstant(aliasEntry.getKey());
            builder.getGlobal(global)
                    .getGlobal(methodName.global).call(stringToJsFunction())
                    .funcRef(getter);
            if (setter != null) {
                builder.funcRef(setter);
            } else {
                builder.nullConst(WasmType.FUNC);
            }
            builder.call(definePropertyFunction());
        }
    }

    private void defineStaticMethods(AliasCollector.Members members, ClassReader cls, WasmGlobal global,
            WasmInstructionBuilder builder, boolean isModule) {
        for (var aliasEntry : members.methods.entrySet()) {
            if (!aliasEntry.getValue().getClassName().equals(cls.getName())) {
                continue;
            }
            var fn = functions.forStaticMethod(aliasEntry.getValue());
            fn.setReferenced(true);
            var methodReader = classes.getMethod(aliasEntry.getValue());
            if (isModule) {
                var globalName = names.topLevel("teavm.js.export.function@" + aliasEntry.getKey());
                var functionGlobal = new WasmGlobal(globalName, WasmType.EXTERN);
                functionGlobal.getInitialValue().add(new WasmNullConstant(WasmType.EXTERN));
                functionGlobal.setExportName(aliasEntry.getKey());
                module.globals.add(functionGlobal);
                builder.funcRef(fn)
                        .i32Const(varargValue(methodReader))
                        .call(defineFunctionFunction())
                        .setGlobal(functionGlobal);
            }
            var methodName = strings.getStringConstant(aliasEntry.getKey());
            builder.getGlobal(global)
                    .getGlobal(methodName.global).call(stringToJsFunction())
                    .funcRef(fn)
                    .i32Const(varargValue(methodReader))
                    .call(defineStaticMethodFunction());
        }
    }

    private int varargValue(MethodReader methodReader) {
        return methodReader.getAnnotations().get(JSVararg.class.getName()) != null ? 1 : 0;
    }

    private void defineStaticProperties(AliasCollector.Members members, ClassReader cls,
            WasmGlobal global, WasmInstructionBuilder builder) {
        for (var aliasEntry : members.properties.entrySet()) {
            var property = aliasEntry.getValue();
            if (!property.getter.getClassName().equals(cls.getName())) {
                continue;
            }
            var getter = functions.forStaticMethod(property.getter);
            getter.setReferenced(true);
            WasmFunction setter = null;
            if (property.setter != null) {
                setter = functions.forStaticMethod(property.setter);
                setter.setReferenced(true);
            }
            var methodName = strings.getStringConstant(aliasEntry.getKey());
            builder.getGlobal(global)
                    .getGlobal(methodName.global).call(stringToJsFunction())
                    .funcRef(getter);
            if (setter != null) {
                builder.funcRef(setter);
            } else {
                builder.nullConst(WasmType.FUNC);
            }
            builder.call(defineStaticPropertyFunction());
        }
    }

    private WasmGlobal exportClass(ClassReader cls, WasmGlobal global, WasmInstructionBuilder builder) {
        var exportName = getClassAliasName(cls);
        var globalName = names.topLevel("teavm.js.export.class@" + exportName);
        var exportGlobal = new WasmGlobal(globalName, WasmType.EXTERN);
        exportGlobal.getInitialValue().add(new WasmNullConstant(WasmType.EXTERN));
        exportGlobal.setExportName(exportName);
        module.globals.add(exportGlobal);

        builder.getGlobal(global).call(exportClassFunction()).setGlobal(exportGlobal);

        return exportGlobal;
    }

    private String parentExportedClass(String className) {
        while (className != null) {
            var cls = classes.get(className);
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

    private WasmFunction createClassFunction() {
        if (createClassFunction == null) {
            createClassFunction = new WasmFunction(functionTypes.of(WasmType.EXTERN, WasmType.EXTERN,
                    WasmType.EXTERN, WasmType.FUNC));
            createClassFunction.setName(names.topLevel("teavm.jso@createClass"));
            createClassFunction.setImportName("createClass");
            createClassFunction.setImportModule("teavmJso");
            module.functions.add(createClassFunction);
        }
        return createClassFunction;
    }

    private WasmFunction defineFunctionFunction() {
        if (defineFunctionFunction == null) {
            defineFunctionFunction = new WasmFunction(functionTypes.of(WasmType.EXTERN, WasmType.FUNC,
                    WasmType.INT32));
            defineFunctionFunction.setName(names.topLevel("teavm.jso@defineFunction"));
            defineFunctionFunction.setImportName("defineFunction");
            defineFunctionFunction.setImportModule("teavmJso");
            module.functions.add(defineFunctionFunction);
        }
        return defineFunctionFunction;
    }

    private WasmFunction defineMethodFunction() {
        if (defineMethodFunction == null) {
            defineMethodFunction = new WasmFunction(functionTypes.of(null, WasmType.EXTERN, WasmType.EXTERN,
                    WasmType.FUNC, WasmType.INT32));
            defineMethodFunction.setName(names.topLevel("teavm.jso@defineMethod"));
            defineMethodFunction.setImportName("defineMethod");
            defineMethodFunction.setImportModule("teavmJso");
            module.functions.add(defineMethodFunction);
        }
        return defineMethodFunction;
    }

    private WasmFunction defineStaticMethodFunction() {
        if (defineStaticMethodFunction == null) {
            defineStaticMethodFunction = new WasmFunction(functionTypes.of(null, WasmType.EXTERN, WasmType.EXTERN,
                    WasmType.FUNC, WasmType.INT32));
            defineStaticMethodFunction.setName(names.topLevel("teavm.jso@defineStaticMethod"));
            defineStaticMethodFunction.setImportName("defineStaticMethod");
            defineStaticMethodFunction.setImportModule("teavmJso");
            module.functions.add(defineStaticMethodFunction);
        }
        return defineStaticMethodFunction;
    }

    private WasmFunction definePropertyFunction() {
        if (definePropertyFunction == null) {
            definePropertyFunction = new WasmFunction(functionTypes.of(null, WasmType.EXTERN,
                    WasmType.EXTERN, WasmType.FUNC, WasmType.FUNC));
            definePropertyFunction.setName(names.topLevel("teavm.jso@defineProperty"));
            definePropertyFunction.setImportName("defineProperty");
            definePropertyFunction.setImportModule("teavmJso");
            module.functions.add(definePropertyFunction);
        }
        return definePropertyFunction;
    }

    private WasmFunction defineStaticPropertyFunction() {
        if (defineStaticPropertyFunction == null) {
            defineStaticPropertyFunction = new WasmFunction(functionTypes.of(null, WasmType.EXTERN, WasmType.EXTERN,
                    WasmType.FUNC, WasmType.FUNC));
            defineStaticPropertyFunction.setName(names.topLevel("teavm.jso@defineStaticProperty"));
            defineStaticPropertyFunction.setImportName("defineStaticProperty");
            defineStaticPropertyFunction.setImportModule("teavmJso");
            module.functions.add(defineStaticPropertyFunction);
        }
        return defineStaticPropertyFunction;
    }

    private WasmFunction exportClassFunction() {
        if (exportClassFunction == null) {
            exportClassFunction = new WasmFunction(functionTypes.of(WasmType.EXTERN, WasmType.EXTERN));
            exportClassFunction.setName(names.topLevel("teavm.jso@exportClass"));
            exportClassFunction.setImportName("exportClass");
            exportClassFunction.setImportModule("teavmJso");
            module.functions.add(exportClassFunction);
        }
        return exportClassFunction;
    }

    WasmFunction javaObjectToJSFunction() {
        if (javaObjectToJSFunction == null) {
            javaObjectToJSFunction = new WasmFunction(functionTypes.of(WasmType.EXTERN,
                    typeMapper.mapType(ValueType.parse(Object.class)), WasmType.EXTERN));
            javaObjectToJSFunction.setName(names.topLevel("teavm.jso@javaObjectToJS"));
            javaObjectToJSFunction.setImportName("javaObjectToJS");
            javaObjectToJSFunction.setImportModule("teavmJso");
            module.functions.add(javaObjectToJSFunction);
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
