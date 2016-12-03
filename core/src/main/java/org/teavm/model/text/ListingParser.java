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
package org.teavm.model.text;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryBranchingCondition;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BinaryOperation;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.NumericOperandType;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.StringConstantInstruction;

public class ListingParser {
    private Program program;
    private ListingLexer lexer;
    private Map<String, Variable> variableMap;
    private Map<String, BasicBlock> blockMap;
    private Map<String, Integer> blockFirstOccurence;
    private Set<String> declaredBlocks = new HashSet<>();
    private TextLocation currentLocation;

    public Program parse(Reader reader) throws IOException, ListingParseException {
        try {
            program = new Program();
            lexer = new ListingLexer(reader);
            variableMap = new HashMap<>();
            blockMap = new HashMap<>();
            blockFirstOccurence = new LinkedHashMap<>();

            lexer.nextToken();
            parsePrologue();

            do {
                parseBasicBlock();
            } while (lexer.getToken() != ListingToken.EOF);

            if (!blockFirstOccurence.isEmpty()) {
                String blockName = blockFirstOccurence.keySet().iterator().next();
                int blockIndex = blockFirstOccurence.get(blockName);
                throw new ListingParseException("Block not defined: " + blockName, blockIndex);
            }

            return program;
        } finally {
            program = null;
            lexer = null;
            variableMap = null;
            blockMap = null;
            blockFirstOccurence = null;
        }
    }

    private void parsePrologue() throws IOException, ListingParseException {
        while (true) {
            while (lexer.getToken() == ListingToken.EOL) {
                lexer.nextToken();
            }

            if (lexer.getToken() != ListingToken.IDENTIFIER || !lexer.getTokenValue().equals("var")) {
                break;
            }
            lexer.nextToken();

            expect(ListingToken.VARIABLE);
            String variableName = (String) lexer.getTokenValue();
            if (variableMap.containsKey(variableName)) {
                throw new ListingParseException("Variable " + variableName + " already declared",
                        lexer.getTokenStart());
            }
            lexer.nextToken();

            expectKeyword("as");

            expect(ListingToken.IDENTIFIER);
            String variableAlias = (String) lexer.getTokenValue();
            lexer.nextToken();

            expectEofOrEol();

            Variable variable = program.createVariable();
            variable.setLabel(variableName);
            variable.setDebugName(variableAlias);
            variableMap.put(variableName, variable);
        }
    }

    private void parseBasicBlock() throws IOException, ListingParseException {
        expect(ListingToken.LABEL);
        String label = (String) lexer.getTokenValue();
        if (!declaredBlocks.add(label)) {
            throw new ListingParseException("Block with label " + label + " already exists", lexer.getTokenStart());
        }
        blockFirstOccurence.remove(label);
        lexer.nextToken();

        expect(ListingToken.EOL);
        while (lexer.getToken() == ListingToken.EOL) {
            lexer.nextToken();
        }

        BasicBlock block = blockMap.computeIfAbsent(label, k -> {
            BasicBlock b = program.createBasicBlock();
            b.setLabel(k);
            return b;
        });

        currentLocation = null;
        do {
            parseInstruction(block);
        } while (lexer.getToken() != ListingToken.LABEL && lexer.getToken() != ListingToken.EOF);

        while (lexer.getToken() == ListingToken.EOL) {
            lexer.nextToken();
        }
    }

    private void parseInstruction(BasicBlock block) throws IOException, ListingParseException {
        switch (lexer.getToken()) {
            case IDENTIFIER: {
                String id = (String) lexer.getTokenValue();
                switch (id) {
                    case "at": {
                        lexer.nextToken();
                        parseLocation();
                        break;
                    }
                    case "nop": {
                        EmptyInstruction insn = new EmptyInstruction();
                        insn.setLocation(currentLocation);
                        block.getInstructions().add(insn);
                        lexer.nextToken();
                        break;
                    }
                    case "goto": {
                        lexer.nextToken();
                        BasicBlock target = expectBlock();
                        JumpInstruction insn = new JumpInstruction();
                        insn.setLocation(currentLocation);
                        insn.setTarget(target);
                        block.getInstructions().add(insn);
                        break;
                    }
                    case "return": {
                        lexer.nextToken();
                        ExitInstruction insn = new ExitInstruction();
                        insn.setLocation(currentLocation);
                        if (lexer.getToken() == ListingToken.VARIABLE) {
                            insn.setValueToReturn(expectVariable());
                        }
                        block.getInstructions().add(insn);
                        break;
                    }
                    case "if": {
                        lexer.nextToken();
                        parseIf(block);
                        break;
                    }
                    case "invoke":
                    case "invokeStatic":
                    case "invokeVirtual": {
                        parseInvoke(block, null);
                        break;
                    }
                    default:
                        unexpected();
                        break;
                }
                break;
            }

            case VARIABLE: {
                Variable receiver = getVariable((String) lexer.getTokenValue());
                lexer.nextToken();

                switch (lexer.getToken()) {
                    case ASSIGN:
                        lexer.nextToken();
                        parseAssignment(block, receiver);
                        break;
                    case LEFT_SQUARE_BRACKET:
                        lexer.nextToken();
                        parseArrayAssignment(block, receiver);
                        break;
                    default:
                        unexpected();
                        break;
                }
                break;
            }

            default:
                unexpected();
                break;
        }
        expectEofOrEol();

        while (lexer.getToken() == ListingToken.EOL) {
            lexer.nextToken();
        }
    }

