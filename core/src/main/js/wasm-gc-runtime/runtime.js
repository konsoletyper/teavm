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
// !BEGINNING!

let globalsCache = new Map();
let stackDeobfuscator = null;
let exceptionFrameRegex = /.+\.wasm:wasm-function\[[0-9]+]:0x([0-9a-f]+).*/;
let getGlobalName = function(name) {
    let result = globalsCache.get(name);
    if (typeof result === "undefined") {
        result = new Function("return " + name + ";");
        globalsCache.set(name, result);
    }
    return result();
}
let setGlobalName = function(name, value) {
    new Function("value", name + " = value;")(value);
}

function defaults(imports, userExports) {
    let context = {
        exports: null,
        userExports: userExports,
        stackDeobfuscator: null
    };
    if (!hasStringBuiltins()) {
        stringImports(imports);
    }
    dateImports(imports);
    consoleImports(imports, context);
    coreImports(imports, context);
    jsoImports(imports, context);
    imports.teavmMath = Math;
    return {
        supplyExports(exports) {
            context.exports = exports;
        },
        supplyStackDeobfuscator(deobfuscator) {
            context.stackDeobfuscator = deobfuscator;
        }
    }
}

let javaExceptionSymbol = Symbol("javaException");
class JavaError extends Error {
    #context

    constructor(context, javaException) {
        super();
        this.#context = context;
        this[javaExceptionSymbol] = javaException;
        context.exports["teavm.setJsException"](javaException, this);
    }
    get message() {
        let exceptionMessage = this.#context.exports["teavm.exceptionMessage"];
        if (typeof exceptionMessage === "function") {
            let message = exceptionMessage(this[javaExceptionSymbol]);
            if (message != null) {
                return message;
            }
        }
        return "(could not fetch message)";
    }
}

function stringImports(imports) {
    imports["wasm:js-string"] = {
        fromCharCode: code => String.fromCharCode(code),
        fromCharCodeArray: () => { throw new Error("Not supported"); },
        intoCharCodeArray: () => { throw new Error("Not supported"); },
        concat: (first, second) => first + second,
        charCodeAt: (string, index) => string.charCodeAt(index),
        length: s => s.length,
        substring: (s, start, end) => s.substring(start, end)
    }
}

function dateImports(imports) {
    imports.teavmDate = {
        currentTimeMillis: () => new Date().getTime(),
        dateToString: timestamp => new Date(timestamp).toString(),
        getYear: timestamp => new Date(timestamp).getFullYear(),
        setYear(timestamp, year) {
            let date = new Date(timestamp);
            date.setFullYear(year);
            return date.getTime();
        },
        getMonth: timestamp =>new Date(timestamp).getMonth(),
        setMonth(timestamp, month) {
            let date = new Date(timestamp);
            date.setMonth(month);
            return date.getTime();
        },
        getDate: timestamp =>new Date(timestamp).getDate(),
        setDate(timestamp, value) {
            let date = new Date(timestamp);
            date.setDate(value);
            return date.getTime();
        },
        create: (year, month, date, hrs, min, sec) => new Date(year, month, date, hrs, min, sec).getTime(),
        createFromUTC: (year, month, date, hrs, min, sec) => Date.UTC(year, month, date, hrs, min, sec)
    };
}

function consoleImports(imports) {
    let stderr = "";
    let stdout = "";
    imports.teavmConsole = {
        putcharStderr(c) {
            if (c === 10) {
                console.error(stderr);
                stderr = "";
            } else {
                stderr += String.fromCharCode(c);
            }
        },
        putcharStdout(c) {
            if (c === 10) {
                console.log(stdout);
                stdout = "";
            } else {
                stdout += String.fromCharCode(c);
            }
        },
    };
}

