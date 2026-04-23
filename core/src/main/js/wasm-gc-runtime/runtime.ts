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

// --- Interfaces ---

interface Context {
    exports: WebAssembly.Exports;
    userExports: Record<string, unknown>;
    stackDeobfuscator: ((addresses: number[]) => StackFrame[]) | null;
}

interface StackFrame {
    className: string;
    method: string;
    file: string;
    line: number;
}

interface DefaultsResult {
    supplyExports(exports: WebAssembly.Exports): void;
    supplyStackDeobfuscator(deobfuscator: (addresses: number[]) => StackFrame[]): void;
}

export interface TeaVMInstance {
    exports: Record<string, unknown>;
    instance: WebAssembly.Instance;
    module: WebAssembly.Module;
}

export interface LoadOptions {
    nodejs?: boolean;
    emscriptenModules?: Record<string, EmscriptenModulePath>;
    stackDeobfuscator?: DeobfuscatorOptions;
    memory?: MemoryOptions;
    stack?: number;
    installImports?: (imports: Record<string, unknown>) => void;
    noAutoImports?: boolean;
}

interface DeobfuscatorOptions {
    enabled?: boolean;
    infoLocation?: "auto" | "embedded" | "external";
    path?: string;
    externalInfoPath?: string | Int8Array;
}

interface MemoryOptions {
    external?: WebAssembly.Memory;
    maxSize?: number;
    minSize?: number;
    shared?: boolean;
    onResize?: () => void;
}

interface EmscriptenModulePath {
    pathToJs: string;
    pathToWasm: string;
}

interface EmscriptenModule {
    name: string;
    wasmModule: WebAssembly.Module;
    jsLoader: (options: EmscriptenJsLoaderOptions) => Promise<any>;
    tableOffset?: number;
    memoryOffset?: number;
}

interface EmscriptenJsLoaderOptions {
    wasmMemory: WebAssembly.Memory;
    instantiateWasm(
        imports: Record<string, unknown>,
        successCallback: (instance: WebAssembly.Instance) => void
    ): void;
}

interface DylinkInfo {
    memorySize?: number;
    memoryAlignment?: number;
    tableSize?: number;
    tableAlignment?: number;
}

interface Allocator {
    malloc: (size: number) => number;
    free: (ptr: number) => void;
    realloc: (ptr: number, newSize: number) => number;
}

interface MemoryDefaults {
    dataSize?: number;
    min?: number;
    shared?: boolean;
}

// --- Implementation ---

const globalsCache = new Map<string, () => unknown>();
const exceptionFrameRegex = /.+:wasm-function\[[0-9]+]:0x([0-9a-f]+).*/;

function getGlobalName(name: string): unknown {
    let result = globalsCache.get(name);
    if (typeof result === "undefined") {
        result = new Function("return " + name + ";") as () => unknown;
        globalsCache.set(name, result);
    }
    return result();
}

function setGlobalName(name: string, value: unknown): void {
    new Function("value", name + " = value;")(value);
}

export function defaults(
    imports: Record<string, unknown>,
    userExports: Record<string, unknown>,
    stringBuiltins: boolean
): DefaultsResult {
    const context: Context = {
        exports: null!,
        userExports,
        stackDeobfuscator: null
    };
    if (!stringBuiltins) {
        stringImports(imports);
    }
    dateImports(imports);
    consoleImports(imports);
    coreImports(imports, context);
    asyncImports(imports);
    jsoImports(imports, context, stringBuiltins);
    imports.teavmMath = Math;
    return {
        supplyExports(exports: WebAssembly.Exports) {
            context.exports = exports;
        },
        supplyStackDeobfuscator(deobfuscator: (addresses: number[]) => StackFrame[]) {
            context.stackDeobfuscator = deobfuscator;
        }
    };
}

const javaExceptionSymbol = Symbol("javaException");

class JavaError extends Error {
    #context: Context;
    [javaExceptionSymbol]: unknown;

    constructor(context: Context, javaException: unknown) {
        super();
        this.#context = context;
        this[javaExceptionSymbol] = javaException;
        (context.exports["teavm.setJsException"] as Function)(javaException, this);
    }

    get message(): string {
        const exceptionMessage = this.#context.exports["teavm.exceptionMessage"];
        const stringToJs = this.#context.exports["teavm.stringToJs"];
        if (typeof exceptionMessage === "function" && typeof stringToJs === "function") {
            const msg = exceptionMessage(this[javaExceptionSymbol]);
            if (msg != null) {
                return (stringToJs as Function)(msg) as string;
            }
        }
        return "(could not fetch message)";
    }
}

function stringImports(imports: Record<string, unknown>): void {
    imports["wasm:js-string"] = {
        fromCharCode: (code: number) => String.fromCharCode(code),
        fromCharCodeArray: () => { throw new Error("Not supported"); },
        intoCharCodeArray: () => { throw new Error("Not supported"); },
        concat: (first: string, second: string) => first + second,
        charCodeAt: (string: string, index: number) => string.charCodeAt(index),
        length: (s: string) => s.length,
        substring: (s: string, start: number, end: number) => s.substring(start, end)
    };
}