    private void parseLocation() throws IOException, ListingParseException {
        if (lexer.getToken() == ListingToken.IDENTIFIER) {
            if (lexer.getTokenValue().equals("unknown")) {
                lexer.nextToken();
                expectKeyword("location");
                currentLocation = null;
                return;
            }
        } else if (lexer.getToken() == ListingToken.STRING) {
            String fileName = (String) lexer.getTokenValue();
            lexer.nextToken();

            if (lexer.getToken() == ListingToken.INTEGER) {
                int lineNumber = (Integer) lexer.getTokenValue();
                lexer.nextToken();
                currentLocation = new TextLocation(fileName, lineNumber);
                return;
            }
        }
        throw new ListingParseException("Unexpected token " + lexer.getToken() + ". "
                + "Expected 'unknown location' or '<string> : <number>'", lexer.getTokenStart());
    }

    private void parseAssignment(BasicBlock block, Variable receiver) throws IOException, ListingParseException {
        switch (lexer.getToken()) {
            case VARIABLE: {
                Variable variable = getVariable((String) lexer.getTokenValue());
                lexer.nextToken();
                parseAssignmentVariable(block, receiver, variable);
                break;
            }
            case IDENTIFIER: {
                String keyword = (String) lexer.getTokenValue();
                switch (keyword) {
                    case "phi":
                        lexer.nextToken();
                        parsePhi(block, receiver);
                        break;
                    case "classOf":
                        lexer.nextToken();
                        parseClassLiteral(block, receiver);
                        break;
                    case "invoke":
                    case "invokeVirtual":
                    case "invokeStatic":
                        parseInvoke(block, receiver);
                        break;
                    default:
                        unexpected();
                        break;
                }
                break;
            }
            case INTEGER: {
                parseIntConstant(block, receiver);
                break;
            }
            case LONG: {
                parseLongConstant(block, receiver);
                break;
            }
            case FLOAT: {
                parseFloatConstant(block, receiver);
                break;
            }
            case DOUBLE: {
                parseDoubleConstant(block, receiver);
                break;
            }
            case STRING: {
                parseStringConstant(block, receiver);
                break;
            }
            default:
                unexpected();
        }
    }

    private void parseAssignmentVariable(BasicBlock block, Variable receiver, Variable variable)
            throws IOException, ListingParseException {
        switch (lexer.getToken()) {
            case EOL:
            case EOF: {
                AssignInstruction insn = new AssignInstruction();
                insn.setLocation(currentLocation);
                insn.setReceiver(receiver);
                insn.setAssignee(variable);
                block.getInstructions().add(insn);
                break;
            }
            case ADD:
                parseBinary(block, receiver, variable, BinaryOperation.ADD);
                break;
            case SUBTRACT:
                parseBinary(block, receiver, variable, BinaryOperation.SUBTRACT);
                break;
            case MULTIPLY:
                parseBinary(block, receiver, variable, BinaryOperation.MULTIPLY);
                break;
            case DIVIDE:
                parseBinary(block, receiver, variable, BinaryOperation.DIVIDE);
                break;
            case REMAINDER:
                parseBinary(block, receiver, variable, BinaryOperation.MODULO);
                break;
            case AND:
                parseBinary(block, receiver, variable, BinaryOperation.AND);
                break;
            case OR:
                parseBinary(block, receiver, variable, BinaryOperation.OR);
                break;
            case XOR:
                parseBinary(block, receiver, variable, BinaryOperation.XOR);
                break;
            case SHIFT_LEFT:
                parseBinary(block, receiver, variable, BinaryOperation.SHIFT_LEFT);
                break;
            case SHIFT_RIGHT:
                parseBinary(block, receiver, variable, BinaryOperation.SHIFT_RIGHT);
                break;
            case SHIFT_RIGHT_UNSIGNED:
                parseBinary(block, receiver, variable, BinaryOperation.SHIFT_RIGHT_UNSIGNED);
                break;
            case IDENTIFIER:
                switch ((String) lexer.getTokenValue()) {
                    case "compareTo":
                        parseBinary(block, receiver, variable, BinaryOperation.COMPARE);
                        break;
                    default:
                        unexpected();
                        break;
                }
                break;
            default:
                unexpected();
        }
    }

