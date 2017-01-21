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

class ListingLexer {
    private Reader reader;
    private int c;
    private ListingToken token;
    private Object tokenValue;
    private int index = -1;
    private int tokenStart;

    public ListingLexer(Reader reader) {
        this.reader = reader;
    }

    public ListingToken getToken() {
        return token;
    }

    public Object getTokenValue() {
        return tokenValue;
    }

    public int getIndex() {
        return index;
    }

    public int getTokenStart() {
        return tokenStart;
    }

    public void nextToken() throws IOException, ListingParseException {
        if (token == ListingToken.EOF) {
            return;
        }

        tokenStart = index;
        if (index < 0) {
            tokenStart = 0;
            nextChar();
        }
        skipWhiteSpace();

        switch (c) {
            case -1:
                token = ListingToken.EOF;
                break;
            case '\n':
                token = ListingToken.EOL;
                nextChar();
                break;
            case '@':
                readVariable();
                break;
            case '$':
                readLabel();
                break;
            case '\'':
                readString();
                break;
            case ':':
                nextChar();
                expect('=');
                token = ListingToken.ASSIGN;
                break;
            case '=':
                nextChar();
                expect('=');
                if (c == '=') {
                    token = ListingToken.REFERENCE_EQUAL;
                    nextChar();
                } else {
                    token = ListingToken.EQUAL;
                }
                break;
            case '!':
                nextChar();
                expect('=');
                if (c == '=') {
                    token = ListingToken.REFERENCE_NOT_EQUAL;
                    nextChar();
                } else {
                    token = ListingToken.NOT_EQUAL;
                }
                break;
            case '<':
                nextChar();
                if (c == '=') {
                    nextChar();
                    token = ListingToken.LESS_OR_EQUAL;
                } else if (c == '<') {
                    nextChar();
                    token = ListingToken.SHIFT_LEFT;
                } else {
                    token = ListingToken.LESS;
                }
                break;
            case '>':
                nextChar();
                if (c == '=') {
                    nextChar();
                    token = ListingToken.GREATER_OR_EQUAL;
                } else if (c == '>') {
                    nextChar();
                    if (c == '>') {
                        nextChar();
                        token = ListingToken.SHIFT_RIGHT_UNSIGNED;
                    } else {
                        token = ListingToken.SHIFT_RIGHT;
                    }
                } else {
                    token = ListingToken.GREATER;
                }
                break;
            case '+':
                nextChar();
                token = ListingToken.ADD;
                break;
            case '-':
                nextChar();
                token = ListingToken.SUBTRACT;
                break;
            case '*':
                nextChar();
                token = ListingToken.SUBTRACT;
                break;
            case '/':
                nextChar();
                if (c == '/') {
                    skipComment();
                } else {
                    token = ListingToken.DIVIDE;
                }
                break;
            case '%':
                nextChar();
                token = ListingToken.REMAINDER;
                break;
            case '&':
                nextChar();
                token = ListingToken.AND;
                break;
            case '|':
                nextChar();
                token = ListingToken.OR;
                break;
            case '^':
                nextChar();
                token = ListingToken.XOR;
                break;
            case '.':
                nextChar();
                token = ListingToken.DOT;
                break;
            case ',':
                nextChar();
                token = ListingToken.COMMA;
                break;
            case '[':
                nextChar();
                token = ListingToken.LEFT_SQUARE_BRACKET;
                break;
            case ']':
                nextChar();
                token = ListingToken.RIGHT_SQUARE_BRACKET;
                break;
            case '`':
                nextChar();
                token = ListingToken.IDENTIFIER;
                readEscapedIdentifier();
                break;
            default:
                if (isIdentifierStart(c)) {
                    readIdentifier();
                } else if (c >= '0' && c <= '9') {
                    readNumber();
                } else {
                    unexpected();
                }
                break;
        }
    }

    private void readVariable() throws IOException {
        nextChar();
        token = ListingToken.VARIABLE;
        readIdentifierLike();
    }

    private void readLabel() throws IOException {
        nextChar();
        token = ListingToken.LABEL;
        readIdentifierLike();
    }

    private void readIdentifierLike() throws IOException {
        StringBuilder sb = new StringBuilder();
        while (isIdentifierPart(c)) {
            sb.append((char) c);
            nextChar();
        }
        tokenValue = sb.toString();
    }

    private void readIdentifier() throws IOException {
        token = ListingToken.IDENTIFIER;
        StringBuilder sb = new StringBuilder();
        sb.append((char) c);
        nextChar();
        while (isIdentifierPart(c)) {
            sb.append((char) c);
            nextChar();
        }
        tokenValue = sb.toString();
    }

