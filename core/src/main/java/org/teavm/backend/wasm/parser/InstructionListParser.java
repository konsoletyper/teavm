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
package org.teavm.backend.wasm.parser;

import java.util.ArrayList;
import java.util.List;
import org.teavm.backend.wasm.model.WasmBlockType;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmArrayCopy;
import org.teavm.backend.wasm.model.instruction.WasmArrayGet;
import org.teavm.backend.wasm.model.instruction.WasmArrayLength;
import org.teavm.backend.wasm.model.instruction.WasmArrayNewDefault;
import org.teavm.backend.wasm.model.instruction.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.instruction.WasmArraySet;
import org.teavm.backend.wasm.model.instruction.WasmBlock;
import org.teavm.backend.wasm.model.instruction.WasmBranch;
import org.teavm.backend.wasm.model.instruction.WasmBreak;
import org.teavm.backend.wasm.model.instruction.WasmCall;
import org.teavm.backend.wasm.model.instruction.WasmCallReference;
import org.teavm.backend.wasm.model.instruction.WasmCast;
import org.teavm.backend.wasm.model.instruction.WasmCastBranch;
import org.teavm.backend.wasm.model.instruction.WasmCastCondition;
import org.teavm.backend.wasm.model.instruction.WasmConditional;
import org.teavm.backend.wasm.model.instruction.WasmConversion;
import org.teavm.backend.wasm.model.instruction.WasmCopy;
import org.teavm.backend.wasm.model.instruction.WasmDrop;
import org.teavm.backend.wasm.model.instruction.WasmExternConversion;
import org.teavm.backend.wasm.model.instruction.WasmFill;
import org.teavm.backend.wasm.model.instruction.WasmFloat32Constant;
import org.teavm.backend.wasm.model.instruction.WasmFloat64Constant;
import org.teavm.backend.wasm.model.instruction.WasmFloatBinary;
import org.teavm.backend.wasm.model.instruction.WasmFloatUnary;
import org.teavm.backend.wasm.model.instruction.WasmFunctionReference;
import org.teavm.backend.wasm.model.instruction.WasmGetGlobal;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmIndirectCall;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmInstructionVisitor;
import org.teavm.backend.wasm.model.instruction.WasmInt31Get;
import org.teavm.backend.wasm.model.instruction.WasmInt31Reference;
import org.teavm.backend.wasm.model.instruction.WasmInt32Constant;
import org.teavm.backend.wasm.model.instruction.WasmInt64Constant;
import org.teavm.backend.wasm.model.instruction.WasmIntBinary;
import org.teavm.backend.wasm.model.instruction.WasmIntUnary;
import org.teavm.backend.wasm.model.instruction.WasmIsNull;
import org.teavm.backend.wasm.model.instruction.WasmLoadFloat32;
import org.teavm.backend.wasm.model.instruction.WasmLoadFloat64;
import org.teavm.backend.wasm.model.instruction.WasmLoadInt32;
import org.teavm.backend.wasm.model.instruction.WasmLoadInt64;
import org.teavm.backend.wasm.model.instruction.WasmMemoryGrow;
import org.teavm.backend.wasm.model.instruction.WasmNullBranch;
import org.teavm.backend.wasm.model.instruction.WasmNullCondition;
import org.teavm.backend.wasm.model.instruction.WasmNullConstant;
import org.teavm.backend.wasm.model.instruction.WasmReferencesEqual;
import org.teavm.backend.wasm.model.instruction.WasmReturn;
import org.teavm.backend.wasm.model.instruction.WasmSetGlobal;
import org.teavm.backend.wasm.model.instruction.WasmSetLocal;
import org.teavm.backend.wasm.model.instruction.WasmStoreFloat32;
import org.teavm.backend.wasm.model.instruction.WasmStoreFloat64;
import org.teavm.backend.wasm.model.instruction.WasmStoreInt32;
import org.teavm.backend.wasm.model.instruction.WasmStoreInt64;
import org.teavm.backend.wasm.model.instruction.WasmStructGet;
import org.teavm.backend.wasm.model.instruction.WasmStructNew;
import org.teavm.backend.wasm.model.instruction.WasmStructNewDefault;
import org.teavm.backend.wasm.model.instruction.WasmStructSet;
import org.teavm.backend.wasm.model.instruction.WasmSwitch;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocal;
import org.teavm.backend.wasm.model.instruction.WasmTest;
import org.teavm.backend.wasm.model.instruction.WasmThrow;
import org.teavm.backend.wasm.model.instruction.WasmTry;
import org.teavm.backend.wasm.model.instruction.WasmUnreachable;