    private void parseBinary(BasicBlock block, Variable receiver, Variable first, BinaryOperation operation)
            throws IOException, ListingParseException {
        lexer.nextToken();
        Variable second = expectVariable();
        expectKeyword("as");
        NumericOperandType type = expectNumericType();

        BinaryInstruction instruction = new BinaryInstruction(operation, type);
        instruction.setFirstOperand(first);
        instruction.setSecondOperand(second);
        instruction.setReceiver(receiver);
        instruction.setLocation(currentLocation);

        block.getInstructions().add(instruction);
    }

    private void parseArrayAssignment(BasicBlock block, Variable array) throws IOException, ListingParseException {
        Variable index = expectVariable();
        expect(ListingToken.RIGHT_SQUARE_BRACKET);
        lexer.nextToken();
        expect(ListingToken.ASSIGN);
        lexer.nextToken();
        Variable value = expectVariable();
        expectKeyword("as");
        ArrayElementType type = expectArrayType();

        PutElementInstruction insn = new PutElementInstruction(type);
        insn.setLocation(currentLocation);
        insn.setArray(array);
        insn.setIndex(index);
        insn.setValue(value);
        block.getInstructions().add(insn);
    }

    private void parsePhi(BasicBlock block, Variable receiver) throws IOException, ListingParseException {
        int phiStart = lexer.getIndex();

        Phi phi = new Phi();
        phi.setReceiver(receiver);
        while (true) {
            Incoming incoming = new Incoming();
            incoming.setValue(expectVariable());
            expectKeyword("from");
            incoming.setSource(expectBlock());
            phi.getIncomings().add(incoming);

            if (lexer.getToken() != ListingToken.COMMA) {
                break;
            }
            lexer.nextToken();
        }

        if (!block.getInstructions().isEmpty() || block.getExceptionVariable() != null) {
            throw new ListingParseException("Phi must be first instruction in block", phiStart);
        }

        block.getPhis().add(phi);
    }

    private void parseClassLiteral(BasicBlock block, Variable receiver) throws IOException, ListingParseException {
        expect(ListingToken.IDENTIFIER);
        String typeString = (String) lexer.getTokenValue();
        ValueType type = ValueType.parseIfPossible(typeString);
        if (type == null) {
            throw new ListingParseException("Unparseable type: " + typeString, lexer.getTokenStart());
        }
        lexer.nextToken();

        ClassConstantInstruction insn = new ClassConstantInstruction();
        insn.setReceiver(receiver);
        insn.setConstant(type);
        insn.setLocation(currentLocation);
        block.getInstructions().add(insn);
    }

    private void parseIntConstant(BasicBlock block, Variable receiver) throws IOException, ListingParseException {
        IntegerConstantInstruction insn = new IntegerConstantInstruction();
        insn.setReceiver(receiver);
        insn.setConstant((Integer) lexer.getTokenValue());
        insn.setLocation(currentLocation);
        block.getInstructions().add(insn);
        lexer.nextToken();
    }

    private void parseLongConstant(BasicBlock block, Variable receiver) throws IOException, ListingParseException {
        LongConstantInstruction insn = new LongConstantInstruction();
        insn.setReceiver(receiver);
        insn.setConstant((Long) lexer.getTokenValue());
        insn.setLocation(currentLocation);
        block.getInstructions().add(insn);
        lexer.nextToken();
    }

    private void parseFloatConstant(BasicBlock block, Variable receiver) throws IOException, ListingParseException {
        FloatConstantInstruction insn = new FloatConstantInstruction();
        insn.setReceiver(receiver);
        insn.setConstant((Float) lexer.getTokenValue());
        insn.setLocation(currentLocation);
        block.getInstructions().add(insn);
        lexer.nextToken();
    }

    private void parseDoubleConstant(BasicBlock block, Variable receiver) throws IOException, ListingParseException {
        DoubleConstantInstruction insn = new DoubleConstantInstruction();
        insn.setReceiver(receiver);
        insn.setConstant((Double) lexer.getTokenValue());
        insn.setLocation(currentLocation);
        block.getInstructions().add(insn);
        lexer.nextToken();
    }