function coreImports(imports, context) {
    let finalizationRegistry = new FinalizationRegistry(heldValue => {
        let report = context.exports["teavm.reportGarbageCollectedValue"];
        if (typeof report !== "undefined") {
            report(heldValue.queue, heldValue.ref);
        }
    });
    let stringFinalizationRegistry = new FinalizationRegistry(heldValue => {
        let report = context.exports["teavm.reportGarbageCollectedString"];
        if (typeof report === "function") {
            report(heldValue);
        }
    });
    imports.teavm = {
        createWeakRef(value, ref, queue) {
            if (queue !== null) {
                finalizationRegistry.register(value, { ref: ref, queue: queue });
            }
            return new WeakRef(value);
        },
        deref: weakRef => {
            let result = weakRef.deref();
            return result !== void 0 ? result : null;
        },
        createStringWeakRef(value, heldValue) {
            stringFinalizationRegistry.register(value, heldValue)
            return new WeakRef(value);
        },
        stringDeref: weakRef => weakRef.deref(),
        takeStackTrace() {
            let stack = new Error().stack;
            let addresses = [];
            for (let line of stack.split("\n")) {
                let match = exceptionFrameRegex.exec(line);
                if (match !== null && match.length >= 2) {
                    let address = parseInt(match[1], 16);
                    addresses.push(address);
                }
            }
            return {
                getStack() {
                    let result;
                    if (context.stackDeobfuscator) {
                        try {
                            result = context.stackDeobfuscator(addresses);
                        } catch (e) {
                            console.warn("Could not deobfuscate stack", e);
                        }
                    }
                    if (!result) {
                        result = addresses.map(address => {
                            return {
                                className: "java.lang.Throwable$FakeClass",
                                method: "fakeMethod",
                                file: "Throwable.java",
                                line: address
                            };
                        });
                    }
                    return result;
                }
            };
        },
        decorateException(javaException) {
            new JavaError(context, javaException);
        },
        linearMemory() {
            return context.exports["teavm.memory"].buffer;
        }
    };
}