/**
 * <p>Reports instructions of Wasm model to {@link CodeListener}, the same way {@link CodeParser} reports
 * instructions decoded from binary code section. This allows to reuse listeners, for example,
 * to print disassembly of a function that is being generated.</p>
 *
 * <p>Since listener refers to entities by indices, entities are mapped to indices by given
 * {@link WasmEntityIndexer}. Locals are reported by {@link org.teavm.backend.wasm.model.WasmLocal#getIndex()},
 * tags by {@link org.teavm.backend.wasm.model.WasmTag#getIndex()}.</p>
 */
public class InstructionListParser {
    private final WasmEntityIndexer indexer;
    private final CodeListener listener;
    private final List<WasmInstruction> blockStack = new ArrayList<>();
    private final List<Integer> tokenStack = new ArrayList<>();
    private final Visitor visitor = new Visitor();

    public InstructionListParser(WasmEntityIndexer indexer, CodeListener listener) {
        this.indexer = indexer;
        this.listener = listener;
    }

    public void parse(WasmInstructionList list) {
        for (var insn : list) {
            insn.acceptVisitor(visitor);
        }
    }

    private void enterBlock(WasmInstruction insn, int token) {
        blockStack.add(insn);
        tokenStack.add(token);
    }

    private void exitBlock() {
        blockStack.remove(blockStack.size() - 1);
        tokenStack.remove(tokenStack.size() - 1);
    }

    /**
     * Returns depth of the given branch target, or {@code -1} if the target is not an enclosing block,
     * for example, when the target is function body.
     */
    private int depth(WasmInstructionList target) {
        var owner = target.getBreakTarget();
        if (owner != null) {
            for (var i = blockStack.size() - 1; i >= 0; --i) {
                if (blockStack.get(i) == owner) {
                    return blockStack.size() - 1 - i;
                }
            }
        }
        return -1;
    }

    private int token(int depth) {
        return depth >= 0 ? tokenStack.get(tokenStack.size() - 1 - depth) : -1;
    }

    private WasmHollowBlockType blockType(WasmBlockType type) {
        if (type == null) {
            return null;
        }
        if (type instanceof WasmBlockType.Function) {
            return new WasmHollowBlockType.Function(indexer.type(((WasmBlockType.Function) type).ref));
        }
        var valueType = ((WasmBlockType.Value) type).type;
        return valueType != null ? new WasmHollowBlockType.Value(type(valueType)) : null;
    }

    private WasmHollowType type(WasmType type) {
        return type(type, indexer);
    }

    private WasmHollowType.Reference referenceType(WasmType.Reference type) {
        return referenceType(type, indexer);
    }

    public static WasmHollowType type(WasmType type, WasmEntityIndexer indexer) {
        if (type == null) {
            return null;
        }
        if (type instanceof WasmType.Number) {
            return WasmHollowType.num(((WasmType.Number) type).number);
        }
        return referenceType((WasmType.Reference) type, indexer);
    }

    public static WasmHollowType.Reference referenceType(WasmType.Reference type, WasmEntityIndexer indexer) {
        if (type instanceof WasmType.CompositeReference) {
            var composite = ((WasmType.CompositeReference) type).composite;
            return new WasmHollowType.CompositeReference(indexer.type(composite), type.isNullable());
        }
        var kind = ((WasmType.SpecialReference) type).kind;
        return type.isNullable()
                ? WasmHollowType.Reference.special(kind)
                : WasmHollowType.Reference.nonNullSpecial(kind);
    }

    private static int alignment(int value) {
        return Integer.highestOneBit(Math.max(1, value));
    }

    private class Visitor implements WasmInstructionVisitor {
        @Override
        public void visit(WasmUnreachable instruction) {
            listener.opcode(Opcode.UNREACHABLE);
        }

        @Override
        public void visit(WasmBlock instruction) {
            var token = listener.startBlock(instruction.isLoop(), blockType(instruction.getType()));
            enterBlock(instruction, token);
            parse(instruction.getBody());
            exitBlock();
            listener.endBlock(token, instruction.isLoop());
        }

        @Override
        public void visit(WasmConditional instruction) {
            var token = listener.startConditionalBlock(blockType(instruction.getType()));
            enterBlock(instruction, token);
            parse(instruction.getThenBlock());
            if (!instruction.getElseBlock().isEmpty()) {
                listener.startElseSection(token);
                parse(instruction.getElseBlock());
            }
            exitBlock();
            listener.endBlock(token, false);
        }

        @Override
        public void visit(WasmBranch instruction) {
            var depth = depth(instruction.getTarget());
            listener.branch(BranchOpcode.BR_IF, depth, token(depth));
        }