    private void parseStringConstant(BasicBlock block, Variable receiver) throws IOException, ListingParseException {
        StringConstantInstruction insn = new StringConstantInstruction();
        insn.setReceiver(receiver);
        insn.setConstant((String) lexer.getTokenValue());
        insn.setLocation(currentLocation);
        block.getInstructions().add(insn);
        lexer.nextToken();
    }

    private void parseInvoke(BasicBlock block, Variable receiver) throws IOException, ListingParseException {
        InvokeInstruction insn = new InvokeInstruction();
        insn.setReceiver(receiver);

        boolean hasInstance = true;
        switch ((String) lexer.getTokenValue()) {
            case "invoke":
                insn.setType(InvocationType.SPECIAL);
                break;
            case "invokeStatic":
                insn.setType(InvocationType.SPECIAL);
                hasInstance = false;
                break;
            case "invokeVirtual":
                insn.setType(InvocationType.VIRTUAL);
                break;
        }
        lexer.nextToken();


        expect(ListingToken.IDENTIFIER);
        MethodReference method = MethodReference.parseIfPossible((String) lexer.getTokenValue());
        if (method == null) {
            throw new ListingParseException("Unparseable method", lexer.getIndex());
        }
        insn.setMethod(method);
        lexer.nextToken();

        List<Variable> arguments = new ArrayList<>();
        if (lexer.getToken() == ListingToken.VARIABLE) {
            arguments.add(expectVariable());
            while (lexer.getToken() == ListingToken.COMMA) {
                lexer.nextToken();
                arguments.add(expectVariable());
            }
        }

        if (hasInstance) {
            if (arguments.isEmpty()) {
                throw new ListingParseException("This kind of invocation requires at least one argument",
                        lexer.getIndex());
            }
            insn.setInstance(arguments.get(0));
            insn.getArguments().addAll(arguments.subList(1, arguments.size()));
        } else {
            insn.getArguments().addAll(arguments);
        }

        block.getInstructions().add(insn);
    }

    private void parseIf(BasicBlock block) throws IOException, ListingParseException {
        Variable first = expectVariable();

        BinaryBranchingCondition binaryCondition = null;
        BranchingCondition condition;
        int operationIndex = lexer.getIndex();
        ListingToken operationToken = lexer.getToken();
        switch (lexer.getToken()) {
            case EQUAL:
                binaryCondition = BinaryBranchingCondition.EQUAL;
                condition = BranchingCondition.EQUAL;
                break;
            case NOT_EQUAL:
                binaryCondition = BinaryBranchingCondition.NOT_EQUAL;
                condition = BranchingCondition.NOT_EQUAL;
                break;
            case REFERENCE_EQUAL:
                binaryCondition = BinaryBranchingCondition.REFERENCE_EQUAL;
                condition = BranchingCondition.NULL;
                break;
            case REFERENCE_NOT_EQUAL:
                binaryCondition = BinaryBranchingCondition.REFERENCE_NOT_EQUAL;
                condition = BranchingCondition.NOT_NULL;
                break;
            case LESS:
                condition = BranchingCondition.LESS;
                break;
            case LESS_OR_EQUAL:
                condition = BranchingCondition.LESS_OR_EQUAL;
                break;
            case GREATER:
                condition = BranchingCondition.GREATER;
                break;
            case GREATER_OR_EQUAL:
                condition = BranchingCondition.GREATER_OR_EQUAL;
                break;
            default:
                throw new ListingParseException("Unexpected token" + lexer.getToken()
                        + ". Expected comparison operator", lexer.getTokenStart());
        }
        lexer.nextToken();

        Variable second = null;
        if (lexer.getToken() == ListingToken.VARIABLE) {
            second = expectVariable();
        } else {
            if (condition == BranchingCondition.NULL || condition == BranchingCondition.NOT_NULL) {
                expectKeyword("null");
            } else {
                expect(ListingToken.INTEGER);
                if (!lexer.getTokenValue().equals(0)) {
                    throw new ListingParseException("Only comparison to 0 is supported", lexer.getTokenStart());
                }
                lexer.nextToken();
            }
        }

        expectKeyword("then");
        expectKeyword("goto");
        BasicBlock consequent = expectBlock();

        expectKeyword("else");
        expectKeyword("goto");
        BasicBlock alternative = expectBlock();

        if (second != null) {
            if (binaryCondition == null) {
                throw new ListingParseException("Unsupported binary operation: " + operationToken, operationIndex);
            }
            BinaryBranchingInstruction insn = new BinaryBranchingInstruction(binaryCondition);
            insn.setLocation(currentLocation);
            insn.setFirstOperand(first);
            insn.setSecondOperand(second);
            insn.setConsequent(consequent);
            insn.setAlternative(alternative);
            block.getInstructions().add(insn);
        } else {
            BranchingInstruction insn = new BranchingInstruction(condition);
            insn.setLocation(currentLocation);
            insn.setOperand(first);
            insn.setConsequent(consequent);
            insn.setAlternative(alternative);
            block.getInstructions().add(insn);
        }
    }

