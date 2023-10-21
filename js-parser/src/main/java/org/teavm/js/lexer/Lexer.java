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

    public void next() {
        skipSpaces();

        tokenStartOffset = reader.offset();
        tokenStartLine = reader.line();
        tokenStartColumn = reader.column();
        tokenValue = null;

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
                reader.next();
                token = Token.EOF;
                break;

            case '$':
            case '_':
            case '\\':
                readIdentifier();
                break;

            case '/':
                readSlash();
                break;

            default:
                if (CharacterClassifier.isIdentifierStart(reader.currentChar())) {
                    readIdentifier();
                }
                break;
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
            token = Token.SLASH;
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
            if (codePoint >= 0) {
                if (!Character.isUnicodeIdentifierStart(codePoint) && codePoint != '$' && codePoint != '_') {
                    errorReporter.reportError(line, column, offset, "Invalid ");
                } else {
                    sb.appendCodePoint(codePoint);
                }
            }
        } else {
            sb.append(reader.currentChar());
            reader.next();
        }

        tokenValue = sb.toString();
        token = Token.IDENTIFER;
    }

    private int tryReadUnicodeEscapeSequence() {

    }

    private int readUnicodeEscapeSequence() {

    }

    private void readRegexLiteral() {

    }

    private void skipSpaces() {
        while (CharacterClassifier.isWhiteSpace(reader.currentChar())) {
            reader.next();
        }
    }
}
