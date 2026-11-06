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
import java.util.HashSet;
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
import org.teavm.model.Phi;
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
import org.teavm.model.util.UsageExtractor;

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
    private int[] usageCounts;
    private int[] branchConditionCounts;
    private Comparison[] comparisons;
    // For each variable defined by a constant: emitter of this constant. Such instructions are not emitted
    // at the place of definition, instead, constant is materialized at each usage
    private ConstantEmitter[] constants;
    private UsageExtractor usageExtractor = new UsageExtractor();
    private List<Variable> handlerPhiValues = new ArrayList<>();
    private List<Variable> handlerPhiReceivers = new ArrayList<>();
    private IntHashSet handlerDefinedVariables = new IntHashSet();
    private DefinitionExtractor handlerDefinitionExtractor = new DefinitionExtractor();
    private WasmInstructionList returnBlock;
    private List<Set<Phi>> targetPhis = new ArrayList<>();

    public WasmCodeGenerator(
            WasmGCGenerationContext context,
            Set<MethodReference> asyncSplitMethods
    ) {
        this.context = context;
        this.asyncSplitMethods = asyncSplitMethods;
    }

    public void setReturnBlock(WasmInstructionList returnBlock) {
        this.returnBlock = returnBlock;
    }

    public void generate(Program program, MethodReference currentMethod, WasmFunction function,
            WasmInstructionList rootBuilder, int firstVariable, boolean async) {
        this.async = async;
        this.currentMethod = currentMethod;
        this.currentProgram = program;
        this.currentWasmFunction = function;
        targetPhis.addAll(Collections.nCopies(program.variableCount(), null));
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
            var local = new WasmLocal(wasmType, program.variableAt(i).getDebugName());
            function.add(local);
        }
        labels = new WasmInstructionList[program.basicBlockCount()];
        exceptionHandlers = new boolean[program.basicBlockCount()];
        usageCounts = new int[program.variableCount()];
        branchConditionCounts = new int[program.variableCount()];
        comparisons = new Comparison[program.variableCount()];
        constants = new ConstantEmitter[program.variableCount()];
        for (var block : program.getBasicBlocks()) {
            for (var tryCatch : block.getTryCatchBlocks()) {
                exceptionHandlers[tryCatch.getHandler().getIndex()] = true;
            }
            for (var insn : block) {
                insn.acceptVisitor(usageExtractor);
                if (usageExtractor.getUsedVariables() != null) {
                    for (var usage : usageExtractor.getUsedVariables()) {
                        usageCounts[usage.getIndex()]++;
                    }
                }
                if (insn instanceof BranchingInstruction branch) {
                    switch (branch.getCondition()) {
                        case NULL, NOT_NULL -> { }
                        default -> branchConditionCounts[branch.getOperand().getIndex()]++;
                    }
                }
            }
            for (var phi : block.getPhis()) {
                for (var input : phi.getIncomings()) {
                    var targetPhisForInput = targetPhis.get(input.getValue().getIndex());
                    if (targetPhisForInput == null) {
                        targetPhisForInput = new HashSet<>();
                        targetPhis.set(input.getValue().getIndex(), targetPhisForInput);
                    }
                    targetPhisForInput.add(phi);
                    usageCounts[input.getValue().getIndex()]++;
                }
            }
        }
        var flowRoots = flowReconstruction.reconstruct(program);
        builder = rootBuilder.builder();
        for (var root : flowRoots) {
            root.acceptVisitor(this);
        }
        // Function may end with a loop that never falls through, however, validator does not know that
        // and expects function result on the stack
        if (!function.getType().getReturnTypes().isEmpty() && !builder.isTerminating()) {
            builder.unreachable();
        }
        builder = null;
        labels = null;
        exceptionHandlers = null;
        typeInference = null;
        definitions = null;
        usageCounts = null;
        branchConditionCounts = null;
        comparisons = null;
        constants = null;
        targetPhis.clear();
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
        BasicBlock fallthroughHandler = null;
        WasmType.Reference fallthroughHandlerType = null;
        for (var tryCatchNode : tryCatchNodes) {
            if (labels[tryCatchNode.catchBlock.getIndex()] == null) {
                fallthroughHandler = tryCatchNode.catchBlock;
                fallthroughHandlerType = handlerType(fallthroughHandler);
                break;
            }
        }
        var throwableType = throwableType();
        var catchBlock = builder.block(fallthroughHandlerType);
        var tryWrapper = catchBlock.block(throwableType);
        var tryInsn = tryWrapper.try_();
        tryInsn.getCatches().add(new WasmCatchClause(context.getExceptionTag(), false, tryWrapper.list));

        var firstHandlerInsn = tryCatchNodes.get(0).catchBlock.getFirstInstruction();
        if (firstHandlerInsn != null) {
            catchBlock.setCurrentLocation(firstHandlerInsn.getLocation());
        }
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

        boolean needLabel = node.blockAfter != null && labels[node.blockAfter.getIndex()] == null;
        if (needLabel) {
            labels[node.blockAfter.getIndex()] = catchBlock.list;
        }
        builder = tryInsn.getBody().builder();
        for (var part : body) {
            part.acceptVisitor(this);
        }
        // Emit after body, so that it gets location of the last instruction of the body
        tryWrapper.setCurrentLocation(builder.getCurrentLocation());
        tryWrapper.unreachable();
        if (needLabel) {
            labels[node.blockAfter.getIndex()] = null;
        }
        builder = outerBuilder;
        if (fallthroughHandler != null) {
            storeException(fallthroughHandler);
        }
    }

    // Consume exception right after the block which produces it. Handler is not necessarily emitted
    // at this point, since blocks starting at the handler are opened before it, and a block can't
    // consume values from the enclosing stack.
    private void storeException(BasicBlock handler) {
        if (handler.getFirstInstruction() != null) {
            builder.setCurrentLocation(handler.getFirstInstruction().getLocation());
        }
        if (handler.getExceptionVariable() != null) {
            builder.setLocal(mapToLocal(handler.getExceptionVariable()));
        } else {
            builder.drop();
        }
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
        var needLabel = labels[node.jumpTarget.getIndex()] == null;
        if (needLabel) {
            labels[node.jumpTarget.getIndex()] = builder.list;
        }
        for (var part : node.body) {
            part.acceptVisitor(this);
        }
        if (isHandler && !builder.isTerminating()) {
            builder.unreachable();
        }
        if (needLabel) {
            labels[node.jumpTarget.getIndex()] = null;
        }
        builder = outerBuilder;
        if (isHandler) {
            storeException(node.jumpTarget);
        }
    }

    private void processBasicBlock(BasicBlock block) {
        //assert block == expectedBasicBlock || dontExpectBasicBlock;
        expectedBasicBlock = null;
        dontExpectBasicBlock = false;
        var hasHandlerPhis = collectHandlerPhis(block);
        if (hasHandlerPhis) {
            var firstInsn = block.getFirstInstruction();
            if (firstInsn != null) {
                builder.setCurrentLocation(firstInsn.getLocation());
            }
            for (var i = 0; i < handlerPhiValues.size(); ++i) {
                var value = handlerPhiValues.get(i);
                if (!handlerDefinedVariables.contains(value.getIndex())) {
                    emitLocal(builder, value).setLocal(mapToLocal(handlerPhiReceivers.get(i)));
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
                    emitLocal(builder, definedVar).setLocal(mapToLocal(handlerPhiReceivers.get(i)));
                }
            }
        }
    }

    @Override
    public void visit(EmptyInstruction insn) {
    }

    @Override
    public void visit(ClassConstantInstruction insn) {
        emitConstant(insn.getReceiver(), b -> {
            WasmGCGenerationUtil.emitClassLiteral(context.classInfoProvider(), b, insn.getConstant());
            return b;
        });
    }

    @Override
    public void visit(NullConstantInstruction insn) {
        var type = typeInference.typeOf(insn.getReceiver());
        var wasmType = context.typeMapper().mapType(type != null
                ? type.valueType
                : ValueType.object("java.lang.Object"));
        emitConstant(insn.getReceiver(), wasmType == WasmType.INT32
                ? b -> b.i32Const(0)
                : b -> b.nullConst((WasmType.Reference) wasmType));
    }

    @Override
    public void visit(IntegerConstantInstruction insn) {
        var value = insn.getConstant();
        emitConstant(insn.getReceiver(), b -> b.i32Const(value));
    }

    @Override
    public void visit(LongConstantInstruction insn) {
        var value = insn.getConstant();
        emitConstant(insn.getReceiver(), b -> b.i64Const(value));
    }

    @Override
    public void visit(FloatConstantInstruction insn) {
        var value = insn.getConstant();
        emitConstant(insn.getReceiver(), b -> b.f32Const(value));
    }

    @Override
    public void visit(DoubleConstantInstruction insn) {
        var value = insn.getConstant();
        emitConstant(insn.getReceiver(), b -> b.f64Const(value));
    }

    @Override
    public void visit(StringConstantInstruction insn) {
        var global = context.strings().getStringConstant(insn.getConstant()).global;
        emitConstant(insn.getReceiver(), b -> b.getGlobal(global));
    }

    private void emitConstant(Variable target, ConstantEmitter emitter) {
        if (hasPhisReadingAsInputMultipleTimes(target)) {
            emitter.emit(builder);
            builder.setLocal(mapToLocal(target));
        } else {
            constants[target.getIndex()] = emitter;
        }
    }

    private boolean hasPhisReadingAsInputMultipleTimes(Variable v) {
        var phis = targetPhis.get(v.getIndex());
        if (phis == null) {
            return false;
        }
        for (var phi : phis) {
            var count = 0;
            for (var input : phi.getIncomings()) {
                if (input.getValue() == v && ++count > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void visit(BinaryInstruction insn) {
        switch (insn.getOperation()) {
            case COMPARE_GREATER, COMPARE_LESS -> {
                // When every usage of comparison result is a branch condition, don't materialize the result,
                // but rather emit comparison of operands at each branch
                var receiverIndex = insn.getReceiver().getIndex();
                if (branchConditionCounts[receiverIndex] > 0
                        && branchConditionCounts[receiverIndex] == usageCounts[receiverIndex]) {
                    var comparisonType = switch (insn.getOperandType()) {
                        case INT -> ComparisonType.COMPARE_INT32;
                        case LONG -> ComparisonType.COMPARE_INT64;
                        case FLOAT -> insn.getOperation() == BinaryOperation.COMPARE_LESS
                                ? ComparisonType.COMPARE_FLOAT32_LESS
                                : ComparisonType.COMPARE_FLOAT32_GREATER;
                        case DOUBLE -> insn.getOperation() == BinaryOperation.COMPARE_LESS
                                ? ComparisonType.COMPARE_FLOAT64_LESS
                                : ComparisonType.COMPARE_FLOAT64_GREATER;
                    };
                    comparisons[insn.getReceiver().getIndex()] = new Comparison(insn.getFirstOperand(),
                            insn.getSecondOperand(), comparisonType);
                    return;
                } else {
                    generateCompare(insn);
                    builder.setLocal(mapToLocal(insn.getReceiver()));
                }
                return;
            }
            default -> { }
        }
        emitLocals(builder, insn.getFirstOperand(), insn.getSecondOperand());
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
                emitLocal(builder.i32Const(0), insn.getOperand());
                builder.intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB);
            }
            case LONG -> {
                emitLocal(builder.i64Const(0), insn.getOperand());
                builder.intBinary(WasmIntType.INT64, WasmIntBinaryOperation.SUB);
            }
            case FLOAT -> {
                emitLocal(builder, insn.getOperand());
                builder.floatUnary(WasmFloatType.FLOAT32, WasmFloatUnaryOperation.NEG);
            }
            case DOUBLE -> {
                emitLocal(builder, insn.getOperand());
                builder.floatUnary(WasmFloatType.FLOAT64, WasmFloatUnaryOperation.NEG);
            }
        }
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(AssignInstruction insn) {
        var constant = constants[insn.getAssignee().getIndex()];
        if (constant != null) {
            constants[insn.getReceiver().getIndex()] = constant;
            return;
        }
        emitLocal(builder, insn.getAssignee()).setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(CastInstruction insn) {
        emitLocal(builder, insn.getValue());
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
                    var blockType = context.functionTypes().of(null, sourceRef).asBlock();
                    var block = builder.block(blockType).inheritLocation();
                    block.nullBranch(WasmNullCondition.NULL, block.list).drop();
                    if (context.isStrict()) {
                        generateThrowCce(block);
                    } else {
                        block.unreachable();
                    }
                    builder.nullConst(targetType);
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
                emitLocal(builder, insn.getValue());
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
        emitLocal(builder, insn.getValue());
        builder.nonTrapConvert(WasmGeneratorUtil.mapType(insn.getSourceType()),
                WasmGeneratorUtil.mapType(insn.getTargetType()), true);
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(CastIntegerInstruction insn) {
        emitLocal(builder, insn.getValue());
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
        if (insn.getConsequent() == insn.getAlternative()) {
            jump(insn.getConsequent());
            return;
        }

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

        switch (insn.getCondition()) {
            case EQUAL -> compare(insn, inverted, WasmIntBinaryOperation.EQ, WasmIntBinaryOperation.NE,
                    WasmFloatBinaryOperation.EQ, WasmFloatBinaryOperation.NE);
            case NOT_EQUAL -> compare(insn, inverted, WasmIntBinaryOperation.NE, WasmIntBinaryOperation.EQ,
                    WasmFloatBinaryOperation.NE, WasmFloatBinaryOperation.EQ);
            case LESS -> compare(insn, inverted, WasmIntBinaryOperation.LT_SIGNED, WasmIntBinaryOperation.GE_SIGNED,
                    WasmFloatBinaryOperation.LT, WasmFloatBinaryOperation.GE);
            case LESS_OR_EQUAL -> compare(insn, inverted, WasmIntBinaryOperation.LE_SIGNED,
                    WasmIntBinaryOperation.GT_SIGNED, WasmFloatBinaryOperation.LE, WasmFloatBinaryOperation.GT);
            case GREATER -> compare(insn, inverted, WasmIntBinaryOperation.GT_SIGNED, WasmIntBinaryOperation.LE_SIGNED,
                    WasmFloatBinaryOperation.GT, WasmFloatBinaryOperation.LE);
            case GREATER_OR_EQUAL -> compare(insn, inverted, WasmIntBinaryOperation.GE_SIGNED,
                    WasmIntBinaryOperation.LT_SIGNED, WasmFloatBinaryOperation.GE, WasmFloatBinaryOperation.LT);
            case NULL -> {
                emitLocal(builder, insn.getOperand());
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
                emitLocal(builder, insn.getOperand());
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


    private void compare(BranchingInstruction insn, boolean inverted,
            WasmIntBinaryOperation intOp, WasmIntBinaryOperation invIntOp,
            WasmFloatBinaryOperation floatOp, WasmFloatBinaryOperation invFloatOp) {
        var comparison = comparisons[insn.getOperand().getIndex()];
        if (comparison != null) {
            emitLocals(builder, comparison.a, comparison.b);
            switch (comparison.type) {
                case COMPARE_INT32 -> builder.intBinary(WasmIntType.INT32, !inverted ? intOp : invIntOp);
                case COMPARE_INT64 -> builder.intBinary(WasmIntType.INT64, !inverted ? intOp : invIntOp);
                case COMPARE_FLOAT32_LESS -> compareFloat(WasmFloatType.FLOAT32, -1, inverted, floatOp, invFloatOp);
                case COMPARE_FLOAT32_GREATER -> compareFloat(WasmFloatType.FLOAT32, 1, inverted, floatOp, invFloatOp);
                case COMPARE_FLOAT64_LESS -> compareFloat(WasmFloatType.FLOAT64, -1, inverted, floatOp, invFloatOp);
                case COMPARE_FLOAT64_GREATER -> compareFloat(WasmFloatType.FLOAT64, 1, inverted, floatOp, invFloatOp);
            }
        } else {
            emitLocal(builder, insn.getOperand());
            var op = !inverted ? intOp : invIntOp;
            switch (op) {
                case EQ -> builder.intUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ);
                case NE -> { }
                default -> {
                    builder.i32Const(0);
                    builder.intBinary(WasmIntType.INT32, op);
                }
            }
        }
    }

    // Emits `cmp(a, b) op 0`, where cmp produces nanValue when either operand is NaN.
    // Wasm float comparisons (except NE) produce false for NaN, so when the expected result for NaN
    // differs, we emit the negation of the opposite comparison instead.
    private void compareFloat(WasmFloatType type, int nanValue, boolean inverted,
            WasmFloatBinaryOperation floatOp, WasmFloatBinaryOperation invFloatOp) {
        var op = !inverted ? floatOp : invFloatOp;
        var oppositeOp = !inverted ? invFloatOp : floatOp;
        var expectedNanResult = switch (op) {
            case EQ -> nanValue == 0;
            case NE -> nanValue != 0;
            case LT -> nanValue < 0;
            case LE -> nanValue <= 0;
            case GT -> nanValue > 0;
            case GE -> nanValue >= 0;
            default -> throw new IllegalArgumentException();
        };
        var actualNanResult = op == WasmFloatBinaryOperation.NE;
        if (expectedNanResult == actualNanResult) {
            builder.floatBinary(type, op);
        } else {
            builder.floatBinary(type, oppositeOp)
                    .intUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ);
        }
    }

    @Override
    public void visit(BinaryBranchingInstruction insn) {
        if (insn.getConsequent() == insn.getAlternative()) {
            jump(insn.getConsequent());
            return;
        }

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

        emitLocals(builder, insn.getFirstOperand(), insn.getSecondOperand());
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
            var condBuilder = ifInsn.getThenBlock().builder().inheritLocation();
            insertPhis(condBuilder, breakTarget);
            condBuilder.breakTo(labels[breakTarget.getIndex()]);
        } else {
            builder.branch(labels[breakTarget.getIndex()]);
        }
        insertPhis(builder, continueTarget);
        var continueLabel = labels[continueTarget.getIndex()];
        if (continueLabel != null && continueLabel.getBreakTarget() != builder.list.getBreakTarget()) {
            builder.breakTo(continueLabel);
        }
    }

    @Override
    public void visit(JumpInstruction insn) {
        jump(insn.getTarget());
    }

    private void jump(BasicBlock target) {
        insertPhis(builder, target);
        var label = labels[target.getIndex()];
        if (label != null) {
            builder.breakTo(label);
        } else {
            assert expectedBasicBlock == null && !dontExpectBasicBlock;
            expectedBasicBlock = target;
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
        var condition = insn.getCondition();
        if (insn.getEntries().isEmpty()) {
            current.breakTo(defaultLabel);
        } else if ((long) max - min >= SWITCH_TABLE_THRESHOLD) {
            translateSwitchToBinarySearch(current, insn, defaultLabel, clauseTargets, condition);
        } else {
            translateSwitchToWasmSwitch(current, insn, defaultLabel, clauseTargets, min, max, condition);
        }
    }

    private void translateSwitchToBinarySearch(WasmInstructionBuilder builder, SwitchInstruction insn,
            WasmInstructionList defaultList, WasmInstructionList[] clauseLists, Variable condition) {
        var entries = new ArrayList<TableEntry>();
        for (int i = 0; i < insn.getEntries().size(); i++) {
            var entry = insn.getEntries().get(i);
            entries.add(new TableEntry(entry.getCondition(), clauseLists[i]));
        }
        entries.sort(Comparator.comparingInt(e -> e.label));
        generateBinarySearch(entries, 0, entries.size() - 1, builder, defaultList, condition);
        builder.breakTo(defaultList);
    }

    private void generateBinarySearch(List<TableEntry> entries, int lower, int upper, WasmInstructionBuilder builder,
            WasmInstructionList defaultTarget, Variable testVar) {
        if (upper - lower == 0) {
            int label = entries.get(lower).label;
            emitLocal(builder, testVar)
                    .i32Const(label)
                    .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ)
                    .branch(entries.get(lower).target)
                    .breakTo(defaultTarget);
        } else if (upper - lower <= 0) {
            builder.breakTo(defaultTarget);
        } else {
            int mid = (upper + lower) / 2;
            int label = entries.get(mid).label;
            emitLocal(builder, testVar)
                    .i32Const(label)
                    .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.GT_SIGNED);
            var cond = builder.conditional();
            generateBinarySearch(entries, mid + 1, upper, cond.getThenBlock().builder().inheritLocation(),
                    defaultTarget, testVar);
            generateBinarySearch(entries, lower, mid, cond.getElseBlock().builder().inheritLocation(),
                    defaultTarget, testVar);
        }
    }

    private void translateSwitchToWasmSwitch(WasmInstructionBuilder builder, SwitchInstruction insn,
            WasmInstructionList defaultTarget, WasmInstructionList[] clauseTargets, int min, int max,
            Variable condition) {
        emitLocal(builder, condition);
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
            emitLocal(builder, insn.getValueToReturn());
        }
        if (returnBlock == null) {
            builder.return_();
        } else {
            builder.breakTo(returnBlock);
        }
    }

    @Override
    public void visit(RaiseInstruction insn) {
        assert !dontExpectBasicBlock && expectedBasicBlock == null;
        dontExpectBasicBlock = true;
        emitLocal(builder, insn.getException());
        builder.throw_(context.getExceptionTag());
    }

    @Override
    public void visit(ConstructArrayInstruction insn) {
        WasmGCGenerationUtil.emitClassInfoLiteral(context.classInfoProvider(), builder, insn.getItemType());
        emitLocal(builder, insn.getSize());
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
        var itemType = (ValueType.Array) insn.getItemType();
        WasmGCGenerationUtil.emitClassInfoLiteral(context.classInfoProvider(), builder, itemType.getItemType());
        for (var dimension : insn.getDimensions()) {
            emitLocal(builder, dimension);
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
            emitLocal(builder, insn.getInstance());
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
            emitLocal(builder, insn.getValue());
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
            emitLocals(builder, insn.getInstance(), insn.getValue());
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
        emitLocal(builder, insn.getArray());
        builder.arrayLength();
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(CloneArrayInstruction insn) {
        virtualCall(new MethodReference("java.lang.Object", "clone", ValueType.object("java.lang.Object")),
                insn.getArray(), Collections.emptyList());
        builder.setLocal(mapToLocal(insn.getReceiver()));
    }

    @Override
    public void visit(UnwrapArrayInstruction insn) {
        emitLocal(builder, insn.getArray());
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
        emitLocals(builder, insn.getArray(), insn.getIndex());
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
        emitLocals(builder, insn.getArray(), insn.getIndex(), insn.getValue());
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
        } else if (insn.getType() == InvocationType.SPECIAL || insn.getInstance() == null) {
            var method = context.hierarchy().resolve(insn.getMethod());
            var reference = method != null ? method.getReference() : insn.getMethod();
            var function = insn.getInstance() == null
                    ? context.functions().forStaticMethod(reference)
                    : context.functions().forInstanceMethod(reference);

            if (insn.getInstance() != null) {
                emitLocal(builder, insn.getInstance());
                forceType(insn.getInstance(), ValueType.object(method.getOwnerName()));
            }
            for (var arg : insn.getArguments()) {
                emitLocal(builder, arg);
            }
            builder.call(function, isAsyncSplit(reference));
        } else {
            virtualCall(insn.getMethod(), insn.getInstance(), insn.getArguments());
        }
        if (insn.getReceiver() != null) {
            builder.setLocal(mapToLocal(insn.getReceiver()));
        } else if (insn.getMethod().getReturnType() != ValueType.VOID) {
            builder.drop();
        }
    }

    private void virtualCall(MethodReference methodRef, Variable instance, List<? extends Variable> args) {
        var vtable = context.virtualTables().lookup(methodRef.getClassName());
        if (vtable == null) {
            builder.unreachable();
            return;
        }

        var entry = vtable.entry(methodRef.getDescriptor());
        var nonInterfaceAncestor = vtable.closestNonInterfaceAncestor();
        if (entry == null || nonInterfaceAncestor == null) {
            builder.unreachable();
            return;
        }

        var objectClass = context.classInfoProvider().getClassInfo("java.lang.Object");
        var instanceValueType = ValueType.object(methodRef.getClassName());
        var instanceType = (WasmType.CompositeReference) context.typeMapper().mapType(instanceValueType);

        var index = WasmGCClassInfoProvider.VIRTUAL_METHOD_OFFSET + entry.getIndex();
        var expectedInstanceClassInfo = context.classInfoProvider().getClassInfo(vtable.getClassName());
        var expectedInstanceClassStruct = context.classInfoProvider().getClassInfo(
                nonInterfaceAncestor.getClassName()).getStructure();
        var vtableStruct = expectedInstanceClassInfo.getVirtualTableStructure();
        var instanceStruct = (WasmStructure) instanceType.composite;

        emitLocal(builder, instance);
        if (!expectedInstanceClassStruct.isSupertypeOf(instanceStruct)) {
            builder.cast(expectedInstanceClassStruct.getNonNullReference());
        }
        for (var arg : args) {
            emitLocal(builder, arg);
        }
        emitLocal(builder, instance);

        builder
                .structGet(objectClass.getStructure(), WasmGCClassInfoProvider.VT_FIELD_OFFSET)
                .cast(vtableStruct.getNonNullReference())
                .structGet(vtableStruct, index);

        var functionTypeRef = (WasmType.CompositeReference) vtableStruct.getFields().get(index).getUnpackedType();
        builder.callReference((WasmFunctionType) functionTypeRef.composite, isAsyncSplit(methodRef));
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
            emitLocal(builder, insn.getValue());
            builder.test(wasmType);
        } else {
            var block = builder.block(WasmType.INT32);

            var sourceValueType = typeInference.typeOf(insn.getValue()).valueType;
            var sourceType = context.typeMapper().mapType(sourceValueType);

            if (sourceType instanceof WasmType.Reference ref) {
                // Get the class ref and call supertype check
                var objectClass = context.classInfoProvider().getClassInfo("java.lang.Object");
                var vtStruct = objectClass.getVirtualTableStructure();

                emitLocal(block, insn.getValue());
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
        emitLocal(builder, insn.getValue());
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
        emitLocal(builder, insn.getObjectRef());
        monitorEnter(builder);
    }

    @Override
    public void visit(MonitorExitInstruction insn) {
        emitLocal(builder, insn.getObjectRef());
        monitorExit(builder);
    }

    @Override
    public void visit(BoundCheckInstruction insn) {
        var block = builder.block();

        if (insn.getArray() != null) {
            var condBlock = block;
            if (insn.isLower()) {
                condBlock = block.block();
                emitLocal(condBlock, insn.getIndex())
                        .i32Const(0)
                        .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED)
                        .branch(condBlock.list);
            }

            emitLocals(condBlock, insn.getIndex(), insn.getArray())
                    .arrayLength()
                    .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED)
                    .branch(block.list);
        } else if (insn.isLower()) {
            emitLocal(block, insn.getIndex())
                    .i32Const(0)
                    .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.GE_SIGNED)
                    .branch(block.list);
        }

        generateThrowAioobe(block);

        var indexConstant = constants[insn.getIndex().getIndex()];
        if (indexConstant != null) {
            constants[insn.getReceiver().getIndex()] = indexConstant;
        } else {
            emitLocal(builder, insn.getIndex()).setLocal(mapToLocal(insn.getReceiver()));
        }
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
        var first = insn.getFirstOperand();
        var second = insn.getSecondOperand();
        switch (insn.getOperandType()) {
            case INT, LONG -> {
                var type = insn.getOperandType() == NumericOperandType.INT ? WasmIntType.INT32 : WasmIntType.INT64;
                emitLocals(builder, first, second)
                        .intBinary(type, WasmIntBinaryOperation.GT_SIGNED);
                emitLocals(builder, first, second)
                        .intBinary(type, WasmIntBinaryOperation.LT_SIGNED)
                        .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB);
            }
            case FLOAT, DOUBLE -> {
                var type = insn.getOperandType() == NumericOperandType.FLOAT
                        ? WasmFloatType.FLOAT32
                        : WasmFloatType.FLOAT64;
                if (insn.getOperation() == BinaryOperation.COMPARE_GREATER) {
                    emitLocals(builder, first, second)
                            .floatBinary(type, WasmFloatBinaryOperation.LE)
                            .intUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ);
                    emitLocals(builder, first, second)
                            .floatBinary(type, WasmFloatBinaryOperation.LT)
                            .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB);
                } else {
                    emitLocals(builder, first, second)
                            .floatBinary(type, WasmFloatBinaryOperation.GT);
                    emitLocals(builder, first, second)
                            .floatBinary(type, WasmFloatBinaryOperation.GE)
                            .intUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ)
                            .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB);
                }
            }
        }
    }

    private boolean isNativeReference(Variable variable) {
        var type = typeInference.typeOf(variable);
        return type != null && context.typeMapper().mapType(type.valueType) == WasmType.INT32;
    }

    private void insertPhis(WasmInstructionBuilder builder, BasicBlock target) {
        if (builder.getCurrentLocation() == null) {
            if (builder.list.getLast() != null) {
                builder.setCurrentLocation(builder.list.getLast().getLocation());
            } else if (builder.list.getBreakTarget() != null) {
                builder.setCurrentLocation(builder.list.getBreakTarget().getLocation());
            }
        }
        for (var arg : extractPhiArgs(target)) {
            emitLocal(builder, arg);
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

    private WasmInstructionBuilder emitLocal(WasmInstructionBuilder builder, Variable variable) {
        var constant = constants[variable.getIndex()];
        return constant != null ? constant.emit(builder) : builder.getLocal(mapToLocal(variable));
    }

    private WasmInstructionBuilder emitLocals(WasmInstructionBuilder builder, Variable... variables) {
        for (var variable : variables) {
            emitLocal(builder, variable);
        }
        return builder;
    }

    private WasmGCInlineIntrinsicContext intrinsicContext = new WasmGCInlineIntrinsicContext() {
        @Override
        public WasmInstructionBuilder emitLocal(WasmInstructionBuilder builder, Variable variable) {
            return WasmCodeGenerator.this.emitLocal(builder, variable);
        }

        @Override
        public WasmInstructionBuilder emitLocals(WasmInstructionBuilder builder, Variable... variables) {
            return WasmCodeGenerator.this.emitLocals(builder, variables);
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

    private interface ConstantEmitter {
        WasmInstructionBuilder emit(WasmInstructionBuilder builder);
    }

    private enum ComparisonType {
        COMPARE_INT32,
        COMPARE_INT64,
        COMPARE_FLOAT32_LESS,
        COMPARE_FLOAT32_GREATER,
        COMPARE_FLOAT64_LESS,
        COMPARE_FLOAT64_GREATER
    }

    private static class Comparison {
        Variable a;
        Variable b;
        ComparisonType type;

        Comparison(Variable a, Variable b, ComparisonType type) {
            this.a = a;
            this.b = b;
            this.type = type;
        }
    }
}
