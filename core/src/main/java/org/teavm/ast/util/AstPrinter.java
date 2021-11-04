/*
 *  Copyright 2021 Alexey Andreev.
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
package org.teavm.ast.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.BinaryExpr;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.BreakStatement;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.ContinueStatement;
import org.teavm.ast.Expr;
import org.teavm.ast.IdentifiedStatement;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.OperationType;
import org.teavm.ast.RecursiveVisitor;
import org.teavm.ast.ReturnStatement;
import org.teavm.ast.Statement;
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.UnaryExpr;
import org.teavm.ast.VariableExpr;
import org.teavm.ast.WhileStatement;
import org.teavm.model.MethodReference;

public class AstPrinter {
    private StringBuilder sb = new StringBuilder();
    private int indentLevel;
    private int lastLineIndex;
    private Map<IdentifiedStatement, Integer> blockIds = new HashMap<>();

    public String print(Statement statement) {
        statement.acceptVisitor(visitor);
        var result = sb.toString();
        sb.setLength(0);
        blockIds.clear();
        return result;
    }

    public String print(Expr expr) {
        expr.acceptVisitor(visitor);
        var result = sb.toString();
        sb.setLength(0);
        return result;
    }

    private void newLine() {
        sb.append("\n");
        for (int i = 0; i < indentLevel; ++i) {
            sb.append("    ");
        }
        lastLineIndex = sb.length();
    }

    private void indent() {
        indentLevel++;
        if (lastLineIndex == sb.length()) {
            sb.append("    ");
            lastLineIndex = sb.length();
        }
    }

    private void outdent() {
        indentLevel--;
        if (lastLineIndex == sb.length()) {
            sb.setLength(sb.length() - 4);
            lastLineIndex = sb.length();
        }
    }

    private RecursiveVisitor visitor = new RecursiveVisitor() {
        private void print(Expr expr) {
            if (expr == null) {
                sb.append("<null>");
            } else {
                expr.acceptVisitor(this);
            }
        }

        private void print(Statement statement) {
            if (statement == null) {
                sb.append("[null]");
                newLine();
            } else {
                statement.acceptVisitor(this);
            }
        }

        @Override
        public void visit(BinaryExpr expr) {
            switch (expr.getOperation()) {
                case ADD:
                    binary(expr, "+");
                    break;
                case SUBTRACT:
                    binary(expr, "-");
                    break;
                case MULTIPLY:
                    binary(expr, "*");
                    break;
                case DIVIDE:
                    binary(expr, "/");
                    break;
                case MODULO:
                    binary(expr, "%");
                    break;
                case COMPARE:
                    binary(expr, "cmp");
                    break;
                case BITWISE_AND:
                    binary(expr, "&");
                    break;
                case BITWISE_OR:
                    binary(expr, "|");
                    break;
                case BITWISE_XOR:
                    binary(expr, "^");
                    break;
                case LEFT_SHIFT:
                    binary(expr, "<<");
                    break;
                case RIGHT_SHIFT:
                    binary(expr, ">>");
                    break;
                case UNSIGNED_RIGHT_SHIFT:
                    binary(expr, ">>>");
                    break;
                case EQUALS:
                    binary(expr, "==");
                    break;
                case NOT_EQUALS:
                    binary(expr, "!=");
                    break;
                case LESS:
                    binary(expr, "<");
                    break;
                case LESS_OR_EQUALS:
                    binary(expr, "<=");
                    break;
                case GREATER:
                    binary(expr, ">");
                    break;
                case GREATER_OR_EQUALS:
                    binary(expr, ">=");
                    break;
                case AND:
                    binary(expr, "&&");
                    break;
                case OR:
                    binary(expr, "||");
                    break;
            }
        }

        @Override
        public void visit(UnaryExpr expr) {
            switch (expr.getOperation()) {
                case NOT:
                    unary(expr, "!");
                    break;
                case NEGATE:
                    unary(expr, "-");
                    break;
                case LENGTH:
                    unary(expr, "length");
                    break;
                case NULL_CHECK:
                    unary(expr, "!!");
                    break;
                case INT_TO_BYTE:
                    unary(expr, "i2b");
                    break;
                case INT_TO_CHAR:
                    unary(expr, "i2c");
                    break;
                case INT_TO_SHORT:
                    unary(expr, "i2s");
                    break;
            }
        }

        @Override
        public void visit(InvocationExpr expr) {
            sb.append("(call-").append(expr.getType().name().toLowerCase()).append(" ");
            append(expr.getMethod());
            for (int i = 0; i < expr.getArguments().size(); ++i) {
                sb.append(" ");
                print(expr.getArguments().get(i));
            }
            sb.append(")");
        }

        private void append(MethodReference methodReference) {
            String className = methodReference.getClassName();
            int index = className.lastIndexOf('.') + 1;
            sb.append(className, index, className.length()).append('.').append(methodReference.getName());
        }

        @Override
        public void visit(ConstantExpr expr) {
            Object value = expr.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof Boolean) {
                sb.append((boolean) value);
            } else if (value instanceof Byte) {
                sb.append((byte) value).append("b");
            } else if (value instanceof Short) {
                sb.append((short) value).append("s");
            } else if (value instanceof Character) {
                sb.append('\'');
                appendCharacter((char) value);
                sb.append("'c");
            } else if (value instanceof Integer) {
                sb.append((int) value).append("i");
            } else if (value instanceof Long) {
                sb.append((long) value).append("l");
            } else if (value instanceof Float) {
                sb.append((float) value).append("f");
            } else if (value instanceof Double) {
                sb.append((double) value).append("d");
            } else if (value instanceof String) {
                String s = (String) value;
                sb.append('\'');
                for (int i = 0; i < s.length(); ++i) {
                    appendCharacter(s.charAt(i));
                }
                sb.append("'s");
            }
        }

        private void appendCharacter(char c) {
            if (c < ' ') {
                sb.append("\\u00");
                sb.append(Character.forDigit(c / 16, 16));
                sb.append(Character.forDigit(c % 16, 16));
            } else if (c == '\'') {
                sb.append("\\'");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else {
                sb.append(c);
            }
        }

        @Override
        public void visit(VariableExpr expr) {
            sb.append("var_").append(expr.getIndex());
        }

        @Override
        public void visit(ReturnStatement statement) {
            sb.append("return");
            if (statement.getResult() != null) {
                sb.append(" ");
                print(statement.getResult());
            }
            newLine();
        }

        @Override
        public void visit(AssignmentStatement statement) {
            if (statement.getLeftValue() != null) {
                sb.append("assign ");
                print(statement.getLeftValue());
                sb.append(" ");
            }
            print(statement.getRightValue());
            newLine();
        }

        @Override
        public void visit(ConditionalStatement statement) {
            sb.append("if ");
            print(statement.getCondition());
            newLine();
            indent();
            visit(statement.getConsequent());
            outdent();
            if (!statement.getAlternative().isEmpty()) {
                sb.append("else");
                newLine();
                indent();
                visit(statement.getAlternative());
                outdent();
            }
            sb.append("end");
            newLine();
        }

        @Override
        public void visit(BlockStatement statement) {
            int id = blockIds.size();
            blockIds.put(statement, id);
            sb.append("block ").append(id);
            newLine();
            indent();
            visit(statement.getBody());
            outdent();
            sb.append("end");
            newLine();
        }

        @Override
        public void visit(BreakStatement statement) {
            var id = statement.getTarget() != null ? blockIds.get(statement.getTarget()) : null;
            sb.append("break ");
            if (id == null) {
                sb.append("<null>");
            } else {
                sb.append((int) id);
            }
            newLine();
        }

        @Override
        public void visit(WhileStatement statement) {
            int id = blockIds.size();
            blockIds.put(statement, id);
            sb.append("loop ").append(id);
            if (statement.getCondition() != null) {
                sb.append(" while ");
                print(statement.getCondition());
            }
            indent();
            newLine();
            visit(statement.getBody());
            outdent();
            sb.append("end");
            newLine();
        }

        @Override
        public void visit(SwitchStatement statement) {
            int id = blockIds.size();
            blockIds.put(statement, id);
            sb.append("switch ").append(id).append(" of ");
            print(statement.getValue());
            indent();
            newLine();
            for (var clause : statement.getClauses()) {
                sb.append("case");
                for (var condition : clause.getConditions()) {
                    sb.append(" ").append(condition);
                    indent();
                    newLine();
                    visit(clause.getBody());
                    outdent();
                    sb.append("end");
                    newLine();
                }
            }

            sb.append("default");
            indent();
            newLine();
            visit(statement.getDefaultClause());
            outdent();
            sb.append("end");
            newLine();

            outdent();
            sb.append("end");
            newLine();
        }

        @Override
        public void visit(ContinueStatement statement) {
            var id = statement.getTarget() != null ? blockIds.get(statement.getTarget()) : null;
            sb.append("continue ");
            if (id == null) {
                sb.append("<null>");
            } else {
                sb.append((int) id);
            }
            newLine();
        }

        private void binary(BinaryExpr expr, String op) {
            sb.append("(").append(op).append(typeToString(expr.getType())).append(" ");
            print(expr.getFirstOperand());
            sb.append(" ");
            print(expr.getSecondOperand());
            sb.append(")");
        }

        private void unary(UnaryExpr expr, String op) {
            sb.append("(").append(op).append(typeToString(expr.getType())).append(" ");
            print(expr.getOperand());
            sb.append(")");
        }

        private String typeToString(OperationType type) {
            if (type == null) {
                return "";
            }
            switch (type) {
                case INT:
                    return "i";
                case LONG:
                    return "l";
                case FLOAT:
                    return "f";
                case DOUBLE:
                    return "d";
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public void visit(List<Statement> statements) {
            for (var statement : statements) {
                print(statement);
            }
        }
    };
}
