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

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Represents a pattern used for matching, searching, or replacing strings.
 * {@code Pattern}s are specified in terms of regular expressions and compiled
 * using an instance of this class. They are then used in conjunction with a
 * {@link TMatcher} to perform the actual search.
 * <p/>
 * A typical use case looks like this:
 * <p/>
 *
 * <pre>
 * Pattern p = Pattern.compile(&quot;Hello, A[a-z]*!&quot;);
 *
 * Matcher m = p.matcher(&quot;Hello, Android!&quot;);
 * boolean b1 = m.matches(); // true
 *
 * m.setInput(&quot;Hello, Robot!&quot;);
 * boolean b2 = m.matches(); // false
 * </pre>
 * <p/>
 * The above code could also be written in a more compact fashion, though this
 * variant is less efficient, since {@code Pattern} and {@code Matcher} objects
 * are created on the fly instead of being reused. fashion:
 *
 * <pre>
 * boolean b1 = Pattern.matches(&quot;Hello, A[a-z]*!&quot;, &quot;Hello, Android!&quot;); // true
 * boolean b2 = Pattern.matches(&quot;Hello, A[a-z]*!&quot;, &quot;Hello, Robot!&quot;); // false
 * </pre>
 *
 * @see TMatcher
 */
public final class TPattern implements Serializable {

    private static final long serialVersionUID = 5073258162644648461L;

    static final boolean _DEBUG_ = false;

    /**
     * This constant specifies that a pattern matches Unix line endings ('\n')
     * only against the '.', '^', and '$' meta characters.
     */
    public static final int UNIX_LINES = 1;

    /**
     * This constant specifies that a {@code Pattern} is matched
     * case-insensitively. That is, the patterns "a+" and "A+" would both match
     * the string "aAaAaA".
     */
    public static final int CASE_INSENSITIVE = 1 << 1;

    /**
     * This constant specifies that a {@code Pattern} may contain whitespace or
     * comments. Otherwise comments and whitespace are taken as literal
     * characters.
     */
    public static final int COMMENTS = 1 << 2;

    /**
     * This constant specifies that the meta characters '^' and '$' match only
     * the beginning and end end of an input line, respectively. Normally, they
     * match the beginning and the end of the complete input.
     */
    public static final int MULTILINE = 1 << 3;

    /**
     * This constant specifies that the whole {@code Pattern} is to be taken
     * literally, that is, all meta characters lose their meanings.
     */
    public static final int LITERAL = 1 << 4;

    /**
     * This constant specifies that the '.' meta character matches arbitrary
     * characters, including line endings, which is normally not the case.
     */
    public static final int DOTALL = 1 << 5;

    /**
     * This constant specifies that a {@code Pattern} is matched
     * case-insensitively with regard to all Unicode characters. It is used in
     * conjunction with the {@link #CASE_INSENSITIVE} constant to extend its
     * meaning to all Unicode characters.
     */
    public static final int UNICODE_CASE = 1 << 6;

    /**
     * This constant specifies that a character in a {@code Pattern} and a
     * character in the input string only match if they are canonically
     * equivalent.
     */
    public static final int CANON_EQ = 1 << 7;

    static final int BACK_REF_NUMBER = 10;

    /**
     * Bit mask that includes all defined match flags
     */
    static final int flagsBitMask = TPattern.UNIX_LINES | TPattern.CASE_INSENSITIVE | TPattern.COMMENTS
            | TPattern.MULTILINE | TPattern.LITERAL | TPattern.DOTALL | TPattern.UNICODE_CASE | TPattern.CANON_EQ;

    /**
     * Current <code>pattern</code> to be compiled;
     */
    private transient TLexer lexemes;

    /**
     * Pattern compile flags;
     */
    private int flags;

    /*
     * All backreferences that may be used in pattern.
     */
    transient private TFSet[] backRefs = new TFSet[BACK_REF_NUMBER];

    /*
     * Is true if backreferenced sets replacement is needed
     */
    transient private boolean needsBackRefReplacement;

    transient private int globalGroupIndex = -1;

    transient private int compCount = -1;

    transient private int consCount = -1;

    transient TAbstractSet start;

    /**
     * Returns a {@link TMatcher} for the {@code Pattern} and a given input. The
     * {@code Matcher} can be used to match the {@code Pattern} against the
     * whole input, find occurrences of the {@code Pattern} in the input, or
     * replace parts of the input.
     *
     * @param input
     *            the input to process.
     *
     * @return the resulting {@code Matcher}.
     */
    public TMatcher matcher(CharSequence input) {
        return new TMatcher(this, input);
    }

