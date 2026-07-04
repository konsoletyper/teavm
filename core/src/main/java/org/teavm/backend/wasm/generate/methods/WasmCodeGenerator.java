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
package org.teavm.backend.wasm.generate.methods;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.generate.WasmGeneratorUtil;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfo;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsicContext;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmNumType;
import org.teavm.backend.wasm.model.WasmStorageType;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmCastCondition;
import org.teavm.backend.wasm.model.instruction.WasmCatchClause;
import org.teavm.backend.wasm.model.instruction.WasmFloatBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmFloatType;
import org.teavm.backend.wasm.model.instruction.WasmFloatUnaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmInt32Subtype;
import org.teavm.backend.wasm.model.instruction.WasmInt64Subtype;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.backend.wasm.model.instruction.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmNullCondition;
import org.teavm.backend.wasm.model.instruction.WasmSignedType;
import org.teavm.backend.wasm.types.PreciseTypeInference;
import org.teavm.common.GraphBuilder;
import org.teavm.flow.FlowReconstruction;
import org.teavm.flow.FlowTreeNode;
import org.teavm.flow.FlowTreeNodeVisitor;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReference;
import org.teavm.model.Instruction;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BinaryOperation;
import org.teavm.model.instructions.BoundCheckInstruction;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.CastIntegerInstruction;
import org.teavm.model.instructions.CastNumberInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InstructionVisitor;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.NegateInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.NumericOperandType;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;
import org.teavm.model.util.DefinitionExtractor;

public class WasmCodeGenerator implements InstructionVisitor, FlowTreeNodeVisitor {
    private static final MethodReference MONITOR_ENTER_SYNC = new MethodReference(Object.class,
            "monitorEnterSync", Object.class, void.class);
    private static final MethodReference MONITOR_EXIT_SYNC = new MethodReference(Object.class,
            "monitorExitSync", Object.class, void.class);
    private static final MethodReference MONITOR_ENTER = new MethodReference(Object.class,
            "monitorEnter", Object.class, void.class);
    private static final MethodReference MONITOR_EXIT = new MethodReference(Object.class,
            "monitorExit", Object.class, void.class);
    private static final MethodDescriptor CLINIT = new MethodDescriptor("<clinit>", ValueType.VOID);

    private static final int SWITCH_TABLE_THRESHOLD = 256;

    private FlowReconstruction flowReconstruction = new FlowReconstruction();
    private WasmGCGenerationContext context;
    private Set<MethodReference> asyncSplitMethods;
    private WasmInstructionBuilder builder;
    private PreciseTypeInference typeInference;
    private WasmInstructionList[] labels;
    private boolean[] exceptionHandlers;
    private MethodReference currentMethod;
    private Program currentProgram;
    private WasmFunction currentWasmFunction;
    private int currentFirstVariable;
    private BasicBlock currentBlock;
    private BasicBlock expectedBasicBlock;
    private boolean dontExpectBasicBlock;
    private boolean async;
    private Instruction[] definitions;
    private List<Variable> handlerPhiValues = new ArrayList<>();
    private List<Variable> handlerPhiReceivers = new ArrayList<>();
    private IntHashSet handlerDefinedVariables = new IntHashSet();
    private DefinitionExtractor handlerDefinitionExtractor = new DefinitionExtractor();

    public WasmCodeGenerator(
            WasmGCGenerationContext context,
            Set<MethodReference> asyncSplitMethods
    ) {
        this.context = context;
        this.asyncSplitMethods = asyncSplitMethods;
    }

    public void generate(Program program, MethodReference currentMethod, WasmFunction function,
            int firstVariable, boolean async) {
        this.async = async;
        this.currentMethod = currentMethod;
        this.currentProgram = program;
        this.currentWasmFunction = function;
        typeInference = new PreciseTypeInference(program, currentMethod, context.hierarchy());
        typeInference.setPhisSkipped(false);
        typeInference.setBackPropagation(true);
        typeInference.ensure();
        currentFirstVariable = firstVariable;
        for (var i = firstVariable; i < program.variableCount(); ++i) {
            var varType = typeInference.typeOf(i);
            WasmType wasmType;
            if (varType == null) {
                wasmType = WasmType.ANY;
            } else if (varType.isArrayUnwrap) {
                var arrayType = context.classInfoProvider().getClassInfo(varType.valueType).getArray();
                wasmType = arrayType.getReference();
            } else {
                wasmType = context.typeMapper().mapType(varType.valueType);
            }
            var local = new WasmLocal(wasmType);
            function.add(local);
        }
        labels = new WasmInstructionList[program.basicBlockCount()];
        exceptionHandlers = new boolean[program.basicBlockCount()];
        for (var block : program.getBasicBlocks()) {
            for (var tryCatch : block.getTryCatchBlocks()) {
                exceptionHandlers[tryCatch.getHandler().getIndex()] = true;
            }
        }
        var flowRoots = flowReconstruction.reconstruct(program);
        builder = function.getBody().builder();
        for (var root : flowRoots) {
            root.acceptVisitor(this);
        }
        builder = null;
        labels = null;
        exceptionHandlers = null;
        typeInference = null;
        definitions = null;
        this.currentMethod = null;
        this.currentProgram = null;
        this.currentWasmFunction = null;
    }

    @Override
    public void visit(FlowTreeNode.Region node) {
        for (var block : node.blocks) {
            currentBlock = block;
            processBasicBlock(block);
            if (builder.isTerminating()) {
                break;
            }
        }
    }

    @Override
    public void visit(FlowTreeNode.TryCatch node) {
        var outerBuilder = builder;

        var tryCatchNodes = extractTryCatches(node);
        var body = tryCatchNodes.get(0).tryBody;

        // When handler has no label, it immediately follows try/catch, so it receives exception
        // by breaking out of the outer block
        WasmType.Reference fallthroughHandlerType = null;
        for (var tryCatchNode : tryCatchNodes) {
            if (labels[tryCatchNode.catchBlock.getIndex()] == null) {
                fallthroughHandlerType = handlerType(tryCatchNode.catchBlock);
                break;
            }
        }
        var throwableType = throwableType();
        var catchBlock = builder.block(fallthroughHandlerType);
        var tryWrapper = catchBlock.block(throwableType);
        var tryInsn = tryWrapper.try_();
        tryWrapper.unreachable();
        tryInsn.getCatches().add(new WasmCatchClause(context.getExceptionTag(), false, tryWrapper.list));

        for (var tryCatchNode : tryCatchNodes) {
            var targetLabel = labels[tryCatchNode.catchBlock.getIndex()];
            if (targetLabel == null) {
                targetLabel = catchBlock.list;
            }
            var targetType = handlerType(tryCatchNode.catchBlock);
            if (tryCatchNode.exceptionType != null) {
                var exceptionType = (WasmType.Reference) context.typeMapper().mapType(
                        ValueType.object(tryCatchNode.exceptionType));
                catchBlock.castBranch(WasmCastCondition.SUCCESS, throwableType, exceptionType, targetLabel);
            } else {
                if (targetType != throwableType) {
                    catchBlock.cast(targetType);
                }
                catchBlock.breakTo(targetLabel);
                break;
            }
        }
        if (!catchBlock.isTerminating()) {
            catchBlock.throw_(context.getExceptionTag());
        }

        labels[node.blockAfter.getIndex()] = catchBlock.list;
        builder = tryInsn.getBody().builder();
        for (var part : body) {
            part.acceptVisitor(this);
        }
        labels[node.blockAfter.getIndex()] = null;
        builder = outerBuilder;
    }

