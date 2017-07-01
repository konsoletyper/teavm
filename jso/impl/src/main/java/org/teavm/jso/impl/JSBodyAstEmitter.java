/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.jso.impl;

import java.io.IOException;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.Block;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.rendering.Precedence;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.model.MethodReference;

class JSBodyAstEmitter implements JSBodyEmitter {
    private boolean isStatic;
    private AstNode ast;
    private String[] parameterNames;

    public JSBodyAstEmitter(boolean isStatic, AstNode ast, String[] parameterNames) {
        this.isStatic = isStatic;
        this.ast = ast;
        this.parameterNames = parameterNames;
    }

    @Override
    public void emit(InjectorContext context) throws IOException {
        AstWriter astWriter = new AstWriter(context.getWriter());
        int paramIndex = 0;
        if (!isStatic) {
            int index = paramIndex++;
            astWriter.declareNameEmitter("this", prec -> context.writeExpr(context.getArgument(index),
                    convert(prec)));
        }
        for (int i = 0; i < parameterNames.length; ++i) {
            int index = paramIndex++;
            astWriter.declareNameEmitter(parameterNames[i],
                    prec -> context.writeExpr(context.getArgument(index), convert(prec)));
        }
        astWriter.hoist(ast);
        astWriter.print(ast, convert(context.getPrecedence()));
    }

    private static int convert(Precedence precedence) {
        switch (precedence) {
            case ADDITION:
                return AstWriter.PRECEDENCE_ADD;
            case ASSIGNMENT:
                return AstWriter.PRECEDENCE_ASSIGN;
            case BITWISE_AND:
                return AstWriter.PRECEDENCE_BITWISE_AND;
            case BITWISE_OR:
                return AstWriter.PRECEDENCE_BITWISE_OR;
            case BITWISE_XOR:
                return AstWriter.PRECEDENCE_BITWISE_XOR;
            case BITWISE_SHIFT:
                return AstWriter.PRECEDENCE_SHIFT;
            case COMMA:
                return AstWriter.PRECEDENCE_COMMA;
            case COMPARISON:
                return AstWriter.PRECEDENCE_RELATION;
            case CONDITIONAL:
                return AstWriter.PRECEDENCE_COND;
            case EQUALITY:
                return AstWriter.PRECEDENCE_EQUALITY;
            case FUNCTION_CALL:
                return AstWriter.PRECEDENCE_FUNCTION;
            case GROUPING:
                return 1;
            case LOGICAL_AND:
                return AstWriter.PRECEDENCE_AND;
            case LOGICAL_OR:
                return AstWriter.PRECEDENCE_OR;
            case MEMBER_ACCESS:
                return AstWriter.PRECEDENCE_MEMBER;
            case MULTIPLICATION:
                return AstWriter.PRECEDENCE_MUL;
            case UNARY:
                return AstWriter.PRECEDENCE_PREFIX;
            default:
                return AstWriter.PRECEDENCE_COMMA;
        }
    }

    private static Precedence convert(int precedence) {
        switch (precedence) {
            case AstWriter.PRECEDENCE_ADD:
                return Precedence.ADDITION;
            case AstWriter.PRECEDENCE_ASSIGN:
                return Precedence.ASSIGNMENT;
            case AstWriter.PRECEDENCE_BITWISE_AND:
                return Precedence.BITWISE_AND;
            case AstWriter.PRECEDENCE_BITWISE_OR:
                return Precedence.BITWISE_OR;
            case AstWriter.PRECEDENCE_BITWISE_XOR:
                return Precedence.BITWISE_XOR;
            case AstWriter.PRECEDENCE_SHIFT:
                return Precedence.BITWISE_SHIFT;
            case AstWriter.PRECEDENCE_COMMA:
                return Precedence.COMMA;
            case AstWriter.PRECEDENCE_RELATION:
                return Precedence.COMPARISON;
            case AstWriter.PRECEDENCE_COND:
                return Precedence.CONDITIONAL;
            case AstWriter.PRECEDENCE_EQUALITY:
                return Precedence.EQUALITY;
            case AstWriter.PRECEDENCE_FUNCTION:
                return Precedence.FUNCTION_CALL;
            case 1:
                return Precedence.GROUPING;
            case AstWriter.PRECEDENCE_AND:
                return Precedence.LOGICAL_AND;
            case AstWriter.PRECEDENCE_OR:
                return Precedence.LOGICAL_OR;
            case AstWriter.PRECEDENCE_MEMBER:
                return Precedence.MEMBER_ACCESS;
            case AstWriter.PRECEDENCE_MUL:
                return Precedence.MULTIPLICATION;
            case AstWriter.PRECEDENCE_PREFIX:
                return Precedence.UNARY;
            default:
                return Precedence.min();
        }
    }

    @Override
    public void emit(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        AstWriter astWriter = new AstWriter(writer);
        int paramIndex = 1;
        if (!isStatic) {
            int index = paramIndex++;
            astWriter.declareNameEmitter("this", prec -> writer.append(context.getParameterName(index)));
        }
        for (int i = 0; i < parameterNames.length; ++i) {
            int index = paramIndex++;
            astWriter.declareNameEmitter(parameterNames[i], prec -> writer.append(context.getParameterName(index)));
        }
        astWriter.hoist(ast);
        if (ast instanceof Block) {
            for (Node child = ast.getFirstChild(); child != null; child = child.getNext()) {
                astWriter.print((AstNode) child);
                writer.softNewLine();
            }
        } else {
            astWriter.print(ast);
            writer.softNewLine();
        }
    }
}