    /**
     * Splits the given input sequence around occurrences of the {@code Pattern}
     * . The function first determines all occurrences of the {@code Pattern}
     * inside the input sequence. It then builds an array of the
     * &quot;remaining&quot; strings before, in-between, and after these
     * occurrences. An additional parameter determines the maximal number of
     * entries in the resulting array and the handling of trailing empty
     * strings.
     *
     * @param inputSeq
     *            the input sequence.
     * @param limit
     *            Determines the maximal number of entries in the resulting
     *            array.
     *            <ul>
     *            <li>For n &gt; 0, it is guaranteed that the resulting array
     *            contains at most n entries.
     *            <li>For n &lt; 0, the length of the resulting array is exactly
     *            the number of occurrences of the {@code Pattern} +1. All
     *            entries are included.
     *            <li>For n == 0, the length of the resulting array is at most
     *            the number of occurrences of the {@code Pattern} +1. Empty
     *            strings at the end of the array are not included.
     *            </ul>
     *
     * @return the resulting array.
     */
    public String[] split(CharSequence inputSeq, int limit) {
        ArrayList<String> res = new ArrayList<>();
        TMatcher mat = matcher(inputSeq);
        int index = 0;
        int curPos = 0;

        if (inputSeq.length() == 0) {
            return new String[] { "" }; //$NON-NLS-1$
        } else {
            while (mat.find() && (index + 1 < limit || limit <= 0)) {
                res.add(inputSeq.subSequence(curPos, mat.start()).toString());
                curPos = mat.end();
                index++;
            }

            res.add(inputSeq.subSequence(curPos, inputSeq.length()).toString());
            index++;

            /*
             * discard trailing empty strings
             */
            if (limit == 0) {
                while (--index >= 0 && res.get(index).toString().length() == 0) {
                    res.remove(index);
                }
            }
        }
        return res.toArray(new String[index >= 0 ? index : 0]);
    }

    /**
     * Splits a given input around occurrences of a regular expression. This is
     * a convenience method that is equivalent to calling the method
     * {@link #split(java.lang.CharSequence, int)} with a limit of 0.
     *
     * @param input
     *            the input sequence.
     *
     * @return the resulting array.
     */
    public String[] split(CharSequence input) {
        return split(input, 0);
    }

    /**
     * Returns the regular expression that was compiled into this
     * {@code Pattern}.
     *
     * @return the regular expression.
     */
    public String pattern() {
        return lexemes.toString();
    }

    @Override
    public String toString() {
        return this.pattern();
    }

    /**
     * Returns the flags that have been set for this {@code Pattern}.
     *
     * @return the flags that have been set. A combination of the constants
     *         defined in this class.
     *
     * @see #CANON_EQ
     * @see #CASE_INSENSITIVE
     * @see #COMMENTS
     * @see #DOTALL
     * @see #LITERAL
     * @see #MULTILINE
     * @see #UNICODE_CASE
     * @see #UNIX_LINES
     */
    public int flags() {
        return this.flags;
    }

    /**
     * Compiles a regular expression, creating a new {@code Pattern} instance in
     * the process. Allows to set some flags that modify the behavior of the
     * {@code Pattern}.
     *
     * @param pattern
     *            the regular expression.
     * @param flags
     *            the flags to set. Basically, any combination of the constants
     *            defined in this class is valid.
     *
     * @return the new {@code Pattern} instance.
     *
     * @throws TPatternSyntaxException
     *             if the regular expression is syntactically incorrect.
     *
     * @see #CANON_EQ
     * @see #CASE_INSENSITIVE
     * @see #COMMENTS
     * @see #DOTALL
     * @see #LITERAL
     * @see #MULTILINE
     * @see #UNICODE_CASE
     * @see #UNIX_LINES
     */
    public static TPattern compile(String pattern, int flags) throws TPatternSyntaxException {
        if (pattern == null) {
            throw new NullPointerException("Patter is null");
        }
        if ((flags != 0) && ((flags | flagsBitMask) != flagsBitMask)) {
            throw new IllegalArgumentException("");
        }
        TAbstractSet.counter = 1;
        return new TPattern().compileImpl(pattern, flags);
    }

    /**
     *
     * @param pattern
     *            - Regular expression to be compiled
     * @param flags
     *            - The bit mask including CASE_INSENSITIVE, MULTILINE, DOTALL,
     *            UNICODE_CASE, and CANON_EQ
     *
     * @return Compiled pattern
     */
    private TPattern compileImpl(String pattern, int flags) throws TPatternSyntaxException {
        this.lexemes = new TLexer(pattern, flags);
        this.flags = flags;

        start = processExpression(-1, this.flags, null);
        if (!lexemes.isEmpty()) {
            throw new TPatternSyntaxException("", lexemes.toString(), lexemes.getIndex());
        }
        finalizeCompile();
        return this;
    }

    /**
     * A->(a|)+
     */
    private TAbstractSet processAlternations(TAbstractSet last) {
        TCharClass auxRange = new TCharClass(hasFlag(TPattern.CASE_INSENSITIVE), hasFlag(TPattern.UNICODE_CASE));
        while (!lexemes.isEmpty()
                && lexemes.isLetter()
                && (lexemes.lookAhead() == 0 || lexemes.lookAhead() == TLexer.CHAR_VERTICAL_BAR
                || lexemes.lookAhead() == TLexer.CHAR_RIGHT_PARENTHESIS)) {
            auxRange.add(lexemes.next());
            if (lexemes.peek() == TLexer.CHAR_VERTICAL_BAR) {
                lexemes.next();
            }
        }
        TAbstractSet rangeSet = processRangeSet(auxRange);
        rangeSet.setNext(last);

        return rangeSet;
    }

