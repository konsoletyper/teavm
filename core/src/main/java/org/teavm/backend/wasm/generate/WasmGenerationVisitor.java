/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.backend.wasm.generate;

import static org.teavm.model.lowlevel.ExceptionHandlingUtil.isManagedMethodCall;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.teavm.ast.ArrayFromDataExpr;
import org.teavm.ast.ArrayType;
import org.teavm.ast.CastExpr;
import org.teavm.ast.Expr;
import org.teavm.ast.InstanceOfExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.InvocationType;
import org.teavm.ast.NewArrayExpr;
import org.teavm.ast.QualificationExpr;
import org.teavm.ast.Statement;
import org.teavm.ast.SubscriptExpr;
import org.teavm.ast.TryCatchStatement;
import org.teavm.backend.wasm.WasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.WasmHeap;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.binary.BinaryWriter;
import org.teavm.backend.wasm.binary.DataPrimitives;
import org.teavm.backend.wasm.generate.common.methods.BaseWasmGenerationVisitor;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsicManager;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmTag;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmBreak;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmConditional;
import org.teavm.backend.wasm.model.expression.WasmDrop;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmIndirectCall;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmInt64Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnary;
import org.teavm.backend.wasm.model.expression.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat32;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat64;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.backend.wasm.model.expression.WasmLoadInt64;
import org.teavm.backend.wasm.model.expression.WasmMemoryAccess;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStoreFloat32;
import org.teavm.backend.wasm.model.expression.WasmStoreFloat64;
import org.teavm.backend.wasm.model.expression.WasmStoreInt32;
import org.teavm.backend.wasm.model.expression.WasmStoreInt64;
import org.teavm.backend.wasm.model.expression.WasmSwitch;
import org.teavm.backend.wasm.model.expression.WasmThrow;
import org.teavm.backend.wasm.model.expression.WasmUnreachable;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.Address;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.classes.VirtualTable;
import org.teavm.model.lowlevel.CallSiteDescriptor;
import org.teavm.model.lowlevel.CallSiteLocation;
import org.teavm.model.lowlevel.ExceptionHandlerDescriptor;
import org.teavm.model.lowlevel.ExceptionHandlingUtil;
import org.teavm.parsing.resource.ResourceProvider;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.ExceptionHandling;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.ShadowStack;

public class WasmGenerationVisitor extends BaseWasmGenerationVisitor {
    private static final FieldReference MONITOR_FIELD = new FieldReference("java.lang.Object", "monitor");
    private static final MethodReference CATCH_METHOD = new MethodReference(ExceptionHandling.class,
            "catchException", Throwable.class);
    private static final MethodReference THROW_METHOD = new MethodReference(ExceptionHandling.class,
            "throwException", Throwable.class, void.class);
    private static final MethodReference THROW_CCE_METHOD = new MethodReference(ExceptionHandling.class,
            "throwClassCastException", void.class);
    private static final MethodReference THROW_NPE_METHOD = new MethodReference(ExceptionHandling.class,
            "throwNullPointerException", void.class);
    private static final MethodReference THROW_AIOOBE_METHOD = new MethodReference(ExceptionHandling.class,
            "throwArrayIndexOutOfBoundsException", void.class);

    private WasmGenerationContext context;
    private WasmClassGenerator classGenerator;

    private List<ExceptionHandlerDescriptor> handlers = new ArrayList<>();
    private WasmBlock lastTryBlock;
    private WasmBlock rethrowBlock;
    private List<WasmBlock> catchLabels = new ArrayList<>();

    private WasmLocal stackVariable;
    private BinaryWriter binaryWriter;
    private boolean managed;

    public WasmGenerationVisitor(WasmGenerationContext context, WasmClassGenerator classGenerator,
            BinaryWriter binaryWriter, WasmFunction function, MethodReference currentMethod,
            int firstVariable, boolean async) {
        super(context, currentMethod, function, firstVariable, async);
        this.context = context;
        this.classGenerator = classGenerator;
        this.binaryWriter = binaryWriter;
        this.managed = context.characteristics.isManaged(currentMethod);
    }

    @Override
    public void generate(Statement statement, List<WasmExpression> target) {
        var lastTargetSize = target.size();
        super.generate(statement, target);
        if (rethrowBlock != null) {
            var body = target.subList(lastTargetSize, target.size());
            rethrowBlock.getBody().addAll(body);
            body.clear();
            target.add(rethrowBlock);
            var valueToReturn = WasmExpression.defaultValueOfType(function.getType().getSingleReturnType());
            if (valueToReturn != null) {
                target.add(new WasmReturn(valueToReturn));
            }
            if (!rethrowBlock.isTerminating()) {
                rethrowBlock.getBody().add(new WasmReturn());
            }
        }
    }

