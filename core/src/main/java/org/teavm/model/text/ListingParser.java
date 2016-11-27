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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.model.BasicBlock;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.PutElementInstruction;

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
            blockFirstOccurence = new HashMap<>();

            lexer.nextToken();
            parsePrologue();

            do {
                parseBasicBlock();
            } while (lexer.getToken() != ListingToken.EOF);

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

        BasicBlock block = program.createBasicBlock();
        block.setLabel(label);
        blockMap.put(label, block);

        currentLocation = null;
        do {
            parseInstruction(block);

        } while (lexer.getToken() != ListingToken.LABEL && lexer.getToken() != ListingToken.EOF);

        expectEofOrEol();
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
                }
                break;
            }
        }
        expectEofOrEol();
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
            default:
                unexpected();
        }
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
        insn.setArray(array);
        insn.setIndex(index);
        insn.setValue(value);
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