    /**
     * E->AE; E->S|E; E->S; A->(a|)+ E->S(|S)*
     */
    private TAbstractSet processExpression(int ch, int newFlags, TAbstractSet last) {
        ArrayList<TAbstractSet> children = new ArrayList<>();
        TAbstractSet child;
        int saveFlags = flags;
        TFSet fSet;
        boolean saveChangedFlags = false;

        if (newFlags != flags) {
            flags = newFlags;
        }

        switch (ch) {
            case TLexer.CHAR_NONCAP_GROUP:
                fSet = new TNonCapFSet(++consCount);
                break;

            case TLexer.CHAR_POS_LOOKAHEAD:
                /* falls through */

            case TLexer.CHAR_NEG_LOOKAHEAD:
                fSet = new TAheadFSet();
                break;

            case TLexer.CHAR_POS_LOOKBEHIND:
                /* falls through */

            case TLexer.CHAR_NEG_LOOKBEHIND:
                fSet = new TBehindFSet(++consCount);
                break;

            case TLexer.CHAR_ATOMIC_GROUP:
                fSet = new TAtomicFSet(++consCount);
                break;

            default:
                globalGroupIndex++;
                if (last == null) {

                    // expr = new StartSet();
                    fSet = new TFinalSet();
                    saveChangedFlags = true;
                } else {

                    // expr = new JointSet(globalGroupIndex);
                    fSet = new TFSet(globalGroupIndex);
                }
                if (globalGroupIndex > -1 && globalGroupIndex < 10) {
                    backRefs[globalGroupIndex] = fSet;
                }
                break;
        }

        do {
            if (lexemes.isLetter() && lexemes.lookAhead() == TLexer.CHAR_VERTICAL_BAR) {
                child = processAlternations(fSet);
            } else if (lexemes.peek() == TLexer.CHAR_VERTICAL_BAR) {
                child = new TEmptySet(fSet);
                lexemes.next();
            } else {
                child = processSubExpression(fSet);
                if (lexemes.peek() == TLexer.CHAR_VERTICAL_BAR) {
                    lexemes.next();
                }
            }
            if (child != null) {

                // expr.addChild(child);
                children.add(child);
            }
        } while (!(lexemes.isEmpty() || (lexemes.peek() == TLexer.CHAR_RIGHT_PARENTHESIS)));

        if (lexemes.back() == TLexer.CHAR_VERTICAL_BAR) {
            children.add(new TEmptySet(fSet));
        }

        if (flags != saveFlags && !saveChangedFlags) {
            flags = saveFlags;
            lexemes.restoreFlags(flags);
        }

        switch (ch) {
            case TLexer.CHAR_NONCAP_GROUP:
                return new TNonCapJointSet(children, fSet);

            case TLexer.CHAR_POS_LOOKAHEAD:
                return new TPositiveLookAhead(children, fSet);

            case TLexer.CHAR_NEG_LOOKAHEAD:
                return new TNegativeLookAhead(children, fSet);

            case TLexer.CHAR_POS_LOOKBEHIND:
                return new TPositiveLookBehind(children, fSet);

            case TLexer.CHAR_NEG_LOOKBEHIND:
                return new TNegativeLookBehind(children, fSet);

            case TLexer.CHAR_ATOMIC_GROUP:
                return new TAtomicJointSet(children, fSet);

            default:
                switch (children.size()) {
                    case 0:
                        return new TEmptySet(fSet);

                    case 1:
                        return new TSingleSet(children.get(0), fSet);

                    default:
                        return new TJointSet(children, fSet);
                }
        }
    }

    /**
     * T->a+
     */
    private TAbstractSet processSequence() {
        StringBuffer substring = new StringBuffer();

        while (!lexemes.isEmpty()
                && lexemes.isLetter()
                && !lexemes.isHighSurrogate()
                && !lexemes.isLowSurrogate()
                && ((!lexemes.isNextSpecial() && lexemes.lookAhead() == 0) // end
                        // of
                        // pattern
                        || (!lexemes.isNextSpecial() && TLexer.isLetter(lexemes.lookAhead()))
                        || lexemes.lookAhead() == TLexer.CHAR_RIGHT_PARENTHESIS
                        || (lexemes.lookAhead() & 0x8000ffff) == TLexer.CHAR_LEFT_PARENTHESIS
                        || lexemes.lookAhead() == TLexer.CHAR_VERTICAL_BAR
                        || lexemes.lookAhead() == TLexer.CHAR_DOLLAR)) {
            int ch = lexemes.next();

            if (Character.isSupplementaryCodePoint(ch)) {
                substring.append(Character.toChars(ch));
            } else {
                substring.append((char) ch);
            }
        }
        if (!hasFlag(TPattern.CASE_INSENSITIVE)) {
            return new TSequenceSet(substring);
        } else if (!hasFlag(TPattern.UNICODE_CASE)) {
            return new TCISequenceSet(substring);
        } else {
            return new TUCISequenceSet(substring);
        }
    }