    private WasmType.Reference throwableType() {
        return (WasmType.Reference) context.typeMapper().mapType(ValueType.object("java.lang.Throwable"));
    }

    private WasmType.Reference handlerType(BasicBlock handler) {
        if (handler.getExceptionVariable() != null
                && mapToLocal(handler.getExceptionVariable()).getType() instanceof WasmType.Reference ref) {
            return ref;
        }
        return throwableType();
    }

    private List<FlowTreeNode.TryCatch> extractTryCatches(FlowTreeNode.TryCatch node) {
        var tryCatchNodes = new ArrayList<FlowTreeNode.TryCatch>();
        while (true) {
            tryCatchNodes.add(node);
            if (node.tryBody.size() != 1) {
                break;
            }
            if (!(node.tryBody.get(0) instanceof FlowTreeNode.TryCatch next)) {
                break;
            }
            node = next;
        }
        Collections.reverse(tryCatchNodes);
        return tryCatchNodes;
    }

    @Override
    public void visit(FlowTreeNode.Loop node) {
        var outerBuilder = builder;
        builder = builder.loop();
        labels[node.head.getIndex()] = builder.list;
        for (var part : node.body) {
            part.acceptVisitor(this);
        }
        labels[node.head.getIndex()] = null;
        builder = outerBuilder;
    }

    @Override
    public void visit(FlowTreeNode.Block node) {
        var outerBuilder = builder;
        // Exception handlers are only reachable via exception dispatch, which passes exception
        // as the block's result
        var isHandler = exceptionHandlers[node.jumpTarget.getIndex()];
        builder = builder.block(isHandler ? handlerType(node.jumpTarget) : null);
        labels[node.jumpTarget.getIndex()] = builder.list;
        for (var part : node.body) {
            part.acceptVisitor(this);
        }
        if (isHandler && !builder.isTerminating()) {
            builder.unreachable();
        }
        labels[node.jumpTarget.getIndex()] = null;
        builder = outerBuilder;
    }

    private void processBasicBlock(BasicBlock block) {
        //assert block == expectedBasicBlock || dontExpectBasicBlock;
        expectedBasicBlock = null;
        dontExpectBasicBlock = false;
        if (exceptionHandlers[block.getIndex()]) {
            if (block.getExceptionVariable() != null) {
                builder.setLocal(mapToLocal(block.getExceptionVariable()));
            } else {
                builder.drop();
            }
        }
        var hasHandlerPhis = collectHandlerPhis(block);
        if (hasHandlerPhis) {
            for (var i = 0; i < handlerPhiValues.size(); ++i) {
                var value = handlerPhiValues.get(i);
                if (!handlerDefinedVariables.contains(value.getIndex())) {
                    builder.getLocal(mapToLocal(value)).setLocal(mapToLocal(handlerPhiReceivers.get(i)));
                }
            }
        }
        for (var insn : block) {
            builder.setCurrentLocation(insn.getLocation());
            insn.acceptVisitor(this);
            if (builder.isTerminating()) {
                break;
            }
            if (hasHandlerPhis) {
                updateHandlerPhis(insn);
            }
        }
    }

    private boolean collectHandlerPhis(BasicBlock block) {
        handlerPhiValues.clear();
        handlerPhiReceivers.clear();
        handlerDefinedVariables.clear();
        for (var tryCatch : block.getTryCatchBlocks()) {
            for (var phi : tryCatch.getHandler().getPhis()) {
                for (var incoming : phi.getIncomings()) {
                    if (incoming.getSource() == block) {
                        handlerPhiValues.add(incoming.getValue());
                        handlerPhiReceivers.add(phi.getReceiver());
                    }
                }
            }
        }
        if (handlerPhiValues.isEmpty()) {
            return false;
        }
        for (var insn : block) {
            insn.acceptVisitor(handlerDefinitionExtractor);
            for (var definedVar : handlerDefinitionExtractor.getDefinedVariables()) {
                handlerDefinedVariables.add(definedVar.getIndex());
            }
        }
        return true;
    }

    private void updateHandlerPhis(Instruction insn) {
        insn.acceptVisitor(handlerDefinitionExtractor);
        for (var definedVar : handlerDefinitionExtractor.getDefinedVariables()) {
            for (var i = 0; i < handlerPhiValues.size(); ++i) {
                if (handlerPhiValues.get(i) == definedVar) {
                    builder.getLocal(mapToLocal(definedVar)).setLocal(mapToLocal(handlerPhiReceivers.get(i)));
                }
            }
        }
    }

    @Override
    public void visit(EmptyInstruction insn) {
    }

