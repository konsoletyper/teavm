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
 * <pre>
 * Pattern p = Pattern.compile("Hello, A[a-z]*!");
 *
 * Matcher m = p.matcher("Hello, Android!");
 * boolean b1 = m.matches(); // true
 *
 * m.setInput("Hello, Robot!");
 * boolean b2 = m.matches(); // false
 * </pre>
 * <p/>
 * The above code could also be written in a more compact fashion, though this
 * variant is less efficient, since {@code Pattern} and {@code Matcher} objects
 * are created on the fly instead of being reused.
 * fashion:
 * <pre>
 *     boolean b1 = Pattern.matches("Hello, A[a-z]*!", "Hello, Android!"); // true
 *     boolean b2 = Pattern.matches("Hello, A[a-z]*!", "Hello, Robot!");   // false
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
    public static final int UNIX_LINES = 1 << 0;

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
    static final int flagsBitMask = TPattern.UNIX_LINES |
                                    TPattern.CASE_INSENSITIVE |
                                    TPattern.COMMENTS |
                                    TPattern.MULTILINE |
                                    TPattern.LITERAL |
                                    TPattern.DOTALL |
                                    TPattern.UNICODE_CASE |
                                    TPattern.CANON_EQ;

    /**
     * Current <code>pattern</code> to be compiled;
     */
    private transient Lexer lexemes = null;

    /**
     * Pattern compile flags;
     */
    private int flags = 0;

    private String pattern = null;

    /*
     * All backreferences that may be used in pattern.
     */
    transient private FSet backRefs [] = new FSet [BACK_REF_NUMBER];

    /*
     * Is true if backreferenced sets replacement is needed
     */
    transient private boolean needsBackRefReplacement = false;

    transient private int globalGroupIndex = -1;

    transient private int compCount = -1;

    transient private int consCount = -1;

    transient AbstractSet start = null;

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
     * Splits the given input sequence around occurrences of the {@code Pattern}.
     * The function first determines all occurrences of the {@code Pattern}
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
     *            <li>For n &lt; 0, the length of the resulting array is
     *            exactly the number of occurrences of the {@code Pattern} +1.
     *            All entries are included.
     *            <li>For n == 0, the length of the resulting array is at most
     *            the number of occurrences of the {@code Pattern} +1. Empty
     *            strings at the end of the array are not included.
     *            </ul>
     *
     * @return the resulting array.
     */
    public String[] split(CharSequence inputSeq, int limit) {
        ArrayList res = new ArrayList();
        TMatcher mat = matcher(inputSeq);
        int index = 0;
        int curPos = 0;

        if (inputSeq.length() == 0) {
            return new String [] {""}; //$NON-NLS-1$
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
        return (String[]) res.toArray(new String[index >= 0 ? index : 0]);
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
    public static TPattern compile(String pattern, int flags)
            throws TPatternSyntaxException {

    	if ((flags != 0) &&
    	   	((flags | flagsBitMask) != flagsBitMask)) {

    	    throw new IllegalArgumentException("");
    	}

        AbstractSet.counter = 1;

        return new TPattern().compileImpl(pattern, flags);
    }

    /**
     *
     * @param pattern -
     *            Regular expression to be compiled
     * @param flags -
     *            The bit mask including CASE_INSENSITIVE, MULTILINE, DOTALL,
     *            UNICODE_CASE, and CANON_EQ
     *
     * @return Compiled pattern
     */
    private TPattern compileImpl(String pattern, int flags)
            throws TPatternSyntaxException {
        this.lexemes = new Lexer(pattern, flags);
        this.flags = flags;
        this.pattern = pattern;

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
    private AbstractSet processAlternations(AbstractSet last) {
        CharClass auxRange = new CharClass(hasFlag(TPattern.CASE_INSENSITIVE),
                hasFlag(TPattern.UNICODE_CASE));
        while (!lexemes.isEmpty()
                && lexemes.isLetter()
                && (lexemes.lookAhead() == 0
                        || lexemes.lookAhead() == Lexer.CHAR_VERTICAL_BAR || lexemes
                        .lookAhead() == Lexer.CHAR_RIGHT_PARENTHESIS)) {
            auxRange.add(lexemes.next());
            if (lexemes.peek() == Lexer.CHAR_VERTICAL_BAR)
                lexemes.next();
        }
        AbstractSet rangeSet = processRangeSet(auxRange);
        rangeSet.setNext(last);

        return rangeSet;
    }

    /**
     * E->AE; E->S|E; E->S; A->(a|)+ E->S(|S)*
     */
    private AbstractSet processExpression(int ch, int newFlags,
            AbstractSet last) {
        ArrayList children = new ArrayList();
        AbstractSet child;
        int saveFlags = flags;
        FSet fSet;
        boolean saveChangedFlags = false;

        if (newFlags != flags) {
            flags = newFlags;
        }

        switch (ch) {
        case Lexer.CHAR_NONCAP_GROUP:
            fSet = new NonCapFSet(++consCount);
            break;

        case Lexer.CHAR_POS_LOOKAHEAD:
        	/* falls through */

        case Lexer.CHAR_NEG_LOOKAHEAD:
            fSet = new AheadFSet();
            break;

        case Lexer.CHAR_POS_LOOKBEHIND:
        	/* falls through */

        case Lexer.CHAR_NEG_LOOKBEHIND:
            fSet = new BehindFSet(++consCount);
            break;

        case Lexer.CHAR_ATOMIC_GROUP:
            fSet = new AtomicFSet(++consCount);
            break;

        default:
            globalGroupIndex++;
            if (last == null) {

            	// expr = new StartSet();
            	fSet = new FinalSet();
            	saveChangedFlags = true;
            } else {

            	// expr = new JointSet(globalGroupIndex);
            	fSet = new FSet(globalGroupIndex);
            }
            if (globalGroupIndex > -1 && globalGroupIndex < 10) {
            	backRefs[globalGroupIndex] = fSet;
            }
            break;
        }

        do {
            if (lexemes.isLetter()
                    && lexemes.lookAhead() == Lexer.CHAR_VERTICAL_BAR) {
                child = processAlternations(fSet);
            } else if (lexemes.peek() == Lexer.CHAR_VERTICAL_BAR){
                child = new EmptySet(fSet);
                lexemes.next();
            } else {
                child = processSubExpression(fSet);
                if (lexemes.peek() == Lexer.CHAR_VERTICAL_BAR) {
                    lexemes.next();
                }
            }
            if (child != null) {

                //expr.addChild(child);
            	children.add(child);
            }
        } while (!(lexemes.isEmpty()
        		   || (lexemes.peek() == Lexer.CHAR_RIGHT_PARENTHESIS)));

        if (lexemes.back() == Lexer.CHAR_VERTICAL_BAR) {
        	children.add(new EmptySet(fSet));
        }

        if (flags != saveFlags && !saveChangedFlags) {
            flags = saveFlags;
            lexemes.restoreFlags(flags);
        }

        switch (ch) {
        case Lexer.CHAR_NONCAP_GROUP:
            return new NonCapJointSet(children, fSet);

        case Lexer.CHAR_POS_LOOKAHEAD:
            return new PositiveLookAhead(children, fSet);

        case Lexer.CHAR_NEG_LOOKAHEAD:
            return new NegativeLookAhead(children, fSet);

        case Lexer.CHAR_POS_LOOKBEHIND:
            return new PositiveLookBehind(children, fSet);

        case Lexer.CHAR_NEG_LOOKBEHIND:
            return new NegativeLookBehind(children, fSet);

        case Lexer.CHAR_ATOMIC_GROUP:
            return new AtomicJointSet(children, fSet);

        default:
            switch (children.size()) {
            case 0:
                return new EmptySet(fSet);

            case 1:
                return new SingleSet((AbstractSet) children.get(0), fSet);

            default:
                return new JointSet(children, fSet);
            }
        }
    }


    /**
     * T->a+
     */
    private AbstractSet processSequence(AbstractSet last) {
        StringBuffer substring = new StringBuffer();

        while (!lexemes.isEmpty()
                && lexemes.isLetter()
                && !lexemes.isHighSurrogate()
                && !lexemes.isLowSurrogate()
                && ((!lexemes.isNextSpecial() && lexemes.lookAhead() == 0) // end
                        // of
                        // pattern
                        || (!lexemes.isNextSpecial() && Lexer.isLetter(lexemes
                                .lookAhead()))
                        || lexemes.lookAhead() == Lexer.CHAR_RIGHT_PARENTHESIS
                        || (lexemes.lookAhead() & 0x8000ffff) == Lexer.CHAR_LEFT_PARENTHESIS
                        || lexemes.lookAhead() == Lexer.CHAR_VERTICAL_BAR || lexemes
                        .lookAhead() == Lexer.CHAR_DOLLAR)) {
            int ch = lexemes.next();

            if (Character.isSupplementaryCodePoint(ch)) {
                substring.append(Character.toChars(ch));
            } else {
                substring.append((char) ch);
            }
        }
        if (!hasFlag(TPattern.CASE_INSENSITIVE)) {
            return new SequenceSet(substring);
        } else if (!hasFlag(TPattern.UNICODE_CASE)) {
            return new CISequenceSet(substring);
        } else {
            return new UCISequenceSet(substring);
        }
    }

    /**
     * D->a
     */
    private AbstractSet processDecomposedChar(AbstractSet last) {
        int [] codePoints = new int [Lexer.MAX_DECOMPOSITION_LENGTH];
        char [] codePointsHangul;
        int readCodePoints = 0;
        int curSymb = -1;
        int curSymbIndex = -1;

        if (!lexemes.isEmpty() && lexemes.isLetter()) {
            curSymb = lexemes.next();
            codePoints [readCodePoints] = curSymb;
            curSymbIndex = curSymb - Lexer.LBase;
        }

        /*
         * We process decomposed Hangul syllable LV or LVT or process jamo L.
         * See http://www.unicode.org/versions/Unicode4.0.0/ch03.pdf
         * "3.12 Conjoining Jamo Behavior"
         */
        if ((curSymbIndex >= 0) && (curSymbIndex < Lexer.LCount)) {
            codePointsHangul = new char [Lexer
                                         .MAX_HANGUL_DECOMPOSITION_LENGTH];
            codePointsHangul[readCodePoints++] = (char) curSymb;

            curSymb = lexemes.peek();
            curSymbIndex = curSymb - Lexer.VBase;
            if ((curSymbIndex >= 0) && (curSymbIndex < Lexer.VCount)) {
                codePointsHangul [readCodePoints++] = (char) curSymb;
                lexemes.next();
                curSymb = lexemes.peek();
                curSymbIndex = curSymb - Lexer.TBase;
                if ((curSymbIndex >= 0) && (curSymbIndex < Lexer.TCount)) {
                    codePointsHangul [readCodePoints++] = (char) curSymb;
                    lexemes.next();

                    //LVT syllable
                    return new HangulDecomposedCharSet(codePointsHangul, 3);
                } else {

                    //LV syllable
                    return new HangulDecomposedCharSet(codePointsHangul, 2);
                }
            } else {

                   //L jamo
                   if (!hasFlag(TPattern.CASE_INSENSITIVE)) {
                       return new CharSet(codePointsHangul[0]);
                   } else if (!hasFlag(TPattern.UNICODE_CASE)) {
                       return new CICharSet(codePointsHangul[0]);
                   } else {
                       return new UCICharSet(codePointsHangul[0]);
                   }
            }

        /*
         * We process single codepoint or decomposed codepoint.
         * We collect decomposed codepoint and obtain
         * one DecomposedCharSet.
         */
        } else {
            readCodePoints++;

            while((readCodePoints < Lexer.MAX_DECOMPOSITION_LENGTH)
                    && !lexemes.isEmpty() && lexemes.isLetter()
                    && !Lexer.isDecomposedCharBoundary(lexemes.peek())) {
                  codePoints [readCodePoints++] = lexemes.next();
            }

            /*
             * We have read an ordinary symbol.
             */
            if (readCodePoints == 1
                && !Lexer.hasSingleCodepointDecomposition(codePoints[0])) {
                return processCharSet(codePoints[0]);
            } else {
                if (!hasFlag(TPattern.CASE_INSENSITIVE)) {
                    return new DecomposedCharSet(codePoints, readCodePoints);
                } else if (!hasFlag(TPattern.UNICODE_CASE)) {
                    return new CIDecomposedCharSet(codePoints, readCodePoints);
                } else {
                    return new UCIDecomposedCharSet(codePoints, readCodePoints);
                }
            }
        }
    }

    /**
     * S->BS; S->QS; S->Q; B->a+
     */
    private AbstractSet processSubExpression(AbstractSet last) {
        AbstractSet cur;
        if (lexemes.isLetter() && !lexemes.isNextSpecial()
                && Lexer.isLetter(lexemes.lookAhead())) {
            if (hasFlag(TPattern.CANON_EQ)) {
                cur = processDecomposedChar(last);
                if (!lexemes.isEmpty()

                        /* && !pattern.isQuantifier() */
                        && (lexemes.peek() != Lexer.CHAR_RIGHT_PARENTHESIS
                                || last instanceof FinalSet)
                        && lexemes.peek() != Lexer.CHAR_VERTICAL_BAR
                        && !lexemes.isLetter()) {
                    cur = processQuantifier(last, cur);
                }
            } else if (lexemes.isHighSurrogate() || lexemes.isLowSurrogate()) {
                AbstractSet term = processTerminal(last);
                cur = processQuantifier(last, term);
            } else {
                cur = processSequence(last);
            }
        } else if (lexemes.peek() == Lexer.CHAR_RIGHT_PARENTHESIS) {
        	if (last instanceof FinalSet) {
        	    throw new TPatternSyntaxException("", lexemes.toString(), lexemes.getIndex());
        	} else {
        	      cur = new EmptySet(last);
        	}
        } else {
            AbstractSet term = processTerminal(last);
            cur = processQuantifier(last, term);
        }

        if (!lexemes.isEmpty()
        // && !pattern.isQuantifier()
                && (lexemes.peek() != Lexer.CHAR_RIGHT_PARENTHESIS
                		|| last instanceof FinalSet)
                && lexemes.peek() != Lexer.CHAR_VERTICAL_BAR) {
            AbstractSet next = processSubExpression(last);
            if (cur instanceof LeafQuantifierSet
            // TODO create personal UnifiedQuantifierSet for composite
                    // quantifiers
                    // to take into account Quantifier counters
                    // ////
                    && !(cur instanceof CompositeQuantifierSet)
                    && !(cur instanceof GroupQuantifierSet)
                    && !(cur instanceof AltQuantifierSet)
                    && !next.first(((LeafQuantifierSet) cur).getInnerSet())) {
                cur = new UnifiedQuantifierSet((LeafQuantifierSet) cur);
            }
            if (((char) next.getType()) == '+') {
                cur.setNext(((LeafQuantifierSet) next).getInnerSet());
            } else {
                cur.setNext(next);
            }
        } else if (cur != null) {
            cur.setNext(last);
        } else {
            return null;
        }

        if (((char) cur.getType()) == '+') {
            return ((QuantifierSet) cur).getInnerSet();
        } else {
            return cur;
        }
    }

    /**
     * Q->T(*|+|?...) also do some optimizations.
     *
     */
    private AbstractSet processQuantifier(AbstractSet last, AbstractSet term) {
        int quant = lexemes.peek();

        if (term != null && !(term instanceof LeafSet)) {
            switch (quant) {
            case Lexer.QUANT_STAR:
            case Lexer.QUANT_PLUS: {
                QuantifierSet q;

                lexemes.next();
                if (term.getType() == AbstractSet.TYPE_DOTSET) {
                    if (!hasFlag(TPattern.DOTALL)) {
                        q = new DotQuantifierSet(term, last, quant,
                                AbstractLineTerminator.getInstance(flags));
                    } else {
                        q = new DotAllQuantifierSet(term, last, quant);
                    }
                } else {
                    q = new GroupQuantifierSet(term, last, quant);
                }
                term.setNext(q);
                return q;
            }

            case Lexer.QUANT_STAR_R:
            case Lexer.QUANT_PLUS_R: {
                lexemes.next();
                GroupQuantifierSet q = new ReluctantGroupQuantifierSet(term,
                        last, quant);
                term.setNext(q);
                return q;
            }

            case Lexer.QUANT_PLUS_P: {
                lexemes.next();
                // possessive plus will be handled by unique class
                // and should not be postprocessed to point previous set
                // to the inner one.
                // //
                return new PosPlusGroupQuantifierSet(term, last,
                        Lexer.QUANT_STAR_P);
            }

            case Lexer.QUANT_STAR_P: {
                lexemes.next();
                return new PossessiveGroupQuantifierSet(term, last, quant);
            }

            case Lexer.QUANT_ALT: {
                lexemes.next();
                AltGroupQuantifierSet q = new AltGroupQuantifierSet(term, last,
                        Lexer.QUANT_ALT);
                term.setNext(last);
                return q;
            }

            case Lexer.QUANT_ALT_P: {
                lexemes.next();
                return new PosAltGroupQuantifierSet(term, last, Lexer.QUANT_ALT);
            }

            case Lexer.QUANT_ALT_R: {
                lexemes.next();
                RelAltGroupQuantifierSet q = new RelAltGroupQuantifierSet(term,
                        last, Lexer.QUANT_ALT);
                term.setNext(last);
                return q;
            }

            case Lexer.QUANT_COMP: {
                CompositeGroupQuantifierSet q = new CompositeGroupQuantifierSet(
                        (Quantifier) lexemes.nextSpecial(), term, last,
                        Lexer.QUANT_ALT, ++compCount);
                term.setNext(q);
                return q;
            }

            case Lexer.QUANT_COMP_P: {
                return new PosCompositeGroupQuantifierSet((Quantifier) lexemes
                        .nextSpecial(), term, last, Lexer.QUANT_ALT,
                        ++compCount);
            }

            case Lexer.QUANT_COMP_R: {
                RelCompositeGroupQuantifierSet q = new RelCompositeGroupQuantifierSet(
                        (Quantifier) lexemes.nextSpecial(), term, last,
                        Lexer.QUANT_ALT, ++compCount);
                term.setNext(q);
                return q;
            }

            default:
                return term;
            }
        } else {
            LeafSet leaf = null;
            if (term != null)
                leaf = (LeafSet) term;
            switch (quant) {
            case Lexer.QUANT_STAR:
            case Lexer.QUANT_PLUS: {
                lexemes.next();
                LeafQuantifierSet q = new LeafQuantifierSet(leaf,
                        last, quant);
                leaf.setNext(q);
                return q;
            }

            case Lexer.QUANT_STAR_R:
            case Lexer.QUANT_PLUS_R: {
                lexemes.next();
                ReluctantQuantifierSet q = new ReluctantQuantifierSet(leaf,
                        last, quant);
                leaf.setNext(q);
                return q;
            }

            case Lexer.QUANT_PLUS_P:
            case Lexer.QUANT_STAR_P: {
                lexemes.next();
                PossessiveQuantifierSet q = new PossessiveQuantifierSet(leaf,
                        last, quant);
                leaf.setNext(q);
                return q;
            }

            case Lexer.QUANT_ALT: {
                lexemes.next();
                return new AltQuantifierSet(leaf, last, Lexer.QUANT_ALT);
            }

            case Lexer.QUANT_ALT_P: {
                lexemes.next();
                return new PossessiveAltQuantifierSet(leaf, last,
                        Lexer.QUANT_ALT_P);
            }

            case Lexer.QUANT_ALT_R: {
                lexemes.next();
                return new ReluctantAltQuantifierSet(leaf, last,
                        Lexer.QUANT_ALT_R);
            }

            case Lexer.QUANT_COMP: {
                return new CompositeQuantifierSet((Quantifier) lexemes
                        .nextSpecial(), leaf, last, Lexer.QUANT_COMP);
            }

            case Lexer.QUANT_COMP_P: {
                return new PossessiveCompositeQuantifierSet(
                        (Quantifier) lexemes.nextSpecial(), leaf, last,
                        Lexer.QUANT_COMP_P);
            }
            case Lexer.QUANT_COMP_R: {
                return new ReluctantCompositeQuantifierSet((Quantifier) lexemes
                        .nextSpecial(), leaf, last, Lexer.QUANT_COMP_R);
            }

            default:
                return term;
            }
        }
    }

    /**
     * T-> letter|[range]|{char-class}|(E)
     */
    private AbstractSet processTerminal(AbstractSet last) {
        int ch;
        AbstractSet term = null;
        do {
            ch = lexemes.peek();
            if ((ch & 0x8000ffff) == Lexer.CHAR_LEFT_PARENTHESIS) {
            	 int newFlags;
             	 lexemes.next();
                 newFlags = (ch & 0x00ff0000) >> 16;
                 ch = ch & 0xff00ffff;
                 if (ch == Lexer.CHAR_FLAGS) {
                     flags = newFlags;
                 } else {
                     newFlags = (ch == Lexer.CHAR_NONCAP_GROUP)
                                 ? newFlags
                                 : flags;
                     term = processExpression(ch, newFlags, last);
                     if (lexemes.peek() != Lexer.CHAR_RIGHT_PARENTHESIS) {
                         throw new TPatternSyntaxException("", lexemes.toString(), lexemes.getIndex());
                     }
                     lexemes.next();
                 }
            } else
                switch (ch) {
                case Lexer.CHAR_LEFT_SQUARE_BRACKET: {
                    lexemes.next();
                    boolean negative = false;
                    if (lexemes.peek() == Lexer.CHAR_CARET) {
                        negative = true;
                        lexemes.next();
                    }

                    term = processRange(negative, last);
                    if (lexemes.peek() != Lexer.CHAR_RIGHT_SQUARE_BRACKET)
                        throw new TPatternSyntaxException("", lexemes.toString(), lexemes.getIndex());
                    lexemes.setMode(Lexer.MODE_PATTERN);
                    lexemes.next();
                    break;
                }

                case Lexer.CHAR_DOT: {
                    lexemes.next();

                    if (!hasFlag(TPattern.DOTALL)) {
                        term = new DotSet(AbstractLineTerminator
                                .getInstance(flags));
                    } else {
                        term = new DotAllSet();
                    }

                    break;
                }

                case Lexer.CHAR_CARET: {
                    lexemes.next();
                    consCount++;
                    if (!hasFlag(TPattern.MULTILINE)) {
                        term = new SOLSet();
                    } else {
                        term = new MultiLineSOLSet(AbstractLineTerminator
                                .getInstance(flags));
                    }

                    break;
                }

                case Lexer.CHAR_DOLLAR: {
                    lexemes.next();
                    consCount++;
                    if (!hasFlag(TPattern.MULTILINE)) {
                        if (!hasFlag(TPattern.UNIX_LINES)) {
                            term = new EOLSet(consCount);
                        } else {
                            term = new UEOLSet(consCount);
                        }
                    } else {
                        if (!hasFlag(TPattern.UNIX_LINES)) {
                            term = new MultiLineEOLSet(consCount);
                        } else {
                            term = new UMultiLineEOLSet(consCount);
                        }
                    }

                    break;
                }

                case Lexer.CHAR_WORD_BOUND: {
                    lexemes.next();
                    term = new WordBoundary(true);
                    break;
                }

                case Lexer.CHAR_NONWORD_BOUND: {
                    lexemes.next();
                    term = new WordBoundary(false);
                    break;
                }

                case Lexer.CHAR_END_OF_INPUT: {
                    lexemes.next();
                    term = new EOISet();
                    break;
                }

                case Lexer.CHAR_END_OF_LINE: {
                    lexemes.next();
                    term = new EOLSet(++consCount);
                    break;
                }

                case Lexer.CHAR_START_OF_INPUT: {
                    lexemes.next();
                    term = new SOLSet();
                    break;
                }

                case Lexer.CHAR_PREVIOUS_MATCH: {
                    lexemes.next();
                    term = new PreviousMatch();
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
                            term = new BackReferenceSet(number, consCount);
                        } else if (!hasFlag(TPattern.UNICODE_CASE)) {
                            term = new CIBackReferenceSet(number, consCount);
                        } else {
                            term = new UCIBackReferenceSet(number, consCount);
                        }
                        (backRefs [number]).isBackReferenced = true;
                        needsBackRefReplacement = true;
                        break;
                    } else {
                        throw new TPatternSyntaxException("", lexemes.toString(), lexemes.getIndex());
                    }
                }

                case 0: {
                    AbstractCharClass cc = null;
                    if ((cc = (AbstractCharClass) lexemes.peekSpecial()) != null) {
                        term = processRangeSet(cc);
                    } else if (!lexemes.isEmpty()) {

                        //ch == 0
                        term = new CharSet((char) ch);
                    } else {
                    	term = new EmptySet(last);
                        break;
                    }
                    lexemes.next();
                    break;
                }

                default: {
                    if (ch >= 0 && !lexemes.isSpecial()) {
                        term = processCharSet(ch);
                        lexemes.next();
                    } else if (ch == Lexer.CHAR_VERTICAL_BAR) {
                    	term = new EmptySet(last);
                    } else if (ch == Lexer.CHAR_RIGHT_PARENTHESIS) {
                        if (last instanceof FinalSet) {
                        	throw new TPatternSyntaxException("", lexemes.toString(), lexemes.getIndex());
                        } else {
                    	    term = new EmptySet(last);
                        }
                    } else {
                        throw new TPatternSyntaxException(
                                 (lexemes.isSpecial() ? lexemes.peekSpecial()
                                        .toString() : Character
                                        .toString((char) ch)), lexemes
                                .toString(), lexemes.getIndex());
                    }
                }
                }
        } while (ch == Lexer.CHAR_FLAGS);
        return term;
    }

    private AbstractSet processRange(boolean negative, AbstractSet last) {
        AbstractCharClass res = processRangeExpression(negative);
        AbstractSet rangeSet = processRangeSet(res);
        rangeSet.setNext(last);

        return rangeSet;
    }

    /**
     * process [...] ranges
     */
    private CharClass processRangeExpression(boolean alt) {
        CharClass res = new CharClass(alt, hasFlag(TPattern.CASE_INSENSITIVE),
                hasFlag(TPattern.UNICODE_CASE));
        int buffer = -1;
        boolean intersection = false;
        boolean notClosed = false;
        boolean firstInClass = true;

        while (!lexemes.isEmpty()
                && (notClosed = (lexemes.peek()) != Lexer.CHAR_RIGHT_SQUARE_BRACKET
                        || firstInClass)) {
            switch (lexemes.peek()) {

            case Lexer.CHAR_RIGHT_SQUARE_BRACKET: {
                if (buffer >= 0)
                    res.add(buffer);
                buffer = ']';
                lexemes.next();
                break;
            }
            case Lexer.CHAR_LEFT_SQUARE_BRACKET: {
                if (buffer >= 0) {
                    res.add(buffer);
                    buffer = -1;
                }
                lexemes.next();
                boolean negative = false;
                if (lexemes.peek() == Lexer.CHAR_CARET) {
                    lexemes.next();
                    negative = true;
                }

                if (intersection)
                    res.intersection(processRangeExpression(negative));
                else
                    res.union(processRangeExpression(negative));
                intersection = false;
                lexemes.next();
                break;
            }

            case Lexer.CHAR_AMPERSAND: {
                if (buffer >= 0)
                    res.add(buffer);
                buffer = lexemes.next();

                /*
                 * if there is a start for subrange we will do an intersection
                 * otherwise treat '&' as a normal character
                 */
                if (lexemes.peek() == Lexer.CHAR_AMPERSAND) {
                    if (lexemes.lookAhead()
                            == Lexer.CHAR_LEFT_SQUARE_BRACKET) {
                        lexemes.next();
                        intersection = true;
                        buffer = -1;
                    } else {
                        lexemes.next();
                        if (firstInClass) {

                            //skip "&&" at "[&&...]" or "[^&&...]"
                            res = processRangeExpression(false);
                        } else {

                            //ignore "&&" at "[X&&]" ending where X != empty string
                            if (!(lexemes.peek()
                                    == Lexer.CHAR_RIGHT_SQUARE_BRACKET)) {
                                res.intersection(processRangeExpression(false));
                            }
                        }

                    }
                } else {

                    //treat '&' as a normal character
                    buffer = '&';
                }

                break;
            }

            case Lexer.CHAR_HYPHEN: {
                if (firstInClass
                        || lexemes.lookAhead() == Lexer.CHAR_RIGHT_SQUARE_BRACKET
                        || lexemes.lookAhead() == Lexer.CHAR_LEFT_SQUARE_BRACKET
                        || buffer < 0) {
                    // treat hypen as normal character
                    if (buffer >= 0)
                        res.add(buffer);
                    buffer = '-';
                    lexemes.next();
                    // range
                } else {
                    lexemes.next();
                    int cur = lexemes.peek();

                    if (!lexemes.isSpecial()
                            && (cur >= 0
                                    || lexemes.lookAhead() == Lexer.CHAR_RIGHT_SQUARE_BRACKET
                                    || lexemes.lookAhead() == Lexer.CHAR_LEFT_SQUARE_BRACKET || buffer < 0)) {

                        try {
                            if (!Lexer.isLetter(cur)) {
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

            case Lexer.CHAR_CARET: {
                if (buffer >= 0)
                    res.add(buffer);
                buffer = '^';
                lexemes.next();
                break;
            }

            case 0: {
                if (buffer >= 0)
                    res.add(buffer);
                AbstractCharClass cs = (AbstractCharClass) lexemes
                        .peekSpecial();
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
                if (buffer >= 0)
                    res.add(buffer);
                buffer = lexemes.next();
                break;
            }
            }

            firstInClass = false;
        }
        if (notClosed) {
            throw new TPatternSyntaxException("", pattern(), lexemes.getIndex() - 1);
        }
        if (buffer >= 0)
            res.add(buffer);
        return res;
    }

    private AbstractSet processCharSet(int ch) {
        boolean isSupplCodePoint = Character
                .isSupplementaryCodePoint(ch);

        if (hasFlag(TPattern.CASE_INSENSITIVE)) {

            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')) {
                return new CICharSet((char) ch);
            } else if (hasFlag(TPattern.UNICODE_CASE)
                    && ch > 128) {
                if (isSupplCodePoint) {
                    return new UCISupplCharSet(ch);
                } else if (Lexer.isLowSurrogate(ch)) {

                    //we need no UCILowSurrogateCharSet
                    return new LowSurrogateCharSet((char) ch);
                } else if (Lexer.isHighSurrogate(ch)) {

                    //we need no UCIHighSurrogateCharSet
                    return new HighSurrogateCharSet((char) ch);
                } else {
                    return new UCICharSet((char) ch);
                }
            }
        }

        if (isSupplCodePoint) {
            return new SupplCharSet(ch);
        } else if (Lexer.isLowSurrogate(ch)) {
            return new LowSurrogateCharSet((char) ch);
        } else if (Lexer.isHighSurrogate(ch)) {
            return new HighSurrogateCharSet((char) ch);
        } else {
            return new CharSet((char) ch);
        }
    }

    private AbstractSet processRangeSet(AbstractCharClass charClass) {
        if (charClass.hasLowHighSurrogates()) {
            AbstractCharClass surrogates = charClass.getSurrogates();
            LowHighSurrogateRangeSet lowHighSurrRangeSet
                    = new LowHighSurrogateRangeSet(surrogates);

            if (charClass.mayContainSupplCodepoints()) {
                if (!charClass.hasUCI()) {
                    return new CompositeRangeSet(
                            new SupplRangeSet(charClass.getWithoutSurrogates()),
                            lowHighSurrRangeSet);
                } else {
                    return new CompositeRangeSet(
                            new UCISupplRangeSet(charClass.getWithoutSurrogates()),
                            lowHighSurrRangeSet);
                }
            }

            if (!charClass.hasUCI()) {
                return new CompositeRangeSet(
                        new RangeSet(charClass.getWithoutSurrogates()),
                        lowHighSurrRangeSet);
            } else {
                return new CompositeRangeSet(
                        new UCIRangeSet(charClass.getWithoutSurrogates()),
                        lowHighSurrRangeSet);
            }
        }

        if (charClass.mayContainSupplCodepoints()) {
            if (!charClass.hasUCI()) {
                return new SupplRangeSet(charClass);
            } else {
                return new UCISupplRangeSet(charClass);
            }
        }

        if (!charClass.hasUCI()) {
            return new RangeSet(charClass);
        } else {
            return new UCIRangeSet(charClass);
        }
    }

    /**
     * Compiles a regular expression, creating a new Pattern instance in the
     * process. This is actually a convenience method that calls {@link
     * #compile(String, int)} with a {@code flags} value of zero.
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
     * This method do traverses of
     * automata to finish compilation.
     */
    private void finalizeCompile() {

    	/*
    	 * Processing second pass
    	 */
    	if (needsBackRefReplacement) { //|| needsReason1 || needsReason2) {
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

    /**
     * Serialization support
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        AbstractSet.counter = 1;
        globalGroupIndex = -1;
        compCount = -1;
        consCount = -1;
        backRefs = new FSet [BACK_REF_NUMBER];

        compileImpl(pattern, flags);

    }
}