    /**
     * D->a
     */
    private TAbstractSet processDecomposedChar() {
        int[] codePoints = new int[TLexer.MAX_DECOMPOSITION_LENGTH];
        char[] codePointsHangul;
        int readCodePoints = 0;
        int curSymb = -1;
        int curSymbIndex = -1;

        if (!lexemes.isEmpty() && lexemes.isLetter()) {
            curSymb = lexemes.next();
            codePoints[readCodePoints] = curSymb;
            curSymbIndex = curSymb - TLexer.LBase;
        }

        /*
         * We process decomposed Hangul syllable LV or LVT or process jamo L.
         * See http://www.unicode.org/versions/Unicode4.0.0/ch03.pdf
         * "3.12 Conjoining Jamo Behavior"
         */
        if ((curSymbIndex >= 0) && (curSymbIndex < TLexer.LCount)) {
            codePointsHangul = new char[TLexer.MAX_HANGUL_DECOMPOSITION_LENGTH];
            codePointsHangul[readCodePoints++] = (char) curSymb;

            curSymb = lexemes.peek();
            curSymbIndex = curSymb - TLexer.VBase;
            if ((curSymbIndex >= 0) && (curSymbIndex < TLexer.VCount)) {
                codePointsHangul[readCodePoints++] = (char) curSymb;
                lexemes.next();
                curSymb = lexemes.peek();
                curSymbIndex = curSymb - TLexer.TBase;
                if ((curSymbIndex >= 0) && (curSymbIndex < TLexer.TCount)) {
                    codePointsHangul[readCodePoints++] = (char) curSymb;
                    lexemes.next();

                    // LVT syllable
                    return new THangulDecomposedCharSet(codePointsHangul, 3);
                } else {

                    // LV syllable
                    return new THangulDecomposedCharSet(codePointsHangul, 2);
                }
            } else {

                // L jamo
                if (!hasFlag(TPattern.CASE_INSENSITIVE)) {
                    return new TCharSet(codePointsHangul[0]);
                } else if (!hasFlag(TPattern.UNICODE_CASE)) {
                    return new TCICharSet(codePointsHangul[0]);
                } else {
                    return new TUCICharSet(codePointsHangul[0]);
                }
            }

            /*
             * We process single codepoint or decomposed codepoint. We collect
             * decomposed codepoint and obtain one DecomposedCharSet.
             */
        } else {
            readCodePoints++;

            while ((readCodePoints < TLexer.MAX_DECOMPOSITION_LENGTH) && !lexemes.isEmpty() && lexemes.isLetter()) {
                codePoints[readCodePoints++] = lexemes.next();
            }

            /*
             * We have read an ordinary symbol.
             */
            if (readCodePoints == 1 && !TLexer.hasSingleCodepointDecomposition(codePoints[0])) {
                return processCharSet(codePoints[0]);
            } else {
                if (!hasFlag(TPattern.CASE_INSENSITIVE)) {
                    return new TDecomposedCharSet(codePoints, readCodePoints);
                } else if (!hasFlag(TPattern.UNICODE_CASE)) {
                    return new TCIDecomposedCharSet(codePoints, readCodePoints);
                } else {
                    return new TUCIDecomposedCharSet(codePoints, readCodePoints);
                }
            }
        }
    }

    /**
     * S->BS; S->QS; S->Q; B->a+
     */
    private TAbstractSet processSubExpression(TAbstractSet last) {
        TAbstractSet cur;
        if (lexemes.isLetter() && !lexemes.isNextSpecial() && TLexer.isLetter(lexemes.lookAhead())) {
            if (hasFlag(TPattern.CANON_EQ)) {
                cur = processDecomposedChar();
                if (!lexemes.isEmpty()

                /* && !pattern.isQuantifier() */
                && (lexemes.peek() != TLexer.CHAR_RIGHT_PARENTHESIS || last instanceof TFinalSet)
                        && lexemes.peek() != TLexer.CHAR_VERTICAL_BAR && !lexemes.isLetter()) {
                    cur = processQuantifier(last, cur);
                }
            } else if (lexemes.isHighSurrogate() || lexemes.isLowSurrogate()) {
                TAbstractSet term = processTerminal(last);
                cur = processQuantifier(last, term);
            } else {
                cur = processSequence();
            }
        } else if (lexemes.peek() == TLexer.CHAR_RIGHT_PARENTHESIS) {
            if (last instanceof TFinalSet) {
                throw new TPatternSyntaxException("", lexemes.toString(), lexemes.getIndex());
            } else {
                cur = new TEmptySet(last);
            }
        } else {
            TAbstractSet term = processTerminal(last);
            cur = processQuantifier(last, term);
        }

        if (!lexemes.isEmpty()
        // && !pattern.isQuantifier()
                && (lexemes.peek() != TLexer.CHAR_RIGHT_PARENTHESIS || last instanceof TFinalSet)
                && lexemes.peek() != TLexer.CHAR_VERTICAL_BAR) {
            TAbstractSet next = processSubExpression(last);
            if (cur instanceof TLeafQuantifierSet
            // TODO create personal UnifiedQuantifierSet for composite
            // quantifiers
            // to take into account Quantifier counters
            // ////
                    && !(cur instanceof TCompositeQuantifierSet)
                    && !(cur instanceof TGroupQuantifierSet)
                    && !(cur instanceof TAltQuantifierSet) && !next.first(((TLeafQuantifierSet) cur).getInnerSet())) {
                cur = new TUnifiedQuantifierSet((TLeafQuantifierSet) cur);
            }
            if (((char) next.getType()) == '+') {
                cur.setNext(((TLeafQuantifierSet) next).getInnerSet());
            } else {
                cur.setNext(next);
            }
        } else if (cur != null) {
            cur.setNext(last);
        } else {
            return null;
        }

        if (((char) cur.getType()) == '+') {
            return ((TQuantifierSet) cur).getInnerSet();
        } else {
            return cur;
        }
    }

