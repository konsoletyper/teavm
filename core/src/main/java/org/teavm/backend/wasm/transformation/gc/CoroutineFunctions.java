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
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.model.MethodReference;
import org.teavm.runtime.Fiber;

class CoroutineFunctions {
    private BaseWasmFunctionRepository functions;
    private WasmGCClassInfoProvider classInfoProvider;

    private WasmFunction pushIntCache;
    private WasmFunction pushLongCache;
    private WasmFunction pushFloatCache;
    private WasmFunction pushDoubleCache;
    private WasmFunction pushObjectCache;

    private WasmFunction popIntCache;
    private WasmFunction popLongCache;
    private WasmFunction popFloatCache;
    private WasmFunction popDoubleCache;
    private WasmFunction popObjectCache;

    private WasmFunction isResumingCache;
    private WasmFunction isSuspendingCache;

    private WasmStructure objectStructureCache;

    CoroutineFunctions(BaseWasmFunctionRepository functions, WasmGCClassInfoProvider classInfoProvider) {
        this.functions = functions;
        this.classInfoProvider = classInfoProvider;
    }

    WasmFunction pushInt() {
        if (pushIntCache == null) {
            pushIntCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "push", int.class,
                    void.class));
        }
        return pushIntCache;
    }

    WasmFunction pushLong() {
        if (pushLongCache == null) {
            pushLongCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "push", long.class,
                    void.class));
        }
        return pushLongCache;
    }

    WasmFunction pushFloat() {
        if (pushFloatCache == null) {
            pushFloatCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "push", float.class,
                    void.class));
        }
        return pushFloatCache;
    }

    WasmFunction pushDouble() {
        if (pushDoubleCache == null) {
            pushDoubleCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "push", double.class,
                    void.class));
        }
        return pushFloatCache;
    }

    WasmFunction pushObject() {
        if (pushObjectCache == null) {
            pushObjectCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "push", double.class,
                    void.class));
        }
        return pushObjectCache;
    }

    WasmFunction popInt() {
        if (popIntCache == null) {
            popIntCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "popInt", int.class));
        }
        return pushIntCache;
    }

    WasmFunction popLong() {
        if (popLongCache == null) {
            popLongCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "popLong", long.class));
        }
        return pushIntCache;
    }

    WasmFunction popFloat() {
        if (popFloatCache == null) {
            popFloatCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "popFloat", float.class));
        }
        return pushIntCache;
    }

    WasmFunction popDouble() {
        if (popDoubleCache == null) {
            popDoubleCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "popDouble", double.class));
        }
        return popDoubleCache;
    }

    WasmFunction popObject() {
        if (popObjectCache == null) {
            popObjectCache = functions.forInstanceMethod(new MethodReference(Fiber.class, "popObject", Object.class));
        }
        return popObjectCache;
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

    WasmStructure objectStructure() {
        if (objectStructureCache == null) {
            objectStructureCache = classInfoProvider.getClassInfo("java.lang.Object").getStructure();
        }
        return objectStructureCache;
    }
}
