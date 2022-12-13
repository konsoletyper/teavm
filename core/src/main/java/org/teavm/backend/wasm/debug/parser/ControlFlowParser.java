/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.wasm.debug.parser;

import com.carrotsearch.hppc.IntArrayList;
import java.util.ArrayList;
import java.util.List;
import org.teavm.backend.wasm.debug.info.ControlFlowInfo;
import org.teavm.backend.wasm.debug.info.FunctionControlFlow;
import org.teavm.backend.wasm.debug.info.FunctionControlFlowBuilder;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.parser.AddressListener;
import org.teavm.backend.wasm.parser.BranchOpcode;
import org.teavm.backend.wasm.parser.CodeListener;
import org.teavm.backend.wasm.parser.CodeSectionListener;
import org.teavm.backend.wasm.parser.Opcode;

public class ControlFlowParser implements CodeSectionListener, CodeListener, AddressListener {
    private int previousAddress;
    private int address;
    private int startAddress;
    private List<Branch> branches = new ArrayList<>();
    private List<FunctionControlFlow> ranges = new ArrayList<>();
    private List<Branch> pendingBranches = new ArrayList<>();
    private List<Block> blocks = new ArrayList<>();

    public ControlFlowInfo build() {
        return new ControlFlowInfo(ranges.toArray(new FunctionControlFlow[0]));
    }

    @Override
    public void address(int address) {
        previousAddress = this.address;
        this.address = address;
        flush();
    }

    @Override
    public boolean functionStart(int index, int size) {
        startAddress = address;
        return true;
    }

    @Override
    public CodeListener code() {
        return this;
    }

    @Override
    public int startBlock(boolean loop, WasmType type) {
        return startBlock(loop);
    }

    @Override
    public int startConditionalBlock(WasmType type) {
        return startBlock(false);
    }

    private int startBlock(boolean loop) {
        var token = blocks.size();
        var branch = !loop ? newPendingBranch(false) : null;
        var block = new Block(branch, address);
        block.loop = loop;
        blocks.add(block);
        if (branch != null) {
            block.pendingBranches.add(branch);
        }
        return token;
    }

    @Override
    public void startElseSection(int token) {
        var block = blocks.get(blocks.size() - 1);
        var lastBranch = branches.get(branches.size() - 1);
        if (lastBranch.address != previousAddress) {
            lastBranch = new Branch(previousAddress, false);
            branches.add(lastBranch);
        }
        block.pendingBranches.add(lastBranch);
        block.branch.targets.add(address);
    }

    @Override
    public void endBlock(int token, boolean loop) {
        var block = blocks.remove(blocks.size() - 1);
        pendingBranches.addAll(block.pendingBranches);
        if (loop) {
            var branch = newBranch(false);
            branch.targets.add(block.address);
        }
    }

    @Override
    public void call(int functionIndex) {
        call();
    }

    @Override
    public void indirectCall(int typeIndex, int tableIndex) {
        call();
    }

    private void call() {
        newPendingBranch(true);
    }

    @Override
    public void opcode(Opcode opcode) {
        switch (opcode) {
            case RETURN:
            case UNREACHABLE: {
                newBranch(false);
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void branch(BranchOpcode opcode, int depth, int target) {
        var branch = newBranch(false);
        if (opcode == BranchOpcode.BR_IF) {
            pendingBranches.add(branch);
        }
        branch.jumpTo(blocks.get(target));
    }

    @Override
    public void tableBranch(int[] depths, int[] targets, int defaultDepth, int defaultTarget) {
        var branch = newPendingBranch(false);
        for (var target : targets) {
            branch.jumpTo(blocks.get(target));
        }
        branch.jumpTo(blocks.get(defaultDepth));
    }

    private Branch newPendingBranch(boolean isCall) {
        var branch = newBranch(isCall);
        pendingBranches.add(branch);
        return branch;
    }

    private Branch newBranch(boolean isCall) {
        var branch = new Branch(address, isCall);
        branches.add(branch);
        return branch;
    }

    private void flush() {
        for (var branch : pendingBranches) {
            branch.targets.add(address);
        }
        pendingBranches.clear();
    }

    @Override
    public void functionEnd() {
        var cfb = new FunctionControlFlowBuilder(startAddress, address);
        for (var branch : branches) {
            if (branch.isCall) {
                cfb.addCall(branch.address, branch.targets.toArray());
            } else {
                cfb.addBranch(branch.address, branch.targets.toArray());
            }
        }
        ranges.add(cfb.build());
        branches.clear();
        pendingBranches.clear();
        blocks.clear();
    }

    private static class Block {
        Branch branch;
        boolean loop;
        final int address;
        List<Branch> pendingBranches = new ArrayList<>();

        Block(Branch branch, int address) {
            this.branch = branch;
            this.address = address;
        }
    }

    private static class Branch {
        final int address;
        final IntArrayList targets = new IntArrayList();
        final boolean isCall;

        Branch(int address, boolean isCall) {
            this.address = address;
            this.isCall = isCall;
        }

        void jumpTo(Block block) {
            if (block.loop) {
                targets.add(block.address);
            } else {
                block.pendingBranches.add(this);
            }
        }
    }
}