    /**
     * Q->T(*|+|?...) also do some optimizations.
     *
     */
    private TAbstractSet processQuantifier(TAbstractSet last, TAbstractSet term) {
        int quant = lexemes.peek();

        if (term != null && !(term instanceof TLeafSet)) {
            switch (quant) {
                case TLexer.QUANT_STAR:
                case TLexer.QUANT_PLUS: {
                    TQuantifierSet q;

                    lexemes.next();
                    if (term.getType() == TAbstractSet.TYPE_DOTSET) {
                        if (!hasFlag(TPattern.DOTALL)) {
                            q = new TDotQuantifierSet(term, last, quant, TAbstractLineTerminator.getInstance(flags));
                        } else {
                            q = new TDotAllQuantifierSet(term, last, quant);
                        }
                    } else {
                        q = new TGroupQuantifierSet(term, last, quant);
                    }
                    term.setNext(q);
                    return q;
                }

                case TLexer.QUANT_STAR_R:
                case TLexer.QUANT_PLUS_R: {
                    lexemes.next();
                    TGroupQuantifierSet q = new TReluctantGroupQuantifierSet(term, last, quant);
                    term.setNext(q);
                    return q;
                }

                case TLexer.QUANT_PLUS_P: {
                    lexemes.next();
                    // possessive plus will be handled by unique class
                    // and should not be postprocessed to point previous set
                    // to the inner one.
                    // //
                    return new TPosPlusGroupQuantifierSet(term, last, TLexer.QUANT_STAR_P);
                }

                case TLexer.QUANT_STAR_P: {
                    lexemes.next();
                    return new TPossessiveGroupQuantifierSet(term, last, quant);
                }

                case TLexer.QUANT_ALT: {
                    lexemes.next();
                    TAltGroupQuantifierSet q = new TAltGroupQuantifierSet(term, last, TLexer.QUANT_ALT);
                    term.setNext(last);
                    return q;
                }

                case TLexer.QUANT_ALT_P: {
                    lexemes.next();
                    return new TPosAltGroupQuantifierSet(term, last, TLexer.QUANT_ALT);
                }

                case TLexer.QUANT_ALT_R: {
                    lexemes.next();
                    TRelAltGroupQuantifierSet q = new TRelAltGroupQuantifierSet(term, last, TLexer.QUANT_ALT);
                    term.setNext(last);
                    return q;
                }

                case TLexer.QUANT_COMP: {
                    TCompositeGroupQuantifierSet q = new TCompositeGroupQuantifierSet(
                            (TQuantifier) lexemes.nextSpecial(), term, last, TLexer.QUANT_ALT, ++compCount);
                    term.setNext(q);
                    return q;
                }

                case TLexer.QUANT_COMP_P: {
                    return new TPosCompositeGroupQuantifierSet((TQuantifier) lexemes.nextSpecial(), term, last,
                            TLexer.QUANT_ALT, ++compCount);
                }

                case TLexer.QUANT_COMP_R: {
                    TRelCompositeGroupQuantifierSet q = new TRelCompositeGroupQuantifierSet(
                            (TQuantifier) lexemes.nextSpecial(), term, last, TLexer.QUANT_ALT, ++compCount);
                    term.setNext(q);
                    return q;
                }

                default:
                    return term;
            }
        } else {
            TLeafSet leaf = null;
            if (term != null) {
                leaf = (TLeafSet) term;
            }
            switch (quant) {
                case TLexer.QUANT_STAR:
                case TLexer.QUANT_PLUS: {
                    lexemes.next();
                    TLeafQuantifierSet q = new TLeafQuantifierSet(leaf, last, quant);
                    leaf.setNext(q);
                    return q;
                }

                case TLexer.QUANT_STAR_R:
                case TLexer.QUANT_PLUS_R: {
                    lexemes.next();
                    TReluctantQuantifierSet q = new TReluctantQuantifierSet(leaf, last, quant);
                    leaf.setNext(q);
                    return q;
                }

                case TLexer.QUANT_PLUS_P:
                case TLexer.QUANT_STAR_P: {
                    lexemes.next();
                    TPossessiveQuantifierSet q = new TPossessiveQuantifierSet(leaf, last, quant);
                    leaf.setNext(q);
                    return q;
                }

                case TLexer.QUANT_ALT: {
                    lexemes.next();
                    return new TAltQuantifierSet(leaf, last, TLexer.QUANT_ALT);
                }

                case TLexer.QUANT_ALT_P: {
                    lexemes.next();
                    return new TPossessiveAltQuantifierSet(leaf, last, TLexer.QUANT_ALT_P);
                }

                case TLexer.QUANT_ALT_R: {
                    lexemes.next();
                    return new TReluctantAltQuantifierSet(leaf, last, TLexer.QUANT_ALT_R);
                }

                case TLexer.QUANT_COMP: {
                    return new TCompositeQuantifierSet((TQuantifier) lexemes.nextSpecial(), leaf, last,
                            TLexer.QUANT_COMP);
                }

                case TLexer.QUANT_COMP_P: {
                    return new TPossessiveCompositeQuantifierSet((TQuantifier) lexemes.nextSpecial(), leaf, last,
                            TLexer.QUANT_COMP_P);
                }
                case TLexer.QUANT_COMP_R: {
                    return new TReluctantCompositeQuantifierSet((TQuantifier) lexemes.nextSpecial(), leaf, last,
                            TLexer.QUANT_COMP_R);
                }

                default:
                    return term;
            }
        }
    }

