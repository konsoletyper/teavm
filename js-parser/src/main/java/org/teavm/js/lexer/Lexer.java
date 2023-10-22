/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.js.lexer;

public class Lexer {
    private CurrentCharReader reader;
    private LexerErrorReporter errorReporter;

    private int tokenStartOffset;
    private int tokenStartLine;
    private int tokenStartColumn;
    private int tokenEndOffset;
    private int tokenEndLine;
    private int tokenEndColumn;
    private boolean expectRegex;
    private boolean hasEscapeSequence;

    private Token token;
    private String tokenValue;

    public Lexer(CodePointReader reader, LexerErrorReporter errorReporter) {
        this.reader = new CurrentCharReader(reader);
        this.errorReporter = errorReporter;
        next();
    }

    public void setExpectRegex(boolean expectRegex) {
        this.expectRegex = expectRegex;
    }

    public Token token() {
        return token;
    }

    public String tokenValue() {
        return tokenValue;
    }

    public boolean hasEscapeSequence() {
        return hasEscapeSequence;
    }

    public int tokenStartLine() {
        return tokenStartLine;
    }

    public int tokenStartColumn() {
        return tokenStartColumn;
    }

    public int tokenStartOffset() {
        return tokenStartOffset;
    }

    public int tokenEndLine() {
        return tokenEndLine;
    }

    public int tokenEndColumn() {
        return tokenEndColumn;
    }

    public int tokenEndOffset() {
        return tokenEndOffset;
    }

    public void next() {
        skipSpaces();

        token = null;
        while (token == null) {
            tokenStartOffset = reader.offset();
            tokenStartLine = reader.line();
            tokenStartColumn = reader.column();
            tokenValue = null;
            hasEscapeSequence = false;

            switch (reader.currentChar()) {
                case CharacterClassifier.EOF:
                    token = Token.EOF;
                    break;
                case 0x000D:
                    readLineTerminator();
                    break;

                case 0x000A:
                case 0x2028:
                case 0x2029:
                    tokenAfterCurrentChar(Token.LINE_TERMINATOR);
                    break;

                case '$':
                case '_':
                case '\\':
                    readIdentifier();
                    break;

                case '/':
                    readSlash();
                    break;

                case '{':
                    tokenAfterCurrentChar(Token.LEFT_CURLY_BRACKET);
                    break;
                case '}':
                    tokenAfterCurrentChar(Token.RIGHT_CURLY_BRACKET);
                    break;
                case '(':
                    tokenAfterCurrentChar(Token.LEFT_PARENTHESIS);
                    break;
                case ')':
                    tokenAfterCurrentChar(Token.RIGHT_PARENTHESIS);
                    break;
                case '[':
                    tokenAfterCurrentChar(Token.LEFT_SQUARE_BRACKET);
                    break;
                case ']':
                    tokenAfterCurrentChar(Token.RIGHT_SQUARE_BRACKET);
                    break;
                case '.':
                    readDot();
                    break;
                case ';':
                    tokenAfterCurrentChar(Token.SEMICOLON);
                    break;
                case ',':
                    tokenAfterCurrentChar(Token.COMMA);
                    break;
                case '<':
                    readLess();
                    break;
                case '>':
                    readGreater();
                    break;
                case '=':
                    readEqual();
                    break;
                case '!':
                    readExclamationSign();
                    break;
                case '+':
                    readOperationOrAssign(Token.PLUS_ASSIGN, Token.PLUS);
                    break;
                case '-':
                    readOperationOrAssign(Token.MINUS_ASSIGN, Token.MINUS);
                    break;
                case '*':
                    readOperationOrAssign(Token.MULTIPLY_ASSIGN, Token.MULTIPLY);
                    break;
                case '%':
                    readOperationOrAssign(Token.REMAINDER_ASSIGN, Token.REMAINDER);
                    break;
                case '&':
                    readLogical(Token.AND_ASSIGN, Token.AND, Token.AND_AND);
                    break;
                case '|':
                    readLogical(Token.OR_ASSIGN, Token.OR, Token.OR_OR);
                    break;
                case '^':
                    readOperationOrAssign(Token.XOR_ASSIGN, Token.XOR);
                    break;
                case '~':
                    tokenAfterCurrentChar(Token.INVERT);
                    break;
                case '?':
                    tokenAfterCurrentChar(Token.QUESTION);
                    break;
                case ':':
                    tokenAfterCurrentChar(Token.COLON);
                    break;

                case '0':
                    readNumericLiteral();
                    break;
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    readDecimalLiteral(new StringBuilder());
                    break;

                default:
                    if (CharacterClassifier.isIdentifierStart(reader.currentChar())) {
                        readIdentifier();
                    }
                    break;
            }
        }

        tokenEndOffset = reader.offset();
        tokenEndLine = reader.line();
        tokenEndColumn = reader.column();
    }

