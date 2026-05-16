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
package org.teavm.backend.wasm.intrinsics;

import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.intrinsics.reflection.AnnotationConstructorIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.AnnotationDataIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.AnnotationInfoIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.AnnotationValueArrayIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.ClassInfoIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.ClassReflectionInfoIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.DerivedClassInfoIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.FieldInfoIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.FieldReflectionInfoIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.GenericArrayInfoIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.GenericTypeInfoIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.MethodInfoIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.MethodReflectionInfoIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.ParameterInfoIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.ParameterizedTypeInfoIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.ProxyIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.ProxyIntrinsicContext;
import org.teavm.backend.wasm.intrinsics.reflection.ProxyMethodIntrinsicProvider;
import org.teavm.backend.wasm.intrinsics.reflection.RawTypeInfoIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.ReflectionMetadataGenerator;
import org.teavm.backend.wasm.intrinsics.reflection.StringInfoIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.TypeVariableInfoIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.TypeVariableReferenceIntrinsic;
import org.teavm.backend.wasm.intrinsics.reflection.WildcardTypeInfoIntrinsic;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.backend.wasm.runtime.StringInternPool;
import org.teavm.backend.wasm.runtime.WasmGCSupport;
import org.teavm.interop.Address;
import org.teavm.interop.Structure;
import org.teavm.model.MethodReference;
import org.teavm.reflection.AnnotationGenerationHelper;
import org.teavm.reflection.ReflectionDependencyListener;
import org.teavm.runtime.EventQueue;
import org.teavm.runtime.StringInfo;
import org.teavm.runtime.heap.Heap;
import org.teavm.runtime.reflect.AnnotationConstructor;
import org.teavm.runtime.reflect.AnnotationInfo;
import org.teavm.runtime.reflect.AnnotationValueArray;
import org.teavm.runtime.reflect.ClassInfo;
import org.teavm.runtime.reflect.ClassReflectionInfo;
import org.teavm.runtime.reflect.DerivedClassInfo;
import org.teavm.runtime.reflect.FieldInfo;
import org.teavm.runtime.reflect.FieldReflectionInfo;
import org.teavm.runtime.reflect.GenericArrayInfo;
import org.teavm.runtime.reflect.GenericTypeInfo;
import org.teavm.runtime.reflect.MethodInfo;
import org.teavm.runtime.reflect.MethodReflectionInfo;
import org.teavm.runtime.reflect.ParameterInfo;
import org.teavm.runtime.reflect.ParameterizedTypeInfo;
import org.teavm.runtime.reflect.RawTypeInfo;
import org.teavm.runtime.reflect.TypeVariableInfo;
import org.teavm.runtime.reflect.TypeVariableReference;
import org.teavm.runtime.reflect.WildcardTypeInfo;
import org.teavm.vm.intrinsic.IntrinsicRegistry;

public class WasmGCIntrinsics {
    private WasmGCIntrinsics() {
    }

    public static void apply(ReflectionDependencyListener reflection, WasmGCCodeGenContext ctx,
            IntrinsicRegistry<WasmGCInlineIntrinsic> inlineReg,
            IntrinsicRegistry<WasmGCBodyIntrinsic> bodyReg) {
        inlineReg.registerIntrinsic(WasmRuntime.class, new WasmRuntimeIntrinsic());
        inlineReg.registerIntrinsic(Object.class, new ObjectIntrinsic(ctx.classInfoProvider(), ctx.functionTypes()),
                "getClassInfo", "getMonitor", "setMonitor", "wasmGCIdentity", "setWasmGCIdentity",
                "cloneObject");
        fillSystem(inlineReg, ctx);
        inlineReg.registerIntrinsic(Heap.class, new HeapIntrinsic());
        inlineReg.registerIntrinsic(Address.class, new AddressIntrinsic(ctx.classInfoProvider(), ctx.functions()));
        inlineReg.registerIntrinsic(Structure.class, new StructureIntrinsic(ctx.classInfoProvider()));
        fillIntLong(inlineReg, ctx);
        inlineReg.registerIntrinsic(Float.class, new FloatIntrinsic(), "isNaN", "isFinite", "floatToRawIntBits",
                "intBitsToFloat");
        inlineReg.registerIntrinsic(Double.class, new DoubleIntrinsic(), "isNaN", "isFinite", "doubleToRawLongBits",
                "longBitsToDouble");
        inlineReg.registerIntrinsic(StringInternPool.class.getName() + "$Entry", new StringInternPoolIntrinsic(
                    ctx.classInfoProvider(), ctx.functionTypes(), ctx.typeMapper(), ctx.names(), ctx.module()));
        fillReflection(inlineReg, ctx, reflection);
        inlineReg.registerIntrinsic(String.class, new StringIntrinsic(ctx));

        bodyReg.registerIntrinsic(WasmGCSupport.class, new WasmGCStringPoolIntrinsic(ctx.module(), ctx.names()));
        bodyReg.registerIntrinsic(WeakReference.class, new WeakReferenceIntrinsic(ctx));
        bodyReg.registerIntrinsic(EventQueue.class, new EventQueueIntrinsic(ctx));
    }