    /**
     * T-> letter|[range]|{char-class}|(E)
     */
    private TAbstractSet processTerminal(TAbstractSet last) {
        int ch;
        TAbstractSet term = null;
        do {
            ch = lexemes.peek();
            if ((ch & 0x8000ffff) == TLexer.CHAR_LEFT_PARENTHESIS) {
                int newFlags;
                lexemes.next();
                newFlags = (ch & 0x00ff0000) >> 16;
                ch = ch & 0xff00ffff;
                if (ch == TLexer.CHAR_FLAGS) {
                    flags = newFlags;
                } else {
                    newFlags = (ch == TLexer.CHAR_NONCAP_GROUP) ? newFlags : flags;
                    term = processExpression(ch, newFlags, last);
                    if (lexemes.peek() != TLexer.CHAR_RIGHT_PARENTHESIS) {
                        throw new TPatternSyntaxException("", lexemes.toString(), lexemes.getIndex());
                    }
                    lexemes.next();
                }
            } else {
                switch (ch) {
                    case TLexer.CHAR_LEFT_SQUARE_BRACKET: {
                        lexemes.next();
                        boolean negative = false;
                        if (lexemes.peek() == TLexer.CHAR_CARET) {
                            negative = true;
                            lexemes.next();
                        }

                        term = processRange(negative, last);
                        if (lexemes.peek() != TLexer.CHAR_RIGHT_SQUARE_BRACKET) {
                            throw new TPatternSyntaxException("", lexemes.toString(), lexemes.getIndex());
                        }
                        lexemes.setMode(TLexer.MODE_PATTERN);
                        lexemes.next();
                        break;
                    }

                    case TLexer.CHAR_DOT: {
                        lexemes.next();

                        if (!hasFlag(TPattern.DOTALL)) {
                            term = new TDotSet(TAbstractLineTerminator.getInstance(flags));
                        } else {
                            term = new TDotAllSet();
                        }

                        break;
                    }

                    case TLexer.CHAR_CARET: {
                        lexemes.next();
                        consCount++;
                        if (!hasFlag(TPattern.MULTILINE)) {
                            term = new TSOLSet();
                        } else {
                            term = new TMultiLineSOLSet(TAbstractLineTerminator.getInstance(flags));
                        }

                        break;
                    }

                    case TLexer.CHAR_DOLLAR: {
                        lexemes.next();
                        consCount++;
                        if (!hasFlag(TPattern.MULTILINE)) {
                            if (!hasFlag(TPattern.UNIX_LINES)) {
                                term = new TEOLSet(consCount);
                            } else {
                                term = new TUEOLSet(consCount);
                            }
                        } else {
                            if (!hasFlag(TPattern.UNIX_LINES)) {
                                term = new TMultiLineEOLSet(consCount);
                            } else {
                                term = new TUMultiLineEOLSet(consCount);
                            }
                        }

                        break;
                    }

                    case TLexer.CHAR_WORD_BOUND: {
                        lexemes.next();
                        term = new TWordBoundary(true);
                        break;
                    }

                    case TLexer.CHAR_NONWORD_BOUND: {
                        lexemes.next();
                        term = new TWordBoundary(false);
                        break;
                    }

                    case TLexer.CHAR_END_OF_INPUT: {
                        lexemes.next();
                        term = new TEOISet();
                        break;
                    }

                    case TLexer.CHAR_END_OF_LINE: {
                        lexemes.next();
                        term = new TEOLSet(++consCount);
                        break;
                    }

                    case TLexer.CHAR_START_OF_INPUT: {
                        lexemes.next();
                        term = new TSOLSet();
                        break;
                    }

                    case TLexer.CHAR_PREVIOUS_MATCH: {
                        lexemes.next();
                        term = new TPreviousMatch();
                        break;
                    }

                    case 0x80000000 | '1':
                    case 0x80000000 | '2':
                    case 0x80000000 | '3':
                    case 0x80000000 | '4':
                    case 0x80000000 | '5':
                    case 0x80000000 | '6':
                    case 0x80000000 | '7':
                    case 0x80000000 | '8':
                    case 0x80000000 | '9': {
                        int number = (ch & 0x7FFFFFFF) - '0';
                        if (globalGroupIndex >= number) {
                            lexemes.next();
                            consCount++;
                            if (!hasFlag(TPattern.CASE_INSENSITIVE)) {
                                term = new TBackReferenceSet(number, consCount);
                            } else if (!hasFlag(TPattern.UNICODE_CASE)) {
                                term = new TCIBackReferenceSet(number, consCount);
                            } else {
                                term = new TUCIBackReferenceSet(number, consCount);
                            }
                            (backRefs[number]).isBackReferenced = true;
                            needsBackRefReplacement = true;
                            break;
                        } else {
                            throw new TPatternSyntaxException("", lexemes.toString(), lexemes.getIndex());
                        }
                    }

                    case 0: {
                        TAbstractCharClass cc = (TAbstractCharClass) lexemes.peekSpecial();
                        if (cc != null) {
                            term = processRangeSet(cc);
                        } else if (!lexemes.isEmpty()) {

                            // ch == 0
                            term = new TCharSet((char) ch);
                        } else {
                            term = new TEmptySet(last);
                            break;
                        }
                        lexemes.next();
                        break;
                    }

                    default: {
                        if (ch >= 0 && !lexemes.isSpecial()) {
                            term = processCharSet(ch);
                            lexemes.next();
                        } else if (ch == TLexer.CHAR_VERTICAL_BAR) {
                            term = new TEmptySet(last);
                        } else if (ch == TLexer.CHAR_RIGHT_PARENTHESIS) {
                            if (last instanceof TFinalSet) {
                                throw new TPatternSyntaxException("", lexemes.toString(), lexemes.getIndex());
                            } else {
                                term = new TEmptySet(last);
                            }
                        } else {
                            throw new TPatternSyntaxException(
                                    lexemes.isSpecial() ? lexemes.peekSpecial().toString()
                                            : Character.toString((char) ch),
                                    lexemes.toString(), lexemes.getIndex());
                        }
                    }
                }
            }
        } while (ch == TLexer.CHAR_FLAGS);
        return term;
    }

