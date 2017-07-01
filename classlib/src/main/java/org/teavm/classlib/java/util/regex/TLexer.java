/*
 *  Copyright 2014 Alexey Andreev.
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

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Nikolay A. Kuznetsov
 */
package org.teavm.classlib.java.util.regex;

import java.util.MissingResourceException;

/**
 * The purpose of this class is to break given pattern into RE tokens;
 *
 * @author Nikolay A. Kuznetsov
 */
class TLexer {

    public static final int CHAR_DOLLAR = 0xe0000000 | '$';

    public static final int CHAR_RIGHT_PARENTHESIS = 0xe0000000 | ')';

    public static final int CHAR_LEFT_SQUARE_BRACKET = 0xe0000000 | '[';

    public static final int CHAR_RIGHT_SQUARE_BRACKET = 0xe0000000 | ']';

    public static final int CHAR_CARET = 0xe0000000 | '^';

    public static final int CHAR_VERTICAL_BAR = 0xe0000000 | '|';

    public static final int CHAR_AMPERSAND = 0xe0000000 | '&';

    public static final int CHAR_HYPHEN = 0xe0000000 | '-';

    public static final int CHAR_DOT = 0xe0000000 | '.';

    public static final int QMOD_GREEDY = 0xe0000000;

    public static final int QMOD_RELUCTANT = 0xc0000000;

    public static final int QMOD_POSSESSIVE = 0x80000000;

    public static final int QUANT_STAR = QMOD_GREEDY | '*';

    public static final int QUANT_STAR_P = QMOD_POSSESSIVE | '*';

    public static final int QUANT_STAR_R = QMOD_RELUCTANT | '*';

    public static final int QUANT_PLUS = QMOD_GREEDY | '+';

    public static final int QUANT_PLUS_P = QMOD_POSSESSIVE | '+';

    public static final int QUANT_PLUS_R = QMOD_RELUCTANT | '+';

    public static final int QUANT_ALT = QMOD_GREEDY | '?';

    public static final int QUANT_ALT_P = QMOD_POSSESSIVE | '?';

    public static final int QUANT_ALT_R = QMOD_RELUCTANT | '?';

    public static final int QUANT_COMP = QMOD_GREEDY | '{';

    public static final int QUANT_COMP_P = QMOD_POSSESSIVE | '{';

    public static final int QUANT_COMP_R = QMOD_RELUCTANT | '{';

    public static final int CHAR_LEFT_PARENTHESIS = 0x80000000 | '(';

    public static final int CHAR_NONCAP_GROUP = 0xc0000000 | '(';

    public static final int CHAR_POS_LOOKAHEAD = 0xe0000000 | '(';

    public static final int CHAR_NEG_LOOKAHEAD = 0xf0000000 | '(';

    public static final int CHAR_POS_LOOKBEHIND = 0xf8000000 | '(';

    public static final int CHAR_NEG_LOOKBEHIND = 0xfc000000 | '(';

    public static final int CHAR_ATOMIC_GROUP = 0xfe000000 | '(';

    public static final int CHAR_FLAGS = 0xff000000 | '(';

    public static final int CHAR_START_OF_INPUT = 0x80000000 | 'A';

    public static final int CHAR_WORD_BOUND = 0x80000000 | 'b';

    public static final int CHAR_NONWORD_BOUND = 0x80000000 | 'B';

    public static final int CHAR_PREVIOUS_MATCH = 0x80000000 | 'G';

    public static final int CHAR_END_OF_INPUT = 0x80000000 | 'z';

    public static final int CHAR_END_OF_LINE = 0x80000000 | 'Z';

    public static final int MODE_PATTERN = 1;

    public static final int MODE_RANGE = 1 << 1;

    public static final int MODE_ESCAPE = 1 << 2;

    // maximum length of decomposition
    static final int MAX_DECOMPOSITION_LENGTH = 4;