    @Override
    protected void generateThrowNPE(TextLocation location, List<WasmExpression> target) {
        var call = new WasmCall(context.functions().forStaticMethod(THROW_NPE_METHOD));
        call.setLocation(location);
        target.add(call);
    }

    @Override
    public void visit(CastExpr expr) {
        var type = expr.getTarget();
        if (type instanceof ValueType.Object) {
            var className = ((ValueType.Object) type).getClassName();
            if (!context.characteristics.isManaged(className)) {
                expr.getValue().acceptVisitor(this);
                return;
            }
        }
        super.visit(expr);
    }

    @Override
    protected WasmType mapType(ValueType type) {
        return WasmGeneratorUtil.mapType(type);
    }

    @Override
    protected WasmExpression generateArrayLength(WasmExpression array) {
        int sizeOffset = classGenerator.getFieldOffset(new FieldReference(RuntimeArray.class.getName(), "size"));
        var length = new WasmLoadInt32(4, array, WasmInt32Subtype.INT32);
        length.setOffset(sizeOffset);
        length.setLocation(array.getLocation());
        return length;
    }

    @Override
    protected void storeField(Expr qualified, FieldReference field, Expr value, TextLocation location) {
        WasmExpression address = getAddress(qualified, field, location);
        accept(value);
        if (field.equals(MONITOR_FIELD)) {
            storeMonitor(address, result, location);
            return;
        }

        ValueType type = context.getFieldType(field);
        WasmMemoryAccess resultExpr;
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    resultExpr = new WasmStoreInt32(1, address, result, WasmInt32Subtype.INT8);
                    break;
                case SHORT:
                    resultExpr = new WasmStoreInt32(2, address, result, WasmInt32Subtype.INT16);
                    break;
                case CHARACTER:
                    resultExpr = new WasmStoreInt32(2, address, result, WasmInt32Subtype.UINT16);
                    break;
                case INTEGER:
                    resultExpr = new WasmStoreInt32(4, address, result, WasmInt32Subtype.INT32);
                    break;
                case LONG:
                    resultExpr = new WasmStoreInt64(8, address, result, WasmInt64Subtype.INT64);
                    break;
                case FLOAT:
                    resultExpr = new WasmStoreFloat32(4, address, result);
                    break;
                case DOUBLE:
                    resultExpr = new WasmStoreFloat64(8, address, result);
                    break;
                default:
                    throw new AssertionError(type.toString());
            }
        } else {
            resultExpr = new WasmStoreInt32(4, address, result, WasmInt32Subtype.INT32);
        }

        resultExpr.setOffset(getOffset(qualified, field));
        var result = (WasmExpression) resultExpr;
        result.setLocation(location);
        resultConsumer.add(result);
    }

    private void storeMonitor(WasmExpression address, WasmExpression value, TextLocation location) {
        value = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHR_UNSIGNED, value,
                new WasmInt32Constant(1));
        value = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.OR, value,
                new WasmInt32Constant(0x80000000));
        var store = new WasmStoreInt32(4, address, value, WasmInt32Subtype.INT32);
        store.setLocation(location);
        store.setOffset(4);
        resultConsumer.add(store);
    }

    @Override
    protected WasmExpression storeArrayItem(WasmExpression array, WasmExpression index, Expr value,
            ArrayType type) {
        accept(value);
        var wasmValue = result;
        return storeArrayItem(getArrayElementPointer(array, index, type), wasmValue, type);
    }

    private static WasmExpression storeArrayItem(WasmExpression array, WasmExpression value, ArrayType type) {
        switch (type) {
            case BYTE:
                return new WasmStoreInt32(1, array, value, WasmInt32Subtype.INT8);
            case SHORT:
                return new WasmStoreInt32(2, array, value, WasmInt32Subtype.INT16);
            case CHAR:
                return new WasmStoreInt32(2, array, value, WasmInt32Subtype.UINT16);
            case INT:
            case OBJECT:
                return new WasmStoreInt32(4, array, value, WasmInt32Subtype.INT32);
            case LONG:
                return new WasmStoreInt64(8, array, value, WasmInt64Subtype.INT64);
            case FLOAT:
                return new WasmStoreFloat32(4, array, value);
            case DOUBLE:
                return new WasmStoreFloat64(8, array, value);
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    protected WasmExpression stringLiteral(String s) {
        return new WasmInt32Constant(context.getStringPool().getStringPointer(s));
    }

    @Override
    protected WasmExpression classLiteral(ValueType type) {
        return new WasmInt32Constant(classGenerator.getClassPointer(type));
    }

    @Override
    protected WasmExpression nullLiteral(Expr expr) {
        return new WasmInt32Constant(0);
    }

    @Override
    protected WasmExpression nullLiteral(WasmType type) {
        return new WasmInt32Constant(0);
    }

    @Override
    protected WasmExpression genIsNull(WasmExpression value) {
        return new WasmIntUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ, value);
    }

    @Override
    public void visit(SubscriptExpr expr) {
        WasmExpression ptr = getArrayElementPointer(expr);
        switch (expr.getType()) {
            case BYTE:
                result = new WasmLoadInt32(1, ptr, WasmInt32Subtype.INT8);
                break;
            case SHORT:
                result = new WasmLoadInt32(2, ptr, WasmInt32Subtype.INT16);
                break;
            case CHAR:
                result = new WasmLoadInt32(2, ptr, WasmInt32Subtype.UINT16);
                break;
            case INT:
            case OBJECT:
                result = new WasmLoadInt32(4, ptr, WasmInt32Subtype.INT32);
                break;
            case LONG:
                result = new WasmLoadInt64(8, ptr, WasmInt64Subtype.INT64);
                break;
            case FLOAT:
                result = new WasmLoadFloat32(4, ptr);
                break;
            case DOUBLE:
                result = new WasmLoadFloat64(8, ptr);
                break;
        }
    }

    private WasmExpression getArrayElementPointer(SubscriptExpr expr) {
        expr.getArray().acceptVisitor(this);
        WasmExpression array = result;
        expr.getIndex().acceptVisitor(this);
        WasmExpression index = result;
        return getArrayElementPointer(array, index, expr.getType());
    }

    private WasmExpression getArrayElementPointer(WasmExpression array, WasmExpression index, ArrayType type) {
        int size = -1;
        switch (type) {
            case BYTE:
                size = 0;
                break;
            case SHORT:
            case CHAR:
                size = 1;
                break;
            case INT:
            case FLOAT:
            case OBJECT:
                size = 2;
                break;
            case LONG:
            case DOUBLE:
                size = 3;
                break;
        }

        int base = BinaryWriter.align(classGenerator.getClassSize(RuntimeArray.class.getName()), 1 << size);
        array = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, array, new WasmInt32Constant(base));
        if (size != 0) {
            index = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL, index,
                    new WasmInt32Constant(size));
        }

        return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, array, index);
    }

    @Override
    protected WasmExpression invocation(InvocationExpr expr, List<WasmExpression> resultConsumer, boolean willDrop) {
        if (expr.getMethod().getClassName().equals(ShadowStack.class.getName())) {
            switch (expr.getMethod().getName()) {
                case "allocStack":
                    generateAllocStack(expr.getArguments().get(0));
                    result.setLocation(expr.getLocation());
                    if (resultConsumer != null) {
                        resultConsumer.add(result);
                        return null;
                    } else {
                        return result;
                    }
                case "releaseStack":
                    generateReleaseStack();
                    result.setLocation(expr.getLocation());
                    if (resultConsumer != null) {
                        resultConsumer.add(result);
                        return null;
                    } else {
                        return result;
                    }
                case "registerGCRoot":
                    generateRegisterGcRoot(expr.getArguments().get(0), expr.getArguments().get(1));
                    result.setLocation(expr.getLocation());
                    if (resultConsumer != null) {
                        resultConsumer.add(result);
                        return null;
                    } else {
                        return result;
                    }
                case "removeGCRoot":
                    generateRemoveGcRoot(expr.getArguments().get(0));
                    result.setLocation(expr.getLocation());
                    if (resultConsumer != null) {
                        resultConsumer.add(result);
                        return null;
                    } else {
                        return result;
                    }
            }
        }

        var intrinsic = context.getIntrinsic(expr.getMethod());
        if (intrinsic != null) {
            var resultExpr = intrinsic.apply(expr, intrinsicManager);
            return trivialInvocation(resultExpr, resultConsumer, expr.getLocation(), willDrop);
        }

        return super.invocation(expr, resultConsumer, willDrop);
    }

    @Override
    protected WasmExpression generateVirtualCall(WasmLocal instance, MethodReference method,
            List<WasmExpression> arguments) {
        int vtableOffset = classGenerator.getClassSize(RuntimeClass.class.getName());
        VirtualTable vtable = context.getVirtualTableProvider().lookup(method.getClassName());
        if (vtable != null) {
            vtable = vtable.findMethodContainer(method.getDescriptor());
        }
        if (vtable == null) {
            return new WasmUnreachable();
        }
        int vtableIndex = vtable.getMethods().indexOf(method.getDescriptor());
        if (vtable.getParent() != null) {
            vtableIndex += vtable.getParent().size();
        }
        var classRef = getReferenceToClass(new WasmGetLocal(instance));
        var methodIndex = new WasmLoadInt32(4, classRef, WasmInt32Subtype.INT32);
        methodIndex.setOffset(vtableIndex * 4 + vtableOffset);

        var parameterTypes = new WasmType[method.parameterCount() + 1];
        parameterTypes[0] = WasmType.INT32;
        for (var i = 0; i < method.parameterCount(); ++i) {
            parameterTypes[i + 1] = WasmGeneratorUtil.mapType(method.parameterType(i));
        }
        var functionType = context.functionTypes().of(WasmGeneratorUtil.mapType(method.getReturnType()),
                parameterTypes);
        var call = new WasmIndirectCall(methodIndex, functionType);
        call.getArguments().addAll(arguments);
        return call;
    }

    private WasmExpression getReferenceToClass(WasmExpression instance) {
        var classIndex = new WasmLoadInt32(4, instance, WasmInt32Subtype.INT32);
        return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL, classIndex,
                new WasmInt32Constant(3));
    }

    private WasmExpression trivialInvocation(WasmExpression resultExpr, List<WasmExpression> resultConsumer,
            TextLocation location, boolean willDrop) {
        if (resultConsumer != null) {
            if (willDrop) {
                var drop = new WasmDrop(resultExpr);
                drop.setLocation(location);
                resultConsumer.add(drop);
            } else {
                resultConsumer.add(resultExpr);
            }
            result = null;
            return null;
        } else {
            return resultExpr;
        }
    }

    @Override
    protected CallSiteIdentifier generateCallSiteId(TextLocation location) {
        return generateCallSiteIdImpl(location);
    }

    private CallSiteIdentifierImpl generateCallSiteIdImpl(TextLocation location) {
        var callSiteLocations = CallSiteLocation.fromTextLocation(location, currentMethod);
        var callSite = new CallSiteDescriptor(context.callSites().size(), callSiteLocations);
        var reverseHandlers = new ArrayList<>(handlers);
        Collections.reverse(reverseHandlers);
        callSite.getHandlers().addAll(reverseHandlers);
        context.callSites().add(callSite);
        return new CallSiteIdentifierImpl(callSite.getId());
    }

    @Override
    public boolean isManaged() {
        return managed;
    }

    @Override
    protected boolean isManagedCall(MethodReference method) {
        return isManagedMethodCall(context.characteristics, method);
    }

    @Override
    protected void generateThrow(WasmExpression expression, TextLocation location, List<WasmExpression> target) {
        if (context.getExceptionTag() == null) {
            var call = new WasmCall(context.functions().forStaticMethod(THROW_METHOD), result);
            call.setLocation(location);
            target.add(call);
        } else {
            var result = new WasmThrow(context.getExceptionTag());
            result.getArguments().add(expression);
            result.setLocation(location);
            target.add(result);
        }
    }

    private class CallSiteIdentifierImpl extends CallSiteIdentifier
            implements WasmIntrinsicManager.CallSiteIdentifier {
        private int id;

        CallSiteIdentifierImpl(int id) {
            this.id = id;
        }

        @Override
        public void generateRegister(List<WasmExpression> consumer, TextLocation location) {
            if (!managed) {
                return;
            }
            var result = new WasmStoreInt32(4, new WasmGetLocal(stackVariable), new WasmInt32Constant(id),
                    WasmInt32Subtype.INT32);
            result.setLocation(location);
            consumer.add(result);
        }

        @Override
        public void checkHandlerId(List<WasmExpression> target, TextLocation location) {
            if (context.getExceptionTag() != null) {
                return;
            }
            var jumpTarget = throwJumpTarget();
            if (jumpTarget == rethrowBlock) {
                var handlerId = generateGetHandlerId(location);
                var br = new WasmBranch(handlerId, throwJumpTarget());
                target.add(br);
            } else {
                var handlerVar = tempVars.acquire(WasmType.INT32);
                var handlerId = generateGetHandlerId(location);
                var saveHandler = new WasmSetLocal(handlerVar, handlerId);
                saveHandler.setLocation(location);
                target.add(saveHandler);
                var br = new WasmBranch(new WasmGetLocal(handlerVar), throwJumpTarget());
                br.setResult(new WasmGetLocal(handlerVar));
                var dropBr = new WasmDrop(br);
                dropBr.setLocation(location);
                target.add(dropBr);
                tempVars.release(handlerVar);
            }
        }

        @Override
        public void generateThrow(List<WasmExpression> target, TextLocation location) {
            if (context.getExceptionTag() == null) {
                var throwTarget = throwJumpTarget();
                var breakExpr = new WasmBreak(throwTarget);
                if (throwTarget != rethrowBlock) {
                    breakExpr.setResult(generateGetHandlerId(location));
                    target.add(new WasmDrop(breakExpr));
                } else {
                    target.add(breakExpr);
                }
            } else {
                target.add(new WasmUnreachable());
            }
        }


        private WasmExpression generateGetHandlerId(TextLocation location) {
            WasmExpression handlerId = new WasmLoadInt32(4, new WasmGetLocal(stackVariable), WasmInt32Subtype.INT32);
            handlerId = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB, handlerId,
                        new WasmInt32Constant(id));
            handlerId.setLocation(location);
            return handlerId;
        }
    }

    private WasmBlock throwJumpTarget() {
        return lastTryBlock != null ? lastTryBlock : rethrowBlock();
    }

    private void generateAllocStack(Expr sizeExpr) {
        if (stackVariable != null) {
            throw new IllegalStateException("Call to ShadowStack.allocStack must be done only once");
        }
        stackVariable = tempVars.acquire(WasmType.INT32);
        stackVariable.setName("__stack__");
        InvocationExpr expr = new InvocationExpr();
        expr.setType(InvocationType.STATIC);
        expr.setMethod(new MethodReference(WasmRuntime.class, "allocStack", int.class, Address.class));
        expr.getArguments().add(sizeExpr);
        expr.acceptVisitor(this);

        result = new WasmSetLocal(stackVariable, result);
    }

    private void generateReleaseStack() {
        if (stackVariable == null) {
            throw new IllegalStateException("Call to ShadowStack.releaseStack must be dominated by "
                    + "Mutator.allocStack");
        }

        int offset = classGenerator.getFieldOffset(new FieldReference(WasmHeap.class.getName(), "stack"));
        WasmExpression oldValue = new WasmGetLocal(stackVariable);
        oldValue = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB, oldValue,
                new WasmInt32Constant(4));
        result = new WasmStoreInt32(4, new WasmInt32Constant(offset), oldValue, WasmInt32Subtype.INT32);
    }

    private void generateRegisterGcRoot(Expr slotExpr, Expr gcRootExpr) {
        if (stackVariable == null) {
            throw new IllegalStateException("Call to ShadowStack.registerGCRoot must be dominated by "
                    + "Mutator.allocStack");
        }

        slotExpr.acceptVisitor(this);
        WasmExpression slotOffset = getSlotOffset(result);
        WasmExpression address = new WasmGetLocal(stackVariable);
        if (!(slotOffset instanceof WasmInt32Constant)) {
            address = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, address, slotOffset);
        }

        gcRootExpr.acceptVisitor(this);
        WasmExpression gcRoot = result;

        WasmStoreInt32 store = new WasmStoreInt32(4, address, gcRoot, WasmInt32Subtype.INT32);
        if (slotOffset instanceof WasmInt32Constant) {
            store.setOffset(((WasmInt32Constant) slotOffset).getValue());
        }
        result = store;
    }

    private void generateRemoveGcRoot(Expr slotExpr) {
        if (stackVariable == null) {
            throw new IllegalStateException("Call to ShadowStack.removeGCRoot must be dominated by "
                    + "Mutator.allocStack");
        }

        slotExpr.acceptVisitor(this);
        WasmExpression slotOffset = getSlotOffset(result);
        WasmExpression address = new WasmGetLocal(stackVariable);
        if (!(slotOffset instanceof WasmInt32Constant)) {
            address = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, address, slotOffset);
        }

        WasmStoreInt32 store = new WasmStoreInt32(4, address, new WasmInt32Constant(0), WasmInt32Subtype.INT32);
        if (slotOffset instanceof WasmInt32Constant) {
            store.setOffset(((WasmInt32Constant) slotOffset).getValue());
        }
        result = store;
    }

    private WasmExpression getSlotOffset(WasmExpression slot) {
        if (slot instanceof WasmInt32Constant) {
            int slotConstant = ((WasmInt32Constant) slot).getValue();
            return new WasmInt32Constant((slotConstant << 2) + 4);
        } else {
            slot = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL, slot, new WasmInt32Constant(2));
            slot = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, slot, new WasmInt32Constant(4));
            return slot;
        }
    }

    @Override
    public void visit(QualificationExpr expr) {
        WasmExpression address = getAddress(expr.getQualified(), expr.getField(), expr.getLocation());
        if (expr.getField().equals(MONITOR_FIELD)) {
            result = getMonitor(address, expr.getLocation());
            return;
        }

        ValueType type = context.getFieldType(expr.getField());
        WasmMemoryAccess resultExpr;
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    resultExpr = new WasmLoadInt32(1, address, WasmInt32Subtype.INT8);
                    break;
                case SHORT:
                    resultExpr = new WasmLoadInt32(2, address, WasmInt32Subtype.INT16);
                    break;
                case CHARACTER:
                    resultExpr = new WasmLoadInt32(2, address, WasmInt32Subtype.UINT16);
                    break;
                case INTEGER:
                    resultExpr = new WasmLoadInt32(4, address, WasmInt32Subtype.INT32);
                    break;
                case LONG:
                    resultExpr = new WasmLoadInt64(8, address, WasmInt64Subtype.INT64);
                    break;
                case FLOAT:
                    resultExpr = new WasmLoadFloat32(4, address);
                    break;
                case DOUBLE:
                    resultExpr = new WasmLoadFloat64(8, address);
                    break;
                default:
                    throw new AssertionError(type.toString());
            }
        } else {
            resultExpr = new WasmLoadInt32(4, address, WasmInt32Subtype.INT32);
        }

        resultExpr.setOffset(getOffset(expr.getQualified(), expr.getField()));
        result = (WasmExpression) resultExpr;
    }

    private WasmExpression getMonitor(WasmExpression address, TextLocation location) {
        var block = new WasmBlock(false);
        block.setType(WasmType.INT32.asBlock());
        block.setLocation(location);

        var tmp = tempVars.acquire(WasmType.INT32);
        var monitor = new WasmLoadInt32(4, address, WasmInt32Subtype.INT32);
        monitor.setOffset(4);
        block.getBody().add(new WasmSetLocal(tmp, monitor));

        var isMonitor = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.AND,
                new WasmGetLocal(tmp), new WasmInt32Constant(0x80000000));
        var shiftMonitor = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL,
                new WasmGetLocal(tmp), new WasmInt32Constant(1));
        var cond = new WasmConditional(isMonitor);
        cond.setType(WasmType.INT32.asBlock());
        cond.getThenBlock().getBody().add(shiftMonitor);
        cond.getElseBlock().getBody().add(new WasmInt32Constant(0));
        block.getBody().add(cond);

        tempVars.release(tmp);
        return block;
    }

    private WasmExpression getAddress(Expr qualified, FieldReference field, TextLocation location) {
        if (qualified == null) {
            int offset = classGenerator.getFieldOffset(field);
            WasmExpression result = new WasmInt32Constant(offset);
            result.setLocation(location);
            return result;
        } else {
            accept(qualified);
            assert result != null;
            return result;
        }
    }

    private int getOffset(Expr qualified, FieldReference field) {
        if (qualified == null) {
            return 0;
        }
        return classGenerator.getFieldOffset(field);
    }

    @Override
    protected void allocateObject(String className, TextLocation location, WasmLocal local,
            List<WasmExpression> target) {
        int tag = classGenerator.getClassPointer(ValueType.object(className));
        var allocFunction = context.functions().forStaticMethod(new MethodReference(Allocator.class, "allocate",
                RuntimeClass.class, Address.class));
        WasmCall call = new WasmCall(allocFunction);
        call.getArguments().add(new WasmInt32Constant(tag));
        call.setLocation(location);
        if (local != null) {
            target.add(new WasmSetLocal(local, call));
        } else {
            target.add(call);
        }
    }

    @Override
    public void visit(NewArrayExpr expr) {
        var block = new WasmBlock(false);
        block.setType(mapType(ValueType.arrayOf(expr.getType())).asBlock());

        var callSiteId = generateCallSiteId(expr.getLocation());
        callSiteId.generateRegister(block.getBody(), expr.getLocation());

        allocateArray(expr.getType(), () -> {
            accept(expr.getLength());
            return result;
        }, expr.getLocation(), null, block.getBody());

        if (block.getBody().size() == 1) {
            result = block.getBody().get(0);
        } else {
            result = block;
        }
    }

    private void allocateArray(ValueType itemType, Supplier<WasmExpression> length, TextLocation location,
            WasmLocal local, List<WasmExpression> target) {
        int classPointer = classGenerator.getClassPointer(ValueType.arrayOf(itemType));
        var allocFunction = context.functions().forStaticMethod(new MethodReference(Allocator.class, "allocateArray",
                RuntimeClass.class, int.class, Address.class));
        var call = new WasmCall(allocFunction);
        call.getArguments().add(new WasmInt32Constant(classPointer));
        call.getArguments().add(length.get());
        call.setLocation(location);
        if (local != null) {
            target.add(new WasmSetLocal(local, call));
        } else {
            target.add(call);
        }
    }

    @Override
    public void visit(ArrayFromDataExpr expr) {
        var type = expr.getType();

        var arrayType = ArrayType.OBJECT;
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    arrayType = ArrayType.BYTE;
                    break;
                case SHORT:
                    arrayType = ArrayType.SHORT;
                    break;
                case CHARACTER:
                    arrayType = ArrayType.CHAR;
                    break;
                case INTEGER:
                    arrayType = ArrayType.INT;
                    break;
                case LONG:
                    arrayType = ArrayType.LONG;
                    break;
                case FLOAT:
                    arrayType = ArrayType.FLOAT;
                    break;
                case DOUBLE:
                    arrayType = ArrayType.DOUBLE;
                    break;
            }
        }

        var wasmArrayType = mapType(ValueType.arrayOf(expr.getType()));
        var block = new WasmBlock(false);
        block.setType(wasmArrayType.asBlock());
        var callSiteId = generateCallSiteId(expr.getLocation());
        callSiteId.generateRegister(block.getBody(), expr.getLocation());

        var array = tempVars.acquire(wasmArrayType);
        allocateArray(expr.getType(), () -> new WasmInt32Constant(expr.getData().size()), expr.getLocation(), array,
                block.getBody());

        for (int i = 0; i < expr.getData().size(); ++i) {
            var arrayData = unwrapArray(new WasmGetLocal(array));
            block.getBody().add(storeArrayItem(arrayData, new WasmInt32Constant(i), expr.getData().get(i),
                    arrayType));
        }

        block.getBody().add(new WasmGetLocal(array));
        block.setLocation(expr.getLocation());
        tempVars.release(array);

        result = block;
    }

    @Override
    protected WasmExpression allocateMultiArray(List<WasmExpression> target, ValueType arrayType,
            Supplier<List<WasmExpression>> dimensions, TextLocation location) {
        int dimensionList = -1;
        var dimensionsValue = dimensions.get();
        for (var dimension : dimensionsValue) {
            int dimensionAddress = binaryWriter.append(DataPrimitives.INT.createValue());
            if (dimensionList < 0) {
                dimensionList = dimensionAddress;
            }
            target.add(new WasmStoreInt32(4, new WasmInt32Constant(dimensionAddress), dimension,
                    WasmInt32Subtype.INT32));
        }

        int classPointer = classGenerator.getClassPointer(arrayType);
        var allocFunction = context.functions().forStaticMethod(new MethodReference(Allocator.class,
                "allocateMultiArray", RuntimeClass.class, Address.class, int.class, RuntimeArray.class));
        var call = new WasmCall(allocFunction);
        call.getArguments().add(new WasmInt32Constant(classPointer));
        call.getArguments().add(new WasmInt32Constant(dimensionList));
        call.getArguments().add(new WasmInt32Constant(dimensionsValue.size()));
        call.setLocation(location);
        return call;
    }

    @Override
    public void visit(InvocationExpr expr) {
        result = invocation(expr, null, false);
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        var type = expr.getType();
        if (type instanceof ValueType.Object) {
            var className = ((ValueType.Object) type).getClassName();
            if (!context.characteristics.isManaged(className)) {
                expr.getExpr().acceptVisitor(this);
                return;
            }
        }

        super.visit(expr);
    }

    @Override
    protected WasmExpression generateInstanceOf(WasmExpression expression, ValueType type) {
        classGenerator.getClassPointer(type);
        var supertypeCall = new WasmCall(context.functions().forSupertype(type));
        WasmExpression classRef = new WasmLoadInt32(4, expression, WasmInt32Subtype.INT32);
        classRef = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL, classRef,
                new WasmInt32Constant(3));
        supertypeCall.getArguments().add(new WasmInt32Constant(0));
        supertypeCall.getArguments().add(classRef);
        return supertypeCall;
    }

    @Override
    protected void generateThrowCCE(TextLocation location, List<WasmExpression> target) {
        var call = new WasmCall(context.functions().forStaticMethod(THROW_CCE_METHOD));
        call.setLocation(location);
        target.add(call);
    }

    @Override
    protected WasmExpression generateClassInitializer(String className, TextLocation location) {
        var call = new WasmCall(context.functions().forClassInitializer(className));
        call.setLocation(location);
        return call;
    }

    @Override
    protected boolean needsClassInitializer(String className) {
        return classGenerator.hasClinit(className);
    }

    @Override
    protected void generateTry(List<TryCatchStatement> tryCatchStatements, List<Statement> protectedBody) {
        if (context.getExceptionTag() == null) {
            emulatedTry(tryCatchStatements, protectedBody);
        } else {
            super.generateTry(tryCatchStatements, protectedBody);
        }
    }

    private void emulatedTry(List<TryCatchStatement> tryCatchStatements, List<Statement> protectedBody) {
        int firstId = handlers.size();

        var innerCatchBlock = new WasmBlock(false);
        var bodyBlock = new WasmBlock(false);
        bodyBlock.setType(WasmType.INT32.asBlock());

        var isTopMostTryCatch = lastTryBlock == null;
        if (isTopMostTryCatch) {
            catchLabels.add(rethrowBlock());
        }

        var catchBlocks = new ArrayList<WasmBlock>();
        for (int i = 0; i < tryCatchStatements.size(); ++i) {
            var tryCatch = tryCatchStatements.get(i);
            handlers.add(new ExceptionHandlerDescriptor(firstId + i, tryCatch.getExceptionType()));
            catchBlocks.add(new WasmBlock(false));
        }
        var outerCatchBlock = catchBlocks.get(0);
        catchLabels.addAll(catchBlocks.subList(1, catchBlocks.size()));
        catchLabels.add(innerCatchBlock);

        var lastTryBlockBackup = lastTryBlock;
        lastTryBlock = bodyBlock;
        visitMany(protectedBody, bodyBlock.getBody());
        lastTryBlock = lastTryBlockBackup;
        handlers.subList(firstId, handlers.size()).clear();

        if (!bodyBlock.isTerminating()) {
            bodyBlock.getBody().add(new WasmBreak(outerCatchBlock));
        }
        var currentBlock = innerCatchBlock;
        var handlerIdExpr = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB,
                bodyBlock, new WasmInt32Constant(1));
        var switchExpr = new WasmSwitch(handlerIdExpr, outerCatchBlock);
        switchExpr.getTargets().addAll(catchLabels);
        innerCatchBlock.getBody().add(switchExpr);

        catchLabels.subList(catchLabels.size() - tryCatchStatements.size(), catchLabels.size()).clear();
        if (isTopMostTryCatch) {
            catchLabels.remove(catchLabels.size() - 1);
            assert catchLabels.isEmpty();
        }

        for (int i = tryCatchStatements.size() - 1; i >= 0; --i) {
            var tryCatch = tryCatchStatements.get(i);
            var catchBlock = catchBlocks.get(i);
            catchBlock.getBody().add(currentBlock);
            var catchFunction = context.functions().forStaticMethod(CATCH_METHOD);
            var catchCall = new WasmCall(catchFunction);
            var catchWrapper = tryCatch.getExceptionVariable() != null
                    ? new WasmSetLocal(localVar(tryCatch.getExceptionVariable()), catchCall)
                    : new WasmDrop(catchCall);
            catchBlock.getBody().add(catchWrapper);
            visitMany(tryCatch.getHandler(), catchBlock.getBody());
            if (!catchBlock.isTerminating() && catchBlock != outerCatchBlock) {
                catchBlock.getBody().add(new WasmBreak(outerCatchBlock));
            }
            currentBlock = catchBlock;
        }

        resultConsumer.add(outerCatchBlock);
    }

    private WasmBlock rethrowBlock() {
        if (rethrowBlock == null) {
            rethrowBlock = new WasmBlock(false);
        }
        return rethrowBlock;
    }

    private void visitMany(List<Statement> statements, List<WasmExpression> target) {
        var oldTarget = resultConsumer;
        resultConsumer = target;
        for (var part : statements) {
            accept(part);
        }
        resultConsumer = oldTarget;
    }

    @Override
    protected void generateThrowAIOOBE(TextLocation location, List<WasmExpression> target) {
        var call = new WasmCall(context.functions().forStaticMethod(THROW_AIOOBE_METHOD));
        call.setLocation(location);
        target.add(call);
    }

    private WasmIntrinsicManager intrinsicManager = new WasmIntrinsicManager() {
        @Override
        public WasmExpression generate(Expr expr) {
            accept(expr);
            return result;
        }

        @Override
        public ResourceProvider getResourceProvider() {
            return context.resources();
        }

        @Override
        public ClassHierarchy getClassHierarchy() {
            return context.getClassHierarchy();
        }

        @Override
        public BinaryWriter getBinaryWriter() {
            return binaryWriter;
        }

        @Override
        public WasmStringPool getStringPool() {
            return context.getStringPool();
        }

        @Override
        public Diagnostics getDiagnostics() {
            return context.getDiagnostics();
        }

        @Override
        public WasmFunctionRepository getFunctions() {
            return context.functions();
        }

        @Override
        public WasmFunctionTypes getFunctionTypes() {
            return context.functionTypes();
        }

        @Override
        public WasmLocal getTemporary(WasmType type) {
            return tempVars.acquire(type);
        }

        @Override
        public void releaseTemporary(WasmLocal local) {
            tempVars.release(local);
        }

        @Override
        public int getStaticField(FieldReference field) {
            return classGenerator.getFieldOffset(field);
        }

        @Override
        public int getClassPointer(ValueType type) {
            return classGenerator.getClassPointer(type);
        }

        @Override
        public int getFunctionPointer(WasmFunction function) {
            return classGenerator.getFunctionPointer(function);
        }

        @Override
        public boolean isManagedMethodCall(MethodReference method) {
            return needsCallSiteId() && ExceptionHandlingUtil.isManagedMethodCall(context.characteristics, method);
        }

        @Override
        public CallSiteIdentifier generateCallSiteId(TextLocation location) {
            return generateCallSiteIdImpl(location);
        }

        @Override
        public WasmTag getExceptionTag() {
            return context.getExceptionTag();
        }
    };
}