    private TAbstractSet processRange(boolean negative, TAbstractSet last) {
        TAbstractCharClass res = processRangeExpression(negative);
        TAbstractSet rangeSet = processRangeSet(res);
        rangeSet.setNext(last);

        return rangeSet;
    }

    /**
     * process [...] ranges
     */
    private TCharClass processRangeExpression(boolean alt) {
        TCharClass res = new TCharClass(alt, hasFlag(TPattern.CASE_INSENSITIVE), hasFlag(TPattern.UNICODE_CASE));
        int buffer = -1;
        boolean intersection = false;
        boolean notClosed = false;
        boolean firstInClass = true;

        while (!lexemes.isEmpty()) {
            notClosed = lexemes.peek() != TLexer.CHAR_RIGHT_SQUARE_BRACKET || firstInClass;
            if (!notClosed) {
                break;
            }
            switch (lexemes.peek()) {

                case TLexer.CHAR_RIGHT_SQUARE_BRACKET: {
                    if (buffer >= 0) {
                        res.add(buffer);
                    }
                    buffer = ']';
                    lexemes.next();
                    break;
                }
                case TLexer.CHAR_LEFT_SQUARE_BRACKET: {
                    if (buffer >= 0) {
                        res.add(buffer);
                        buffer = -1;
                    }
                    lexemes.next();
                    boolean negative = false;
                    if (lexemes.peek() == TLexer.CHAR_CARET) {
                        lexemes.next();
                        negative = true;
                    }

                    if (intersection) {
                        res.intersection(processRangeExpression(negative));
                    } else {
                        res.union(processRangeExpression(negative));
                    }
                    intersection = false;
                    lexemes.next();
                    break;
                }

                case TLexer.CHAR_AMPERSAND: {
                    if (buffer >= 0) {
                        res.add(buffer);
                    }
                    buffer = lexemes.next();

                    /*
                     * if there is a start for subrange we will do an
                     * intersection otherwise treat '&' as a normal character
                     */
                    if (lexemes.peek() == TLexer.CHAR_AMPERSAND) {
                        if (lexemes.lookAhead() == TLexer.CHAR_LEFT_SQUARE_BRACKET) {
                            lexemes.next();
                            intersection = true;
                            buffer = -1;
                        } else {
                            lexemes.next();
                            if (firstInClass) {

                                // skip "&&" at "[&&...]" or "[^&&...]"
                                res = processRangeExpression(false);
                            } else {

                                // ignore "&&" at "[X&&]" ending where X !=
                                // empty string
                                if (!(lexemes.peek() == TLexer.CHAR_RIGHT_SQUARE_BRACKET)) {
                                    res.intersection(processRangeExpression(false));
                                }
                            }

                        }
                    } else {

                        // treat '&' as a normal character
                        buffer = '&';
                    }

                    break;
                }

                case TLexer.CHAR_HYPHEN: {
                    if (firstInClass || lexemes.lookAhead() == TLexer.CHAR_RIGHT_SQUARE_BRACKET
                            || lexemes.lookAhead() == TLexer.CHAR_LEFT_SQUARE_BRACKET || buffer < 0) {
                        // treat hypen as normal character
                        if (buffer >= 0) {
                            res.add(buffer);
                        }
                        buffer = '-';
                        lexemes.next();
                        // range
                    } else {
                        lexemes.next();
                        int cur = lexemes.peek();

                        if (!lexemes.isSpecial()
                                && (cur >= 0 || lexemes.lookAhead() == TLexer.CHAR_RIGHT_SQUARE_BRACKET
                                        || lexemes.lookAhead() == TLexer.CHAR_LEFT_SQUARE_BRACKET || buffer < 0)) {

                            try {
                                if (!TLexer.isLetter(cur)) {
                                    cur = cur & 0xFFFF;
                                }
                                res.add(buffer, cur);
                            } catch (Exception e) {
                                throw new TPatternSyntaxException("", pattern(), lexemes.getIndex());
                            }
                            lexemes.next();
                            buffer = -1;
                        } else {
                            throw new TPatternSyntaxException("", pattern(), lexemes.getIndex());
                        }
                    }

                    break;
                }

                case TLexer.CHAR_CARET: {
                    if (buffer >= 0) {
                        res.add(buffer);
                    }
                    buffer = '^';
                    lexemes.next();
                    break;
                }

                case 0: {
                    if (buffer >= 0) {
                        res.add(buffer);
                    }
                    TAbstractCharClass cs = (TAbstractCharClass) lexemes.peekSpecial();
                    if (cs != null) {
                        res.add(cs);
                        buffer = -1;
                    } else {
                        buffer = 0;
                    }

                    lexemes.next();
                    break;
                }

                default: {
                    if (buffer >= 0) {
                        res.add(buffer);
                    }
                    buffer = lexemes.next();
                    break;
                }
            }

            firstInClass = false;
        }
        if (notClosed) {
            throw new TPatternSyntaxException("", pattern(), lexemes.getIndex() - 1);
        }
        if (buffer >= 0) {
            res.add(buffer);
        }
        return res;
    }