function dateImports(imports: Record<string, unknown>): void {
    imports.teavmDate = {
        currentTimeMillis: () => new Date().getTime(),
        dateToString: (timestamp: number) => new Date(timestamp).toString(),
        getYear: (timestamp: number) => new Date(timestamp).getFullYear(),
        setYear(timestamp: number, year: number) {
            const date = new Date(timestamp);
            date.setFullYear(year);
            return date.getTime();
        },
        getMonth: (timestamp: number) => new Date(timestamp).getMonth(),
        setMonth(timestamp: number, month: number) {
            const date = new Date(timestamp);
            date.setMonth(month);
            return date.getTime();
        },
        getDate: (timestamp: number) => new Date(timestamp).getDate(),
        setDate(timestamp: number, value: number) {
            const date = new Date(timestamp);
            date.setDate(value);
            return date.getTime();
        },
        create: (year: number, month: number, date: number, hrs: number, min: number, sec: number) =>
            new Date(year, month, date, hrs, min, sec).getTime(),
        createFromUTC: (year: number, month: number, date: number, hrs: number, min: number, sec: number) =>
            Date.UTC(year, month, date, hrs, min, sec)
    };
}

function consoleImports(imports: Record<string, unknown>): void {
    let stderr = "";
    let stdout = "";
    imports.teavmConsole = {
        putcharStderr(c: number) {
            if (c === 10) {
                console.error(stderr);
                stderr = "";
            } else {
                stderr += String.fromCharCode(c);
            }
        },
        putcharStdout(c: number) {
            if (c === 10) {
                console.log(stdout);
                stdout = "";
            } else {
                stdout += String.fromCharCode(c);
            }
        },
    };
}

function align(address: number, shift: number): number {
    return (((address - 1) >> shift) + 1) << shift;
}

async function linkImports(
    imports: Record<string, unknown>,
    options: LoadOptions,
    module: WebAssembly.Module | null,
    emscriptenModules: EmscriptenModule[]
): Promise<Allocator> {
    const memoryOptions = options.memory ?? {};
    const memDefaults = module ? getMemoryDefaults(module) : {};
    let memoryInstance = memoryOptions.external;
    const stackSize = options.stack ?? 2 * (1 << 20);

    let ptr = memDefaults.dataSize ?? 0;
    let tablePtr = 0;
    for (const emscriptenModule of emscriptenModules) {
        const dylinkInfo = extractDylinkInfo(emscriptenModule.wasmModule);
        const memSize = dylinkInfo.memorySize ?? 0;
        const memAlign = dylinkInfo.memoryAlignment ?? 1;
        const tableSize = dylinkInfo.tableSize ?? 0;
        const tableAlign = dylinkInfo.tableAlignment ?? 1;
        if (memSize > 0) {
            ptr = align(ptr, memAlign);
            emscriptenModule.memoryOffset = ptr;
            ptr += memSize;
        }
        if (tableSize > 0) {
            tablePtr = align(tablePtr, tableAlign);
            emscriptenModule.tableOffset = tablePtr;
            tablePtr += tableSize;
        }
    }

    let stackPtr = -1;
    let stackLow = -1;
    let stackHigh = -1;
    if (emscriptenModules.length > 0) {
        ptr = align(ptr, 4);
        stackPtr = ptr;
        stackLow = ptr;
        ptr += stackSize;
        ptr = align(ptr, 4);
        stackHigh = ptr;
    }

    let maxSize = memoryOptions.maxSize;
    ptr = align(ptr, 8);
    const heapOffset = ptr;

    if (!memoryInstance) {
        const minSize = memDefaults.min ?? 0;
        ptr += minSize;
        ptr = Math.max(ptr, memoryOptions.minSize ?? 0);
        const shared = memoryOptions.shared ?? memDefaults.shared ?? false;
        maxSize ??= (((1 << 31) - 1) | 0);
        const initialPages = Math.max(minSize, emscriptenModules.length === 0 ? 1 : 256);
        const maxPages = shared ? initialPages : ((maxSize - 1) >> 16) + 1;
        memoryInstance = new WebAssembly.Memory({
            shared,
            initial: initialPages,
            maximum: maxPages
        });
    }
    const tableInstance = tablePtr > 0
        ? new WebAssembly.Table({ initial: tablePtr, element: "anyfunc" })
        : null;
    imports.env = { memory: memoryInstance };
    imports.teavmMemory = {
        linearMemory() {
            return memoryInstance.buffer;
        },
        notifyHeapResized: memoryOptions.onResize ?? function() {},
        heapOffset: new WebAssembly.Global({ value: "i32", mutable: false }, heapOffset),
        maxSize: new WebAssembly.Global({ value: "i32", mutable: false }, maxSize)
    };

    const allocator: Allocator = { malloc: null!, free: null!, realloc: null! };

    await Promise.all(emscriptenModules.map(async ({ name, wasmModule, jsLoader, tableOffset, memoryOffset }) => {
        const loadedEmscripten = await jsLoader({
            wasmMemory: memoryInstance!,
            instantiateWasm(emscriptenImports: Record<string, unknown>, successCallback) {
                emscriptenImports.env = {
                    memory: memoryInstance,
                    malloc: (size: number) => allocator.malloc(size),
                    free: (p: number) => allocator.free(p),
                    realloc: (p: number, newSize: number) => allocator.realloc(p, newSize),
                    __indirect_function_table: tableInstance,
                    __memory_base: new WebAssembly.Global({ value: "i32", mutable: false }, memoryOffset ?? 0),
                    __table_base: new WebAssembly.Global({ value: "i32", mutable: false }, tableOffset ?? 0),
                    __stack_pointer: new WebAssembly.Global({ value: "i32", mutable: true }, stackPtr),
                };
                emscriptenImports["GOT.mem"] = {
                    __stack_low: new WebAssembly.Global({ value: "i32", mutable: true }, stackLow),
                    __stack_high: new WebAssembly.Global({ value: "i32", mutable: true }, stackHigh),
                };
                WebAssembly.instantiate(wasmModule, emscriptenImports as WebAssembly.Imports).then(successCallback);
            }
        });
        const importObj: Record<string, unknown> = {};
        for (const { name: exportName, kind: exportKind } of WebAssembly.Module.exports(wasmModule)) {
            if (exportKind === "function") {
                const fn = loadedEmscripten["_" + exportName];
                if (typeof fn === "function") {
                    importObj[exportName] = fn;
                }
            }
        }
        imports[name] = importObj;
    }));

    return allocator;
}

