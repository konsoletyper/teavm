/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.backend.c.util.json;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

public class JsonParser {
    private JsonConsumer consumer;
    private int lastChar;
    private int lineNumber;
    private int columnNumber;
    private boolean cr;

    public JsonParser(JsonConsumer consumer) {
        this.consumer = consumer;
    }

    public void parse(Reader reader) throws IOException {
        lastChar = reader.read();
        skipWhitespaces(reader);
        if (lastChar == -1) {
            error("Unexpected end of file");
        }
        parseValue(reader);
        skipWhitespaces(reader);
        if (lastChar != -1) {
            error("Unexpected characters after end of JSON string");
        }
    }

    private void parseValue(Reader reader) throws IOException {
        switch (lastChar) {
            case '{':
                parseObject(reader);
                break;
            case '[':
                parseArray(reader);
                break;
            case '\"':
                parseString(reader);
                break;
            case '-':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                parseNumber(reader);
                break;
            case 'n':
                parseNull(reader);
                break;
            case 't':
                parseTrue(reader);
                break;
            case 'f':
                parseFalse(reader);
                break;
            default:
                error("Unexpected character");
                break;
        }
    }

    private void parseObject(Reader reader) throws IOException {
        Set<String> usedPropertyNames = new HashSet<>();

        consumer.enterObject(errorReporter);
        nextChar(reader);
        skipWhitespaces(reader);
        if (lastChar == '}') {
            nextChar(reader);
            consumer.exitObject(errorReporter);
            return;
        }

        parseProperty(reader, usedPropertyNames);
        while (lastChar != '}') {
            if (lastChar != ',') {
                error("Either property delimiter (',') or end of object ('}') expected");
            }
            nextChar(reader);
            skipWhitespaces(reader);
            parseProperty(reader, usedPropertyNames);
        }
        nextChar(reader);
        consumer.exitObject(errorReporter);
    }

    private void parseProperty(Reader reader, Set<String> usedPropertyNames) throws IOException {
        skipWhitespaces(reader);
        if (lastChar != '"') {
            error("Object key (string literal) expected");
        }
        String name = parseStringLiteral(reader);
        if (!usedPropertyNames.add(name)) {
            error("Duplicate object property: " + name);
        }
        consumer.enterProperty(errorReporter, name);
        skipWhitespaces(reader);
        if (lastChar != ':') {
            error("':' character expected after property name");
        }
        nextChar(reader);
        skipWhitespaces(reader);
        parseValue(reader);
        consumer.exitProperty(errorReporter, name);
        skipWhitespaces(reader);
    }

    private void parseArray(Reader reader) throws IOException {
        consumer.enterArray(errorReporter);
        nextChar(reader);
        skipWhitespaces(reader);

        if (lastChar == ']') {
            nextChar(reader);
            consumer.exitObject(errorReporter);
            return;
        }

        parseValue(reader);
        skipWhitespaces(reader);
        while (lastChar != ']') {
            if (lastChar != ',') {
                error("Either array item delimiter (',') or end of array (']') expected");
            }
            nextChar(reader);
            skipWhitespaces(reader);
            parseValue(reader);
            skipWhitespaces(reader);
        }
        nextChar(reader);

        consumer.exitArray(errorReporter);
    }

    private void parseString(Reader reader) throws IOException {
        consumer.stringValue(errorReporter, parseStringLiteral(reader));
    }

    private String parseStringLiteral(Reader reader) throws IOException {
        nextChar(reader);
        StringBuilder sb = new StringBuilder();
        while (lastChar != '\"') {
            if (lastChar == -1) {
                error("Unexpected end of input inside string literal");
            } else if (lastChar < ' ') {
                error("Unexpected control character inside string literal");
            }
            if (lastChar == '\\') {
                nextChar(reader);
                switch (lastChar) {
                    case '\"':
                    case '\\':
                    case '/':
                        sb.append(lastChar);
                        nextChar(reader);
                        break;
                    case 'b':
                        sb.append('\b');
                        nextChar(reader);
                        break;
                    case 'f':
                        sb.append('\f');
                        nextChar(reader);
                        break;
                    case 'n':
                        sb.append('\n');
                        nextChar(reader);
                        break;
                    case 'r':
                        sb.append('\r');
                        nextChar(reader);
                        break;
                    case 't':
                        sb.append('\t');
                        nextChar(reader);
                        break;
                    case 'u':
                        nextChar(reader);
                        int code = (getHexDigit(reader) << 12) | (getHexDigit(reader) << 8)
                                | (getHexDigit(reader) << 4) | getHexDigit(reader);
                        sb.append((char) code);
                        break;
                    default:
                        error("Wrong escape sequence");
                        break;
                }
            } else {
                sb.append((char) lastChar);
                nextChar(reader);
            }
        }
        nextChar(reader);
        return sb.toString();
    }