    /*
     * maximum length of Hangul decomposition note that
     * MAX_HANGUL_DECOMPOSITION_LENGTH <= MAX_DECOMPOSITION_LENGTH
     */
    static final int MAX_HANGUL_DECOMPOSITION_LENGTH = 3;

    /*
     * Following constants are needed for Hangul canonical decomposition. Hangul
     * decomposition algorithm and constants are taken according to description
     * at http://www.unicode.org/versions/Unicode4.0.0/ch03.pdf
     * "3.12 Conjoining Jamo Behavior"
     */
    static final int SBase = 0xAC00;

    static final int LBase = 0x1100;

    static final int VBase = 0x1161;

    static final int TBase = 0x11A7;

    static final int SCount = 11172;

    static final int LCount = 19;

    static final int VCount = 21;

    static final int TCount = 28;

    static final int NCount = 588;

    // table that contains canonical decomposition mappings
    private static TIntArrHash decompTable;
    /*
     * Table that contains information about Unicode codepoints with single
     * codepoint decomposition
     */
    private static TIntHash singleDecompTable;

    private static int singleDecompTableSize;

    private char[] pattern;

    private int flags;

    private int mode = 1;

    // when in literal mode, this field will save the previous one
    private int savedMode;

    // previous char read
    private int lookBack;

    // current character read
    private int ch;

    // next character
    private int lookAhead;

    // index of last char in pattern plus one
    private int patternFullLength;

    // cur special token
    private TSpecialToken curST;

    // next special token
    private TSpecialToken lookAheadST;

    // cur char being processed
    private int index;

    // previous non-whitespace character index;
    private int prevNW;

    // cur token start index
    private int curToc;

    // look ahead token index
    private int lookAheadToc;

    // original string representing pattern
    private String orig;

    public TLexer(String pattern, int flags) {
        orig = pattern;
        if ((flags & TPattern.LITERAL) > 0) {
            pattern = TPattern.quote(pattern);
        } else if ((flags & TPattern.CANON_EQ) > 0) {
            pattern = TLexer.normalize(pattern);
        }

        this.pattern = new char[pattern.length() + 2];
        System.arraycopy(pattern.toCharArray(), 0, this.pattern, 0, pattern.length());
        this.pattern[this.pattern.length - 1] = 0;
        this.pattern[this.pattern.length - 2] = 0;
        patternFullLength = this.pattern.length;
        this.flags = flags;
        // read first two tokens;
        movePointer();
        movePointer();

    }

    /**
     * Returns current character w/o reading next one; if there are no more
     * characters returns 0;
     *
     * @return current character;
     */
    public int peek() {
        return ch;
    }

    /**
     * Set the Lexer to PATTERN or RANGE mode; Lexer interpret character two
     * different ways in parser or range modes.
     *
     * @param mode
     *            Lexer.PATTERN or Lexer.RANGE
     */
    public void setMode(int mode) {
        if (mode > 0 && mode < 3) {
            this.mode = mode;
        }

        if (mode == TLexer.MODE_PATTERN) {
            reread();
        }
    }

    /**
     * Restores flags for Lexer
     *
     * @param flags
     */
    public void restoreFlags(int flags) {
        this.flags = flags;
        lookAhead = ch;
        lookAheadST = curST;

        // curToc is an index of closing bracket )
        index = curToc + 1;
        lookAheadToc = curToc;
        movePointer();
    }

    public TSpecialToken peekSpecial() {
        return curST;
    }

    /**
     * Returns true, if current token is special, i.e. quantifier, or other
     * compound token.
     *
     * @return - true if current token is special, false otherwise.
     */
    public boolean isSpecial() {
        return curST != null;
    }

    public boolean isQuantifier() {
        return isSpecial() && curST.getType() == TSpecialToken.TOK_QUANTIFIER;
    }

    public boolean isNextSpecial() {
        return lookAheadST != null;
    }

    public int next() {
        movePointer();
        return lookBack;
    }

    public TSpecialToken nextSpecial() {
        TSpecialToken res = curST;
        movePointer();
        return res;
    }