function jsoImports(imports, context) {
    let javaObjectSymbol = Symbol("javaObject");
    let functionsSymbol = Symbol("functions");
    let functionOriginSymbol = Symbol("functionOrigin");
    let wrapperCallMarkerSymbol = Symbol("wrapperCallMarker");

    let jsWrappers = new WeakMap();
    let javaWrappers = new WeakMap();
    let primitiveWrappers = new Map();
    let primitiveFinalization = new FinalizationRegistry(token => primitiveWrappers.delete(token));
    let hashCodes = new WeakMap();
    let lastHashCode = 2463534242;
    let nextHashCode = () => {
        let x = lastHashCode;
        x ^= x << 13;
        x ^= x >>> 17;
        x ^= x << 5;
        lastHashCode = x;
        return x;
    }

    function identity(value) {
        return value;
    }
    function sanitizeName(str) {
        let result = "";
        let firstChar = str.charAt(0);
        result += isIdentifierStart(firstChar) ? firstChar : '_';
        for (let i = 1; i < str.length; ++i) {
            let c = str.charAt(i)
            result += isIdentifierPart(c) ? c : '_';
        }
        return result;
    }
    function isIdentifierStart(s) {
        return s >= 'A' && s <= 'Z' || s >= 'a' && s <= 'z' || s === '_' || s === '$';
    }
    function isIdentifierPart(s) {
        return isIdentifierStart(s) || s >= '0' && s <= '9';
    }
    function setProperty(obj, prop, value) {
        if (obj === null) {
            setGlobalName(prop, value);
        } else {
            obj[prop] = value;
        }
    }
    function javaExceptionToJs(e) {
        if (e instanceof WebAssembly.Exception) {
            let tag = context.exports["teavm.javaException"];
            let getJsException = context.exports["teavm.getJsException"];
            if (e.is(tag)) {
                let javaException = e.getArg(tag, 0);
                let extracted = extractException(javaException);
                if (extracted !== null) {
                    return extracted;
                }
                let wrapper = getJsException(javaException);
                if (typeof wrapper === "undefined") {
                    wrapper = new JavaError(context, javaException);
                }
                return wrapper;
            }
        }
        return e;
    }
    function jsExceptionAsJava(e) {
        if (javaExceptionSymbol in e) {
            return e[javaExceptionSymbol];
        } else {
            return context.exports["teavm.js.wrapException"](e);
        }
    }
    function rethrowJsAsJava(e) {
        context.exports["teavm.js.throwException"](jsExceptionAsJava(e));
    }
    function extractException(e) {
        return context.exports["teavm.js.extractException"](e);
    }
    function rethrowJavaAsJs(e) {
        throw javaExceptionToJs(e);
    }
    function getProperty(obj, prop) {
        try {
            return obj !== null ? obj[prop] : getGlobalName(prop)
        } catch (e) {
            rethrowJsAsJava(e);
        }
    }
    function defineFunction(fn) {
        let params = [];
        for (let i = 0; i < fn.length; ++i) {
            params.push("p" + i);
        }
        let paramsAsString = params.length === 0 ? "" : params.join(", ");
        return new Function("rethrowJavaAsJs", "fn",
            `return function(${paramsAsString}) {\n` +
            `    try {\n` +
            `        return fn(${paramsAsString});\n` +
            `    } catch (e) {\n` +
            `        rethrowJavaAsJs(e);\n` +
            `    }\n` +
            `};`
        )(rethrowJavaAsJs, fn);
    }
    function renameConstructor(name, c) {
        return new Function(
            "constructor",
            `return function ${name}(marker, javaObject) {\n` +
            `    return constructor.call(this, marker, javaObject);\n` +
            `}\n`
        )(c);
    }
    imports.teavmJso = {
        stringBuiltinsSupported: () => hasStringBuiltins(),
        isUndefined: o => typeof o === "undefined",
        emptyArray: () => [],
        appendToArray: (array, e) => array.push(e),
        unwrapBoolean: value => value ? 1 : 0,
        wrapBoolean: value => !!value,
        getProperty: getProperty,
        setProperty: setProperty,
        setPropertyPure: setProperty,
        global(name) {
            try {
                return getGlobalName(name);
            } catch (e) {
                rethrowJsAsJava(e);
            }
        },
        createClass(name, parent, constructor) {
            name = sanitizeName(name || "JavaObject");
            let action;
            if (parent === null) {
                action = function (javaObject) {
                    this[javaObjectSymbol] = javaObject;
                    this[functionsSymbol] = null;
                };
            } else {
                action = function (javaObject) {
                    parent.call(this, javaObject);
                };
                fn.prototype = Object.create(parent);
                fn.prototype.constructor = parent;
            }
            let fn = renameConstructor(name, function (marker, javaObject) {
                if (marker === wrapperCallMarkerSymbol) {
                    action.call(this, javaObject);
                } else if (constructor === null) {
                    throw new Error("This class can't be instantiated directly");
                } else {
                    try {
                        return constructor.apply(null, arguments);
                    } catch (e) {
                        rethrowJavaAsJs(e);
                    }
                }
            });
            fn.prototype = Object.create(parent || Object.prototype);
            fn.prototype.constructor = fn;
            let boundFn = renameConstructor(name, function(javaObject) {
                return fn.call(this, wrapperCallMarkerSymbol, javaObject);
            });
            boundFn[wrapperCallMarkerSymbol] = fn;
            boundFn.prototype = fn.prototype;
            return boundFn;
        },
        exportClass(cls) {
            return cls[wrapperCallMarkerSymbol];
        },
        defineMethod(cls, name, fn) {
            let params = [];
            for (let i = 1; i < fn.length; ++i) {
                params.push("p" + i);
            }
            let paramsAsString = params.length === 0 ? "" : params.join(", ");
            cls.prototype[name] = new Function("rethrowJavaAsJs", "fn",
                `return function(${paramsAsString}) {\n` +
                `    try {\n` +
                `        return fn(${['this', params].join(", ")});\n` +
                `    } catch (e) {\n` +
                `        rethrowJavaAsJs(e);\n` +
                `    }\n` +
                `};`
            )(rethrowJavaAsJs, fn);
        },
        defineStaticMethod(cls, name, fn) {
            cls[name] = defineFunction(fn);
        },
        defineFunction: defineFunction,
        defineProperty(cls, name, getFn, setFn) {
            let descriptor = {
                get() {
                    try {
                        return getFn(this);
                    } catch (e) {
                        rethrowJavaAsJs(e);
                    }
                }
            };
            if (setFn !== null) {
                descriptor.set = function(value) {
                    try {
                        setFn(this, value);
                    } catch (e) {
                        rethrowJavaAsJs(e);
                    }
                }
            }
            Object.defineProperty(cls.prototype, name, descriptor);
        },
        defineStaticProperty(cls, name, getFn, setFn) {
            let descriptor = {
                get() {
                    try {
                        return getFn();
                    } catch (e) {
                        rethrowJavaAsJs(e);
                    }
                }
            };
            if (setFn !== null) {
                descriptor.set = function(value) {
                    try {
                        setFn(value);
                    } catch (e) {
                        rethrowJavaAsJs(e);
                    }
                }
            }
            Object.defineProperty(cls, name, descriptor);
        },
        javaObjectToJS(instance, cls) {
            if (instance === null) {
                return null;
            }
            let existing = jsWrappers.get(instance);
            if (typeof existing != "undefined") {
                let result = existing.deref();
                if (typeof result !== "undefined") {
                    return result;
                }
            }
            let obj = new cls(instance);
            jsWrappers.set(instance, new WeakRef(obj));
            return obj;
        },
        unwrapJavaObject(instance) {
            return instance[javaObjectSymbol];
        },
        asFunction(instance, propertyName) {
            let functions = instance[functionsSymbol];
            if (functions === null) {
                functions = Object.create(null);
                instance[functionsSymbol] = functions;
            }
            let result = functions[propertyName];
            if (typeof result !== 'function') {
                result = function() {
                    return instance[propertyName].apply(instance, arguments);
                }
                result[functionOriginSymbol] = instance;
                functions[propertyName] = result;
            }
            return result;
        },
        functionAsObject(fn, property) {
            let origin = fn[functionOriginSymbol];
            if (typeof origin !== 'undefined') {
                let functions = origin[functionsSymbol];
                if (functions !== void 0 && functions[property] === fn) {
                    return origin;
                }
            }
            return {
                [property]: function(...args) {
                    try {
                        return fn(...args);
                    } catch (e) {
                        rethrowJavaAsJs(e);
                    }
                }
            };
        },
        wrapObject(obj) {
            if (obj === null) {
                return null;
            }
            if (typeof obj === "object" || typeof obj === "function" || typeof "obj" === "symbol") {
                let result = obj[javaObjectSymbol];
                if (typeof result === "object") {
                    return result;
                }
                result = javaWrappers.get(obj);
                if (result !== void 0) {
                    result = result.deref();
                    if (result !== void 0) {
                        return result;
                    }
                }
                result = context.exports["teavm.jso.createWrapper"](obj);
                javaWrappers.set(obj, new WeakRef(result));
                return result;
            } else {
                let result = primitiveWrappers.get(obj);
                if (result !== void 0) {
                    result = result.deref();
                    if (result !== void 0) {
                        return result;
                    }
                }
                result = context.exports["teavm.jso.createWrapper"](obj);
                primitiveWrappers.set(obj, new WeakRef(result));
                primitiveFinalization.register(result, obj);
                return result;
            }
        },
        isPrimitive: (value, type) => typeof value === type,
        instanceOf: (value, type) => value instanceof type,
        instanceOfOrNull: (value, type) => value === null || value instanceof type,
        sameRef: (a, b) => a === b,
        hashCode: (obj) => {
            if (typeof obj === "object" || typeof obj === "function" || typeof obj === "symbol") {
                let code = hashCodes.get(obj);
                if (typeof code === "number") {
                    return code;
                }
                code = nextHashCode();
                hashCodes.set(obj, code);
                return code;
            } else if (typeof obj === "number") {
                return obj | 0;
            } else if (typeof obj === "bigint") {
                return BigInt.asIntN(obj, 32);
            } else if (typeof obj === "boolean") {
                return obj ? 1 : 0;
            } else {
                return 0;
            }
        },
        apply: (instance, method, args) => {
            try {
                if (instance === null) {
                    let fn = getGlobalName(method);
                    return fn(...args);
                } else {
                    return instance[method](...args);
                }
            } catch (e) {
                rethrowJsAsJava(e);
            }
        },
        concatArray: (a, b) => [...a, ...b],
        getJavaException: e => e[javaExceptionSymbol],
        getJSException: e => {
            let getJsException = context.exports["teavm.getJsException"]
            return getJsException(e);
        },
        jsExports: () => context.userExports
    };
    for (let name of ["wrapByte", "wrapShort", "wrapChar", "wrapInt", "wrapLong", "wrapFloat", "wrapDouble",
        "unwrapByte", "unwrapShort", "unwrapChar", "unwrapInt", "unwrapLong", "unwrapFloat", "unwrapDouble"]) {
        imports.teavmJso[name] = identity;
    }
    function wrapCallFromJavaToJs(call) {
        try {
            return call();
        } catch (e) {
            rethrowJsAsJava(e);
        }
    }
    let argumentList = [];
    for (let i = 0; i < 32; ++i) {
        let args = argumentList.length === 0 ? "" : argumentList.join(", ");
        let argsAndBody = [...argumentList, "body"].join(", ");
        imports.teavmJso["createFunction" + i] = new Function("wrapCallFromJavaToJs", ...argumentList, "body",
            `return new Function('wrapCallFromJavaToJs', ${argsAndBody}).bind(this, wrapCallFromJavaToJs);`
        ).bind(null, wrapCallFromJavaToJs);
        imports.teavmJso["bindFunction" + i] = (f, ...args) => f.bind(null, ...args);
        imports.teavmJso["callFunction" + i] = new Function("rethrowJsAsJava", "fn", ...argumentList,
            `try {\n` +
            `    return fn(${args});\n` +
            `} catch (e) {\n` +
            `    rethrowJsAsJava(e);\n` +
            `}`
        ).bind(null, rethrowJsAsJava);
        imports.teavmJso["callMethod" + i] = new Function("rethrowJsAsJava", "getGlobalName", "instance",
            "method", ...argumentList,
            `try {\n`+
            `    return instance !== null\n` +
            `        ? instance[method](${args})\n` +
            `        : getGlobalName(method)(${args});\n` +
            `} catch (e) {\n` +
            `    rethrowJsAsJava(e);\n` +
            `}`
        ).bind(null, rethrowJsAsJava, getGlobalName);
        imports.teavmJso["construct" + i] = new Function("rethrowJsAsJava", "constructor", ...argumentList,
            `try {\n` +
            `    return new constructor(${args});\n` +
            `} catch (e) {\n` +
            `    rethrowJsAsJava(e);\n` +
            `}`
        ).bind(null, rethrowJsAsJava);
        imports.teavmJso["arrayOf" + i] = new Function(...argumentList, "return [" + args + "]");

        let param = "p" + (i + 1);
        argumentList.push(param);
    }
}