        @Override
        public void visit(WasmNullBranch instruction) {
            var depth = depth(instruction.getTarget());
            var opcode = instruction.getCondition() == WasmNullCondition.NULL
                    ? BranchOpcode.BR_ON_NULL
                    : BranchOpcode.BR_ON_NON_NULL;
            listener.branch(opcode, depth, token(depth));
        }

        @Override
        public void visit(WasmCastBranch instruction) {
            var depth = depth(instruction.getTarget());
            listener.castBranch(instruction.getCondition() == WasmCastCondition.SUCCESS, depth, token(depth),
                    referenceType(instruction.getSourceType()), referenceType(instruction.getTargetType()));
        }

        @Override
        public void visit(WasmBreak instruction) {
            var depth = depth(instruction.getTarget());
            listener.branch(BranchOpcode.BR, depth, token(depth));
        }

        @Override
        public void visit(WasmSwitch instruction) {
            var targets = instruction.getTargets();
            var depths = new int[targets.size()];
            var tokens = new int[targets.size()];
            for (var i = 0; i < targets.size(); ++i) {
                depths[i] = depth(targets.get(i));
                tokens[i] = token(depths[i]);
            }
            var defaultDepth = depth(instruction.getDefaultTarget());
            listener.tableBranch(depths, tokens, defaultDepth, token(defaultDepth));
        }

        @Override
        public void visit(WasmReturn instruction) {
            listener.opcode(Opcode.RETURN);
        }

        @Override
        public void visit(WasmInt32Constant instruction) {
            listener.int32Constant(instruction.getValue());
        }

        @Override
        public void visit(WasmInt64Constant instruction) {
            listener.int64Constant(instruction.getValue());
        }

        @Override
        public void visit(WasmFloat32Constant instruction) {
            listener.float32Constant(instruction.getValue());
        }

        @Override
        public void visit(WasmFloat64Constant instruction) {
            listener.float64Constant(instruction.getValue());
        }

        @Override
        public void visit(WasmNullConstant instruction) {
            listener.nullConstant(referenceType(instruction.getType()));
        }

        @Override
        public void visit(WasmIsNull instruction) {
            listener.opcode(Opcode.IS_NULL);
        }

        @Override
        public void visit(WasmGetLocal instruction) {
            listener.local(LocalOpcode.GET, instruction.getLocal().getIndex());
        }

        @Override
        public void visit(WasmSetLocal instruction) {
            listener.local(LocalOpcode.SET, instruction.getLocal().getIndex());
        }

        @Override
        public void visit(WasmTeeLocal instruction) {
            listener.local(LocalOpcode.TEE, instruction.getLocal().getIndex());
        }

        @Override
        public void visit(WasmGetGlobal instruction) {
            listener.getGlobal(indexer.global(instruction.getGlobal()));
        }

        @Override
        public void visit(WasmSetGlobal instruction) {
            listener.setGlobal(indexer.global(instruction.getGlobal()));
        }

        @Override
        public void visit(WasmIntBinary instruction) {
            listener.binary(instruction.getOperation(), instruction.getType());
        }

        @Override
        public void visit(WasmFloatBinary instruction) {
            listener.binary(instruction.getOperation(), instruction.getType());
        }

        @Override
        public void visit(WasmIntUnary instruction) {
            listener.unary(instruction.getOperation(), instruction.getType());
        }

        @Override
        public void visit(WasmFloatUnary instruction) {
            listener.unary(instruction.getOperation(), instruction.getType());
        }

        @Override
        public void visit(WasmConversion instruction) {
            listener.convert(instruction.getSourceType(), instruction.getTargetType(), instruction.isSigned(),
                    instruction.isReinterpret(), instruction.isNonTrapping());
        }

        @Override
        public void visit(WasmCall instruction) {
            listener.call(indexer.function(instruction.getFunction()));
        }

        @Override
        public void visit(WasmIndirectCall instruction) {
            listener.indirectCall(indexer.type(instruction.getType()), 0);
        }

        @Override
        public void visit(WasmCallReference instruction) {
            listener.callReference(indexer.type(instruction.getType()));
        }

        @Override
        public void visit(WasmDrop instruction) {
            listener.opcode(Opcode.DROP);
        }

        @Override
        public void visit(WasmLoadInt32 instruction) {
            listener.loadInt32(instruction.getConvertFrom(), alignment(instruction.getAlignment()),
                    instruction.getOffset());
        }

        @Override
        public void visit(WasmLoadInt64 instruction) {
            listener.loadInt64(instruction.getConvertFrom(), alignment(instruction.getAlignment()),
                    instruction.getOffset());
        }

        @Override
        public void visit(WasmLoadFloat32 instruction) {
            listener.loadFloat32(alignment(instruction.getAlignment()), instruction.getOffset());
        }