    private void readEscapedIdentifier() throws IOException, ListingParseException {
        StringBuilder sb = new StringBuilder();
        while (c != '`') {
            if (c < ' ') {
                throw new ListingParseException("Unexpected char in escaped identifier", index);
            }
            sb.append((char) c);
            nextChar();
        }
        nextChar();
        tokenValue = sb.toString();
    }

    static boolean isIdentifierStart(int c) {
        if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z') {
            return true;
        }
        switch (c) {
            case '_':
                return true;
            default:
                return false;
        }
    }

    static boolean isIdentifierPart(int c) {
        if (isIdentifierStart(c) || c >= '0' && c <= '9') {
            return true;
        }
        switch (c) {
            case '.':
            case '$':
            case '#':
                return true;
            default:
                return false;
        }
    }

    private void readString() throws IOException, ListingParseException {
        nextChar();
        token = ListingToken.STRING;
        StringBuilder sb = new StringBuilder();

        while (true) {
            switch (c) {
                case '\'':
                    nextChar();
                    tokenValue = sb.toString();
                    return;
                case '\\':
                    nextChar();
                    switch (c) {
                        case 'n':
                            sb.append('\n');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case '\'':
                        case '\\':
                            sb.append(c);
                            break;
                        case 'u':
                            int codePoint = 0;
                            for (int i = 0; i < 4; ++i) {
                                nextChar();
                                if (c == -1) {
                                    throw new ListingParseException("Wrong escape sequence", index);
                                }
                                int digit = Character.digit((char) c, 16);
                                if (digit < 0) {
                                    throw new ListingParseException("Wrong escape sequence", index);
                                }
                                codePoint = codePoint * 16 + digit;
                            }
                            sb.appendCodePoint(codePoint);
                            break;
                        default:
                            throw new ListingParseException("Wrong escape sequence", index);
                    }
                    nextChar();
                    break;
                default:
                    if (c < ' ') {
                        throw new ListingParseException("Unexpected character in string literal: " + c, index);
                    }
                    sb.append((char) c);
                    nextChar();
                    break;
            }
        }
    }

    private void readNumber() throws IOException, ListingParseException {
        StringBuilder sb = new StringBuilder();

        token = ListingToken.INTEGER;

        if (c == '-') {
            sb.append(c);
            nextChar();
        }

        while (c >= '0' && c <= '9') {
            sb.append((char) c);
            nextChar();
        }

        if (c == '.') {
            sb.append('.');
            token = ListingToken.DOUBLE;
            nextChar();

            if (c < '0' || c > '9') {
                throw new ListingParseException("Wrong number", index);
            }

            while (c >= '0' && c <= '9') {
                sb.append((char) c);
                nextChar();
            }
        }

        if (c == 'E' || c == 'e') {
            sb.append('e');
            nextChar();
            if (c == '+' || c == '-') {
                sb.append((char) c);
                nextChar();
            }

            if (c < '0' || c > '9') {
                throw new ListingParseException("Wrong number", index);
            }

            while (c >= '0' && c <= '9') {
                sb.append((char) c);
                nextChar();
            }
        }

        if (c == 'F' || c == 'f') {
            nextChar();
            token = ListingToken.FLOAT;
        } else if (c == 'l' || c == 'L') {
            nextChar();
            token = ListingToken.LONG;
        } else if (isIdentifierStart(c)) {
            throw new ListingParseException("Wrong number", index);
        }

        switch (token) {
            case INTEGER:
                tokenValue = Integer.parseInt(sb.toString());
                break;
            case LONG:
                tokenValue = Long.parseLong(sb.toString());
                break;
            case FLOAT:
                tokenValue = Float.parseFloat(sb.toString());
                break;
            case DOUBLE:
                tokenValue = Double.parseDouble(sb.toString());
                break;
            default:
                break;
        }
    }

    private void expect(char expected) throws IOException, ListingParseException {
        if (c != expected) {
            unexpected();
        }
        nextChar();
    }

    private void unexpected() throws ListingParseException {
        throw new ListingParseException("Unexpected character: " + (char) c, index);
    }

    private void skipWhiteSpace() throws IOException {
        while (true) {
            switch (c) {
                case ' ':
                case '\t':
                    nextChar();
                    break;
                default:
                    return;
            }
        }
    }

    private void skipComment() throws IOException {
        while (true) {
            switch (c) {
                case '\n':
                    nextChar();
                    token = ListingToken.EOL;
                    return;
                case -1:
                    token = ListingToken.EOF;
                    return;
                default:
                    nextChar();
                    break;
            }
        }
    }

    private void nextChar() throws IOException {
        c = reader.read();
        ++index;
    }
}
