/*
 *  Copyright 2017 Alexey Andreev.
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

package org.teavm.classlib.java.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class TStreamTokenizer {
    public double nval;
    public String sval;
    public static final int TT_EOF = -1;
    public static final int TT_EOL = '\n';
    public static final int TT_NUMBER = -2;
    public static final int TT_WORD = -3;
    private static final int TT_UNKNOWN = -4;
    public int ttype = TT_UNKNOWN;

    /**
     * Internal character meanings, 0 implies TOKEN_ORDINARY
     */
    private byte[] tokenTypes = new byte[256];

    private static final byte TOKEN_COMMENT = 1;

    private static final byte TOKEN_QUOTE = 2;

    private static final byte TOKEN_WHITE = 4;

    private static final byte TOKEN_WORD = 8;

    private static final byte TOKEN_DIGIT = 16;

    private int lineNumber = 1;

    private boolean forceLowercase;

    private boolean isEOLSignificant;

    private boolean slashStarComments;

    private boolean slashSlashComments;

    private boolean pushBackToken;

    private boolean lastCr;

    /* One of these will have the stream */
    private InputStream inStream;

    private Reader inReader;

    private int peekChar = -2;

    private TStreamTokenizer() {
        /*
         * Initialize the default state per specification. All byte values 'A'
         * through 'Z', 'a' through 'z', and '\u00A0' through '\u00FF' are
         * considered to be alphabetic.
         */
        wordChars('A', 'Z');
        wordChars('a', 'z');
        wordChars(160, 255);
        /*
         * All byte values '\u0000' through '\u0020' are considered to be white
         * space.
         */
        whitespaceChars(0, 32);
        /*
         * '/' is a comment character. Single quote '\'' and double quote '"'
         * are string quote characters.
         */
        commentChar('/');
        quoteChar('"');
        quoteChar('\'');
        /*
         * Numbers are parsed.
         */
        parseNumbers();
        /*
         * Ends of lines are treated as white space, not as separate tokens.
         * C-style and C++-style comments are not recognized. These are the
         * defaults and are not needed in constructor.
         */
    }

    @Deprecated
    public TStreamTokenizer(InputStream is) {
        this();
        if (is == null) {
            throw new NullPointerException();
        }
        inStream = is;
    }

    public TStreamTokenizer(Reader r) {
        this();
        if (r == null) {
            throw new NullPointerException();
        }
        inReader = r;
    }

    public void commentChar(int ch) {
        if (0 <= ch && ch < tokenTypes.length) {
            tokenTypes[ch] = TOKEN_COMMENT;
        }
    }

    public void eolIsSignificant(boolean flag) {
        isEOLSignificant = flag;
    }

    public int lineno() {
        return lineNumber;
    }

    public void lowerCaseMode(boolean flag) {
        forceLowercase = flag;
    }

    public int nextToken() throws IOException {
        if (pushBackToken) {
            pushBackToken = false;
            if (ttype != TT_UNKNOWN) {
                return ttype;
            }
        }
        sval = null; // Always reset sval to null
        int currentChar = peekChar == -2 ? read() : peekChar;

        if (lastCr && currentChar == '\n') {
            lastCr = false;
            currentChar = read();
        }
        if (currentChar == -1) {
            ttype = TT_EOF;
            return ttype;
        }

        byte currentType = currentChar > 255 ? TOKEN_WORD : tokenTypes[currentChar];
        while ((currentType & TOKEN_WHITE) != 0) {
            /*
             * Skip over white space until we hit a new line or a real token
             */
            if (currentChar == '\r') {
                lineNumber++;
                if (isEOLSignificant) {
                    lastCr = true;
                    peekChar = -2;
                    ttype = TT_EOL;
                    return ttype;
                }
                currentChar = read();
                if (currentChar == '\n') {
                    currentChar = read();
                }
            } else if (currentChar == '\n') {
                lineNumber++;
                if (isEOLSignificant) {
                    peekChar = -2;
                    ttype = TT_EOL;
                    return ttype;
                }
                currentChar = read();
            } else {
                // Advance over this white space character and try again.
                currentChar = read();
            }
            if (currentChar == -1) {
                ttype = TT_EOF;
                return ttype;
            }
            currentType = currentChar > 255 ? TOKEN_WORD : tokenTypes[currentChar];
        }

        /*
         * Check for digits before checking for words since digits can be
         * contained within words.
         */
        if ((currentType & TOKEN_DIGIT) != 0) {
            StringBuilder digits = new StringBuilder(20);
            boolean haveDecimal = false;
            boolean checkJustNegative = currentChar == '-';
            while (true) {
                if (currentChar == '.') {
                    haveDecimal = true;
                }
                digits.append((char) currentChar);
                currentChar = read();
                if ((currentChar < '0' || currentChar > '9')
                        && (haveDecimal || currentChar != '.')) {
                    break;
                }
            }
            peekChar = currentChar;
            if (checkJustNegative && digits.length() == 1) {
                // Didn't get any other digits other than '-'
                ttype = '-';
                return ttype;
            }
            try {
                nval = Double.valueOf(digits.toString());
            } catch (NumberFormatException e) {
                // Unsure what to do, will write test.
                nval = 0;
            }
            ttype = TT_NUMBER;
            return ttype;
        }
        // Check for words
        if ((currentType & TOKEN_WORD) != 0) {
            StringBuilder word = new StringBuilder(20);
            while (true) {
                word.append((char) currentChar);
                currentChar = read();
                if (currentChar == -1
                        || (currentChar < 256 && (tokenTypes[currentChar] & (TOKEN_WORD | TOKEN_DIGIT)) == 0)) {
                    break;
                }
            }
            peekChar = currentChar;
            sval = forceLowercase ? word.toString().toLowerCase() : word
                    .toString();
            ttype = TT_WORD;
            return ttype;
        }
        // Check for quoted character
        if (currentType == TOKEN_QUOTE) {
            StringBuilder quoteString = new StringBuilder();
            int peekOne = read();
            while (peekOne >= 0 && peekOne != currentChar && peekOne != '\r' && peekOne != '\n') {
                boolean readPeek = true;
                if (peekOne == '\\') {
                    int c1 = read();
                    // Check for quoted octal IE: \377
                    if (c1 <= '7' && c1 >= '0') {
                        int digitValue = c1 - '0';
                        c1 = read();
                        if (c1 > '7' || c1 < '0') {
                            readPeek = false;
                        } else {
                            digitValue = digitValue * 8 + (c1 - '0');
                            c1 = read();
                            // limit the digit value to a byte
                            if (digitValue > 31 || c1 > '7' || c1 < '0') {
                                readPeek = false;
                            } else {
                                digitValue = digitValue * 8 + (c1 - '0');
                            }
                        }
                        if (!readPeek) {
                            // We've consumed one to many
                            quoteString.append((char) digitValue);
                            peekOne = c1;
                        } else {
                            peekOne = digitValue;
                        }
                    } else {
                        switch (c1) {
                            case 'a':
                                peekOne = 0x7;
                                break;
                            case 'b':
                                peekOne = 0x8;
                                break;
                            case 'f':
                                peekOne = 0xc;
                                break;
                            case 'n':
                                peekOne = 0xA;
                                break;
                            case 'r':
                                peekOne = 0xD;
                                break;
                            case 't':
                                peekOne = 0x9;
                                break;
                            case 'v':
                                peekOne = 0xB;
                                break;
                            default:
                                peekOne = c1;
                        }
                    }
                }
                if (readPeek) {
                    quoteString.append((char) peekOne);
                    peekOne = read();
                }
            }
            if (peekOne == currentChar) {
                peekOne = read();
            }
            peekChar = peekOne;
            ttype = currentChar;
            sval = quoteString.toString();
            return ttype;
        }
        // Do comments, both "//" and "/*stuff*/"
        if (currentChar == '/' && (slashSlashComments || slashStarComments)) {
            currentChar = read();
            if (currentChar == '*' && slashStarComments) {
                int peekOne = read();
                while (true) {
                    currentChar = peekOne;
                    peekOne = read();
                    if (currentChar == -1) {
                        peekChar = -1;
                        ttype = TT_EOF;
                        return ttype;
                    }
                    if (currentChar == '\r') {
                        if (peekOne == '\n') {
                            peekOne = read();
                        }
                        lineNumber++;
                    } else if (currentChar == '\n') {
                        lineNumber++;
                    } else if (currentChar == '*' && peekOne == '/') {
                        peekChar = read();
                        return nextToken();
                    }
                }
            } else if (currentChar == '/' && slashSlashComments) {
                // Skip to EOF or new line then return the next token
                do {
                    currentChar = read();
                } while (currentChar >= 0 && currentChar != '\r' && currentChar != '\n');
                peekChar = currentChar;
                return nextToken();
            } else if (currentType != TOKEN_COMMENT) {
                // Was just a slash by itself
                peekChar = currentChar;
                ttype = '/';
                return ttype;
            }
        }
        // Check for comment character
        if (currentType == TOKEN_COMMENT) {
            // Skip to EOF or new line then return the next token
            do {
                currentChar = read();
            } while (currentChar >= 0 && currentChar != '\r' && currentChar != '\n');
            peekChar = currentChar;
            return nextToken();
        }

        peekChar = read();
        ttype = currentChar;
        return ttype;
    }

    public void ordinaryChar(int ch) {
        if (0 <= ch && ch < tokenTypes.length) {
            tokenTypes[ch] = 0;
        }
    }

    public void ordinaryChars(int low, int hi) {
        if (low < 0) {
            low = 0;
        }
        if (hi > tokenTypes.length) {
            hi = tokenTypes.length - 1;
        }
        for (int i = low; i <= hi; i++) {
            tokenTypes[i] = 0;
        }
    }

    public void parseNumbers() {
        for (int i = '0'; i <= '9'; i++) {
            tokenTypes[i] |= TOKEN_DIGIT;
        }
        tokenTypes['.'] |= TOKEN_DIGIT;
        tokenTypes['-'] |= TOKEN_DIGIT;
    }

    public void pushBack() {
        pushBackToken = true;
    }

    public void quoteChar(int ch) {
        if (0 <= ch && ch < tokenTypes.length) {
            tokenTypes[ch] = TOKEN_QUOTE;
        }
    }

    private int read() throws IOException {
        // Call the read for the appropriate stream
        if (inStream == null) {
            return inReader.read();
        }
        return inStream.read();
    }

    public void resetSyntax() {
        for (int i = 0; i < 256; i++) {
            tokenTypes[i] = 0;
        }
    }

    public void slashSlashComments(boolean flag) {
        slashSlashComments = flag;
    }

    public void slashStarComments(boolean flag) {
        slashStarComments = flag;
    }

    @Override
    public String toString() {
        // Values determined through experimentation
        StringBuilder result = new StringBuilder();
        result.append("Token[");
        switch (ttype) {
            case TT_EOF:
                result.append("EOF");
                break;
            case TT_EOL:
                result.append("EOL");
                break;
            case TT_NUMBER:
                result.append("n=");
                result.append(nval);
                break;
            case TT_WORD:
                result.append(sval);
                break;
            default:
                if (ttype == TT_UNKNOWN || tokenTypes[ttype] == TOKEN_QUOTE) {
                    result.append(sval);
                } else {
                    result.append('\'');
                    result.append((char) ttype);
                    result.append('\'');
                }
        }
        result.append("], line ");
        result.append(lineNumber);
        return result.toString();
    }

    public void whitespaceChars(int low, int hi) {
        if (low < 0) {
            low = 0;
        }
        if (hi > tokenTypes.length) {
            hi = tokenTypes.length - 1;
        }
        for (int i = low; i <= hi; i++) {
            tokenTypes[i] = TOKEN_WHITE;
        }
    }

    public void wordChars(int low, int hi) {
        if (low < 0) {
            low = 0;
        }
        if (hi > tokenTypes.length) {
            hi = tokenTypes.length - 1;
        }
        for (int i = low; i <= hi; i++) {
            tokenTypes[i] |= TOKEN_WORD;
        }
    }
}