function wrapImport(importObj) {
    return new Proxy(importObj, {
        get(target, prop) {
            let result = target[prop];
            return new WebAssembly.Global({ value: "externref", mutable: false }, result);
        }
    });
}

async function wrapImports(wasmModule, imports) {
    let promises = [];
    let propertiesToAdd = {};
    for (let { module, name, kind } of WebAssembly.Module.imports(wasmModule)) {
        if (kind !== "global" || module in imports) {
            continue;
        }
        let names = propertiesToAdd[module];
        if (names === void 0) {
            let namesByModule = [];
            names = namesByModule;
            propertiesToAdd[module] = names;
            promises.push((async () => {
                let moduleInstance = await import(module);
                let importsByModule = {};
                for (let name of namesByModule) {
                    let importedName = name === "__self__" ? moduleInstance : moduleInstance[name];
                    importsByModule[name] = new WebAssembly.Global(
                        { value: "externref", mutable: false },
                        importedName
                    );
                }
                imports[module] = importsByModule;
            })());
        }
        names.push(name);
    }
    if (promises.length === 0) {
        return;
    }
    await Promise.all(promises);
}

async function load(src, options) {
    if (!options) {
        options = {};
    }

    let deobfuscatorOptions = options.stackDeobfuscator || {};
    let debugInfoLocation = deobfuscatorOptions.infoLocation || "auto";
    let compilationPromise = typeof src === "string"
        ? WebAssembly.compileStreaming(fetch(src), { builtins: ["js-string"] })
        : WebAssembly.compile(src, { builtins: ["js-string"] });
    let [deobfuscatorFactory, module, debugInfo] = await Promise.all([
        deobfuscatorOptions.enabled ? getDeobfuscator(src, deobfuscatorOptions) : Promise.resolve(null),
        compilationPromise,
        fetchExternalDebugInfo(src, debugInfoLocation, deobfuscatorOptions)
    ]);

    const importObj = {};
    let userExports = {};
    const defaultsResult = defaults(importObj, userExports);
    if (typeof options.installImports !== "undefined") {
        options.installImports(importObj);
    }
    if (!options.noAutoImports) {
        await wrapImports(module, importObj);
    }
    let instance = await WebAssembly.instantiate(module, importObj);

    defaultsResult.supplyExports(instance.exports);
    if (deobfuscatorFactory) {
        let moduleToPass = debugInfoLocation === "auto" || debugInfoLocation === "embedded" ? module : null;
        let deobfuscator = createDeobfuscator(moduleToPass, debugInfo, deobfuscatorFactory);
        if (deobfuscator !== null) {
            defaultsResult.supplyStackDeobfuscator(deobfuscator);
        }
    }
    let teavm = {
        exports: userExports,
        instance: instance,
        module: instance.module
    };
    for (let key in instance.exports) {
        let exportObj = instance.exports[key];
        if (exportObj instanceof WebAssembly.Global) {
            Object.defineProperty(userExports, key, {
                get: () => exportObj.value
            });
        }
    }
    return teavm;
}

