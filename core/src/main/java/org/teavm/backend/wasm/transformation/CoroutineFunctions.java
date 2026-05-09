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
package org.teavm.backend.wasm.transformation;

import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmCall;
import org.teavm.backend.wasm.model.instruction.WasmCast;
import org.teavm.backend.wasm.model.instruction.WasmExternConversion;
import org.teavm.backend.wasm.model.instruction.WasmExternConversionType;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;
import org.teavm.runtime.Fiber;

class CoroutineFunctions {
    private BaseWasmFunctionRepository functions;

    private WasmFunction pushIntCache;
    private WasmFunction pushLongCache;
    private WasmFunction pushFloatCache;
    private WasmFunction pushDoubleCache;
    private WasmFunction pushObjectCache;
    private WasmFunction pushFunctionCache;
    private WasmFunction pushExceptionCache;

    private WasmFunction popIntCache;
    private WasmFunction popLongCache;
    private WasmFunction popFloatCache;
    private WasmFunction popDoubleCache;
    private WasmFunction popObjectCache;
    private WasmFunction popFunctionCache;
    private WasmFunction popExceptionCache;

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

    WasmFunction pushPlatformException() {
        if (pushExceptionCache == null) {
            pushExceptionCache = functions.forStaticMethod(new MethodReference(Fiber.class, "reversePush",
                    Fiber.PlatformException.class, Fiber.class, void.class));
        }
        return pushExceptionCache;
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

    WasmFunction popPlatformException() {
        if (popExceptionCache == null) {
            popExceptionCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "popPlatformException",
                    Fiber.PlatformException.class));
        }
        return popExceptionCache;
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

    void restoreValue(WasmType type, WasmInstructionList target, TextLocation location) {
        if (type instanceof WasmType.Number) {
            switch (((WasmType.Number) type).number) {
                case INT32:
                    target.add(new WasmCall(popInt()), location);
                    break;
                case INT64:
                    target.add(new WasmCall(popLong()), location);
                    break;
                case FLOAT32:
                    target.add(new WasmCall(popFloat()), location);
                    break;
                case FLOAT64:
                    target.add(new WasmCall(popDouble()), location);
                    break;
            }
        } else if (type instanceof WasmType.Reference) {
            var refType = (WasmType.Reference) type;
            if (type instanceof WasmType.SpecialReference) {
                switch (((WasmType.SpecialReference) type).kind) {
                    case EXTERN:
                        target.add(new WasmCall(popPlatformObject()), location);
                        target.add(new WasmExternConversion(WasmExternConversionType.ANY_TO_EXTERN),
                                location);
                        return;
                    case FUNC:
                        target.add(new WasmCall(popPlatformFunction()), location);
                        break;
                    case EXN:
                        target.add(new WasmCall(popPlatformException()), location);
                        break;
                    default:
                        target.add(new WasmCall(popPlatformObject()), location);
                        break;
                }
            } else if (type instanceof WasmType.CompositeReference) {
                var composite = ((WasmType.CompositeReference) type).composite;
                if (composite instanceof WasmFunctionType) {
                    target.add(new WasmCall(popPlatformFunction()), location);
                } else {
                    target.add(new WasmCall(popPlatformObject()), location);
                }
            }
            target.add(new WasmCast(refType), location);
        } else {
            throw new IllegalArgumentException();
        }
    }

    void saveValue(WasmType type, WasmInstructionList list, WasmLocal fiberLocal, TextLocation location) {
        if (type instanceof WasmType.Number) {
            list.add(new WasmGetLocal(fiberLocal), location);
            switch (((WasmType.Number) type).number) {
                case INT32:
                    list.add(new WasmCall(pushInt()), location);
                    break;
                case INT64:
                    list.add(new WasmCall(pushLong()), location);
                    break;
                case FLOAT32:
                    list.add(new WasmCall(pushFloat()), location);
                    break;
                case FLOAT64:
                    list.add(new WasmCall(pushDouble()), location);
                    break;
            }
        } else {
            if (type instanceof WasmType.SpecialReference) {
                switch (((WasmType.SpecialReference) type).kind) {
                    case EXTERN:
                        list.add(new WasmExternConversion(WasmExternConversionType.EXTERN_TO_ANY),
                                location);
                        break;
                    case FUNC:
                        list.add(new WasmGetLocal(fiberLocal), location);
                        list.add(new WasmCall(pushPlatformFunction()), location);
                        return;
                    case EXN:
                        list.add(new WasmGetLocal(fiberLocal), location);
                        list.add(new WasmCall(pushPlatformException()), location);
                        return;
                }
            } else if (type instanceof WasmType.CompositeReference) {
                var composite = ((WasmType.CompositeReference) type).composite;
                if (composite instanceof WasmFunctionType) {
                    list.add(new WasmGetLocal(fiberLocal), location);
                    list.add(new WasmCall(pushPlatformFunction()), location);
                    return;
                }
            }
            list.add(new WasmGetLocal(fiberLocal), location);
            list.add(new WasmCall(pushPlatformObject()), location);
        }
    }
}