    private void readLineTerminator() {
        reader.next();
        if (reader.currentChar() == 0x000A) {
            reader.next();
        }
        token = Token.LINE_TERMINATOR;
    }

    private void readSlash() {
        reader.next();
        if (maybeReadComment()) {
            return;
        }
        if (expectRegex) {
            readRegexLiteral();
        } else {
            if (reader.currentChar() == '=') {
                tokenAfterCurrentChar(Token.DIVIDE_ASSIGN);
            } else {
                token = Token.DIVIDE;
            }
        }
    }

    private boolean maybeReadComment() {
        switch (reader.currentChar()) {
            case '/':
                reader.next();
                readSingleLineComment();
                return true;
            case '*':
                reader.next();
                readMultiLineComment();
                return true;
            default:
                return false;
        }
    }

    private void readSingleLineComment() {
        var sb = new StringBuilder();
        while (!CharacterClassifier.isLineTerminator(reader.currentChar())) {
            sb.append(reader.currentChar());
            reader.next();
        }
        tokenValue = sb.toString();
        token = Token.COMMENT;
    }

    private void readMultiLineComment() {
        var sb = new StringBuilder();
        while (true) {
            if (reader.currentChar() == '*') {
                reader.next();
                if (reader.currentChar() == '/') {
                    reader.next();
                    break;
                }
            } else {
                if (CharacterClassifier.isEof(reader.currentChar())) {
                    break;
                }
                sb.append(reader.currentChar());
                reader.next();
            }
        }
        tokenValue = sb.toString();
        token = Token.COMMENT;
    }

    private void readIdentifier() {
        var sb = new StringBuilder();
        if (reader.currentChar() == '\\') {
            reader.next();
            int line = reader.line();
            int column = reader.column();
            int offset = reader.offset();
            int codePoint = tryReadUnicodeEscapeSequence();
            if (codePoint < 0) {
                return;
            }
            if (!CharacterClassifier.isIdentifierStart(codePoint)) {
                errorReporter.reportError(line, column, offset, "Invalid identifier start character"
                        + "represented by escape sequence");
                return;
            }
            sb.appendCodePoint(codePoint);
            hasEscapeSequence = true;
        } else {
            sb.appendCodePoint(reader.currentChar());
            reader.next();
        }

        while (true) {
            if (reader.currentChar() == '\\') {
                reader.next();
                int line = reader.line();
                int column = reader.column();
                int offset = reader.offset();
                int codePoint = tryReadUnicodeEscapeSequence();
                if (codePoint < 0) {
                    break;
                }
                if (!CharacterClassifier.isIdentifierPart(codePoint)) {
                    errorReporter.reportError(line, column, offset, "Invalid identifier character "
                            + "represented by escape sequence");
                } else {
                    sb.appendCodePoint(codePoint);
                    hasEscapeSequence = true;
                }
            } else if (CharacterClassifier.isIdentifierStart(reader.currentChar())) {
                sb.appendCodePoint(reader.currentChar());
                reader.next();
            } else {
                break;
            }
        }

        tokenValue = sb.toString();
        token = Token.IDENTIFER;
    }