        @Override
        public void visit(WasmLoadFloat64 instruction) {
            listener.loadFloat64(alignment(instruction.getAlignment()), instruction.getOffset());
        }

        @Override
        public void visit(WasmStoreInt32 instruction) {
            listener.storeInt32(instruction.getConvertTo(), alignment(instruction.getAlignment()),
                    instruction.getOffset());
        }

        @Override
        public void visit(WasmStoreInt64 instruction) {
            listener.storeInt64(instruction.getConvertTo(), alignment(instruction.getAlignment()),
                    instruction.getOffset());
        }

        @Override
        public void visit(WasmStoreFloat32 instruction) {
            listener.storeFloat32(alignment(instruction.getAlignment()), instruction.getOffset());
        }

        @Override
        public void visit(WasmStoreFloat64 instruction) {
            listener.storeFloat64(alignment(instruction.getAlignment()), instruction.getOffset());
        }

        @Override
        public void visit(WasmMemoryGrow instruction) {
            listener.memoryGrow();
        }

        @Override
        public void visit(WasmFill instruction) {
            listener.memoryFill();
        }

        @Override
        public void visit(WasmCopy instruction) {
            listener.memoryCopy();
        }

        @Override
        public void visit(WasmTry instruction) {
            var type = instruction.getType();
            var token = listener.tryTable(type != null ? new WasmHollowBlockType.Value(type(type)) : null);
            for (var catchClause : instruction.getCatches()) {
                var depth = depth(catchClause.getTarget());
                if (catchClause.getTag() != null) {
                    listener.catchTag(catchClause.getTag().getIndex(), catchClause.isRef(), depth, token(depth));
                } else {
                    listener.catchAll(catchClause.isRef(), depth, token(depth));
                }
            }
            enterBlock(instruction, token);
            parse(instruction.getBody());
            exitBlock();
            listener.endBlock(token, false);
        }

        @Override
        public void visit(WasmThrow instruction) {
            if (instruction.getTag() != null) {
                listener.throwInstruction(instruction.getTag().getIndex());
            } else {
                listener.opcode(Opcode.THROW_REF);
            }
        }

        @Override
        public void visit(WasmReferencesEqual instruction) {
            listener.opcode(Opcode.REF_EQ);
        }

        @Override
        public void visit(WasmCast instruction) {
            listener.cast(referenceType(instruction.getTargetType()));
        }

        @Override
        public void visit(WasmTest instruction) {
            listener.test(referenceType(instruction.getTestType()));
        }

        @Override
        public void visit(WasmExternConversion instruction) {
            switch (instruction.getType()) {
                case EXTERN_TO_ANY:
                    listener.opcode(Opcode.EXTERN_TO_ANY);
                    break;
                case ANY_TO_EXTERN:
                    listener.opcode(Opcode.ANY_TO_EXTERN);
                    break;
            }
        }

        @Override
        public void visit(WasmStructNew instruction) {
            listener.structNew(indexer.type(instruction.getType()));
        }

        @Override
        public void visit(WasmStructNewDefault instruction) {
            listener.structNewDefault(indexer.type(instruction.getType()));
        }

        @Override
        public void visit(WasmStructGet instruction) {
            listener.structGet(instruction.getSignedType(), indexer.type(instruction.getType()),
                    instruction.getFieldIndex());
        }

        @Override
        public void visit(WasmStructSet instruction) {
            listener.structSet(indexer.type(instruction.getType()), instruction.getFieldIndex());
        }

        @Override
        public void visit(WasmArrayNewDefault instruction) {
            listener.arrayNewDefault(indexer.type(instruction.getType()));
        }

        @Override
        public void visit(WasmArrayNewFixed instruction) {
            listener.arrayNewFixed(indexer.type(instruction.getType()), instruction.getSize());
        }

        @Override
        public void visit(WasmArrayGet instruction) {
            listener.arrayGet(instruction.getSignedType(), indexer.type(instruction.getType()));
        }

        @Override
        public void visit(WasmArraySet instruction) {
            listener.arraySet(indexer.type(instruction.getType()));
        }

        @Override
        public void visit(WasmArrayLength instruction) {
            listener.opcode(Opcode.ARRAY_LENGTH);
        }

        @Override
        public void visit(WasmArrayCopy instruction) {
            listener.arrayCopy(indexer.type(instruction.getTargetArrayType()),
                    indexer.type(instruction.getSourceArrayType()));
        }

        @Override
        public void visit(WasmFunctionReference instruction) {
            listener.functionReference(indexer.function(instruction.getFunction()));
        }

        @Override
        public void visit(WasmInt31Reference instruction) {
            listener.int31Reference();
        }

        @Override
        public void visit(WasmInt31Get instruction) {
            listener.int31Get(instruction.getSignedType());
        }
    }
}