    private static void fillIntLong(IntrinsicRegistry<WasmGCInlineIntrinsic> reg,
            WasmGCCodeGenContext ctx) {
        reg.registerIntrinsic(Integer.class, new IntNumIntrinsic(int.class, WasmIntType.INT32, ctx.functions()));
        reg.registerIntrinsic(Long.class, new IntNumIntrinsic(long.class, WasmIntType.INT64, ctx.functions()));
    }

    private static void fillReflection(IntrinsicRegistry<WasmGCInlineIntrinsic> reg,
            WasmGCCodeGenContext ctx, ReflectionDependencyListener reflection) {
        var classInfoProvider = ctx.classInfoProvider();
        var metadataGen = new ReflectionMetadataGenerator(ctx.names(), ctx.module(), ctx.functionTypes(),
                ctx.dependency(), reflection, ctx.classes(), classInfoProvider, ctx.functions(),
                ctx.typeMapper(), ctx.strings(), ctx.classInitInfo(), ctx.virtualTables(),
                ctx::isAsyncMethod);
        metadataGen.generate();
        ctx.initializerRegistry().register(fn -> fn.getBody().builder().call(metadataGen.initFunction()));
        reg.registerIntrinsic(ClassInfo.class, new ClassInfoIntrinsic(classInfoProvider, ctx.functionTypes(),
                metadataGen));
        reg.registerIntrinsic(ClassReflectionInfo.class, new ClassReflectionInfoIntrinsic(classInfoProvider));
        reg.registerIntrinsic(StringInfo.class, new StringInfoIntrinsic());
        reg.registerIntrinsic(AnnotationConstructor.class, new AnnotationConstructorIntrinsic(classInfoProvider));
        reg.registerIntrinsic(AnnotationInfo.class, new AnnotationInfoIntrinsic(classInfoProvider));
        reg.registerIntrinsic(DerivedClassInfo.class, new DerivedClassInfoIntrinsic(classInfoProvider));
        reg.registerIntrinsic(AnnotationValueArray.class, new AnnotationValueArrayIntrinsic(classInfoProvider));
        reg.registerIntrinsic(FieldInfo.class, new FieldInfoIntrinsic(classInfoProvider));
        reg.registerIntrinsic(FieldReflectionInfo.class, new FieldReflectionInfoIntrinsic(classInfoProvider));
        reg.registerIntrinsic(MethodInfo.class, new MethodInfoIntrinsic(classInfoProvider));
        reg.registerIntrinsic(MethodReflectionInfo.class, new MethodReflectionInfoIntrinsic(classInfoProvider));
        reg.registerIntrinsic(ParameterInfo.class, new ParameterInfoIntrinsic(classInfoProvider));
        reg.registerIntrinsic(TypeVariableInfo.class, new TypeVariableInfoIntrinsic(classInfoProvider));
        reg.registerIntrinsic(GenericTypeInfo.class, new GenericTypeInfoIntrinsic(classInfoProvider,
                ctx.functionTypes(), ctx.names(), ctx.module()));
        reg.registerIntrinsic(ParameterizedTypeInfo.class, new ParameterizedTypeInfoIntrinsic(classInfoProvider));
        reg.registerIntrinsic(TypeVariableReference.class, new TypeVariableReferenceIntrinsic(classInfoProvider));
        reg.registerIntrinsic(GenericArrayInfo.class, new GenericArrayInfoIntrinsic(classInfoProvider));
        reg.registerIntrinsic(WildcardTypeInfo.class, new WildcardTypeInfoIntrinsic(classInfoProvider));
        reg.registerIntrinsic(RawTypeInfo.class, new RawTypeInfoIntrinsic());
        reg.registerIntrinsic(m -> {
            if (!m.getClassName().endsWith(AnnotationGenerationHelper.ANNOTATION_DATA_SUFFIX)) {
                return null;
            }
            var className = m.getClassName();
            var nameLength = className.length() - AnnotationGenerationHelper.ANNOTATION_DATA_SUFFIX.length();
            var annotationClassName = className.substring(0, nameLength);
            return new AnnotationDataIntrinsic(classInfoProvider, ctx.classes(), annotationClassName);
        });
        
        var proxyIntrinsicContext = new ProxyIntrinsicContext(ctx);
        reg.registerIntrinsic(Proxy.class, new ProxyIntrinsic(reflection, ctx, proxyIntrinsicContext));
        reg.registerIntrinsic(new ProxyMethodIntrinsicProvider(reflection, proxyIntrinsicContext));
    }

    private static void fillSystem(IntrinsicRegistry<WasmGCInlineIntrinsic> reg,
            WasmGCCodeGenContext ctx) {
        var arrayCopyIntrinsic = new SystemArrayCopyIntrinsic(ctx.hierarchy(), ctx.module(), ctx.functions(),
                ctx.classInfoProvider(), ctx.typeMapper(), ctx.functionTypes(), ctx.names(), ctx.exceptionTag());
        reg.registerIntrinsic(new MethodReference(System.class, "arraycopy", Object.class, int.class, Object.class,
                int.class, int.class, void.class), arrayCopyIntrinsic);
        reg.registerIntrinsic(new MethodReference(System.class, "doArrayCopy", Object.class, int.class, Object.class,
                int.class, int.class, void.class), arrayCopyIntrinsic);
        reg.registerIntrinsic(System.class, new SystemIntrinsic(ctx.functionTypes(), ctx.module()));
    }
}