    private int getHexDigit(Reader reader) throws IOException {
        int value;
        if (lastChar >= '0' && lastChar <= '9') {
            value = lastChar - '0';
        } else if (lastChar >= 'A' && lastChar <= 'F') {
            value = lastChar - 'A' + 10;
        } else if (lastChar >= 'a' && lastChar <= 'f') {
            value = lastChar - 'a' + 10;
        } else {
            error("Wrong escape sequence");
            value = 0;
        }
        nextChar(reader);
        return value;
    }

    private void parseNumber(Reader reader) throws IOException {
        boolean isFloatingPoint = false;
        StringBuilder sb = new StringBuilder();
        if (lastChar == '-') {
            acceptChar(sb, reader);
        }
        if (lastChar == '0') {
            acceptChar(sb, reader);
        } else {
            if (!isDigit(lastChar)) {
                if (lastChar == 'e' || lastChar == 'E' || lastChar == '.') {
                    error("Wrong number, at least one digit expected in integer part");
                } else {
                    error("Wrong number, digits must follow '-' sign");
                }
            }
            acceptChar(sb, reader);
            while (isDigit(lastChar)) {
                acceptChar(sb, reader);
            }
        }

        if (lastChar == '.') {
            isFloatingPoint = true;
            acceptChar(sb, reader);
            if (!isDigit(lastChar)) {
                error("Wrong number, at least one digit must be in fraction part");
            }
            acceptChar(sb, reader);
            while (isDigit(lastChar)) {
                acceptChar(sb, reader);
            }
        }

        if (lastChar == 'e' || lastChar == 'E') {
            isFloatingPoint = true;
            acceptChar(sb, reader);
            if (lastChar == '+' || lastChar == '-') {
                acceptChar(sb, reader);
            }
            if (!isDigit(lastChar)) {
                error("Wrong number, at least one digit must be in exponent");
            }
            acceptChar(sb, reader);
            while (isDigit(lastChar)) {
                acceptChar(sb, reader);
            }
        }

        expectEndOfToken("Wrong number");

        if (isFloatingPoint) {
            tryParseDouble(sb);
        } else {
            long value;
            try {
                value = Long.parseLong(sb.toString());
            } catch (NumberFormatException e) {
                tryParseDouble(sb);
                return;
            }
            consumer.intValue(errorReporter, value);
        }
    }

    private void tryParseDouble(StringBuilder sb) {
        double value;
        try {
            value = Double.parseDouble(sb.toString());
        } catch (NumberFormatException e) {
            error("Wrong number");
            value = 0;
        }
        consumer.floatValue(errorReporter, value);
    }

    private void acceptChar(StringBuilder sb, Reader reader) throws IOException {
        sb.append((char) lastChar);
        nextChar(reader);
    }

    private void parseNull(Reader reader) throws IOException {
        expectIdentifier(reader, "null");
        consumer.nullValue(errorReporter);
    }

    private void parseTrue(Reader reader) throws IOException {
        expectIdentifier(reader, "true");
        consumer.booleanValue(errorReporter, true);
    }

    private void parseFalse(Reader reader) throws IOException {
        expectIdentifier(reader, "false");
        consumer.booleanValue(errorReporter, false);
    }

    private void expectIdentifier(Reader reader, String identifier) throws IOException {
        for (int i = 0; i < identifier.length(); ++i) {
            if (lastChar != identifier.charAt(i)) {
                error("Unexpected identifier");
            }
            nextChar(reader);
        }
        expectEndOfToken("Wrong identifier");
    }

    private void expectEndOfToken(String errorMessage) {
        switch (lastChar) {
            case '}':
            case '{':
            case '[':
            case ']':
            case ',':
            case ':':
                break;
            default:
                if (!isWhitespace(lastChar)) {
                    error(errorMessage);
                }
                break;
        }
    }

    private void skipWhitespaces(Reader reader) throws IOException {
        while (isWhitespace(lastChar)) {
            nextChar(reader);
        }
    }

    private void nextChar(Reader reader) throws IOException {
        boolean wasCr = cr;
        if (cr) {
            lineNumber++;
            columnNumber = 0;
            cr = false;
        }
        switch (lastChar) {
            case '\r':
                cr = true;
                break;
            case '\n':
                if (!wasCr) {
                    lineNumber++;
                    columnNumber = 0;
                }
                break;
            default:
                columnNumber++;
                break;
        }
        lastChar = reader.read();
    }

    void error(String error) {
        throw new JsonSyntaxException(lineNumber, columnNumber, error);
    }

    private JsonErrorReporter errorReporter = new JsonErrorReporter() {
        @Override
        public void error(String message) {
            JsonParser.this.error(message);
        }
    };

    private static boolean isWhitespace(int c) {
        switch (c) {
            case ' ':
            case '\t':
            case '\n':
            case '\r':
                return true;
            default:
                return false;
        }
    }

    private static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }
}