class ReaderHelper {
    ptr = 0;
    array: Int8Array;

    constructor(array: Int8Array) {
        this.array = array;
    }

    readVarInt(): number {
        let result = 0;
        let shift = 0;
        while (true) {
            const b = this.array[this.ptr++];
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) === 0) {
                return result;
            }
            shift += 7;
        }
    }
}

function extractDylinkInfo(module: WebAssembly.Module): DylinkInfo {
    const sections = WebAssembly.Module.customSections(module, "dylink.0");
    if (sections.length !== 1) {
        return {};
    }
    const section = new ReaderHelper(new Int8Array(sections[0]));
    const dylinkInfo: DylinkInfo = {};
    while (section.ptr < section.array.length) {
        const subsectionType = section.array[section.ptr++];
        const nextSubsection = section.ptr + section.readVarInt();
        switch (subsectionType) {
            case 1:
                dylinkInfo.memorySize = section.readVarInt();
                dylinkInfo.memoryAlignment = section.readVarInt();
                dylinkInfo.tableSize = section.readVarInt();
                dylinkInfo.tableAlignment = section.readVarInt();
                break;
        }
        section.ptr = nextSubsection;
    }
    return dylinkInfo;
}

function coreImports(imports: Record<string, unknown>, context: Context): void {
    const finalizationRegistry = new FinalizationRegistry<{ queue: unknown; ref: unknown }>(heldValue => {
        const report = context.exports["teavm.reportGarbageCollectedValue"];
        if (typeof report !== "undefined") {
            (report as Function)(heldValue.queue, heldValue.ref);
        }
    });
    const stringFinalizationRegistry = new FinalizationRegistry<unknown>(heldValue => {
        const report = context.exports?.["teavm.reportGarbageCollectedString"];
        if (typeof report === "function") {
            (report as Function)(heldValue);
        }
    });
    imports.teavm = {
        createWeakRef(value: object, ref: unknown, queue: unknown) {
            if (queue !== null) {
                finalizationRegistry.register(value, { ref, queue });
            }
            return new WeakRef(value);
        },
        deref: (weakRef: WeakRef<object>) => {
            const result = weakRef.deref();
            return result !== void 0 ? result : null;
        },
        createStringWeakRef(value: object, heldValue: unknown) {
            stringFinalizationRegistry.register(value, heldValue);
            return new WeakRef(value);
        },
        stringDeref: (weakRef: WeakRef<object>) => weakRef.deref(),
        takeStackTrace(exceptionClassName: string | null) {
            const stack = new Error().stack ?? "";
            const addresses: number[] = [];
            for (const line of stack.split("\n")) {
                const match = exceptionFrameRegex.exec(line);
                if (match !== null && match.length >= 2) {
                    addresses.push(parseInt(match[1], 16));
                }
            }
            return {
                getStack(): StackFrame[] {
                    let result: StackFrame[] | undefined;
                    if (context.stackDeobfuscator) {
                        try {
                            result = context.stackDeobfuscator(addresses);
                        } catch (e) {
                            console.warn("Could not deobfuscate stack", e);
                        }
                    }
                    if (!result) {
                        result = addresses.map(address => ({
                            className: "java.lang.Throwable$FakeClass",
                            method: "fakeMethod",
                            file: "Throwable.java",
                            line: address
                        }));
                    } else if (exceptionClassName !== null) {
                        if (result.length > 0 && result[0].className === "java.lang.Throwable"
                                && result[0].method === "fillInStackTrace") {
                            result.shift();
                        }
                        let foundIndex = -1;
                        for (let i = 0; i < result.length; ++i) {
                            if (result[i].method !== "<init>") {
                                break;
                            }
                            if (result[i].className === exceptionClassName) {
                                foundIndex = i + 1;
                                break;
                            }
                        }
                        if (foundIndex >= 0) {
                            result.splice(0, foundIndex);
                        }
                    }
                    return result;
                }
            };
        },
        decorateException(javaException: unknown) {
            new JavaError(context, javaException);
        }
    };
}

function asyncImports(imports: Record<string, unknown>): void {
    imports.teavmAsync = {
        offer(instance: unknown, fn: (instance: unknown) => void, time: number) {
            const dt = Math.max(0, time - Date.now());
            return setTimeout(() => { fn(instance); }, dt);
        },
        kill(id: ReturnType<typeof setTimeout>) {
            clearTimeout(id);
        }
    };
}