let stringBuiltinsCache = null;

function hasStringBuiltins() {
    if (stringBuiltinsCache === null) {
        /*
          (module
           (type  (func))
           (import "wasm:js-string" "cast" (func (type 0)))
          )
         */
        let bytes = new Int8Array([0, 97, 115, 109, 1, 0, 0, 0, 1, 4, 1, 96, 0, 0, 2, 23, 1, 14, 119, 97,
            115, 109, 58, 106, 115, 45, 115, 116, 114, 105, 110, 103, 4, 99, 97, 115, 116, 0, 0, 3, 1, 0,
            5, 4, 1, 1, 0, 0, 10, -127, -128, -128, 0, 0]);
        stringBuiltinsCache = !WebAssembly.validate(bytes, {builtins: ["js-string"]});
    }
    return stringBuiltinsCache;
}

async function getDeobfuscator(path, options) {
    if (typeof path !== "string") {
        return null;
    }
    try {
        const importObj = {};
        const defaultsResult = defaults(importObj, {});
        const deobfuscatorPath = options.path || path + "-deobfuscator.wasm";
        const { instance } = await WebAssembly.instantiateStreaming(
            fetch(deobfuscatorPath),
            importObj,
            {
                builtins: ["js-string"]
            }
        );
        defaultsResult.supplyExports(instance.exports)
        return instance;
    } catch (e) {
        console.warn("Could not load deobfuscator", e);
        return null;
    }
}

