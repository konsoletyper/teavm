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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.teavm.model.BasicBlock;
import org.teavm.model.FieldReference;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryBranchingCondition;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BinaryOperation;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.CastIntegerDirection;
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
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.IntegerSubtype;
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
import org.teavm.model.instructions.SwitchTableEntry;
import org.teavm.model.instructions.UnwrapArrayInstruction;

public class ListingParser {
    private Program program;
    private ListingLexer lexer;
    private Map<String, Variable> variableMap;
    private Map<String, BasicBlock> blockMap;
    private Map<String, Integer> blockFirstOccurrence;
    private Set<String> declaredBlocks = new HashSet<>();
    private List<BasicBlock> orderedBlocks = new ArrayList<>();
    private TextLocation currentLocation;
    private BasicBlock currentBlock;

    public Program parse(Reader reader) throws IOException, ListingParseException {
        try {
            program = new Program();
            lexer = new ListingLexer(reader);
            variableMap = new HashMap<>();
            blockMap = new HashMap<>();
            blockFirstOccurrence = new LinkedHashMap<>();

            lexer.nextToken();
            parsePrologue();

            do {
                parseBasicBlock();
            } while (lexer.getToken() != ListingToken.EOF);

            if (!blockFirstOccurrence.isEmpty()) {
                String blockName = blockFirstOccurrence.keySet().iterator().next();
                int blockIndex = blockFirstOccurrence.get(blockName);
                throw new ListingParseException("Block not defined: " + blockName, blockIndex);
            }

            program.pack();
            program.rearrangeBasicBlocks(orderedBlocks);

            return program;
        } finally {
            program = null;
            lexer = null;
            variableMap = null;
            blockMap = null;
            blockFirstOccurrence = null;
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
        blockFirstOccurrence.remove(label);
        lexer.nextToken();

        expect(ListingToken.EOL);
        while (lexer.getToken() == ListingToken.EOL) {
            lexer.nextToken();
        }

        currentBlock = blockMap.computeIfAbsent(label, k -> {
            BasicBlock b = program.createBasicBlock();
            b.setLabel(k);
            return b;
        });
        orderedBlocks.add(currentBlock);

        currentLocation = null;
        do {
            parseInstruction();
        } while (lexer.getToken() != ListingToken.LABEL && lexer.getToken() != ListingToken.EOF);

        while (lexer.getToken() == ListingToken.EOL) {
            lexer.nextToken();
        }
    }

    private void parseInstruction() throws IOException, ListingParseException {
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
                        addInstruction(insn);
                        lexer.nextToken();
                        break;
                    }
                    case "goto": {
                        lexer.nextToken();
                        BasicBlock target = expectBlock();
                        JumpInstruction insn = new JumpInstruction();
                        insn.setTarget(target);
                        addInstruction(insn);
                        break;
                    }
                    case "return": {
                        lexer.nextToken();
                        ExitInstruction insn = new ExitInstruction();
                        if (lexer.getToken() == ListingToken.VARIABLE) {
                            insn.setValueToReturn(expectVariable());
                        }
                        addInstruction(insn);
                        break;
                    }
                    case "throw": {
                        lexer.nextToken();
                        RaiseInstruction insn = new RaiseInstruction();
                        insn.setException(expectVariable());
                        addInstruction(insn);
                        break;
                    }
                    case "if": {
                        lexer.nextToken();
                        parseIf();
                        break;
                    }
                    case "invoke":
                    case "invokeStatic":
                    case "invokeVirtual": {
                        parseInvoke(null);
                        break;
                    }
                    case "initClass": {
                        lexer.nextToken();
                        InitClassInstruction insn = new InitClassInstruction();
                        expect(ListingToken.IDENTIFIER);
                        insn.setClassName((String) lexer.getTokenValue());
                        lexer.nextToken();
                        addInstruction(insn);
                        break;
                    }
                    case "monitorEnter": {
                        lexer.nextToken();
                        MonitorEnterInstruction insn = new MonitorEnterInstruction();
                        insn.setObjectRef(expectVariable());
                        addInstruction(insn);
                        break;
                    }
                    case "monitorExit": {
                        lexer.nextToken();
                        MonitorExitInstruction insn = new MonitorExitInstruction();
                        insn.setObjectRef(expectVariable());
                        addInstruction(insn);
                        break;
                    }
                    case "field": {
                        lexer.nextToken();
                        parseFieldSet();
                        break;
                    }
                    case "switch": {
                        lexer.nextToken();
                        parseSwitch();
                        break;
                    }
                    case "catch": {
                        lexer.nextToken();
                        parseCatch();
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
                        parseAssignment(receiver);
                        break;
                    case LEFT_SQUARE_BRACKET:
                        lexer.nextToken();
                        parseArrayAssignment(receiver);
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

    private void parseAssignment(Variable receiver) throws IOException, ListingParseException {
        switch (lexer.getToken()) {
            case VARIABLE: {
                Variable variable = getVariable((String) lexer.getTokenValue());
                lexer.nextToken();
                parseAssignmentVariable(receiver, variable);
                break;
            }
            case IDENTIFIER: {
                String keyword = (String) lexer.getTokenValue();
                switch (keyword) {
                    case "null": {
                        lexer.nextToken();
                        NullConstantInstruction insn = new NullConstantInstruction();
                        insn.setReceiver(receiver);
                        addInstruction(insn);
                        break;
                    }
                    case "phi":
                        lexer.nextToken();
                        parsePhi(receiver);
                        break;
                    case "classOf":
                        lexer.nextToken();
                        parseClassLiteral(receiver);
                        break;
                    case "invoke":
                    case "invokeVirtual":
                    case "invokeStatic":
                        parseInvoke(receiver);
                        break;
                    case "cast":
                        parseCast(receiver);
                        break;
                    case "new":
                        parseNew(receiver);
                        break;
                    case "newArray":
                        parseNewArray(receiver);
                        break;
                    case "nullCheck": {
                        lexer.nextToken();
                        NullCheckInstruction insn = new NullCheckInstruction();
                        insn.setReceiver(receiver);
                        insn.setValue(expectVariable());
                        addInstruction(insn);
                        break;
                    }
                    case "data": {
                        lexer.nextToken();
                        Variable value = expectVariable();
                        expectKeyword("as");
                        ArrayElementType type = expectArrayType();
                        UnwrapArrayInstruction insn = new UnwrapArrayInstruction(type);
                        insn.setArray(value);
                        insn.setReceiver(receiver);
                        addInstruction(insn);
                        break;
                    }
                    case "lengthOf": {
                        lexer.nextToken();
                        ArrayLengthInstruction insn = new ArrayLengthInstruction();
                        insn.setArray(expectVariable());
                        insn.setReceiver(receiver);
                        addInstruction(insn);
                        break;
                    }
                    case "clone": {
                        lexer.nextToken();
                        CloneArrayInstruction insn = new CloneArrayInstruction();
                        insn.setArray(expectVariable());
                        insn.setReceiver(receiver);
                        addInstruction(insn);
                        break;
                    }
                    case "field": {
                        lexer.nextToken();
                        parseFieldGet(receiver);
                        break;
                    }
                    case "exception": {
                        lexer.nextToken();
                        if (currentBlock.instructionCount() > 0 || currentBlock.getExceptionVariable() != null) {
                            throw new ListingParseException("Exception can be read as a first instruction",
                                    lexer.getTokenStart());
                        }
                        currentBlock.setExceptionVariable(receiver);
                        break;
                    }
                    default:
                        unexpected();
                        break;
                }
                break;
            }
            case INTEGER: {
                parseIntConstant(receiver);
                break;
            }
            case LONG: {
                parseLongConstant(receiver);
                break;
            }
            case FLOAT: {
                parseFloatConstant(receiver);
                break;
            }
            case DOUBLE: {
                parseDoubleConstant(receiver);
                break;
            }
            case STRING: {
                parseStringConstant(receiver);
                break;
            }
            case SUBTRACT: {
                parseNegate(receiver);
                break;
            }
            default:
                unexpected();
        }
    }

    private void parseAssignmentVariable(Variable receiver, Variable variable)
            throws IOException, ListingParseException {
        switch (lexer.getToken()) {
            case EOL:
            case EOF: {
                AssignInstruction insn = new AssignInstruction();
                insn.setReceiver(receiver);
                insn.setAssignee(variable);
                addInstruction(insn);
                break;
            }
            case ADD:
                parseBinary(receiver, variable, BinaryOperation.ADD);
                break;
            case SUBTRACT:
                parseBinary(receiver, variable, BinaryOperation.SUBTRACT);
                break;
            case MULTIPLY:
                parseBinary(receiver, variable, BinaryOperation.MULTIPLY);
                break;
            case DIVIDE:
                parseBinary(receiver, variable, BinaryOperation.DIVIDE);
                break;
            case REMAINDER:
                parseBinary(receiver, variable, BinaryOperation.MODULO);
                break;
            case AND:
                parseBinary(receiver, variable, BinaryOperation.AND);
                break;
            case OR:
                parseBinary(receiver, variable, BinaryOperation.OR);
                break;
            case XOR:
                parseBinary(receiver, variable, BinaryOperation.XOR);
                break;
            case SHIFT_LEFT:
                parseBinary(receiver, variable, BinaryOperation.SHIFT_LEFT);
                break;
            case SHIFT_RIGHT:
                parseBinary(receiver, variable, BinaryOperation.SHIFT_RIGHT);
                break;
            case SHIFT_RIGHT_UNSIGNED:
                parseBinary(receiver, variable, BinaryOperation.SHIFT_RIGHT_UNSIGNED);
                break;
            case LEFT_SQUARE_BRACKET:
                parseSubscript(receiver, variable);
                break;
            case IDENTIFIER:
                switch ((String) lexer.getTokenValue()) {
                    case "compareTo":
                        parseBinary(receiver, variable, BinaryOperation.COMPARE);
                        break;
                    case "instanceOf": {
                        lexer.nextToken();
                        ValueType type = expectValueType();
                        IsInstanceInstruction insn = new IsInstanceInstruction();
                        insn.setValue(variable);
                        insn.setReceiver(receiver);
                        insn.setType(type);
                        addInstruction(insn);
                        break;
                    }
                    default:
                        unexpected();
                        break;
                }
                break;
            default:
                unexpected();
        }
    }

    private void parseBinary(Variable receiver, Variable first, BinaryOperation operation)
            throws IOException, ListingParseException {
        lexer.nextToken();
        Variable second = expectVariable();
        expectKeyword("as");
        NumericOperandType type = expectNumericType();

        BinaryInstruction insn = new BinaryInstruction(operation, type);
        insn.setFirstOperand(first);
        insn.setSecondOperand(second);
        insn.setReceiver(receiver);

        addInstruction(insn);
    }

    private void parseSubscript(Variable receiver, Variable array) throws IOException, ListingParseException {
        lexer.nextToken();
        Variable index = expectVariable();
        expect(ListingToken.RIGHT_SQUARE_BRACKET);
        lexer.nextToken();
        expectKeyword("as");

        ArrayElementType type = expectArrayType();

        GetElementInstruction insn = new GetElementInstruction(type);
        insn.setReceiver(receiver);
        insn.setArray(array);
        insn.setIndex(index);

        addInstruction(insn);
    }

    private void parseArrayAssignment(Variable array) throws IOException, ListingParseException {
        Variable index = expectVariable();
        expect(ListingToken.RIGHT_SQUARE_BRACKET);
        lexer.nextToken();
        expect(ListingToken.ASSIGN);
        lexer.nextToken();
        Variable value = expectVariable();
        expectKeyword("as");
        ArrayElementType type = expectArrayType();

        PutElementInstruction insn = new PutElementInstruction(type);
        insn.setArray(array);
        insn.setIndex(index);
        insn.setValue(value);
        addInstruction(insn);
    }

    private void parsePhi(Variable receiver) throws IOException, ListingParseException {
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

        if (currentBlock.instructionCount() > 0 || currentBlock.getExceptionVariable() != null) {
            throw new ListingParseException("Phi must be first instruction in block", phiStart);
        }

        currentBlock.getPhis().add(phi);
    }

    private void parseClassLiteral(Variable receiver) throws IOException, ListingParseException {
        ValueType type = expectValueType();
        ClassConstantInstruction insn = new ClassConstantInstruction();
        insn.setReceiver(receiver);
        insn.setConstant(type);
        addInstruction(insn);
    }

    private void parseIntConstant(Variable receiver) throws IOException, ListingParseException {
        IntegerConstantInstruction insn = new IntegerConstantInstruction();
        insn.setReceiver(receiver);
        insn.setConstant((Integer) lexer.getTokenValue());
        lexer.nextToken();
        addInstruction(insn);
    }

    private void parseLongConstant(Variable receiver) throws IOException, ListingParseException {
        LongConstantInstruction insn = new LongConstantInstruction();
        insn.setReceiver(receiver);
        insn.setConstant((Long) lexer.getTokenValue());
        lexer.nextToken();
        addInstruction(insn);
    }

    private void parseFloatConstant(Variable receiver) throws IOException, ListingParseException {
        FloatConstantInstruction insn = new FloatConstantInstruction();
        insn.setReceiver(receiver);
        insn.setConstant((Float) lexer.getTokenValue());
        lexer.nextToken();
        addInstruction(insn);
    }

    private void parseDoubleConstant(Variable receiver) throws IOException, ListingParseException {
        DoubleConstantInstruction insn = new DoubleConstantInstruction();
        insn.setReceiver(receiver);
        insn.setConstant((Double) lexer.getTokenValue());
        lexer.nextToken();
        addInstruction(insn);
    }

    private void parseStringConstant(Variable receiver) throws IOException, ListingParseException {
        StringConstantInstruction insn = new StringConstantInstruction();
        insn.setReceiver(receiver);
        insn.setConstant((String) lexer.getTokenValue());
        lexer.nextToken();
        addInstruction(insn);
    }

    private void parseNegate(Variable receiver) throws IOException, ListingParseException {
        lexer.nextToken();
        Variable value = expectVariable();
        expectKeyword("as");
        NumericOperandType type = expectNumericType();
        NegateInstruction insn = new NegateInstruction(type);
        insn.setReceiver(receiver);
        insn.setOperand(value);
        addInstruction(insn);
    }

    private void parseInvoke(Variable receiver) throws IOException, ListingParseException {
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
            insn.setArguments(arguments.subList(1, arguments.size()).toArray(new Variable[0]));
        } else {
            insn.setArguments(arguments.toArray(new Variable[0]));
        }

        addInstruction(insn);
    }

    private void parseFieldSet() throws IOException, ListingParseException {
        FieldReference field = expectField();
        Variable instance = null;
        if (lexer.getToken() == ListingToken.VARIABLE) {
            instance = expectVariable();
        }
        expect(ListingToken.ASSIGN);
        lexer.nextToken();
        Variable value = expectVariable();
        expectKeyword("as");
        ValueType type = expectValueType();

        PutFieldInstruction insn = new PutFieldInstruction();
        insn.setValue(value);
        insn.setField(field);
        insn.setInstance(instance);
        insn.setFieldType(type);
        addInstruction(insn);
    }

    private void parseFieldGet(Variable receiver) throws IOException, ListingParseException {
        FieldReference field = expectField();
        Variable instance = null;
        if (lexer.getToken() == ListingToken.VARIABLE) {
            instance = expectVariable();
        }
        expectKeyword("as");
        ValueType type = expectValueType();

        GetFieldInstruction insn = new GetFieldInstruction();
        insn.setReceiver(receiver);
        insn.setInstance(instance);
        insn.setField(field);
        insn.setFieldType(type);
        addInstruction(insn);
    }

    private void parseSwitch() throws IOException, ListingParseException {
        SwitchInstruction insn = new SwitchInstruction();
        insn.setCondition(expectVariable());

        while (true) {
            expect(ListingToken.IDENTIFIER);
            switch ((String) lexer.getTokenValue()) {
                case "case":
                    lexer.nextToken();
                    SwitchTableEntry entry = new SwitchTableEntry();
                    expect(ListingToken.INTEGER);
                    entry.setCondition((Integer) lexer.getTokenValue());
                    lexer.nextToken();
                    expectKeyword("goto");
                    entry.setTarget(expectBlock());
                    insn.getEntries().add(entry);
                    break;
                case "else":
                    lexer.nextToken();
                    expectKeyword("goto");
                    insn.setDefaultTarget(expectBlock());
                    addInstruction(insn);
                    return;
            }
        }
    }

    private void parseCatch() throws IOException, ListingParseException {
        TryCatchBlock tryCatch = new TryCatchBlock();
        if (lexer.getToken() == ListingToken.IDENTIFIER && !lexer.getTokenValue().equals("goto")) {
            tryCatch.setExceptionType((String) lexer.getTokenValue());
            lexer.nextToken();
        }
        expectKeyword("goto");
        tryCatch.setHandler(expectBlock());
        currentBlock.getTryCatchBlocks().add(tryCatch);
    }

    private void parseCast(Variable receiver) throws IOException, ListingParseException {
        lexer.nextToken();
        Variable value = expectVariable();
        expect(ListingToken.IDENTIFIER);
        switch ((String) lexer.getTokenValue()) {
            case "from":
                parseNumericCast(receiver, value);
                break;
            case "to":
                parseObjectCast(receiver, value);
                break;
            default:
                unexpected();
        }
    }

    private void parseNumericCast(Variable receiver, Variable value) throws IOException, ListingParseException {
        lexer.nextToken();

        NumericTypeOrIntegerSubtype source = expectTypeOrIntegerSubtype();

        if (source.subtype != null) {
            CastIntegerInstruction insn = new CastIntegerInstruction(source.subtype, CastIntegerDirection.TO_INTEGER);
            expectKeyword("to");
            expectKeyword("int");
            insn.setReceiver(receiver);
            insn.setValue(value);
            addInstruction(insn);
        } else {
            expectKeyword("to");
            expect(ListingToken.IDENTIFIER);
            NumericTypeOrIntegerSubtype target = expectTypeOrIntegerSubtype();
            if (target.subtype != null) {
                if (source.type != NumericOperandType.INT) {
                    throw new ListingParseException("Only int can be cast to "
                            + target.subtype.name().toLowerCase(Locale.ROOT), lexer.getIndex());
                }
                CastIntegerInstruction insn = new CastIntegerInstruction(source.subtype,
                        CastIntegerDirection.FROM_INTEGER);
                insn.setReceiver(receiver);
                insn.setValue(value);
                addInstruction(insn);
            } else {
                CastNumberInstruction insn = new CastNumberInstruction(source.type, target.type);
                insn.setReceiver(receiver);
                insn.setValue(value);
                addInstruction(insn);
            }
        }
    }

    private NumericTypeOrIntegerSubtype expectTypeOrIntegerSubtype() throws IOException, ListingParseException {
        IntegerSubtype subtype = null;
        NumericOperandType type = null;
        expect(ListingToken.IDENTIFIER);
        switch ((String) lexer.getTokenValue()) {
            case "byte":
                subtype = IntegerSubtype.BYTE;
                break;
            case "short":
                subtype = IntegerSubtype.SHORT;
                break;
            case "char":
                subtype = IntegerSubtype.CHAR;
                break;
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
                unexpected();
                return null;
        }
        lexer.nextToken();
        return new NumericTypeOrIntegerSubtype(type, subtype);
    }

    private static class NumericTypeOrIntegerSubtype {
        NumericOperandType type;
        IntegerSubtype subtype;

        NumericTypeOrIntegerSubtype(NumericOperandType type, IntegerSubtype subtype) {
            this.type = type;
            this.subtype = subtype;
        }
    }

    private void parseObjectCast(Variable receiver, Variable value) throws IOException, ListingParseException {
        lexer.nextToken();
        ValueType type = expectValueType();

        CastInstruction insn = new CastInstruction();
        insn.setReceiver(receiver);
        insn.setValue(value);
        insn.setTargetType(type);
        addInstruction(insn);
    }

    private void parseNew(Variable receiver) throws IOException, ListingParseException {
        lexer.nextToken();
        expect(ListingToken.IDENTIFIER);
        String type = (String) lexer.getTokenValue();
        lexer.nextToken();

        ConstructInstruction insn = new ConstructInstruction();
        insn.setReceiver(receiver);
        insn.setType(type);
        addInstruction(insn);
    }

    private void parseNewArray(Variable receiver) throws IOException, ListingParseException {
        lexer.nextToken();
        ValueType type = expectValueType();
        List<Variable> dimensions = new ArrayList<>();
        expect(ListingToken.LEFT_SQUARE_BRACKET);
        do {
            lexer.nextToken();
            dimensions.add(expectVariable());
        } while (lexer.getToken() == ListingToken.COMMA);
        expect(ListingToken.RIGHT_SQUARE_BRACKET);
        lexer.nextToken();

        if (dimensions.size() == 1) {
            ConstructArrayInstruction insn = new ConstructArrayInstruction();
            insn.setReceiver(receiver);
            insn.setItemType(type);
            insn.setSize(dimensions.get(0));
            addInstruction(insn);
        } else {
            ConstructMultiArrayInstruction insn = new ConstructMultiArrayInstruction();
            insn.setReceiver(receiver);
            insn.setItemType(type);
            insn.getDimensions().addAll(dimensions);
            addInstruction(insn);
        }
    }

    private void parseIf() throws IOException, ListingParseException {
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
            insn.setFirstOperand(first);
            insn.setSecondOperand(second);
            insn.setConsequent(consequent);
            insn.setAlternative(alternative);
            addInstruction(insn);
        } else {
            BranchingInstruction insn = new BranchingInstruction(condition);
            insn.setOperand(first);
            insn.setConsequent(consequent);
            insn.setAlternative(alternative);
            addInstruction(insn);
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

    private ValueType expectValueType() throws IOException, ListingParseException {
        expect(ListingToken.IDENTIFIER);
        ValueType type = ValueType.parseIfPossible((String) lexer.getTokenValue());
        if (type == null) {
            throw new ListingParseException("Unparseable type", lexer.getTokenStart());
        }
        lexer.nextToken();
        return type;
    }

    private FieldReference expectField() throws IOException, ListingParseException {
        expect(ListingToken.IDENTIFIER);
        String s = (String) lexer.getTokenValue();
        int index = s.lastIndexOf('.');
        if (index < 0) {
            throw new ListingParseException("Unparseable field", lexer.getTokenStart());
        }
        lexer.nextToken();
        return new FieldReference(s.substring(0, index), s.substring(index + 1));
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
            blockFirstOccurrence.put(k, lexer.getTokenStart());
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

    private void addInstruction(Instruction instruction) throws ListingParseException {
        instruction.setLocation(currentLocation);
        currentBlock.add(instruction);
    }
}
