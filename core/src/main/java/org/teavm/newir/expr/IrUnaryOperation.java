/*
 *  Copyright 2021 konsoletyper.
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
package org.teavm.newir.expr;

public enum IrUnaryOperation {
    BOOLEAN_TO_INT,
    BYTE_TO_INT,
    SHORT_TO_INT,
    CHAR_TO_INT,
    INT_TO_BOOLEAN,
    INT_TO_BYTE,
    INT_TO_SHORT,
    INT_TO_CHAR,
    INT_TO_LONG,
    INT_TO_FLOAT,
    INT_TO_DOUBLE,
    LONG_TO_INT,
    LONG_TO_FLOAT,
    LONG_TO_DOUBLE,
    FLOAT_TO_INT,
    FLOAT_TO_LONG,
    FLOAT_TO_DOUBLE,
    DOUBLE_TO_INT,
    DOUBLE_TO_LONG,
    DOUBLE_TO_FLOAT,

    ARRAY_LENGTH,

    IGNORE,

    NULL_CHECK,
    NOT,

    IINV,
    LINV,

    INEG,
    LNEG,
    FNEG,
    DNEG
}