    private int tryReadUnicodeEscapeSequence() {
        if (reader.currentChar() != 'u') {
            reportError("Invalid unicode escape sequence in identifier");
            return -1;
        }
        reader.next();
        return readUnicodeEscapeSequence();
    }

    private void readDot() {
        reader.next();
        if (reader.currentChar() == '.') {
            reader.next();
            if (reader.currentChar() != '.') {
                token = Token.DOT;
                reportError("One more dot expected to for ellipsis (...)");
            } else {
                token = Token.ELLIPSIS;
            }
        } else if (reader.currentChar() >= '0' && reader.currentChar() <= '9') {
            token = Token.NUMERIC_LITERAL;
            readFractionalAfterDot(new StringBuilder("."));
            checkCharAfterNumber();
        } else {
            token = Token.DOT;
        }
    }

    private void readLess() {
        reader.next();
        switch (reader.currentChar()) {
            case '=':
                tokenAfterCurrentChar(Token.LESS_OR_EQUAL);
                break;
            case '<':
                reader.next();
                if (reader.currentChar() == '=') {
                    tokenAfterCurrentChar(Token.SHIFT_LEFT_ASSIGN);
                } else {
                    token = Token.SHIFT_LEFT;
                }
                break;
            default:
                token = Token.LESS;
                break;
        }
    }

    private void readGreater() {
        reader.next();
        switch (reader.currentChar()) {
            case '=':
                tokenAfterCurrentChar(Token.GREATER_OR_EQUAL);
                break;
            case '>':
                reader.next();
                switch (reader.currentChar()) {
                    case '>':
                        if (reader.currentChar() == '=') {
                            tokenAfterCurrentChar(Token.SHIFT_RIGHT_UNSIGNED);
                        } else {
                            token = Token.SHIFT_RIGHT_UNSIGNED;
                        }
                        break;
                    case '=':
                        tokenAfterCurrentChar(Token.SHIFT_RIGHT_ASSIGN);
                        break;
                    default:
                        token = Token.SHIFT_RIGHT;
                        break;
                }
                break;
            default:
                token = Token.GREATER;
                break;
        }
    }

    private void readEqual() {
        reader.next();
        switch (reader.currentChar()) {
            case '=':
                reader.next();
                if (reader.currentChar() == '=') {
                    tokenAfterCurrentChar(Token.STRICT_EQUAL);
                } else {
                    token = Token.EQUAL;
                }
                break;
            case '>':
                token = Token.ARROW;
                break;
            default:
                token = Token.ASSIGN;
                break;
        }
    }

    private void readExclamationSign() {
        reader.next();
        if (reader.currentChar() == '=') {
            reader.next();
            if (reader.currentChar() == '=') {
                tokenAfterCurrentChar(Token.STRICT_NOT_EQUAL);
            } else {
                token = Token.NOT_EQUAL;
            }
        } else {
            token = Token.NOT;
        }
    }

    private void readOperationOrAssign(Token assign, Token regular) {
        reader.next();
        if (reader.currentChar() == '=') {
            tokenAfterCurrentChar(assign);
        } else {
            token = regular;
        }
    }

    private void readLogical(Token assign, Token bitwise, Token logical) {
        var c = reader.currentChar();
        reader.next();
        if (reader.currentChar() == '=') {
            tokenAfterCurrentChar(assign);
        } else if (reader.currentChar() == c) {
            tokenAfterCurrentChar(logical);
        } else {
            token = bitwise;
        }
    }