function getMemoryDefaults(module: WebAssembly.Module): MemoryDefaults {
    const sections = WebAssembly.Module.customSections(module, "teavm.memoryRequirements");
    if (sections.length !== 1) {
        return {};
    }
    return JSON.parse(new TextDecoder().decode(sections[0])) as MemoryDefaults;
}

function jsoImports(imports: Record<string, any>, context: Context, stringBuiltins: boolean): void {
    const javaObjectSymbol = Symbol("javaObject");
    const functionsSymbol = Symbol("functions");
    const functionOriginSymbol = Symbol("functionOrigin");
    const wrapperCallMarkerSymbol = Symbol("wrapperCallMarker");

    const jsWrappers = new WeakMap<object, WeakRef<object>>();
    const javaWrappers = new WeakMap<object, WeakRef<object>>();
    const primitiveWrappers = new Map<unknown, WeakRef<object>>();
    const primitiveFinalization = new FinalizationRegistry<unknown>(token => primitiveWrappers.delete(token));
    const hashCodes = new WeakMap<object, number>();
    let lastHashCode = 2463534242;
    const nextHashCode = () => {
        let x = lastHashCode;
        x ^= x << 13;
        x ^= x >>> 17;
        x ^= x << 5;
        lastHashCode = x;
        return x;
    };

    function identity(value: unknown): unknown {
        return value;
    }
    function sanitizeName(str: string): string {
        let result = "";
        const firstChar = str.charAt(0);
        result += isIdentifierStart(firstChar) ? firstChar : '_';
        for (let i = 1; i < str.length; ++i) {
            const c = str.charAt(i);
            result += isIdentifierPart(c) ? c : '_';
        }
        return result;
    }
    function isIdentifierStart(s: string): boolean {
        return s >= 'A' && s <= 'Z' || s >= 'a' && s <= 'z' || s === '_' || s === '$';
    }
    function isIdentifierPart(s: string): boolean {
        return isIdentifierStart(s) || s >= '0' && s <= '9';
    }
    function setProperty(obj: Record<string, unknown> | null, prop: string, value: unknown): void {
        if (obj === null) {
            setGlobalName(prop, value);
        } else {
            obj[prop] = value;
        }
    }
    function javaExceptionToJs(e: unknown): unknown {
        if (e instanceof WebAssembly.Exception) {
            const tag = context.exports["teavm.javaException"] as WebAssembly.Tag;
            const getJsException = context.exports["teavm.getJsException"] as Function;
            if (e.is(tag)) {
                const javaException = e.getArg(tag, 0);
                const extracted = extractException(javaException);
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
    function jsExceptionAsJava(e: unknown): unknown {
        if (javaExceptionSymbol in (e as object)) {
            return (e as Record<symbol, unknown>)[javaExceptionSymbol];
        } else {
            return (context.exports["teavm.js.wrapException"] as Function)(e);
        }
    }
    function rethrowJsAsJava(e: unknown): never {
        (context.exports["teavm.js.throwException"] as Function)(jsExceptionAsJava(e));
        throw e;
    }
    function extractException(e: unknown): unknown {
        return (context.exports["teavm.js.extractException"] as Function)(e);
    }
    function rethrowJavaAsJs(e: unknown): never {
        throw javaExceptionToJs(e);
    }
    function getProperty(obj: Record<string, unknown> | null, prop: string): unknown {
        try {
            return obj !== null ? obj[prop] : getGlobalName(prop);
        } catch (e) {
            rethrowJsAsJava(e);
        }
    }
    function defineFunction(fn: Function, vararg: boolean): Function {
        const params: string[] = [];
        const paramsForString: string[] = [];
        for (let i = 0; i < fn.length; ++i) {
            const name = "p" + i;
            params.push(name);
            paramsForString.push(name);
        }
        if (vararg) {
            const last = paramsForString.length - 1;
            paramsForString[last] = "..." + paramsForString[last];
        }
        const paramsAsString = paramsForString.join(", ");
        return new Function("rethrowJavaAsJs", "fn",
            `return function(${paramsAsString}) {\n` +
            `    try {\n` +
            `        return fn(${params.join(', ')});\n` +
            `    } catch (e) {\n` +
            `        rethrowJavaAsJs(e);\n` +
            `    }\n` +
            `};`
        )(rethrowJavaAsJs, fn) as Function;
    }
    function renameConstructor(name: string, c: Function): Function {
        return new Function(
            "constructor",
            `return function ${name}(marker, javaObject) {\n` +
            `    return constructor.call(this, marker, javaObject);\n` +
            `}\n`
        )(c) as Function;
    }
    imports.teavmJso = {
        stringBuiltinsSupported: () => stringBuiltins,
        isUndefined: (o: unknown) => typeof o === "undefined",
        emptyArray: () => [],
        appendToArray: (array: unknown[], e: unknown) => array.push(e),
        unwrapBoolean: (value: unknown) => value ? 1 : 0,
        wrapBoolean: (value: unknown) => !!value,
        getProperty,
        setProperty,
        setPropertyPure: setProperty,
        global(name: string) {
            try {
                return getGlobalName(name);
            } catch (e) {
                rethrowJsAsJava(e);
            }
        },
        createClass(name: string | null, parent: Function | null, constructor: Function | null) {
            name = sanitizeName(name ?? "JavaObject");
            let action: (this: any, javaObject: unknown) => void;
            const fn: any = renameConstructor(name, function(this: any, marker: unknown, javaObject: unknown) {
                if (marker === wrapperCallMarkerSymbol) {
                    action.call(this, javaObject);
                } else if (constructor === null) {
                    throw new Error("This class can't be instantiated directly");
                } else {
                    try {
                        return constructor.apply(null, arguments as unknown as unknown[]);
                    } catch (e) {
                        rethrowJavaAsJs(e);
                    }
                }
            });
            if (parent === null) {
                action = function(this: any, javaObject: unknown) {
                    this[javaObjectSymbol] = javaObject;
                    this[functionsSymbol] = null;
                };
            } else {
                action = function(this: any, javaObject: unknown) {
                    parent.call(this, javaObject);
                };
            }
            fn.prototype = Object.create(parent ? (parent as any).prototype : Object.prototype);
            fn.prototype.constructor = fn;
            const boundFn: any = renameConstructor(name, function(this: unknown, javaObject: unknown) {
                return fn.call(this, wrapperCallMarkerSymbol, javaObject);
            });
            boundFn[wrapperCallMarkerSymbol] = fn;
            boundFn.prototype = fn.prototype;
            return boundFn;
        },
        exportClass(cls: any) {
            return cls[wrapperCallMarkerSymbol];
        },
        defineMethod(cls: any, name: string, fn: Function, vararg: boolean) {
            const params: string[] = [];
            const paramsForString: string[] = [];
            for (let i = 1; i < fn.length; ++i) {
                const p = "p" + i;
                params.push(p);
                paramsForString.push(p);
            }
            if (vararg) {
                const last = paramsForString.length - 1;
                paramsForString[last] = "..." + paramsForString[last];
            }
            const paramsAsString = paramsForString.join(", ");
            cls.prototype[name] = new Function("rethrowJavaAsJs", "fn",
                `return function(${paramsAsString}) {\n` +
                `    try {\n` +
                `        return fn(${['this', ...params].join(", ")});\n` +
                `    } catch (e) {\n` +
                `        rethrowJavaAsJs(e);\n` +
                `    }\n` +
                `};`
            )(rethrowJavaAsJs, fn);
        },
        defineStaticMethod(cls: any, name: string, fn: Function, vararg: boolean) {
            cls[name] = defineFunction(fn, vararg);
        },
        defineFunction,
        defineProperty(cls: any, name: string, getFn: Function, setFn: Function | null) {
            const descriptor: PropertyDescriptor = {
                get() {
                    try {
                        return getFn(this);
                    } catch (e) {
                        rethrowJavaAsJs(e);
                    }
                }
            };
            if (setFn !== null) {
                descriptor.set = function(value: unknown) {
                    try {
                        setFn(this, value);
                    } catch (e) {
                        rethrowJavaAsJs(e);
                    }
                };
            }
            Object.defineProperty(cls.prototype, name, descriptor);
        },
        defineStaticProperty(cls: any, name: string, getFn: Function, setFn: Function | null) {
            const descriptor: PropertyDescriptor = {
                get() {
                    try {
                        return getFn();
                    } catch (e) {
                        rethrowJavaAsJs(e);
                    }
                }
            };
            if (setFn !== null) {
                descriptor.set = function(value: unknown) {
                    try {
                        setFn(value);
                    } catch (e) {
                        rethrowJavaAsJs(e);
                    }
                };
            }
            Object.defineProperty(cls, name, descriptor);
        },
        javaObjectToJS(instance: object | null, cls: any) {
            if (instance === null) {
                return null;
            }
            const existing = jsWrappers.get(instance);
            if (typeof existing != "undefined") {
                const result = existing.deref();
                if (typeof result !== "undefined") {
                    return result;
                }
            }
            const obj = new cls(instance);
            jsWrappers.set(instance, new WeakRef(obj));
            return obj;
        },
        unwrapJavaObject(instance: any) {
            return instance[javaObjectSymbol];
        },
        asFunction(instance: any, propertyName: string) {
            if (instance === null || instance === undefined) {
                return null;
            }
            let functions = instance[functionsSymbol];
            if (functions === null) {
                functions = Object.create(null);
                instance[functionsSymbol] = functions;
            }
            let result = functions[propertyName];
            if (typeof result !== 'function') {
                result = function() {
                    return instance[propertyName].apply(instance, arguments);
                };
                result[functionOriginSymbol] = instance;
                functions[propertyName] = result;
            }
            return result;
        },
        functionAsObject(fn: any, property: string) {
            if (fn === null || fn === void 0) {
                return null;
            }
            const origin = fn[functionOriginSymbol];
            if (typeof origin !== 'undefined') {
                const functions = origin[functionsSymbol] as Record<string, Function>;
                if (functions !== void 0 && functions[property] === fn) {
                    return origin;
                }
            }
            return {
                [property]: function(...args: unknown[]) {
                    try {
                        return fn(...args);
                    } catch (e) {
                        rethrowJavaAsJs(e);
                    }
                }
            };
        },
        wrapObject(obj: unknown) {
            if (obj === null) {
                return null;
            }
            if (typeof obj === "object" || typeof obj === "function" || typeof obj === "symbol") {
                const asRecord = obj as Record<symbol, any>;
                let result = asRecord[javaObjectSymbol];
                if (typeof result === "object") {
                    return result;
                }
                result = javaWrappers.get(obj as object)?.deref();
                if (result !== void 0) {
                    return result;
                }
                result = (context.exports["teavm.jso.createWrapper"] as Function)(obj);
                javaWrappers.set(obj as object, new WeakRef(result as object));
                return result;
            } else {
                let result = primitiveWrappers.get(obj)?.deref();
                if (result !== void 0) {
                    return result;
                }
                result = (context.exports["teavm.jso.createWrapper"] as Function)(obj);
                primitiveWrappers.set(obj, new WeakRef(result!));
                primitiveFinalization.register(result!, obj);
                return result;
            }
        },
        isPrimitive: (value: unknown, type: string) => typeof value === type || value === null,
        instanceOf: (value: unknown, type: Function) => value instanceof type,
        instanceOfOrNull: (value: unknown, type: Function) => value === null || value instanceof type,
        sameRef: (a: unknown, b: unknown) => a === b,
        hashCode: (obj: unknown) => {
            if (typeof obj === "object" || typeof obj === "function" || typeof obj === "symbol") {
                let code = hashCodes.get(obj as object);
                if (typeof code === "number") {
                    return code;
                }
                code = nextHashCode();
                hashCodes.set(obj as object, code);
                return code;
            } else if (typeof obj === "number") {
                return obj | 0;
            } else if (typeof obj === "bigint") {
                return BigInt.asIntN(32, obj);
            } else if (typeof obj === "boolean") {
                return obj ? 1 : 0;
            } else {
                return 0;
            }
        },
        apply: (instance: object | null, method: string, args: unknown[]) => {
            try {
                if (instance === null) {
                    const fn = getGlobalName(method) as Function;
                    return fn(...args);
                } else {
                    return (instance as Record<string, Function>)[method](...args);
                }
            } catch (e) {
                rethrowJsAsJava(e);
            }
        },
        concatArray: (a: unknown[], b: unknown[]) => [...a, ...b],
        getJavaException: (e: Record<symbol, unknown>) => e[javaExceptionSymbol],
        getJSException: (e: unknown) => {
            const getJsException = context.exports["teavm.getJsException"] as Function;
            return getJsException(e);
        },
        jsExports: () => context.userExports
    };
    for (const name of ["wrapByte", "wrapShort", "wrapChar", "wrapInt", "wrapLong", "wrapFloat", "wrapDouble",
        "unwrapByte", "unwrapShort", "unwrapChar", "unwrapInt", "unwrapLong", "unwrapFloat", "unwrapDouble"]) {
        imports.teavmJso[name] = identity;
    }
    function wrapCallFromJavaToJs(call: () => unknown): unknown {
        try {
            return call();
        } catch (e) {
            rethrowJsAsJava(e);
        }
    }
    const argumentList: string[] = [];
    for (let i = 0; i < 32; ++i) {
        const args = argumentList.length === 0 ? "" : argumentList.join(", ");
        const argsAndBody = [...argumentList, "body"].join(", ");
        imports.teavmJso["createFunction" + i] = new Function(
            "wrapCallFromJavaToJs", ...argumentList, "body",
            `return new Function('wrapCallFromJavaToJs', ${argsAndBody}).bind(this, wrapCallFromJavaToJs);`
        ).bind(null, wrapCallFromJavaToJs);
        imports.teavmJso["bindFunction" + i] = (f: Function, ...bindArgs: unknown[]) =>
            f.bind(null, ...bindArgs);
        imports.teavmJso["callFunction" + i] = new Function(
            "rethrowJsAsJava", "fn", ...argumentList,
            `try {\n` +
            `    return fn(${args});\n` +
            `} catch (e) {\n` +
            `    rethrowJsAsJava(e);\n` +
            `}`
        ).bind(null, rethrowJsAsJava);
        imports.teavmJso["callMethod" + i] = new Function(
            "rethrowJsAsJava", "getGlobalName", "instance", "method", ...argumentList,
            `try {\n` +
            `    return instance !== null\n` +
            `        ? instance[method](${args})\n` +
            `        : getGlobalName(method)(${args});\n` +
            `} catch (e) {\n` +
            `    rethrowJsAsJava(e);\n` +
            `}`
        ).bind(null, rethrowJsAsJava, getGlobalName);
        imports.teavmJso["construct" + i] = new Function(
            "rethrowJsAsJava", "constructor", ...argumentList,
            `try {\n` +
            `    return new constructor(${args});\n` +
            `} catch (e) {\n` +
            `    rethrowJsAsJava(e);\n` +
            `}`
        ).bind(null, rethrowJsAsJava);
        imports.teavmJso["arrayOf" + i] = new Function(
            ...argumentList, "return [" + args + "]"
        );

        argumentList.push("p" + (i + 1));
    }
}

export function wrapImport(importObj: Record<string, unknown>): Record<string, unknown> {
    return new Proxy(importObj, {
        get(target, prop) {
            const result = target[prop as string];
            return new WebAssembly.Global({ value: "externref", mutable: false }, result);
        }
    });
}

async function wrapImports(wasmModule: WebAssembly.Module, imports: Record<string, unknown>): Promise<void> {
    const promises: Promise<void>[] = [];
    const propertiesToAdd: Record<string, string[]> = {};
    for (const { module, name } of extractImports(wasmModule)) {
        if (module in imports) {
            continue;
        }
        let names = propertiesToAdd[module];
        if (names === void 0) {
            const namesByModule: string[] = [];
            names = namesByModule;
            propertiesToAdd[module] = names;
            promises.push((async () => {
                const moduleInstance = await import(module) as Record<string, unknown>;
                const importsByModule: Record<string, unknown> = {};
                for (const n of namesByModule) {
                    const importedName = n === "__self__" ? moduleInstance : moduleInstance[n];
                    importsByModule[n] = new WebAssembly.Global(
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

interface ImportDescriptor {
    module: string;
    name: string;
}

function extractImports(module: WebAssembly.Module): ImportDescriptor[] {
    const sections = WebAssembly.Module.customSections(module, "teavm.imports");
    if (sections.length !== 1) {
        return WebAssembly.Module.imports(module)
            .filter(importDecl => importDecl.kind === "global");
    }
    return JSON.parse(new TextDecoder().decode(sections[0]));
}

export async function load(src: string | BufferSource, options?: LoadOptions): Promise<TeaVMInstance> {
    if (!options) {
        options = {};
    }

    const isNodeJs = options.nodejs || typeof process !== "undefined";
    const emscriptenModulePaths = options.emscriptenModules ?? {};
    const deobfuscatorOptions = options.stackDeobfuscator ?? {};
    const debugInfoLocation = deobfuscatorOptions.infoLocation ?? "auto";
    const compilationPromise = compileModule(src, isNodeJs);
    const [deobfuscatorFactory, module, debugInfo, emscriptenModules] = await Promise.all([
        deobfuscatorOptions.enabled ? getDeobfuscator(src, deobfuscatorOptions, isNodeJs) : Promise.resolve(null),
        compilationPromise,
        fetchExternalDebugInfo(src, debugInfoLocation, deobfuscatorOptions, isNodeJs),
        loadEmscriptenModules(emscriptenModulePaths, isNodeJs)
    ]);

    const importObj: WebAssembly.Imports = {};
    const userExports: Record<string, unknown> = {};
    const defaultsResult = defaults(importObj, userExports, await hasStringBuiltins());
    const allocator = await linkImports(importObj, options, module, emscriptenModules);
    if (typeof options.installImports !== "undefined") {
        options.installImports(importObj);
    }
    if (!options.noAutoImports) {
        await wrapImports(module, importObj);
    }
    const instance = await WebAssembly.instantiate(module, importObj as WebAssembly.Imports);
    allocator.malloc = instance.exports["teavm.malloc"] as (size: number) => number;
    allocator.free = instance.exports["teavm.free"] as (ptr: number) => void;
    allocator.realloc = instance.exports["teavm.realloc"] as (ptr: number, newSize: number) => number;

    defaultsResult.supplyExports(instance.exports);
    if (deobfuscatorFactory) {
        const moduleToPass = debugInfoLocation === "auto" || debugInfoLocation === "embedded" ? module : null;
        const deobfuscator = createDeobfuscator(moduleToPass, debugInfo, deobfuscatorFactory);
        if (deobfuscator !== null) {
            defaultsResult.supplyStackDeobfuscator(deobfuscator);
        }
    }
    const teavm: TeaVMInstance = {
        exports: userExports,
        instance,
        module
    };
    for (const key in instance.exports) {
        const exportObj = instance.exports[key];
        if (exportObj instanceof WebAssembly.Global) {
            Object.defineProperty(userExports, key, {
                get: () => (exportObj as WebAssembly.Global).value
            });
        }
    }
    return teavm;
}

async function compileModule(src: string | BufferSource, isNodeJs: boolean): Promise<WebAssembly.Module> {
    if (typeof src !== "string") {
        return await WebAssembly.compile(src, { builtins: ["js-string"] });
    }
    const [response, close] = await openPath(src, isNodeJs);
    const result = await WebAssembly.compileStreaming(response, { builtins: ["js-string"] });
    close();
    return result;
}

let hasStringBuiltinsPromise: Promise<boolean> | null = null;
function hasStringBuiltins(): Promise<boolean> {
    if (hasStringBuiltinsPromise === null) {
        hasStringBuiltinsPromise = (async () => {
            // A more sophisticated way to detect string builtins support due to bug in Safari 26.2
            const bytes = new Int8Array([0, 97, 115, 109, 1, 0, 0, 0, 1, 7, 1, 96, 1, 127, 1, 100, 111, 2, 31, 1, 14,
                119, 97, 115, 109, 58, 106, 115, 45, 115, 116, 114, 105, 110, 103, 12, 102, 114, 111, 109, 67, 104,
                97, 114, 67, 111, 100, 101, 0, 0, 3, 1, 0, 5, 4, 1, 1, 0, 0, 7, 10, 1, 6, 109, 101, 109, 111, 114,
                121, 2, 0, 10, -127, -128, -128, 0, 0]);
            try {
                const response = new Response(bytes, {
                    headers: { "Content-Type": "application/wasm" }
                });
                const module = await WebAssembly.compileStreaming(response, { builtins: ["js-string"] });
                await WebAssembly.instantiate(module, {});
                return true;
            } catch (e) {
                return false;
            }
        })();
    }
    return hasStringBuiltinsPromise;
}

async function getDeobfuscator(
    path: string | BufferSource,
    options: DeobfuscatorOptions,
    isNodeJs: boolean
): Promise<WebAssembly.Instance | null> {
    if (typeof path !== "string" && !options.path) {
        return null;
    }
    try {
        const importObj: WebAssembly.Imports = {};
        const module = await compileModule(options.path ?? `${path as string}-deobfuscator.wasm`, isNodeJs);
        const defaultsResult = defaults(importObj, {}, await hasStringBuiltins());
        await linkImports(importObj, {}, module, []);
        const instance = await WebAssembly.instantiate(module, importObj);
        defaultsResult.supplyExports(instance.exports);
        return instance;
    } catch (e) {
        console.warn("Could not load deobfuscator", e);
        return null;
    }
}

async function openPath(src: string, isNodeJs: boolean): Promise<[Response, () => void]> {
    if (!isNodeJs) {
        const response = await fetch(src);
        return [response, () => {}];
    } else {
        const fs = await importNodeFs();
        const fileHandle = await fs.open(src, "r");
        const stream = await fileHandle.readableWebStream();
        const response = new Response(stream, {
            headers: { 'Content-Type': 'application/wasm' },
        });
        return [response, () => fileHandle.close()];
    }
}

let nodeFsImportObject: Promise<any> | undefined;
async function importNodeFs(): Promise<any> {
    if (!nodeFsImportObject) {
        nodeFsImportObject = import('node:fs/promises');
    }
    return await nodeFsImportObject;
}

function createDeobfuscator(
    module: WebAssembly.Module | null,
    externalData: Int8Array | null,
    deobfuscatorFactory: WebAssembly.Instance
): ((addresses: number[]) => StackFrame[]) | null {
    let deobfuscator: { deobfuscate: (addresses: number[]) => StackFrame[] } | null = null;
    let deobfuscatorInitialized = false;
    function ensureDeobfuscator() {
        if (!deobfuscatorInitialized) {
            deobfuscatorInitialized = true;
            if (externalData !== null) {
                try {
                    deobfuscator = (deobfuscatorFactory.exports["createFromExternalFile"] as WebAssembly.Global)
                        .value(externalData) as typeof deobfuscator;
                } catch (e) {
                    console.warn("Could not load create deobfuscator", e);
                }
            }
            if (deobfuscator == null && module !== null) {
                try {
                    deobfuscator = (deobfuscatorFactory.exports["createForModule"] as WebAssembly.Global)
                        .value(module) as typeof deobfuscator;
                } catch (e) {
                    console.warn("Could not create deobfuscator from module data", e);
                }
            }
        }
    }
    return addresses => {
        ensureDeobfuscator();
        return deobfuscator !== null ? deobfuscator.deobfuscate(addresses) : [];
    };
}

async function fetchExternalDebugInfo(
    path: string | BufferSource,
    debugInfoLocation: string,
    options: DeobfuscatorOptions,
    isNodeJs: boolean
): Promise<Int8Array | null> {
    if (!options.enabled) {
        return null;
    }
    if (typeof path !== "string" && !options.externalInfoPath) {
        return null;
    }
    if (debugInfoLocation !== "auto" && debugInfoLocation !== "external") {
        return null;
    }
    if (typeof options.externalInfoPath === "object" && options.externalInfoPath instanceof Int8Array) {
        return options.externalInfoPath;
    }
    const location = (options.externalInfoPath ?? path + ".teadbg") as string;
    let buffer: ArrayBuffer;
    if (!isNodeJs) {
        const response = await fetch(location);
        if (!response.ok) {
            return null;
        }
        buffer = await response.arrayBuffer();
    } else {
        const fs = await importNodeFs();
        buffer = (await fs.readFile(location)).buffer as ArrayBuffer;
    }
    return new Int8Array(buffer);
}

// emscripten parameters for TeaVM interop:
// emcc \
//   -s MODULARIZE \
//   -s RELOCATABLE \
//   -s EXPORT_ES6=1 \
//   -s ALLOW_MEMORY_GROWTH=1 \
//   -s EXPORTED_FUNCTIONS=<exported functions>
//   -s STACK_OVERFLOW_CHECK=0
//   -s MALLOC=none
//   --no-entry
//
// and add _malloc,_free and _realloc to exported functions
async function loadEmscriptenModules(
    emscriptenModulePaths: Record<string, EmscriptenModulePath>,
    isNodeJs: boolean
): Promise<EmscriptenModule[]> {
    const keysValues = Object.entries(emscriptenModulePaths);
    return Promise.all(keysValues.map(async ([key, { pathToJs, pathToWasm }]) => {
        const [response, close] = await openPath(pathToWasm, isNodeJs);
        const [jsLoader, wasmModule] = await Promise.all([
            import(pathToJs) as Promise<{ default: EmscriptenModule["jsLoader"] }>,
            WebAssembly.compileStreaming(response)
        ]);
        const result: EmscriptenModule = {
            name: key,
            jsLoader: jsLoader.default,
            wasmModule
        };
        close();
        return result;
    }));
}