    private TAbstractSet processCharSet(int ch) {
        boolean isSupplCodePoint = Character.isSupplementaryCodePoint(ch);

        if (hasFlag(TPattern.CASE_INSENSITIVE)) {

            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                return new TCICharSet((char) ch);
            } else if (hasFlag(TPattern.UNICODE_CASE) && ch > 128) {
                if (isSupplCodePoint) {
                    return new TUCISupplCharSet(ch);
                } else if (TLexer.isLowSurrogate(ch)) {

                    // we need no UCILowSurrogateCharSet
                    return new TLowSurrogateCharSet((char) ch);
                } else if (TLexer.isHighSurrogate(ch)) {

                    // we need no UCIHighSurrogateCharSet
                    return new THighSurrogateCharSet((char) ch);
                } else {
                    return new TUCICharSet((char) ch);
                }
            }
        }

        if (isSupplCodePoint) {
            return new TSupplCharSet(ch);
        } else if (TLexer.isLowSurrogate(ch)) {
            return new TLowSurrogateCharSet((char) ch);
        } else if (TLexer.isHighSurrogate(ch)) {
            return new THighSurrogateCharSet((char) ch);
        } else {
            return new TCharSet((char) ch);
        }
    }

    private TAbstractSet processRangeSet(TAbstractCharClass charClass) {
        if (charClass.hasLowHighSurrogates()) {
            TAbstractCharClass surrogates = charClass.getSurrogates();
            TLowHighSurrogateRangeSet lowHighSurrRangeSet = new TLowHighSurrogateRangeSet(surrogates);

            if (charClass.mayContainSupplCodepoints()) {
                if (!charClass.hasUCI()) {
                    return new TCompositeRangeSet(new TSupplRangeSet(charClass.getWithoutSurrogates()),
                            lowHighSurrRangeSet);
                } else {
                    return new TCompositeRangeSet(new TUCISupplRangeSet(charClass.getWithoutSurrogates()),
                            lowHighSurrRangeSet);
                }
            }

            if (!charClass.hasUCI()) {
                return new TCompositeRangeSet(new TRangeSet(charClass.getWithoutSurrogates()), lowHighSurrRangeSet);
            } else {
                return new TCompositeRangeSet(new TUCIRangeSet(charClass.getWithoutSurrogates()), lowHighSurrRangeSet);
            }
        }

        if (charClass.mayContainSupplCodepoints()) {
            if (!charClass.hasUCI()) {
                return new TSupplRangeSet(charClass);
            } else {
                return new TUCISupplRangeSet(charClass);
            }
        }

        if (!charClass.hasUCI()) {
            return new TRangeSet(charClass);
        } else {
            return new TUCIRangeSet(charClass);
        }
    }

    /**
     * Compiles a regular expression, creating a new Pattern instance in the
     * process. This is actually a convenience method that calls
     * {@link #compile(String, int)} with a {@code flags} value of zero.
     *
     * @param pattern
     *            the regular expression.
     *
     * @return the new {@code Pattern} instance.
     *
     * @throws TPatternSyntaxException
     *             if the regular expression is syntactically incorrect.
     */
    public static TPattern compile(String pattern) {
        return compile(pattern, 0);
    }

    /*
     * This method do traverses of automata to finish compilation.
     */
    private void finalizeCompile() {

        /*
         * Processing second pass
         */
        if (needsBackRefReplacement) { // || needsReason1 || needsReason2) {
            start.processSecondPass();
        }

    }

    /**
     * Tries to match a given regular expression against a given input. This is
     * actually nothing but a convenience method that compiles the regular
     * expression into a {@code Pattern}, builds a {@link TMatcher} for it, and
     * then does the match. If the same regular expression is used for multiple
     * operations, it is recommended to compile it into a {@code Pattern}
     * explicitly and request a reusable {@code Matcher}.
     *
     * @param regex
     *            the regular expression.
     * @param input
     *            the input to process.
     *
     * @return true if and only if the {@code Pattern} matches the input.
     *
     * @see TPattern#compile(java.lang.String, int)
     * @see TMatcher#matches()
     */
    public static boolean matches(String regex, CharSequence input) {
        return TPattern.compile(regex).matcher(input).matches();
    }

    /**
     * Quotes a given string using "\Q" and "\E", so that all other
     * meta-characters lose their special meaning. If the string is used for a
     * {@code Pattern} afterwards, it can only be matched literally.
     *
     * @param s
     *            the string to quote.
     *
     * @return the quoted string.
     */
    public static String quote(String s) {
        StringBuilder sb = new StringBuilder().append("\\Q"); //$NON-NLS-1$
        int apos = 0;
        int k;
        while ((k = s.indexOf("\\E", apos)) >= 0) { //$NON-NLS-1$
            sb.append(s.substring(apos, k + 2)).append("\\\\E\\Q"); //$NON-NLS-1$
            apos = k + 2;
        }

        return sb.append(s.substring(apos)).append("\\E").toString(); //$NON-NLS-1$
    }

    /**
     * return number of groups found at compile time
     */
    int groupCount() {
        return globalGroupIndex;
    }

    int compCount() {
        return this.compCount + 1;
    }

    int consCount() {
        return this.consCount + 1;
    }

    /**
     * Returns supplementary character. At this time only for ASCII chars.
     */
    static char getSupplement(char ch) {
        char res = ch;
        if (ch >= 'a' && ch <= 'z') {
            res -= 32;
        } else if (ch >= 'A' && ch <= 'Z') {
            res += 32;
        }

        return res;
    }

    /**
     * @return true if pattern has specified flag
     */
    private boolean hasFlag(int flag) {
        return (flags & flag) == flag;
    }

    /**
     * Dismiss public constructor.
     *
     */
    private TPattern() {
    }
}