    private ArrayElementType expectArrayType() throws IOException, ListingParseException {
        expect(ListingToken.IDENTIFIER);
        ArrayElementType type;
        switch ((String) lexer.getTokenValue()) {
            case "char":
                type = ArrayElementType.CHAR;
                break;
            case "byte":
                type = ArrayElementType.BYTE;
                break;
            case "short":
                type = ArrayElementType.SHORT;
                break;
            case "int":
                type = ArrayElementType.INT;
                break;
            case "long":
                type = ArrayElementType.LONG;
                break;
            case "float":
                type = ArrayElementType.FLOAT;
                break;
            case "double":
                type = ArrayElementType.DOUBLE;
                break;
            case "object":
                type = ArrayElementType.OBJECT;
                break;
            default:
                throw new ListingParseException("Unknown array type: " + lexer.getTokenValue(), lexer.getTokenStart());
        }
        lexer.nextToken();
        return type;
    }

    private NumericOperandType expectNumericType() throws IOException, ListingParseException {
        expect(ListingToken.IDENTIFIER);
        NumericOperandType type;
        switch ((String) lexer.getTokenValue()) {
            case "int":
                type = NumericOperandType.INT;
                break;
            case "long":
                type = NumericOperandType.LONG;
                break;
            case "float":
                type = NumericOperandType.FLOAT;
                break;
            case "double":
                type = NumericOperandType.DOUBLE;
                break;
            default:
                throw new ListingParseException("Unknown numeric type: " + lexer.getTokenValue(),
                        lexer.getTokenStart());
        }
        lexer.nextToken();
        return type;
    }

    private Variable expectVariable() throws IOException, ListingParseException {
        expect(ListingToken.VARIABLE);
        String variableName = (String) lexer.getTokenValue();
        Variable variable = getVariable(variableName);
        lexer.nextToken();
        return variable;
    }

    private Variable getVariable(String name) {
        return variableMap.computeIfAbsent(name, k -> {
            Variable variable = program.createVariable();
            variable.setLabel(k);
            return variable;
        });
    }

    private BasicBlock expectBlock() throws IOException, ListingParseException {
        expect(ListingToken.LABEL);
        String blockName = (String) lexer.getTokenValue();
        BasicBlock block = getBlock(blockName);
        lexer.nextToken();
        return block;
    }

    private BasicBlock getBlock(String name) {
        return blockMap.computeIfAbsent(name, k -> {
            BasicBlock block = program.createBasicBlock();
            block.setLabel(k);
            blockFirstOccurence.put(k, lexer.getTokenStart());
            return block;
        });
    }

    private void expect(ListingToken expected) throws IOException, ListingParseException {
        if (lexer.getToken() != expected) {
            throw new ListingParseException("Unexpected token " + lexer.getToken()
                    + ". Expected " + expected, lexer.getTokenStart());
        }
    }

    private void expectEofOrEol() throws IOException, ListingParseException {
        if (lexer.getToken() != ListingToken.EOL && lexer.getToken() != ListingToken.EOF) {
            throw new ListingParseException("Unexpected token " + lexer.getToken()
                    + ". Expected new line", lexer.getTokenStart());
        }
        if (lexer.getToken() != ListingToken.EOF) {
            lexer.nextToken();
        }
    }

    private String expectKeyword(String expected) throws IOException, ListingParseException {
        if (lexer.getToken() != ListingToken.IDENTIFIER || !lexer.getTokenValue().equals(expected)) {
            throw new ListingParseException("Unexpected token " + lexer.getToken()
                    + ". Expected " + expected, lexer.getTokenStart());
        }
        String value = (String) lexer.getTokenValue();
        lexer.nextToken();
        return value;
    }

    private void unexpected() throws IOException, ListingParseException {
        throw new ListingParseException("Unexpected token " + lexer.getToken(), lexer.getTokenStart());
    }
}