    private void readNumericLiteral() {
        var sb = new StringBuilder();
        sb.appendCodePoint(reader.currentChar());
        reader.next();
        switch (reader.currentChar()) {
            case '.':
                readFractional(sb);
                break;
            case 'x':
            case 'X':
                readHexLiteral();
                break;
            case 'b':
            case 'B':
                readBinaryLiteral();
                break;
            case 'o':
            case 'O':
                readOctalLiteral();
                break;
            case 'E':
            case 'e':
                readExponent(new StringBuilder());
                break;
            default:
                readDecimalLiteral(sb);
                break;
        }
    }

    private void readDecimalLiteral(StringBuilder sb) {
        token = Token.NUMERIC_LITERAL;
        sb.appendCodePoint(reader.currentChar());
        reader.next();
        while (CharacterClassifier.isDecimalDigit(reader.currentChar())) {
            sb.append(reader.currentChar());
            reader.next();
        }
        if (reader.currentChar() == '.') {
            sb.append('.');
            reader.next();
            readFractional(sb);
        } else if (reader.currentChar() == 'e' || reader.currentChar() == 'E') {
            readExponent(sb);
        }
        checkCharAfterNumber();
    }

    private void readFractional(StringBuilder sb) {
        sb.append('.');
        reader.next();
        readFractionalAfterDot(sb);
    }

    private void readFractionalAfterDot(StringBuilder sb) {
        sb.append(reader.currentChar());
        while (CharacterClassifier.isDecimalDigit(reader.currentChar())) {
            sb.append(reader.currentChar());
        }
        if (reader.currentChar() == 'e' || reader.currentChar() == 'E') {
            readExponent(sb);
        }
    }

    private void readExponent(StringBuilder sb) {
        sb.appendCodePoint(reader.currentChar());
        reader.next();
        if (reader.currentChar() == '+' || reader.currentChar() == '-') {
            sb.appendCodePoint(reader.currentChar());
            reader.next();
        }
        if (CharacterClassifier.isDecimalDigit(reader.currentChar())) {

        }
    }

    private void readHexLiteral() {

    }

    private void readBinaryLiteral() {

    }

    private void readOctalLiteral() {

    }

    private void checkCharAfterNumber() {

    }

    private int readUnicodeEscapeSequence() {
        var codePoint = 0;
        if (reader.currentChar() == '{') {
            reader.next();
            var hasError = false;
            var line = reader.line();
            var column = reader.column();
            int offset = reader.offset();
            while (reader.currentChar() != '}') {
                var digit = readHexDigit();
                if (digit < 0) {
                    reportError("Invalid hex digit in unicode escape sequence");
                    hasError = true;
                    break;
                }
                codePoint <<= 4;
                codePoint += digit;
                if (codePoint >= CharacterClassifier.CODEPOINT_COUNT) {
                    codePoint = CharacterClassifier.CODEPOINT_COUNT - 1;
                    errorReporter.reportError(line, column, offset, "Too big codepoint value in escape sequence");
                }
            }
            if (hasError) {
                while (reader.currentChar() != '}' && !CharacterClassifier.isLineTerminator(reader.currentChar())) {
                    reader.next();
                }
            }
        } else {
            for (var i = 0; i < 4; ++i) {
                var digit = readHexDigit();
                if (digit < 0) {
                    reportError("Invalid hex digit in unicode escape sequence");
                    break;
                }
                codePoint <<= 4;
                codePoint |= digit;
            }
        }
        return codePoint;
    }

    private int readHexDigit() {
        var digit = CharacterClassifier.hexDigit(reader.currentChar());
        if (digit >= 0) {
            reader.next();
        }
        return digit;
    }

    private void readRegexLiteral() {

    }

    private void tokenAfterCurrentChar(Token token) {
        reader.next();
        this.token = token;
    }

    private void skipSpaces() {
        while (CharacterClassifier.isWhiteSpace(reader.currentChar())) {
            reader.next();
        }
    }

    private void reportError(String error) {
        errorReporter.reportError(reader.line(), reader.column(), reader.offset(), error);
    }
}