    public int lookAhead() {
        return lookAhead;
    }

    public int back() {
        return lookBack;
    }

    /**
     * Normalize given expression.
     *
     * @param input
     *            - expression to normalize
     * @return normalized expression.
     */
    static String normalize(String input) {
        return input;
    }


    /**
     * Reread current character, may be require if previous token changes mode
     * to one with different character interpretation.
     *
     */
    private void reread() {
        lookAhead = ch;
        lookAheadST = curST;
        index = lookAheadToc;
        lookAheadToc = curToc;
        movePointer();
    }

    /**
     * Moves pointer one position right; save current character to lookBack;
     * lookAhead to current one and finally read one more to lookAhead;
     */
    private void movePointer() {
        // swap pointers
        lookBack = ch;
        ch = lookAhead;
        curST = lookAheadST;
        curToc = lookAheadToc;
        lookAheadToc = index;
        boolean reread;
        do {
            reread = false;
            // read next character analyze it and construct token:
            // //

            lookAhead = (index < pattern.length) ? nextCodePoint() : 0;
            lookAheadST = null;

            if (mode == TLexer.MODE_ESCAPE) {
                if (lookAhead == '\\') {

                    // need not care about supplementary codepoints here
                    lookAhead = (index < pattern.length) ? pattern[nextIndex()] : 0;

                    switch (lookAhead) {
                        case 'E': {
                            mode = savedMode;

                            lookAhead = (index <= pattern.length - 2) ? nextCodePoint() : 0;
                            break;
                        }

                        default: {
                            lookAhead = '\\';
                            index = prevNW;
                            return;
                        }
                    }
                } else {
                    return;
                }
            }

            if (lookAhead == '\\') {

                lookAhead = (index < pattern.length - 2) ? nextCodePoint() : -1;
                switch (lookAhead) {
                    case -1:
                        throw new TPatternSyntaxException("", this.toString(), index);
                    case 'P':
                    case 'p': {
                        String cs = parseCharClassName();
                        boolean negative = false;

                        if (lookAhead == 'P') {
                            negative = true;
                        }
                        try {
                            lookAheadST = TAbstractCharClass.getPredefinedClass(cs, negative);
                        } catch (MissingResourceException mre) {
                            throw new TPatternSyntaxException("", this.toString(), index);
                        }
                        lookAhead = 0;
                        break;
                    }

                    case 'w':
                    case 's':
                    case 'd':
                    case 'W':
                    case 'S':
                    case 'D': {
                        lookAheadST = TCharClass.getPredefinedClass(new String(pattern, prevNW, 1), false);
                        lookAhead = 0;
                        break;
                    }

                    case 'Q': {
                        savedMode = mode;
                        mode = TLexer.MODE_ESCAPE;
                        reread = true;
                        break;
                    }

                    case 't':
                        lookAhead = '\t';
                        break;
                    case 'n':
                        lookAhead = '\n';
                        break;
                    case 'r':
                        lookAhead = '\r';
                        break;
                    case 'f':
                        lookAhead = '\f';
                        break;
                    case 'a':
                        lookAhead = '\u0007';
                        break;
                    case 'e':
                        lookAhead = '\u001B';
                        break;

                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9': {
                        if (mode == TLexer.MODE_PATTERN) {
                            lookAhead = 0x80000000 | lookAhead;
                        }
                        break;
                    }

                    case '0':
                        lookAhead = readOctals();
                        break;
                    case 'x':
                        lookAhead = readHex(2);
                        break;
                    case 'u':
                        lookAhead = readHex(4);
                        break;

                    case 'b':
                        lookAhead = CHAR_WORD_BOUND;
                        break;
                    case 'B':
                        lookAhead = CHAR_NONWORD_BOUND;
                        break;
                    case 'A':
                        lookAhead = CHAR_START_OF_INPUT;
                        break;
                    case 'G':
                        lookAhead = CHAR_PREVIOUS_MATCH;
                        break;
                    case 'Z':
                        lookAhead = CHAR_END_OF_LINE;
                        break;
                    case 'z':
                        lookAhead = CHAR_END_OF_INPUT;
                        break;
                    case 'c': {
                        if (index < pattern.length - 2) {

                            // need not care about supplementary codepoints here
                            lookAhead = pattern[nextIndex()] & 0x1f;
                            break;
                        } else {
                            throw new TPatternSyntaxException("", this.toString(), index);
                        }
                    }
                    case 'C':
                    case 'E':
                    case 'F':
                    case 'H':
                    case 'I':
                    case 'J':
                    case 'K':
                    case 'L':
                    case 'M':
                    case 'N':
                    case 'O':
                    case 'R':
                    case 'T':
                    case 'U':
                    case 'V':
                    case 'X':
                    case 'Y':
                    case 'g':
                    case 'h':
                    case 'i':
                    case 'j':
                    case 'k':
                    case 'l':
                    case 'm':
                    case 'o':
                    case 'q':
                    case 'y':
                        throw new TPatternSyntaxException("", this.toString(), index);

                    default:
                        break;
                }
            } else if (mode == TLexer.MODE_PATTERN) {
                switch (lookAhead) {
                    case '+':
                    case '*':
                    case '?': {
                        char mod = (index < pattern.length) ? pattern[index] : '*';
                        switch (mod) {
                            case '+': {
                                lookAhead = lookAhead | TLexer.QMOD_POSSESSIVE;
                                nextIndex();
                                break;
                            }
                            case '?': {
                                lookAhead = lookAhead | TLexer.QMOD_RELUCTANT;
                                nextIndex();
                                break;
                            }
                            default: {
                                lookAhead = lookAhead | TLexer.QMOD_GREEDY;
                                break;
                            }
                        }

                        break;
                    }

                    case '{': {
                        lookAheadST = processQuantifier(lookAhead);
                        break;
                    }

                    case '$':
                        lookAhead = CHAR_DOLLAR;
                        break;
                    case '(': {
                        if (pattern[index] == '?') {
                            nextIndex();
                            char nonCap = pattern[index];
                            boolean behind = false;
                            do {
                                if (!behind) {
                                    switch (nonCap) {
                                        case '!':
                                            lookAhead = CHAR_NEG_LOOKAHEAD;
                                            nextIndex();
                                            break;
                                        case '=':
                                            lookAhead = CHAR_POS_LOOKAHEAD;
                                            nextIndex();
                                            break;
                                        case '>':
                                            lookAhead = CHAR_ATOMIC_GROUP;
                                            nextIndex();
                                            break;
                                        case '<': {
                                            nextIndex();
                                            nonCap = pattern[index];
                                            behind = true;
                                            break;
                                        }
                                        default: {
                                            lookAhead = readFlags();

                                            /*
                                             * We return res = res | 1 << 8 from
                                             * readFlags() if we read
                                             * (?idmsux-idmsux)
                                             */
                                            if (lookAhead >= 256) {

                                                // Erase auxiliary bit
                                                lookAhead = lookAhead & 0xff;
                                                flags = lookAhead;
                                                lookAhead = lookAhead << 16;
                                                lookAhead = CHAR_FLAGS | lookAhead;
                                            } else {
                                                flags = lookAhead;
                                                lookAhead = lookAhead << 16;
                                                lookAhead = CHAR_NONCAP_GROUP | lookAhead;
                                            }
                                            break;
                                        }
                                    }
                                } else {
                                    behind = false;
                                    switch (nonCap) {
                                        case '!':
                                            lookAhead = CHAR_NEG_LOOKBEHIND;
                                            nextIndex();
                                            break;
                                        case '=':
                                            lookAhead = CHAR_POS_LOOKBEHIND;
                                            nextIndex();
                                            break;
                                        default:
                                            throw new TPatternSyntaxException("", this.toString(), index);
                                    }
                                }
                            } while (behind);
                        } else {
                            lookAhead = CHAR_LEFT_PARENTHESIS;
                        }
                        break;
                    }

                    case ')':
                        lookAhead = CHAR_RIGHT_PARENTHESIS;
                        break;
                    case '[': {
                        lookAhead = CHAR_LEFT_SQUARE_BRACKET;
                        setMode(TLexer.MODE_RANGE);
                        break;
                    }
                    case ']': {
                        if (mode == TLexer.MODE_RANGE) {
                            lookAhead = CHAR_RIGHT_SQUARE_BRACKET;
                        }
                        break;
                    }
                    case '^':
                        lookAhead = CHAR_CARET;
                        break;
                    case '|':
                        lookAhead = CHAR_VERTICAL_BAR;
                        break;
                    case '.':
                        lookAhead = CHAR_DOT;
                        break;
                    default:
                        break;
                }
            } else if (mode == TLexer.MODE_RANGE) {
                switch (lookAhead) {
                    case '[':
                        lookAhead = CHAR_LEFT_SQUARE_BRACKET;
                        break;
                    case ']':
                        lookAhead = CHAR_RIGHT_SQUARE_BRACKET;
                        break;
                    case '^':
                        lookAhead = CHAR_CARET;
                        break;
                    case '&':
                        lookAhead = CHAR_AMPERSAND;
                        break;
                    case '-':
                        lookAhead = CHAR_HYPHEN;
                        break;
                    default:
                        break;
                }
            }
        } while (reread);
    }

    /**
     * Parse character classes names and verifies correction of the syntax;
     */
    private String parseCharClassName() {
        StringBuilder sb = new StringBuilder(10);
        if (index < pattern.length - 2) {
            // one symbol family
            if (pattern[index] != '{') {
                return "Is" + new String(pattern, nextIndex(), 1); //$NON-NLS-1$
            }

            nextIndex();
            char ch = 0;
            while (index < pattern.length - 2) {
                ch = pattern[nextIndex()];
                if (ch == '}') {
                    break;
                }
                sb.append(ch);
            }
            if (ch != '}') {
                throw new TPatternSyntaxException("", this.toString(), index);
            }
        }

        if (sb.length() == 0) {
            throw new TPatternSyntaxException("", this.toString(), index);
        }

        String res = sb.toString();
        if (res.length() == 1) {
            return "Is" + res;
        }
        return (res.length() > 3 && (res.startsWith("Is") || res.startsWith("In"))) ? res.substring(2) : res;
    }

    /**
     * Process given character in assumption that it's quantifier.
     */
    private TQuantifier processQuantifier(int ch) {
        StringBuilder sb = new StringBuilder(4);
        int min = -1;
        int max = Integer.MAX_VALUE;
        while (index < pattern.length) {
            ch = pattern[nextIndex()];
            if (ch == '}') {
                break;
            }
            if (ch == ',' && min < 0) {
                try {
                    min = Integer.parseInt(sb.toString(), 10);
                    sb.delete(0, sb.length());
                } catch (NumberFormatException nfe) {
                    throw new TPatternSyntaxException("", this.toString(), index);
                }
            } else {
                sb.append((char) ch);
            }
        }
        if (ch != '}') {
            throw new TPatternSyntaxException("", this.toString(), index);
        }
        if (sb.length() > 0) {
            try {
                max = Integer.parseInt(sb.toString(), 10);
                if (min < 0) {
                    min = max;
                }
            } catch (NumberFormatException nfe) {
                throw new TPatternSyntaxException("", this.toString(), index);
            }
        } else if (min < 0) {
            throw new TPatternSyntaxException("", this.toString(), index);
        }
        if ((min | max | max - min) < 0) {
            throw new TPatternSyntaxException("", this.toString(), index);
        }

        char mod = (index < pattern.length) ? pattern[index] : '*';

        switch (mod) {
            case '+':
                lookAhead = TLexer.QUANT_COMP_P;
                nextIndex();
                break;
            case '?':
                lookAhead = TLexer.QUANT_COMP_R;
                nextIndex();
                break;
            default:
                lookAhead = TLexer.QUANT_COMP;
                break;
        }
        return new TQuantifier(min, max);
    }

    @Override
    public String toString() {
        return orig;
    }

    /**
     * Checks if there are any characters in the pattern.
     *
     * @return true if there are no more characters in the pattern.
     */
    public boolean isEmpty() {
        return ch == 0 && lookAhead == 0 && index == patternFullLength && !isSpecial();
    }

    public static boolean isLetter(int ch) {

        // all supplementary codepoints have integer value that is >= 0;
        return ch >= 0;
    }

    /**
     * Return true if current character is letter, false otherwise; This is
     * shortcut to static method isLetter to check the current character.
     *
     * @return true if current character is letter, false otherwise
     */
    public boolean isLetter() {
        return !isEmpty() && !isSpecial() && isLetter(ch);
    }

    /*
     * Note that Character class methods isHighSurrogate(), isLowSurrogate()
     * take char parameter while we need an int parameter without truncation to
     * char value
     */
    public boolean isHighSurrogate() {
        return (ch <= 0xDBFF) && (ch >= 0xD800);
    }

    public boolean isLowSurrogate() {
        return (ch <= 0xDFFF) && (ch >= 0xDC00);
    }

    public static boolean isHighSurrogate(int ch) {
        return (ch <= 0xDBFF) && (ch >= 0xD800);
    }

    public static boolean isLowSurrogate(int ch) {
        return (ch <= 0xDFFF) && (ch >= 0xDC00);
    }

    /**
     * Process hexadecimal integer.
     */
    private int readHex(int max) {
        StringBuilder st = new StringBuilder(max);
        int length = pattern.length - 2;
        int i;
        for (i = 0; i < max && index < length; i++) {
            st.append(pattern[nextIndex()]);
        }
        if (i == max) {
            try {
                return Integer.parseInt(st.toString(), 16);
            } catch (NumberFormatException nfe) {
                // do nothing
            }
        }

        throw new TPatternSyntaxException("", this.toString(), index);
    }

    /**
     * Process octal integer.
     */
    private int readOctals() {
        int max = 3;
        int i = 1;
        int first;
        int res;
        int length = pattern.length - 2;

        first = Character.digit(pattern[index], 8);
        switch (first) {
            case -1:
                throw new TPatternSyntaxException("", this.toString(), index);
            default: {
                if (first > 3) {
                    max--;
                }
                nextIndex();
                res = first;
            }
        }

        while (i < max && index < length) {
            first = Character.digit(pattern[index], 8);
            if (first < 0) {
                break;
            }
            res = res * 8 + first;
            nextIndex();
            i++;
        }

        return res;
    }

    /**
     * Process expression flags given with (?idmsux-idmsux)
     */
    private int readFlags() {
        char ch;
        boolean pos = true;
        int res = flags;

        while (index < pattern.length) {
            ch = pattern[index];
            switch (ch) {
                case '-':
                    if (!pos) {
                        throw new TPatternSyntaxException("", this.toString(), index);
                    }
                    pos = false;
                    break;

                case 'i':
                    res = pos ? res | TPattern.CASE_INSENSITIVE : (res ^ TPattern.CASE_INSENSITIVE) & res;
                    break;

                case 'd':
                    res = pos ? res | TPattern.UNIX_LINES : (res ^ TPattern.UNIX_LINES) & res;
                    break;

                case 'm':
                    res = pos ? res | TPattern.MULTILINE : (res ^ TPattern.MULTILINE) & res;
                    break;

                case 's':
                    res = pos ? res | TPattern.DOTALL : (res ^ TPattern.DOTALL) & res;
                    break;

                case 'u':
                    res = pos ? res | TPattern.UNICODE_CASE : (res ^ TPattern.UNICODE_CASE) & res;
                    break;

                case 'x':
                    res = pos ? res | TPattern.COMMENTS : (res ^ TPattern.COMMENTS) & res;
                    break;

                case ':':
                    nextIndex();
                    return res;

                case ')':
                    nextIndex();
                    return res | (1 << 8);

                default:
                    // ignore invalid flags (HARMONY-2127)
            }
            nextIndex();
        }
        throw new TPatternSyntaxException("", this.toString(), index);
    }

    /**
     * Returns next character index to read and moves pointer to the next one.
     * If comments flag is on this method will skip comments and whitespaces.
     *
     * The following actions are equivalent if comments flag is off ch =
     * pattern[index++] == ch = pattern[nextIndex]
     *
     * @return next character index to read.
     */
    private int nextIndex() {
        prevNW = index;
        if ((flags & TPattern.COMMENTS) != 0) {
            skipComments();
        } else {
            index++;
        }
        return prevNW;
    }

    /**
     * Skips comments and whitespaces
     */
    private int skipComments() {
        int length = pattern.length - 2;
        index++;
        do {
            while (index < length && Character.isWhitespace(pattern[index])) {
                index++;
            }
            if (index < length && pattern[index] == '#') {
                index++;
                while (index < length && !isLineSeparator(pattern[index])) {
                    index++;
                }
            } else {
                return index;
            }
        } while (true);
    }

    private boolean isLineSeparator(int ch) {
        return ch == '\n' || ch == '\r' || ch == '\u0085' || (ch | 1) == '\u2029';
    }

    /**
     * Gets decomposition for given codepoint from decomposition mappings table.
     *
     * @param ch
     *            - Unicode codepoint
     * @return array of codepoints that is a canonical decomposition of ch.
     */
    static int[] getDecomposition(int ch) {
        return decompTable.get(ch);
    }

    /**
     * Gets decomposition for given Hangul syllable. This is an implementation
     * of Hangul decomposition algorithm according to
     * http://www.unicode.org/versions/Unicode4.0.0/ch03.pdf
     * "3.12 Conjoining Jamo Behavior".
     *
     * @param ch
     *            - given Hangul syllable
     * @return canonical decomposition of ch.
     */
    static int[] getHangulDecomposition(int ch) {
        int sIndex = ch - SBase;

        if (sIndex < 0 || sIndex >= SCount) {
            return null;
        } else {
            int l = LBase + sIndex / NCount;
            int v = VBase + (sIndex % NCount) / TCount;
            int t = sIndex % TCount;
            int[] decomp;

            if (t == 0) {
                decomp = new int[] { l, v };
            } else {
                t = TBase + t;
                decomp = new int[] { l, v, t };
            }
            return decomp;
        }
    }

    /**
     * Tests if given codepoint is a canonical decomposition of another
     * codepoint.
     *
     * @param ch
     *            - codepoint to test
     * @return true if ch is a decomposition.
     */
    static boolean hasSingleCodepointDecomposition(int ch) {
        int hasSingleDecomp = singleDecompTable.get(ch);

        /*
         * singleDecompTable doesn't contain ch == (hasSingleDecomp ==
         * singleDecompTableSize)
         */
        return hasSingleDecomp != singleDecompTableSize;
    }

    /**
     * Tests if given codepoint has canonical decomposition and given
     * codepoint's canonical class is not 0.
     *
     * @param ch
     *            - codepoint to test
     * @return true if canonical class is not 0 and ch has a decomposition.
     */
    static boolean hasDecompositionNonNullCanClass(int ch) {
        return ch == 0x0340 | ch == 0x0341 | ch == 0x0343 | ch == 0x0344;
    }

    private int nextCodePoint() {
        char high = pattern[nextIndex()];

        if (Character.isHighSurrogate(high)) {

            // low and high char may be delimited by spaces
            int lowExpectedIndex = prevNW + 1;

            if (lowExpectedIndex < pattern.length) {
                char low = pattern[lowExpectedIndex];
                if (Character.isLowSurrogate(low)) {
                    nextIndex();
                    return Character.toCodePoint(high, low);
                }
            }
        }

        return high;
    }

    public int getIndex() {
        return curToc;
    }
}
