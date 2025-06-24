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
package org.teavm.backend.wasm.transformation.gc;

import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmExternConversion;
import org.teavm.backend.wasm.model.expression.WasmExternConversionType;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmPush;
import org.teavm.model.MethodReference;
import org.teavm.runtime.Fiber;

class CoroutineFunctions {
    private BaseWasmFunctionRepository functions;

    private WasmFunction pushIntCache;
    private WasmFunction pushLongCache;
    private WasmFunction pushFloatCache;
    private WasmFunction pushDoubleCache;
    private WasmFunction pushObjectCache;
    private WasmFunction pushFunctionCache;

    private WasmFunction popIntCache;
    private WasmFunction popLongCache;
    private WasmFunction popFloatCache;
    private WasmFunction popDoubleCache;
    private WasmFunction popObjectCache;
    private WasmFunction popFunctionCache;

    private WasmFunction isResumingCache;
    private WasmFunction isSuspendingCache;

    private WasmFunction currentFiberCache;

    CoroutineFunctions(BaseWasmFunctionRepository functions) {
        this.functions = functions;
    }

    WasmFunction pushInt() {
        if (pushIntCache == null) {
            pushIntCache = functions.forStaticMethod(new MethodReference(Fiber.class, "reversePush", int.class,
                    Fiber.class, void.class));
        }
        return pushIntCache;
    }

    WasmFunction pushLong() {
        if (pushLongCache == null) {
            pushLongCache = functions.forStaticMethod(new MethodReference(Fiber.class, "reversePush", long.class,
                    Fiber.class, void.class));
        }
        return pushLongCache;
    }

    WasmFunction pushFloat() {
        if (pushFloatCache == null) {
            pushFloatCache = functions.forStaticMethod(new MethodReference(Fiber.class, "reversePush", float.class,
                    Fiber.class, void.class));
        }
        return pushFloatCache;
    }

    WasmFunction pushDouble() {
        if (pushDoubleCache == null) {
            pushDoubleCache = functions.forStaticMethod(new MethodReference(Fiber.class, "reversePush", double.class,
                    Fiber.class, void.class));
        }
        return pushDoubleCache;
    }

    WasmFunction pushPlatformObject() {
        if (pushObjectCache == null) {
            pushObjectCache = functions.forStaticMethod(new MethodReference(Fiber.class, "reversePush",
                    Fiber.PlatformObject.class, Fiber.class, void.class));
        }
        return pushObjectCache;
    }

    WasmFunction pushPlatformFunction() {
        if (pushFunctionCache == null) {
            pushFunctionCache = functions.forStaticMethod(new MethodReference(Fiber.class, "reversePush",
                    Fiber.PlatformFunction.class, Fiber.class, void.class));
        }
        return pushFunctionCache;
    }

    WasmFunction popInt() {
        if (popIntCache == null) {
            popIntCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "popInt", int.class));
        }
        return popIntCache;
    }

    WasmFunction popLong() {
        if (popLongCache == null) {
            popLongCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "popLong", long.class));
        }
        return popLongCache;
    }

    WasmFunction popFloat() {
        if (popFloatCache == null) {
            popFloatCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "popFloat", float.class));
        }
        return popFloatCache;
    }

    WasmFunction popDouble() {
        if (popDoubleCache == null) {
            popDoubleCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "popDouble", double.class));
        }
        return popDoubleCache;
    }

    WasmFunction popPlatformObject() {
        if (popObjectCache == null) {
            popObjectCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "popPlatformObject",
                    Fiber.PlatformObject.class));
        }
        return popObjectCache;
    }

    WasmFunction popPlatformFunction() {
        if (popFunctionCache == null) {
            popFunctionCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "popPlatformFunction",
                    Fiber.PlatformFunction.class));
        }
        return popFunctionCache;
    }

    WasmFunction isResuming() {
        if (isResumingCache == null) {
            isResumingCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "isResuming",
                    boolean.class));
        }
        return isResumingCache;
    }

    WasmFunction isSuspending() {
        if (isSuspendingCache == null) {
            isSuspendingCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "isSuspending",
                    boolean.class));
        }
        return isSuspendingCache;
    }

    WasmFunction currentFiber() {
        if (currentFiberCache == null) {
            currentFiberCache = functions.forStaticMethod(new MethodReference(Fiber.class, "current", Fiber.class));
        }
        return currentFiberCache;
    }

    WasmExpression restoreValue(WasmType type, WasmLocal fiberLocal) {
        if (type instanceof WasmType.Number) {
            switch (((WasmType.Number) type).number) {
                case INT32:
                    return new WasmCall(popInt(), new WasmGetLocal(fiberLocal));
                case INT64:
                    return new WasmCall(popLong(), new WasmGetLocal(fiberLocal));
                case FLOAT32:
                    return new WasmPush(new WasmCall(popFloat(), new WasmGetLocal(fiberLocal)));
                case FLOAT64:
                    new WasmPush(new WasmCall(popDouble(), new WasmGetLocal(fiberLocal)));
            }
            throw new IllegalArgumentException();
        } else if (type instanceof WasmType.Reference) {
            var refType = (WasmType.Reference) type;
            WasmExpression obj = new WasmCall(popPlatformObject(), new WasmGetLocal(fiberLocal));
            if (type instanceof WasmType.SpecialReference) {
                switch (((WasmType.SpecialReference) type).kind) {
                    case EXTERN:
                        obj = new WasmExternConversion(WasmExternConversionType.ANY_TO_EXTERN, obj);
                        break;
                    case FUNC:
                        obj = new WasmCall(popPlatformFunction(), new WasmGetLocal(fiberLocal));
                        break;
                }
            } else if (type instanceof WasmType.CompositeReference) {
                var composite = ((WasmType.CompositeReference) type).composite;
                if (composite instanceof WasmFunctionType) {
                    obj = new WasmCall(popPlatformFunction(), new WasmGetLocal(fiberLocal));
                }
            }
            return new WasmCast(obj, refType);
        } else {
            throw new IllegalArgumentException();
        }
    }

    WasmExpression saveValue(WasmType type, WasmLocal fiberLocal, WasmExpression value) {
        if (type instanceof WasmType.Number) {
            switch (((WasmType.Number) type).number) {
                case INT32:
                    return new WasmCall(pushInt(), value, new WasmGetLocal(fiberLocal));
                case INT64:
                    return new WasmCall(pushLong(), value, new WasmGetLocal(fiberLocal));
                case FLOAT32:
                    return new WasmCall(pushFloat(), value, new WasmGetLocal(fiberLocal));
                case FLOAT64:
                    return new WasmCall(pushDouble(), value, new WasmGetLocal(fiberLocal));
            }
            throw new IllegalArgumentException();
        } else {
            if (type instanceof WasmType.SpecialReference) {
                switch (((WasmType.SpecialReference) type).kind) {
                    case EXTERN:
                        value = new WasmExternConversion(WasmExternConversionType.EXTERN_TO_ANY, value);
                        break;
                    case FUNC:
                        return new WasmCall(pushPlatformFunction(), value, new WasmGetLocal(fiberLocal));
                }
            } else if (type instanceof WasmType.CompositeReference) {
                var composite = ((WasmType.CompositeReference) type).composite;
                if (composite instanceof WasmFunctionType) {
                    return new WasmCall(pushPlatformFunction(), value, new WasmGetLocal(fiberLocal));
                }
            }
            return new WasmCall(pushPlatformObject(), value, new WasmGetLocal(fiberLocal));
        }
    }
}
