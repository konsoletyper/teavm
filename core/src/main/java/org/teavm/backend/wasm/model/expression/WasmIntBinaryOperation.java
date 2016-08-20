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
package org.teavm.backend.wasm.model.expression;

public enum WasmIntBinaryOperation {
    ADD,
    SUB,
    MUL,
    DIV_SIGNED,
    DIV_UNSIGNED,
    REM_SIGNED,
    REM_UNSIGNED,
    OR,
    AND,
    XOR,
    SHL,
    SHR_SIGNED,
    SHR_UNSIGNED,
    ROTL,
    ROTR,
    EQ,
    NE,
    LT_SIGNED,
    LT_UNSIGNED,
    LE_SIGNED,
    LE_UNSIGNED,
    GT_SIGNED,
    GT_UNSIGNED,
    GE_SIGNED,
    GE_UNSIGNED
}
