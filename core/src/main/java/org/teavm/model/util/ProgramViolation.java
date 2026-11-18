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
package org.teavm.model.util;

/**
 * IR invariant violation found by {@link ProgramChecker}.
 */
public class ProgramViolation {
    private final Kind kind;
    private final int block;
    private final String message;

    public ProgramViolation(Kind kind, int block, String message) {
        this.kind = kind;
        this.block = block;
        this.message = message;
    }

    public Kind getKind() {
        return kind;
    }

    /**
     * Index of the block where violation was found.
     */
    public int getBlock() {
        return block;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return kind + " in $" + block + ": " + message;
    }

    public enum Kind {
        /** Variable is defined more than once. */
        MULTIPLE_DEFINITION,
        /** Variable is used, but never defined. */
        UNDEFINED_VARIABLE,
        /** Variable is used in a place which its definition does not dominate. */
        NOT_DOMINATED,
        /** Entry block contains phis. */
        PHI_IN_ENTRY_BLOCK,
        /** Phi has an incoming from a block which is not a predecessor of the phi's block. */
        INCOMING_FROM_NON_PREDECESSOR,
        /** Phi does not have incoming from one of predecessors of the phi's block. */
        MISSING_INCOMING,
        /** Phi has more than one incoming from a block which jumps to the phi's block by a normal edge. */
        MULTIPLE_INCOMINGS,
        /** Block is both normal and exceptional predecessor of another block. */
        NORMAL_AND_EXCEPTIONAL_EDGE,
        /**
         * In a group of handler phi incomings from a protected block, an incoming not defined by
         * an instruction of the protected block is not the first one.
         */
        HANDLER_PHI_ENTRY_VALUE,
        /**
         * In a group of handler phi incomings from a protected block, incomings defined by instructions of
         * the protected block don't follow order of the instructions.
         */
        HANDLER_PHI_ORDER
    }
}