    @Override
    public void visit(ClassConstantInstruction insn) {
        WasmGCGenerationUtil.emitClassLiteral(context.classInfoProvider(), builder, insn.getConstant());
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(NullConstantInstruction insn) {
        var type = typeInference.typeOf(insn.getReceiver());
        var wasmType = context.typeMapper().mapType(type.valueType);
        if (wasmType == WasmType.INT32) {
            builder.i32Const(0);
        } else {
            builder.nullConst((WasmType.Reference) wasmType);
        }
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(IntegerConstantInstruction insn) {
        builder.i32Const(insn.getConstant());
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(LongConstantInstruction insn) {
        builder.i64Const(insn.getConstant());
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(FloatConstantInstruction insn) {
        builder.f32Const(insn.getConstant());
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(DoubleConstantInstruction insn) {
        builder.f64Const(insn.getConstant());
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(StringConstantInstruction insn) {
        var stringConstant = context.strings().getStringConstant(insn.getConstant());
        builder.getGlobal(stringConstant.global);
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(BinaryInstruction insn) {
        switch (insn.getOperation()) {
            case COMPARE_GREATER, COMPARE_LESS -> {
                generateCompare(insn);
                builder.setLocal(mapToLocal(insn.getReceiver()));
                return;
            }
            default -> { }
        }
        builder.getLocal(mapToLocal(insn.getFirstOperand()));
        builder.getLocal(mapToLocal(insn.getSecondOperand()));
        switch (insn.getOperation()) {
            case MODULO -> {
                if (insn.getOperandType() == NumericOperandType.FLOAT) {
                    var method = new MethodReference(WasmRuntime.class, "remainder", float.class, float.class,
                            float.class);
                    var function = context.functions().forStaticMethod(method);
                    builder.call(function);
                    builder.setLocal(mapToLocal(insn.getReceiver()));
                    return;
                } else if (insn.getOperandType() == NumericOperandType.DOUBLE) {
                    var method = new MethodReference(WasmRuntime.class, "remainder", double.class, double.class,
                            double.class);
                    var function = context.functions().forStaticMethod(method);
                    builder.call(function);
                    builder.setLocal(mapToLocal(insn.getReceiver()));
                    return;
                }
            }
            default -> { }
        }
        switch (insn.getOperandType()) {
            case INT, LONG -> {
                var wasmOp = switch (insn.getOperation()) {
                    case ADD -> WasmIntBinaryOperation.ADD;
                    case SUBTRACT -> WasmIntBinaryOperation.SUB;
                    case MULTIPLY -> WasmIntBinaryOperation.MUL;
                    case DIVIDE -> WasmIntBinaryOperation.DIV_SIGNED;
                    case MODULO -> WasmIntBinaryOperation.REM_SIGNED;
                    case AND -> WasmIntBinaryOperation.AND;
                    case OR -> WasmIntBinaryOperation.OR;
                    case XOR -> WasmIntBinaryOperation.XOR;
                    case SHIFT_LEFT -> WasmIntBinaryOperation.SHL;
                    case SHIFT_RIGHT -> WasmIntBinaryOperation.SHR_SIGNED;
                    case SHIFT_RIGHT_UNSIGNED -> WasmIntBinaryOperation.SHR_UNSIGNED;
                    case COMPARE_GREATER, COMPARE_LESS -> null;
                };
                var wasmType = insn.getOperandType() == NumericOperandType.INT ? WasmIntType.INT32 : WasmIntType.INT64;
                if (wasmType == WasmIntType.INT64) {
                    switch (insn.getOperation()) {
                        case SHIFT_LEFT, SHIFT_RIGHT, SHIFT_RIGHT_UNSIGNED ->
                                builder.convert(WasmNumType.INT32, WasmNumType.INT64, false);
                        default -> { }
                    }
                }
                builder.intBinary(wasmType, wasmOp);
            }
            case FLOAT, DOUBLE -> {
                var wasmOp = switch (insn.getOperation()) {
                    case ADD -> WasmFloatBinaryOperation.ADD;
                    case SUBTRACT -> WasmFloatBinaryOperation.SUB;
                    case MULTIPLY -> WasmFloatBinaryOperation.MUL;
                    case DIVIDE -> WasmFloatBinaryOperation.DIV;
                    case MODULO,
                         COMPARE_GREATER,
                         COMPARE_LESS,
                         AND,
                         OR,
                         XOR,
                         SHIFT_LEFT,
                         SHIFT_RIGHT,
                         SHIFT_RIGHT_UNSIGNED -> null;
                };
                var wasmType = insn.getOperandType() == NumericOperandType.FLOAT
                        ? WasmFloatType.FLOAT32
                        : WasmFloatType.FLOAT64;
                builder.floatBinary(wasmType, wasmOp);
            }
        }
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(NegateInstruction insn) {
        switch (insn.getOperandType()) {
            case INT -> {
                builder.i32Const(0).getLocal(mapToLocal(insn.getOperand()));
                builder.intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB);
            }
            case LONG -> {
                builder.i64Const(0).getLocal(mapToLocal(insn.getOperand()));
                builder.intBinary(WasmIntType.INT64, WasmIntBinaryOperation.SUB);
            }
            case FLOAT -> {
                builder.getLocal(mapToLocal(insn.getOperand()));
                builder.floatUnary(WasmFloatType.FLOAT32, WasmFloatUnaryOperation.NEG);
            }
            case DOUBLE -> {
                builder.getLocal(mapToLocal(insn.getOperand()));
                builder.floatUnary(WasmFloatType.FLOAT64, WasmFloatUnaryOperation.NEG);
            }
        }
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(AssignInstruction insn) {
        builder.getLocal(mapToLocal(insn.getAssignee()));
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(CastInstruction insn) {
        builder.getLocal(mapToLocal(insn.getValue()));
        generateCast(insn);
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    private void generateCast(CastInstruction insn) {
        if (insn.getTargetType() instanceof ValueType.Object objTarget) {
            var className = objTarget.getClassName();
            if (context.classInfoProvider().getClassInfo(className).isHeapStructure()) {
                return;
            }
        }

        var sourceType = context.typeMapper().mapType(typeInference.typeOf(insn.getValue()).valueType);
        if (!(sourceType instanceof WasmType.Reference sourceRef)) {
            return;
        }

        var targetType = (WasmType.Reference) context.typeMapper().mapType(insn.getTargetType());
        WasmStructure targetStruct = null;
        if (targetType instanceof WasmType.CompositeReference targetTypeRef) {
            var composite = targetTypeRef.composite;
            if (composite instanceof WasmStructure) {
                targetStruct = (WasmStructure) composite;
            }
        }

        var canInsertCast = true;
        if (targetStruct != null && sourceRef instanceof WasmType.CompositeReference sourceComposite) {
            var sourceStruct = sourceComposite.composite instanceof WasmStructure
                    ? (WasmStructure) sourceComposite.composite : null;
            if (sourceStruct != null) {
                if (targetStruct.isSupertypeOf(sourceStruct)) {
                    canInsertCast = false;
                } else if (!sourceStruct.isSupertypeOf(targetStruct)) {
                    generateThrowCce(builder);
                    return;
                }
            }
        }

        if (!insn.isWeak() && context.isStrict()) {
            if (canCastNatively(insn.getTargetType())) {
                if (canInsertCast) {
                    var block = builder.block(context.functionTypes().of(targetType, sourceRef).asBlock());
                    block.castBranch(WasmCastCondition.SUCCESS, sourceRef, targetType, block);
                    generateThrowCce(block);
                }
            } else {
                var objectClass = context.classInfoProvider().getClassInfo("java.lang.Object");
                var vtStruct = objectClass.getVirtualTableStructure();
                var block = builder.block(context.functionTypes().of(null, sourceType).asBlock());
                block
                        .nullBranch(WasmNullCondition.NULL, block)
                        .structGet(objectClass.getStructure(), WasmGCClassInfoProvider.VT_FIELD_OFFSET)
                        .structGet(vtStruct, WasmGCClassInfoProvider.CLASS_FIELD_OFFSET);
                WasmGCGenerationUtil.emitClassInfoLiteral(context.classInfoProvider(), block, insn.getTargetType());
                block
                        .call(context.supertypeFunctions().getIsSupertypeFunction(insn.getTargetType()))
                        .branch(block);
                generateThrowCce(block);
                builder.getLocal(mapToLocal(insn.getValue()));
                if (canInsertCast) {
                    builder.cast(targetType);
                }
            }
        } else if (canInsertCast) {
            builder.cast(targetType);
        }
    }

    @Override
    public void visit(CastNumberInstruction insn) {
        builder.getLocal(mapToLocal(insn.getValue()));
        builder.nonTrapConvert(WasmGeneratorUtil.mapType(insn.getSourceType()),
                WasmGeneratorUtil.mapType(insn.getTargetType()), true);
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(CastIntegerInstruction insn) {
        builder.getLocal(mapToLocal(insn.getValue()));
        switch (insn.getDirection()) {
            case FROM_INTEGER -> {
                switch (insn.getTargetType()) {
                    case BYTE -> {
                        builder
                                .i32Const(24)
                                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL)
                                .i32Const(24)
                                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHR_SIGNED);
                    }
                    case SHORT -> {
                        builder
                                .i32Const(16)
                                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL)
                                .i32Const(16)
                                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHR_SIGNED);
                    }
                    case CHAR -> {
                        builder
                                .i32Const(16)
                                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL)
                                .i32Const(16)
                                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHR_UNSIGNED);
                    }
                }
            }
            case TO_INTEGER -> {
                // Do nothing
            }
        }
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(BranchingInstruction insn) {
        boolean inverted;
        BasicBlock breakTarget;
        BasicBlock continueTarget;
        if (labels[insn.getConsequent().getIndex()] != null) {
            breakTarget = insn.getConsequent();
            continueTarget = insn.getAlternative();
            inverted = false;
        } else {
            assert labels[insn.getAlternative().getIndex()] != null;
            breakTarget = insn.getAlternative();
            continueTarget = insn.getConsequent();
            inverted = true;
        }

        builder.getLocal(mapToLocal(insn.getOperand()));
        switch (insn.getCondition()) {
            case EQUAL -> {
                if (!inverted) {
                    builder.intUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ);
                }
            }
            case NOT_EQUAL -> {
                if (inverted) {
                    builder.intUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ);
                }
            }
            case LESS -> {
                builder.i32Const(0);
                builder.intBinary(WasmIntType.INT32, !inverted
                        ? WasmIntBinaryOperation.LT_SIGNED
                        : WasmIntBinaryOperation.GE_SIGNED);
            }
            case LESS_OR_EQUAL -> {
                builder.i32Const(0);
                builder.intBinary(WasmIntType.INT32, !inverted
                        ? WasmIntBinaryOperation.LE_SIGNED
                        : WasmIntBinaryOperation.GT_SIGNED);
            }
            case GREATER -> {
                builder.i32Const(0);
                builder.intBinary(WasmIntType.INT32, !inverted
                        ? WasmIntBinaryOperation.GT_SIGNED
                        : WasmIntBinaryOperation.LE_SIGNED);
            }
            case GREATER_OR_EQUAL -> {
                builder.i32Const(0);
                builder.intBinary(WasmIntType.INT32, !inverted
                        ? WasmIntBinaryOperation.GE_SIGNED
                        : WasmIntBinaryOperation.LT_SIGNED);
            }
            case NULL -> {
                if (isNativeReference(insn.getOperand())) {
                    if (!inverted) {
                        builder.intUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ);
                    }
                } else {
                    builder.isNull();
                    if (inverted) {
                        builder.intUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ);
                    }
                }
            }
            case NOT_NULL -> {
                if (isNativeReference(insn.getOperand())) {
                    if (inverted) {
                        builder.intUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ);
                    }
                } else {
                    builder.isNull();
                    if (!inverted) {
                        builder.intUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ);
                    }
                }
            }
        }

        branchEither(breakTarget, continueTarget);
    }