function createDeobfuscator(module, externalData, deobfuscatorFactory) {
    let deobfuscator = null;
    let deobfuscatorInitialized = false;
    function ensureDeobfuscator() {
        if (!deobfuscatorInitialized) {
            deobfuscatorInitialized = true;
            if (externalData !== null) {
                try {
                    deobfuscator = deobfuscatorFactory.exports.createFromExternalFile.value(externalData);
                } catch (e) {
                    console.warn("Could not load create deobfuscator", e);
                }
            }
            if (deobfuscator == null && module !== null) {
                try {
                    deobfuscator = deobfuscatorFactory.exports.createForModule.value(module);
                } catch (e) {
                    console.warn("Could not create deobfuscator from module data", e);
                }
            }
        }
    }
    return addresses => {
        ensureDeobfuscator();
        return deobfuscator !== null ? deobfuscator.deobfuscate(addresses) : [];
    }
}

async function fetchExternalDebugInfo(path, debugInfoLocation, options) {
    if (!options.enabled || typeof path !== "string") {
        return null;
    }
    if (debugInfoLocation !== "auto" && debugInfoLocation !== "external") {
        return null;
    }
    let location = options.externalInfoPath || path + ".teadbg";
    let response = await fetch(location);
    if (!response.ok) {
        return null;
    }
    return new Int8Array(await response.arrayBuffer());
}