    @Override
    public void visit(BinaryBranchingInstruction insn) {
        boolean inverted;
        BasicBlock breakTarget;
        BasicBlock continueTarget;
        if (labels[insn.getConsequent().getIndex()] != null) {
            breakTarget = insn.getConsequent();
            continueTarget = insn.getAlternative();
            inverted = false;
        } else {
            assert labels[insn.getAlternative().getIndex()] != null;
            breakTarget = insn.getAlternative();
            continueTarget = insn.getConsequent();
            inverted = true;
        }

        builder.getLocal(mapToLocal(insn.getFirstOperand()));
        builder.getLocal(mapToLocal(insn.getSecondOperand()));
        switch (insn.getCondition()) {
            case EQUAL -> {
                builder.intBinary(WasmIntType.INT32, !inverted
                        ? WasmIntBinaryOperation.EQ
                        : WasmIntBinaryOperation.SUB);
            }
            case NOT_EQUAL -> {
                builder.intBinary(WasmIntType.INT32, !inverted
                        ? WasmIntBinaryOperation.SUB
                        : WasmIntBinaryOperation.EQ);
            }
            case REFERENCE_EQUAL -> {
                if (isNativeReference(insn.getFirstOperand())) {
                    builder.intBinary(WasmIntType.INT32, !inverted
                            ? WasmIntBinaryOperation.EQ
                            : WasmIntBinaryOperation.NE);
                } else {
                    builder.refEqual();
                    if (inverted) {
                        builder.intUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ);
                    }
                }
            }
            case REFERENCE_NOT_EQUAL -> {
                if (isNativeReference(insn.getFirstOperand())) {
                    builder.intBinary(WasmIntType.INT32, !inverted
                            ? WasmIntBinaryOperation.NE
                            : WasmIntBinaryOperation.EQ);
                } else {
                    builder.refEqual();
                    if (!inverted) {
                        builder.intUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ);
                    }
                }
            }
        }

        branchEither(breakTarget, continueTarget);
    }

    private void branchEither(BasicBlock breakTarget, BasicBlock continueTarget) {
        if (!extractPhiArgs(breakTarget).isEmpty()) {
            var ifInsn = builder.conditional();
            var condBuilder = ifInsn.getThenBlock().builder();
            insertPhis(condBuilder, breakTarget);
            condBuilder.breakTo(labels[breakTarget.getIndex()]);
        } else {
            builder.branch(labels[breakTarget.getIndex()]);
        }
        insertPhis(builder, continueTarget);
        var continueLabel = labels[continueTarget.getIndex()];
        if (continueLabel != null) {
            builder.breakTo(continueLabel);
        }
    }

    @Override
    public void visit(JumpInstruction insn) {
        insertPhis(builder, insn.getTarget());
        var label = labels[insn.getTarget().getIndex()];
        if (label != null) {
            builder.breakTo(label);
        } else {
            assert expectedBasicBlock == null && !dontExpectBasicBlock;
            expectedBasicBlock = insn.getTarget();
        }
    }

    @Override
    public void visit(SwitchInstruction insn) {
        var min = 0;
        var max = 0;
        var first = true;
        for (var entry : insn.getEntries()) {
            if (first) {
                min = entry.getCondition();
                max = entry.getCondition();
                first = false;
            } else {
                min = Math.min(min, entry.getCondition());
                max = Math.max(max, entry.getCondition());
            }
        }

        var wrapper = builder.block();
        var targetLists = new HashMap<BasicBlock, WasmInstructionList>();
        var trampolines = new ArrayList<BasicBlock>();
        var allTargets = new ArrayList<BasicBlock>();
        for (var entry : insn.getEntries()) {
            allTargets.add(entry.getTarget());
        }
        allTargets.add(insn.getDefaultTarget());
        for (var target : allTargets) {
            if (targetLists.containsKey(target)) {
                continue;
            }
            var label = labels[target.getIndex()];
            if (extractPhiArgs(target).isEmpty()) {
                targetLists.put(target, label != null ? label : wrapper.list);
            } else {
                targetLists.put(target, null);
                trampolines.add(target);
            }
        }

        var current = wrapper;
        for (var target : trampolines) {
            var inner = current.block();
            targetLists.put(target, inner.list);
            insertPhis(current, target);
            var label = labels[target.getIndex()];
            current.breakTo(label != null ? label : wrapper.list);
            current = inner;
        }

        var clauseTargets = new WasmInstructionList[insn.getEntries().size()];
        for (var i = 0; i < clauseTargets.length; ++i) {
            clauseTargets[i] = targetLists.get(insn.getEntries().get(i).getTarget());
        }
        var defaultLabel = targetLists.get(insn.getDefaultTarget());
        var condLocal = mapToLocal(insn.getCondition());
        if (insn.getEntries().isEmpty()) {
            current.breakTo(defaultLabel);
        } else if ((long) max - min >= SWITCH_TABLE_THRESHOLD) {
            translateSwitchToBinarySearch(current, insn, defaultLabel, clauseTargets, condLocal);
        } else {
            translateSwitchToWasmSwitch(current, insn, defaultLabel, clauseTargets, min, max, condLocal);
        }
    }

    private void translateSwitchToBinarySearch(WasmInstructionBuilder builder, SwitchInstruction insn,
            WasmInstructionList defaultList, WasmInstructionList[] clauseLists, WasmLocal local) {
        var entries = new ArrayList<TableEntry>();
        for (int i = 0; i < insn.getEntries().size(); i++) {
            var entry = insn.getEntries().get(i);
            entries.add(new TableEntry(entry.getCondition(), clauseLists[i]));
        }
        entries.sort(Comparator.comparingInt(e -> e.label));
        generateBinarySearch(entries, 0, entries.size() - 1, builder, defaultList, local);
        builder.breakTo(defaultList);
    }

    private void generateBinarySearch(List<TableEntry> entries, int lower, int upper, WasmInstructionBuilder builder,
            WasmInstructionList defaultTarget, WasmLocal testVar) {
        if (upper - lower == 0) {
            int label = entries.get(lower).label;
            builder
                    .getLocal(testVar)
                    .i32Const(label)
                    .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ)
                    .branch(entries.get(lower).target)
                    .breakTo(defaultTarget);
        } else if (upper - lower <= 0) {
            builder.breakTo(defaultTarget);
        } else {
            int mid = (upper + lower) / 2;
            int label = entries.get(mid).label;
            builder
                    .getLocal(testVar)
                    .i32Const(label)
                    .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.GT_SIGNED);
            var cond = builder.conditional();
            generateBinarySearch(entries, mid + 1, upper, cond.getThenBlock().builder(), defaultTarget, testVar);
            generateBinarySearch(entries, lower, mid, cond.getElseBlock().builder(), defaultTarget, testVar);
        }
    }

    private void translateSwitchToWasmSwitch(WasmInstructionBuilder builder, SwitchInstruction insn,
            WasmInstructionList defaultTarget, WasmInstructionList[] clauseTargets, int min, int max,
            WasmLocal condLocal) {
        builder.getLocal(condLocal);
        if (min != 0) {
            builder.i32Const(min);
            builder.intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB);
        }

        var wasmSwitch = builder.switch_(defaultTarget);

        var expandedTargets = new WasmInstructionList[max - min + 1];
        for (int i = 0; i < insn.getEntries().size(); i++) {
            var clause = insn.getEntries().get(i);
            expandedTargets[clause.getCondition() - min] = clauseTargets[i];
        }

        for (var target : expandedTargets) {
            wasmSwitch.getTargets().add(target != null ? target : defaultTarget);
        }
    }

    static class TableEntry {
        final int label;
        final WasmInstructionList target;

        TableEntry(int label, WasmInstructionList target) {
            this.label = label;
            this.target = target;
        }
    }

    @Override
    public void visit(ExitInstruction insn) {
        assert !dontExpectBasicBlock && expectedBasicBlock == null;
        dontExpectBasicBlock = true;
        if (insn.getValueToReturn() != null) {
            builder.getLocal(mapToLocal(insn.getValueToReturn()));
        }
        builder.return_();
    }

    @Override
    public void visit(RaiseInstruction insn) {
        assert !dontExpectBasicBlock && expectedBasicBlock == null;
        dontExpectBasicBlock = true;
        builder.getLocal(mapToLocal(insn.getException()));
        builder.throw_(context.getExceptionTag());
    }

    @Override
    public void visit(ConstructArrayInstruction insn) {
        WasmGCGenerationUtil.emitClassInfoLiteral(context.classInfoProvider(), builder, insn.getItemType());
        builder.getLocal(mapToLocal(insn.getSize()));
        builder.call(context.classInfoProvider().getArrayConstructor(insn.getItemType()));
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(ConstructInstruction insn) {
        var classInfo = context.classInfoProvider().getClassInfo(insn.getType());

        builder.structNewDefault(classInfo.getStructure());
        builder.teeLocal(mapToLocal(insn.getReceiver()));
        builder
                .getGlobal(classInfo.getVirtualTablePointer())
                .structSet(classInfo.getStructure(), WasmGCClassInfoProvider.VT_FIELD_OFFSET);
    }

    @Override
    public void visit(ConstructMultiArrayInstruction insn) {
        WasmGCGenerationUtil.emitClassInfoLiteral(context.classInfoProvider(), builder, insn.getItemType());
        for (var dimension : insn.getDimensions()) {
            builder.getLocal(mapToLocal(dimension));
        }

        builder.call(context.classInfoProvider().getMultiArrayConstructor(insn.getDimensions().size()));
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(GetFieldInstruction insn) {
        if (insn.getInstance() == null) {
            var global = context.classInfoProvider().getStaticFieldLocation(insn.getField());
            builder.getGlobal(global);
        } else {
            var cls = context.classes().get(insn.getField().getClassName());
            if (cls == null) {
                builder.unreachable();
                return;
            }
            var classInfo = context.classInfoProvider().getClassInfo(cls.getName());
            builder.getLocal(mapToLocal(insn.getInstance()));
            if (classInfo.isHeapStructure()) {
                var offset = context.classInfoProvider().getHeapFieldOffset(insn.getField());
                var fieldReader = cls.getField(insn.getField().getFieldName());
                if (fieldReader != null) {
                    loadHeapField(fieldReader.getType(), offset);
                } else {
                    builder.unreachable();
                }
            } else {
                loadNormalField(insn.getField(), classInfo);
            }
        }
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    private void loadNormalField(FieldReference fieldRef, WasmGCClassInfo classInfo) {
        var fieldIndex = context.classInfoProvider().getFieldIndex(fieldRef);
        if (fieldIndex < 0) {
            builder.unreachable();
            return;
        }
        var struct = classInfo.getStructure();
        var cls = context.classes().get(fieldRef.getClassName());
        if (cls != null) {
            var field = cls.getField(fieldRef.getFieldName());
            if (field != null) {
                var fieldType = field.getType();
                if (fieldType instanceof ValueType.Primitive primitiveType) {
                    switch (primitiveType.getKind()) {
                        case BOOLEAN:
                        case CHARACTER:
                            builder.structGet(struct, fieldIndex, WasmSignedType.UNSIGNED);
                            return;
                        case BYTE:
                        case SHORT:
                            builder.structGet(struct, fieldIndex, WasmSignedType.SIGNED);
                            return;
                        default:
                            break;
                    }
                }
            }
        }
        builder.structGet(struct, fieldIndex);
    }

    private void loadHeapField(ValueType type, int offset) {
        if (type instanceof ValueType.Primitive primitiveType) {
            switch (primitiveType.getKind()) {
                case BOOLEAN:
                case BYTE:
                    builder.loadI32(1, offset, WasmInt32Subtype.INT8);
                    return;
                case CHARACTER:
                    builder.loadI32(2, offset, WasmInt32Subtype.UINT16);
                    return;
                case SHORT:
                    builder.loadI32(2, offset, WasmInt32Subtype.INT16);
                    return;
                case INTEGER:
                    builder.loadI32(4, offset, WasmInt32Subtype.INT32);
                    return;
                case LONG:
                    builder.loadI64(8, offset, WasmInt64Subtype.INT64);
                    return;
                case FLOAT:
                    builder.loadF32(4, offset);
                    return;
                case DOUBLE:
                    builder.loadF64(8, offset);
                    return;
                default:
                    break;
            }
        }
        builder.loadI32(4, offset, WasmInt32Subtype.INT32);
    }

    @Override
    public void visit(PutFieldInstruction insn) {
        if (insn.getInstance() == null) {
            var global = context.classInfoProvider().getStaticFieldLocation(insn.getField());
            builder.getLocal(mapToLocal(insn.getValue()));
            builder.setGlobal(global);
        } else {
            var cls = context.classes().get(insn.getField().getClassName());
            if (cls == null) {
                builder.unreachable();
                return;
            }
            var fieldReader = cls.getField(insn.getField().getFieldName());
            if (fieldReader == null) {
                builder.unreachable();
                return;
            }
            builder.getLocal(mapToLocal(insn.getInstance())).getLocal(mapToLocal(insn.getValue()));
            var classInfo = context.classInfoProvider().getClassInfo(insn.getField().getClassName());
            if (!classInfo.isHeapStructure()) {
                storeNormalField(classInfo, insn.getField());
            } else {
                storeHeapField(insn.getField());
            }
        }
    }

    private void storeNormalField(WasmGCClassInfo classInfo, FieldReference fieldRef) {
        var fieldIndex = context.classInfoProvider().getFieldIndex(fieldRef);
        if (fieldIndex >= 0) {
            builder.structSet(classInfo.getStructure(), fieldIndex);
        } else {
            builder.unreachable();
        }
    }

    private void storeHeapField(FieldReference field) {
        var cls = context.classes().get(field.getClassName());
        var type = cls.getField(field.getFieldName()).getType();
        var offset = context.classInfoProvider().getHeapFieldOffset(field);
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    builder.storeI32(1, offset, WasmInt32Subtype.INT8);
                    return;
                case CHARACTER:
                    builder.storeI32(2, offset, WasmInt32Subtype.UINT16);
                    return;
                case SHORT:
                    builder.storeI32(2, offset, WasmInt32Subtype.INT16);
                    return;
                case INTEGER:
                    builder.storeI32(4, offset, WasmInt32Subtype.INT32);
                    return;
                case LONG:
                    builder.storeI64(8, offset, WasmInt64Subtype.INT64);
                    return;
                case FLOAT:
                    builder.storeF32(4, offset);
                    return;
                case DOUBLE:
                    builder.storeF64(8, offset);
                    return;
                default:
                    break;
            }
        }
        builder.storeI32(4, offset, WasmInt32Subtype.INT32);
    }

    @Override
    public void visit(ArrayLengthInstruction insn) {
        builder.getLocal(mapToLocal(insn.getArray()));
        builder.arrayLength();
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(CloneArrayInstruction insn) {

    }

    @Override
    public void visit(UnwrapArrayInstruction insn) {
        builder.getLocal(mapToLocal(insn.getArray()));
        var type = typeInference.typeOf(insn.getArray());
        var arrayType = (WasmType.CompositeReference) context.typeMapper().mapType(type.valueType);
        var arrayStruct = (WasmStructure) arrayType.composite;
        builder.structGet(arrayStruct, WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(GetElementInstruction insn) {
        var arrayValueType = typeInference.typeOf(insn.getArray()).valueType;
        var arrayType = (WasmType.CompositeReference) context.typeMapper().mapType(arrayValueType);
        var arrayWrapperStruct = (WasmStructure) arrayType.composite;
        var arrayFieldType = (WasmType.CompositeReference) arrayWrapperStruct.getFields()
                .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET).getType().asUnpackedType();
        var wasmArray = (WasmArray) arrayFieldType.composite;
        builder.getLocal(mapToLocal(insn.getArray())).getLocal(mapToLocal(insn.getIndex()));
        switch (insn.getType()) {
            case BYTE:
                builder.arrayGet(wasmArray, WasmSignedType.SIGNED);
                break;
            case SHORT:
                builder.arrayGet(wasmArray, WasmSignedType.SIGNED);
                break;
            case CHAR:
                builder.arrayGet(wasmArray, WasmSignedType.UNSIGNED);
                break;
            default:
                builder.arrayGet(wasmArray);
                break;
        }
        if (insn.getType() == ArrayElementType.OBJECT) {
            var targetType = typeInference.typeOf(insn.getReceiver());
            var wasmTargetType = (WasmType.Reference) context.typeMapper().mapType(targetType.valueType);
            if (!isExtern(wasmTargetType)) {
                builder.cast(wasmTargetType);
            }
        }
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(PutElementInstruction insn) {
        builder.getLocal(mapToLocal(insn.getArray())).getLocal(mapToLocal(insn.getIndex()))
                .getLocal(mapToLocal(insn.getValue()));
        var array = switch (insn.getType()) {
            case BYTE -> {
                yield context.classInfoProvider().getPrimitiveArrayType(WasmStorageType.INT8);
            }
            case SHORT, CHAR -> {
                yield context.classInfoProvider().getPrimitiveArrayType(WasmStorageType.INT16);
            }
            case INT -> {
                yield context.classInfoProvider().getPrimitiveArrayType(WasmType.INT32.asStorage());
            }
            case LONG -> {
                yield context.classInfoProvider().getPrimitiveArrayType(WasmType.INT64.asStorage());
            }
            case FLOAT -> {
                yield context.classInfoProvider().getPrimitiveArrayType(WasmType.FLOAT32.asStorage());
            }
            case DOUBLE -> {
                yield context.classInfoProvider().getPrimitiveArrayType(WasmType.FLOAT64.asStorage());
            }
            default -> {
                yield context.classInfoProvider().getObjectArrayType();
            }
        };
        builder.arraySet(array);
    }

    @Override
    public void visit(InvokeInstruction insn) {
        var csIntrinsic = context.callSiteIntrinsics().getIntrinsic(insn.getMethod());
        if (csIntrinsic != null) {
            csIntrinsic.apply(insn, intrinsicContext, builder);
        } else if (insn.getType() == InvocationType.SPECIAL) {
            var method = context.classes().resolve(insn.getMethod());
            var reference = method != null ? method.getReference() : insn.getMethod();
            var function = insn.getInstance() == null
                    ? context.functions().forStaticMethod(reference)
                    : context.functions().forInstanceMethod(reference);

            if (insn.getInstance() != null) {
                builder.getLocal(mapToLocal(insn.getInstance()));
                forceType(insn.getInstance(), ValueType.object(method.getOwnerName()));
            }
            for (var arg : insn.getArguments()) {
                builder.getLocal(mapToLocal(arg));
            }
            builder.call(function, isAsyncSplit(reference));
        } else {
            virtualCall(insn);
        }
        if (insn.getReceiver() != null) {
            builder.setLocal(mapToLocal(insn.getReceiver()));
        } else if (insn.getMethod().getReturnType() != ValueType.VOID) {
            builder.drop();
        }
    }

    private void virtualCall(InvokeInstruction insn) {
        var vtable = context.virtualTables().lookup(insn.getMethod().getClassName());
        if (vtable == null) {
            builder.unreachable();
            return;
        }

        var entry = vtable.entry(insn.getMethod().getDescriptor());
        var nonInterfaceAncestor = vtable.closestNonInterfaceAncestor();
        if (entry == null || nonInterfaceAncestor == null) {
            builder.unreachable();
            return;
        }

        var objectClass = context.classInfoProvider().getClassInfo("java.lang.Object");
        var instanceValueType = ValueType.object(insn.getMethod().getClassName());
        var instanceType = (WasmType.CompositeReference) context.typeMapper().mapType(instanceValueType);

        var index = WasmGCClassInfoProvider.VIRTUAL_METHOD_OFFSET + entry.getIndex();
        var expectedInstanceClassInfo = context.classInfoProvider().getClassInfo(vtable.getClassName());
        var expectedInstanceClassStruct = context.classInfoProvider().getClassInfo(
                nonInterfaceAncestor.getClassName()).getStructure();
        var vtableStruct = expectedInstanceClassInfo.getVirtualTableStructure();
        var instanceStruct = (WasmStructure) instanceType.composite;

        builder.getLocal(mapToLocal(insn.getInstance()));
        if (!expectedInstanceClassStruct.isSupertypeOf(instanceStruct)) {
            builder.cast(expectedInstanceClassStruct.getNonNullReference());
        }
        for (var arg : insn.getArguments()) {
            builder.getLocal(mapToLocal(arg));
        }
        builder.getLocal(mapToLocal(insn.getInstance()));

        builder
                .structGet(objectClass.getStructure(), WasmGCClassInfoProvider.VT_FIELD_OFFSET)
                .cast(vtableStruct.getNonNullReference())
                .structGet(vtableStruct, index);

        var functionTypeRef = (WasmType.CompositeReference) vtableStruct.getFields().get(index).getUnpackedType();
        builder.callReference((WasmFunctionType) functionTypeRef.composite, isAsyncSplit(insn.getMethod()));

    }

    private void forceType(Variable variable, ValueType expectedValueType) {
        var actualValueType = typeInference.typeOf(variable).valueType;
        if (expectedValueType.equals(actualValueType)) {
            return;
        }
        var expectedType = context.typeMapper().mapType(actualValueType);
        var actualWasmType = context.typeMapper().mapType(actualValueType);
        if (!(actualWasmType instanceof WasmType.CompositeReference actualWasmRef)) {
            return;
        }
        var actualComposite = actualWasmRef.composite;
        var expectedComposite = ((WasmType.CompositeReference) expectedType).composite;
        if (!(actualComposite instanceof WasmStructure actualStruct)
                || !(expectedComposite instanceof WasmStructure expectedStruct)) {
            return;
        }

        if (actualStruct == expectedStruct || !actualStruct.isSupertypeOf(expectedStruct)) {
            return;
        }

        builder.cast(expectedStruct.getReference());
    }

    @Override
    public void visit(InvokeDynamicInstruction insn) {
        context.diagnostics().error(new CallLocation(currentMethod, insn.getLocation()),
                "InvokeDynamic instruction should have been eliminated by previous lowering phases");
    }

    @Override
    public void visit(IsInstanceInstruction insn) {
        var type = insn.getType();
        if (canCastNatively(type)) {
            var wasmType = context.classInfoProvider().getClassInfo(type).getStructure().getNonNullReference();
            builder.getLocal(mapToLocal(insn.getValue()));
            builder.test(wasmType);
        } else {
            var block = builder.block(WasmType.INT32);

            var sourceValueType = typeInference.typeOf(insn.getValue()).valueType;
            var sourceType = context.typeMapper().mapType(sourceValueType);

            if (sourceType instanceof WasmType.Reference ref) {
                // Get the class ref and call supertype check
                var objectClass = context.classInfoProvider().getClassInfo("java.lang.Object");
                var vtStruct = objectClass.getVirtualTableStructure();

                block.getLocal(mapToLocal(insn.getValue()));
                var innerBlock = block.block(context.functionTypes().of(null, ref).asBlock());
                innerBlock
                        .nullBranch(WasmNullCondition.NULL, innerBlock)
                        .structGet(objectClass.getStructure(), WasmGCClassInfoProvider.VT_FIELD_OFFSET)
                        .structGet(vtStruct, WasmGCClassInfoProvider.CLASS_FIELD_OFFSET);
                WasmGCGenerationUtil.emitClassInfoLiteral(context.classInfoProvider(), innerBlock, type);
                innerBlock
                        .call(context.supertypeFunctions().getIsSupertypeFunction(type))
                        .breakTo(block);
            }
            block.i32Const(0);
        }
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(InitClassInstruction insn) {
        var isSuspend = isAsyncSplit(new MethodReference(insn.getClassName(), CLINIT));
        var pointer = context.classInfoProvider().getClassInfo(insn.getClassName()).getInitializerPointer();
        builder.getGlobal(pointer);
        builder.callReference(context.functionTypes().of(null), isSuspend);
    }

    @Override
    public void visit(NullCheckInstruction insn) {
        builder.getLocal(mapToLocal(insn.getValue()));
        var valueType = typeInference.typeOf(insn.getValue()).valueType;
        var type = context.typeMapper().mapType(valueType);
        if (type instanceof WasmType.Reference) {
            var blockBuilder = builder.block(context.functionTypes().of(type, type).asBlock());
            blockBuilder.nullBranch(WasmNullCondition.NOT_NULL, blockBuilder);
            generateThrowNpe(blockBuilder);
        }
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(MonitorEnterInstruction insn) {
        builder.getLocal(mapToLocal(insn.getObjectRef()));
        monitorEnter(builder);
    }

    @Override
    public void visit(MonitorExitInstruction insn) {
        builder.getLocal(mapToLocal(insn.getObjectRef()));
        monitorExit(builder);
    }

    @Override
    public void visit(BoundCheckInstruction insn) {
        var block = builder.block();

        if (insn.getArray() != null) {
            var condBlock = block;
            if (insn.isLower()) {
                condBlock = block.block();
                condBlock
                        .getLocal(mapToLocal(insn.getIndex()))
                        .i32Const(0)
                        .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED)
                        .branch(condBlock.list);
            }

            condBlock.getLocal(mapToLocal(insn.getIndex()));
            condBlock
                    .getLocal(mapToLocal(insn.getArray()))
                    .arrayLength()
                    .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED)
                    .branch(block.list);
        } else if (insn.isLower()) {
            block
                    .getLocal(mapToLocal(insn.getIndex()))
                    .i32Const(0)
                    .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.GE_SIGNED)
                    .branch(block.list);
        }

        generateThrowAioobe(block);

        builder.getLocal(mapToLocal(insn.getIndex()));
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    public void monitorEnter(WasmInstructionBuilder builder) {
        builder.call(context.functions().forStaticMethod(async ? MONITOR_ENTER : MONITOR_ENTER_SYNC), async);
    }

    public void monitorExit(WasmInstructionBuilder builder) {
        builder.call(context.functions().forStaticMethod(async ? MONITOR_EXIT : MONITOR_EXIT_SYNC), async);
    }

    private boolean canCastNatively(ValueType type) {
        if (type instanceof ValueType.Array) {
            // arrays with non-primitive item types all share the Array<java.lang.Object>
            // structure, so ref.test can only distinguish primitive arrays and Object[]
            // itself; other item types need a class check
            var itemType = ((ValueType.Array) type).getItemType();
            return itemType instanceof ValueType.Primitive
                    || itemType.equals(ValueType.object("java.lang.Object"));
        }
        if (!(type instanceof ValueType.Object)) {
            return false;
        }
        var className = ((ValueType.Object) type).getClassName();
        var cls = context.classes().get(className);
        if (cls == null) {
            return false;
        }
        return !cls.hasModifier(ElementModifier.INTERFACE);
    }

    // Computes (a > b) - (a < b), taking into account NaN handling for floating-point numbers:
    // for COMPARE_GREATER NaN produces 1, for COMPARE_LESS it produces -1
    private void generateCompare(BinaryInstruction insn) {
        var first = mapToLocal(insn.getFirstOperand());
        var second = mapToLocal(insn.getSecondOperand());
        switch (insn.getOperandType()) {
            case INT, LONG -> {
                var type = insn.getOperandType() == NumericOperandType.INT ? WasmIntType.INT32 : WasmIntType.INT64;
                builder
                        .getLocal(first)
                        .getLocal(second)
                        .intBinary(type, WasmIntBinaryOperation.GT_SIGNED)
                        .getLocal(first)
                        .getLocal(second)
                        .intBinary(type, WasmIntBinaryOperation.LT_SIGNED)
                        .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB);
            }
            case FLOAT, DOUBLE -> {
                var type = insn.getOperandType() == NumericOperandType.FLOAT
                        ? WasmFloatType.FLOAT32
                        : WasmFloatType.FLOAT64;
                if (insn.getOperation() == BinaryOperation.COMPARE_GREATER) {
                    builder
                            .getLocal(first)
                            .getLocal(second)
                            .floatBinary(type, WasmFloatBinaryOperation.LE)
                            .intUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ)
                            .getLocal(first)
                            .getLocal(second)
                            .floatBinary(type, WasmFloatBinaryOperation.LT)
                            .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB);
                } else {
                    builder
                            .getLocal(first)
                            .getLocal(second)
                            .floatBinary(type, WasmFloatBinaryOperation.GT)
                            .getLocal(first)
                            .getLocal(second)
                            .floatBinary(type, WasmFloatBinaryOperation.GE)
                            .intUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ)
                            .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB);
                }
            }
        }
    }

    private boolean isNativeReference(Variable variable) {
        var type = typeInference.typeOf(variable);
        return context.typeMapper().mapType(type.valueType) == WasmType.INT32;
    }

    private void insertPhis(WasmInstructionBuilder builder, BasicBlock target) {
        for (var arg : extractPhiArgs(target)) {
            builder.getLocal(mapToLocal(arg));
        }
        readPhis(builder, target);
    }

    private void readPhis(WasmInstructionBuilder builder, BasicBlock target) {
        var phis = target.getPhis();
        for (var i = phis.size() - 1; i >= 0; --i) {
            builder.setLocal(mapToLocal(phis.get(i).getReceiver()));
        }
    }

    private List<Variable> extractPhiArgs(BasicBlock target) {
        var phiArgs = new ArrayList<Variable>();
        for (var phi : target.getPhis()) {
            for (var incoming : phi.getIncomings()) {
                if (incoming.getSource() == currentBlock) {
                    phiArgs.add(incoming.getValue());
                }
            }
        }
        return phiArgs;
    }

    private void generateThrowNpe(WasmInstructionBuilder target) {
        target.call(context.npeMethod());
        target.throw_(context.getExceptionTag());
    }

    private void generateThrowAioobe(WasmInstructionBuilder target) {
        target.call(context.aaiobeMethod());
        target.throw_(context.getExceptionTag());
    }

    private void generateThrowCce(WasmInstructionBuilder target) {
        target.call(context.cceMethod());
        target.throw_(context.getExceptionTag());
    }

    protected boolean isAsyncSplit(MethodReference methodRef) {
        if (!async) {
            return false;
        }
        return isAsyncSplitImpl(findRealMethod(methodRef));
    }

    private boolean isAsyncSplitImpl(MethodReference methodRef) {
        if (asyncSplitMethods.isEmpty()) {
            return false;
        }
        if (asyncSplitMethods.contains(methodRef)) {
            return true;
        }

        var cls = context.classes().get(methodRef.getClassName());
        if (cls == null) {
            return false;
        }

        if (cls.getParent() != null) {
            if (isAsyncSplitImpl(new MethodReference(cls.getParent(), methodRef.getDescriptor()))) {
                return true;
            }
        }
        for (var itf : cls.getInterfaces()) {
            if (isAsyncSplitImpl(new MethodReference(itf, methodRef.getDescriptor()))) {
                return true;
            }
        }

        return false;
    }

    private MethodReference findRealMethod(MethodReference method) {
        var clsName = method.getClassName();
        while (clsName != null) {
            var cls = context.classes().get(clsName);
            if (cls == null) {
                break;
            }
            var methodReader = cls.getMethod(method.getDescriptor());
            if (methodReader != null) {
                return new MethodReference(clsName, method.getDescriptor());
            }
            clsName = cls.getParent();
            if (clsName != null && clsName.equals(cls.getName())) {
                break;
            }
        }
        return method;
    }

    private static boolean isExtern(WasmType.Reference type) {
        return type instanceof WasmType.SpecialReference ref && ref.kind == WasmType.SpecialReferenceKind.EXTERN;
    }


    private WasmLocal mapToLocal(Variable variable) {
        return currentWasmFunction.getLocalVariables().get(variable.getIndex() - currentFirstVariable);
    }

    private WasmGCInlineIntrinsicContext intrinsicContext = new WasmGCInlineIntrinsicContext() {
        @Override
        public WasmLocal mapToLocal(Variable variable) {
            return WasmCodeGenerator.this.mapToLocal(variable);
        }

        @Override
        public boolean isAsync() {
            return WasmCodeGenerator.this.async;
        }

        @Override
        public boolean isAsyncMethod(MethodReference method) {
            return asyncSplitMethods.contains(method);
        }

        @Override
        public MethodReference currentMethod() {
            return currentMethod;
        }

        @Override
        public Program currentProgram() {
            return currentProgram;
        }

        @Override
        public WasmLocal newWasmLocal(WasmType type) {
            var local = new WasmLocal(type);
            currentWasmFunction.add(local);
            return local;
        }

        @Override
        public PreciseTypeInference types() {
            return typeInference;
        }

        @Override
        public Instruction definition(Variable variable) {
            ensureDefinitions();
            return definitions[variable.getIndex()];
        }
    };

    private void ensureDefinitions() {
        if (definitions != null) {
            return;
        }
        definitions = new Instruction[currentProgram.variableCount()];
        var graphBuilder = new GraphBuilder(currentProgram.variableCount());
        var stack = new IntStack();
        var definitionExtractor = new DefinitionExtractor();
        for (var block : currentProgram.getBasicBlocks()) {
            for (var insn : block) {
                if (insn instanceof AssignInstruction assign) {
                    graphBuilder.addEdge(assign.getAssignee().getIndex(), assign.getReceiver().getIndex());
                } else {
                    insn.acceptVisitor(definitionExtractor);
                    for (var definedVar : definitionExtractor.getDefinedVariables()) {
                        definitions[definedVar.getIndex()] = insn;
                        stack.push(definedVar.getIndex());
                    }
                }
            }
        }
        var graph = graphBuilder.build();
        while (!stack.isEmpty()) {
            var node = stack.pop();
            for (var successor : graph.outgoingEdges(node)) {
                if (definitions[successor] == null) {
                    definitions[successor] = definitions[node];
                    stack.push(successor);
                }
            }
        }
    }
}
