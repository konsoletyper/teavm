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

import java.util.BitSet;
import java.util.MissingResourceException;

/**
 * This class represents character classes, i.e. sets of character either
 * predefined or user defined.
 *
 * Note, this class represent token, not node, so being constructed by lexer.
 *
 * @author Nikolay A. Kuznetsov
 */
abstract class TAbstractCharClass extends TSpecialToken {
    protected boolean alt;

    protected boolean altSurrogates;

    // Character.MAX_SURROGATE - Character.MIN_SURROGATE + 1
    static final int SURROGATE_CARDINALITY = 2048;

    BitSet lowHighSurrogates = new BitSet(SURROGATE_CARDINALITY);

    TAbstractCharClass charClassWithoutSurrogates;

    TAbstractCharClass charClassWithSurrogates;

    static PredefinedCharacterClasses charClasses = new PredefinedCharacterClasses();

    /*
     * Indicates if this class may contain supplementary Unicode codepoints. If
     * this flag is specified it doesn't mean that this class contains
     * supplementary characters but may contain.
     */
    protected boolean mayContainSupplCodepoints;

    abstract public boolean contains(int ch);

    /**
     * Returns BitSet representing this character class or <code>null</code> if
     * this character class does not have character representation;
     *
     * @return bitset
     */
    protected BitSet getBits() {
        return null;
    }

    protected BitSet getLowHighSurrogates() {
        return lowHighSurrogates;
    }

    public boolean hasLowHighSurrogates() {
        return altSurrogates ? lowHighSurrogates.nextClearBit(0) < SURROGATE_CARDINALITY : lowHighSurrogates
                .nextSetBit(0) < SURROGATE_CARDINALITY;
    }

    public boolean mayContainSupplCodepoints() {
        return mayContainSupplCodepoints;
    }

    @Override
    public int getType() {
        return TSpecialToken.TOK_CHARCLASS;
    }

    public TAbstractCharClass getInstance() {
        return this;
    }

    public TAbstractCharClass getSurrogates() {

        if (charClassWithSurrogates == null) {
            final BitSet lHS = getLowHighSurrogates();

            charClassWithSurrogates = new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    int index = ch - Character.MIN_SURROGATE;

                    return ((index >= 0) && (index < TAbstractCharClass.SURROGATE_CARDINALITY)) ? this.altSurrogates
                            ^ lHS.get(index) : false;
                }
            };
            charClassWithSurrogates.setNegative(this.altSurrogates);
        }

        return charClassWithSurrogates;
    }

    public TAbstractCharClass getWithoutSurrogates() {
        if (charClassWithoutSurrogates == null) {
            final BitSet lHS = getLowHighSurrogates();
            final TAbstractCharClass thisClass = this;

            charClassWithoutSurrogates = new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    int index = ch - Character.MIN_SURROGATE;

                    boolean containslHS = (index >= 0 && index < TAbstractCharClass.SURROGATE_CARDINALITY)
                            ? this.altSurrogates ^ lHS.get(index)
                            : false;

                    return thisClass.contains(ch) && !containslHS;
                }
            };
            charClassWithoutSurrogates.setNegative(isNegative());
            charClassWithoutSurrogates.mayContainSupplCodepoints = mayContainSupplCodepoints;
        }

        return charClassWithoutSurrogates;
    }

    public boolean hasUCI() {
        return false;
    }

    public TAbstractCharClass setNegative(boolean value) {
        if (alt ^ value) {
            alt = !alt;
            altSurrogates = !altSurrogates;
        }
        if (!mayContainSupplCodepoints) {
            mayContainSupplCodepoints = true;
        }
        return this;
    }

    public boolean isNegative() {
        return alt;
    }

    // -----------------------------------------------------------------
    // Static methods and predefined classes
    // -----------------------------------------------------------------

    public static boolean intersects(int ch1, int ch2) {
        return ch1 == ch2;
    }

    public static boolean intersects(TAbstractCharClass cc, int ch) {
        return cc.contains(ch);
    }

    public static boolean intersects(TAbstractCharClass cc1, TAbstractCharClass cc2) {
        if (cc1.getBits() == null || cc2.getBits() == null) {
            return true;
        }
        return cc1.getBits().intersects(cc2.getBits());
    }

    public static TAbstractCharClass getPredefinedClass(String name, boolean negative) {
        return ((LazyCharClass) charClasses.getObject(name)).getValue(negative);
    }

    abstract static class LazyCharClass {
        TAbstractCharClass posValue;

        TAbstractCharClass negValue;

        public TAbstractCharClass getValue(boolean negative) {
            if (!negative && posValue == null) {
                posValue = computeValue();
            } else if (negative && negValue == null) {
                negValue = computeValue().setNegative(true);
            }
            if (!negative) {
                return posValue;
            }
            return negValue;
        }

        protected abstract TAbstractCharClass computeValue();
    }

    static class LazyDigit extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            return new TCharClass().add('0', '9');
        }
    }

    static class LazyNonDigit extends LazyDigit {
        @Override
        protected TAbstractCharClass computeValue() {
            TAbstractCharClass chCl = super.computeValue().setNegative(true);

            chCl.mayContainSupplCodepoints = true;
            return chCl;
        }
    }

    static class LazySpace extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            /* 9-13 - \t\n\x0B\f\r; 32 - ' ' */
            return new TCharClass().add(9, 13).add(32);
        }
    }

    static class LazyNonSpace extends LazySpace {
        @Override
        protected TAbstractCharClass computeValue() {
            TAbstractCharClass chCl = super.computeValue().setNegative(true);

            chCl.mayContainSupplCodepoints = true;
            return chCl;
        }
    }

    static class LazyWord extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            return new TCharClass().add('a', 'z').add('A', 'Z').add('0', '9').add('_');
        }
    }

    static class LazyNonWord extends LazyWord {
        @Override
        protected TAbstractCharClass computeValue() {
            TAbstractCharClass chCl = super.computeValue().setNegative(true);

            chCl.mayContainSupplCodepoints = true;
            return chCl;
        }
    }

    static class LazyLower extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            return new TCharClass().add('a', 'z');
        }
    }

    static class LazyUpper extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            return new TCharClass().add('A', 'Z');
        }
    }

    static class LazyASCII extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            return new TCharClass().add(0x00, 0x7F);
        }
    }

    static class LazyAlpha extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            return new TCharClass().add('a', 'z').add('A', 'Z');
        }
    }

    static class LazyAlnum extends LazyAlpha {
        @Override
        protected TAbstractCharClass computeValue() {
            return ((TCharClass) super.computeValue()).add('0', '9');
        }
    }

    static class LazyPunct extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            /* Punctuation !"#$%&'()*+,-./:;<=>?@ [\]^_` {|}~ */
            return new TCharClass().add(0x21, 0x40).add(0x5B, 0x60).add(0x7B, 0x7E);
        }
    }

    static class LazyGraph extends LazyAlnum {
        @Override
        protected TAbstractCharClass computeValue() {
            /* plus punctuation */
            return ((TCharClass) super.computeValue()).add(0x21, 0x40).add(0x5B, 0x60).add(0x7B, 0x7E);
        }
    }

    static class LazyPrint extends LazyGraph {
        @Override
        protected TAbstractCharClass computeValue() {
            return ((TCharClass) super.computeValue()).add(0x20);
        }
    }

    static class LazyBlank extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            return new TCharClass().add(' ').add('\t');
        }
    }

    static class LazyCntrl extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            return new TCharClass().add(0x00, 0x1F).add(0x7F);
        }
    }

    static class LazyXDigit extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            return new TCharClass().add('0', '9').add('a', 'f').add('A', 'F');
        }
    }

    static class LazyRange extends LazyCharClass {
        int start;
        int end;

        public LazyRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public TAbstractCharClass computeValue() {
            TAbstractCharClass chCl = new TCharClass().add(start, end);
            return chCl;
        }
    }

    static class LazySpecialsBlock extends LazyCharClass {
        @Override
        public TAbstractCharClass computeValue() {
            return new TCharClass().add(0xFEFF, 0xFEFF).add(0xFFF0, 0xFFFD);
        }
    }

    static class LazyCategoryScope extends LazyCharClass {
        int category;

        boolean mayContainSupplCodepoints;

        boolean containsAllSurrogates;

        public LazyCategoryScope(int cat, boolean mayContainSupplCodepoints) {
            this.mayContainSupplCodepoints = mayContainSupplCodepoints;
            this.category = cat;
        }

        public LazyCategoryScope(int cat, boolean mayContainSupplCodepoints, boolean containsAllSurrogates) {
            this.containsAllSurrogates = containsAllSurrogates;
            this.mayContainSupplCodepoints = mayContainSupplCodepoints;
            this.category = cat;
        }

        @Override
        protected TAbstractCharClass computeValue() {
            TAbstractCharClass chCl = new TUnicodeCategoryScope(category);
            if (containsAllSurrogates) {
                chCl.lowHighSurrogates.set(0, SURROGATE_CARDINALITY);
            }

            chCl.mayContainSupplCodepoints = mayContainSupplCodepoints;
            return chCl;
        }
    }

    static class LazyCategory extends LazyCharClass {
        int category;

        boolean mayContainSupplCodepoints;

        boolean containsAllSurrogates;

        public LazyCategory(int cat, boolean mayContainSupplCodepoints) {
            this.mayContainSupplCodepoints = mayContainSupplCodepoints;
            this.category = cat;
        }

        public LazyCategory(int cat, boolean mayContainSupplCodepoints, boolean containsAllSurrogates) {
            this.containsAllSurrogates = containsAllSurrogates;
            this.mayContainSupplCodepoints = mayContainSupplCodepoints;
            this.category = cat;
        }

        @Override
        protected TAbstractCharClass computeValue() {
            TAbstractCharClass chCl = new TUnicodeCategory(category);
            if (containsAllSurrogates) {
                chCl.lowHighSurrogates.set(0, SURROGATE_CARDINALITY);
            }
            chCl.mayContainSupplCodepoints = mayContainSupplCodepoints;
            return chCl;
        }
    }

    static class LazyJavaLowerCase extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            TAbstractCharClass chCl = new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    return Character.isLowerCase(ch);
                }
            };

            chCl.mayContainSupplCodepoints = true;
            return chCl;
        }
    }

    static class LazyJavaUpperCase extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            TAbstractCharClass chCl = new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    return Character.isUpperCase(ch);
                }
            };

            chCl.mayContainSupplCodepoints = true;
            return chCl;
        }
    }

    static class LazyJavaWhitespace extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            return new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    return Character.isWhitespace(ch);
                }
            };
        }
    }

    static class LazyJavaMirrored extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            return new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    // TODO implement this method and uncomment
                    // return Character.isMirrored(ch);
                    return false;
                }
            };
        }
    }

    static class LazyJavaDefined extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            TAbstractCharClass chCl = new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    return Character.isDefined(ch);
                }
            };
            chCl.lowHighSurrogates.set(0, SURROGATE_CARDINALITY);

            chCl.mayContainSupplCodepoints = true;
            return chCl;
        }
    }

    static class LazyJavaDigit extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            TAbstractCharClass chCl = new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    return Character.isDigit(ch);
                }
            };

            chCl.mayContainSupplCodepoints = true;
            return chCl;
        }
    }

    static class LazyJavaIdentifierIgnorable extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            TAbstractCharClass chCl = new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    return Character.isIdentifierIgnorable(ch);
                }
            };

            chCl.mayContainSupplCodepoints = true;
            return chCl;
        }
    }

    static class LazyJavaISOControl extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            return new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    return Character.isISOControl(ch);
                }
            };
        }
    }

    static class LazyJavaJavaIdentifierPart extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            TAbstractCharClass chCl = new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    return Character.isJavaIdentifierPart(ch);
                }
            };

            chCl.mayContainSupplCodepoints = true;
            return chCl;
        }
    }

    static class LazyJavaJavaIdentifierStart extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            TAbstractCharClass chCl = new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    return Character.isJavaIdentifierStart(ch);
                }
            };

            chCl.mayContainSupplCodepoints = true;
            return chCl;
        }
    }

    static class LazyJavaLetter extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            TAbstractCharClass chCl = new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    return Character.isLetter(ch);
                }
            };

            chCl.mayContainSupplCodepoints = true;
            return chCl;
        }
    }

    static class LazyJavaLetterOrDigit extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            TAbstractCharClass chCl = new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    return Character.isLetterOrDigit(ch);
                }
            };

            chCl.mayContainSupplCodepoints = true;
            return chCl;
        }
    }

    static class LazyJavaSpaceChar extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            return new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    return Character.isSpaceChar(ch);
                }
            };
        }
    }

    static class LazyJavaTitleCase extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            return new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    return Character.isTitleCase(ch);
                }
            };
        }
    }

    static class LazyJavaUnicodeIdentifierPart extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            TAbstractCharClass chCl = new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    return Character.isUnicodeIdentifierPart(ch);
                }
            };

            chCl.mayContainSupplCodepoints = true;
            return chCl;
        }
    }

    static class LazyJavaUnicodeIdentifierStart extends LazyCharClass {
        @Override
        protected TAbstractCharClass computeValue() {
            TAbstractCharClass chCl = new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    return Character.isUnicodeIdentifierStart(ch);
                }
            };

            chCl.mayContainSupplCodepoints = true;
            return chCl;
        }
    }

    /**
     * character classes generated from http://www.unicode.org/reports/tr18/
     * http://www.unicode.org/Public/4.1.0/ucd/Blocks.txt
     */
    static final class PredefinedCharacterClasses {
        static LazyCharClass space = new LazySpace();

        static LazyCharClass digit = new LazyDigit();

        static final Object[][] contents = {
                { "Lower", new LazyLower() }, //$NON-NLS-1$
                { "Upper", new LazyUpper() }, //$NON-NLS-1$
                { "ASCII", new LazyASCII() }, //$NON-NLS-1$
                { "Alpha", new LazyAlpha() }, //$NON-NLS-1$
                { "Digit", digit }, //$NON-NLS-1$
                { "Alnum", new LazyAlnum() }, //$NON-NLS-1$
                { "Punct", new LazyPunct() }, //$NON-NLS-1$
                { "Graph", new LazyGraph() }, //$NON-NLS-1$
                { "Print", new LazyPrint() }, //$NON-NLS-1$
                { "Blank", new LazyBlank() }, //$NON-NLS-1$
                { "Cntrl", new LazyCntrl() }, //$NON-NLS-1$
                { "XDigit", new LazyXDigit() }, //$NON-NLS-1$
                { "javaLowerCase", new LazyJavaLowerCase() }, //$NON-NLS-1$
                { "javaUpperCase", new LazyJavaUpperCase() }, //$NON-NLS-1$
                { "javaWhitespace", new LazyJavaWhitespace() }, //$NON-NLS-1$
                { "javaMirrored", new LazyJavaMirrored() }, //$NON-NLS-1$
                { "javaDefined", new LazyJavaDefined() }, //$NON-NLS-1$
                { "javaDigit", new LazyJavaDigit() }, //$NON-NLS-1$
                { "javaIdentifierIgnorable", new LazyJavaIdentifierIgnorable() }, //$NON-NLS-1$
                { "javaISOControl", new LazyJavaISOControl() }, //$NON-NLS-1$
                { "javaJavaIdentifierPart", new LazyJavaJavaIdentifierPart() }, //$NON-NLS-1$
                { "javaJavaIdentifierStart", new LazyJavaJavaIdentifierStart() }, //$NON-NLS-1$
                { "javaLetter", new LazyJavaLetter() }, //$NON-NLS-1$
                { "javaLetterOrDigit", new LazyJavaLetterOrDigit() }, //$NON-NLS-1$
                { "javaSpaceChar", new LazyJavaSpaceChar() }, //$NON-NLS-1$
                { "javaTitleCase", new LazyJavaTitleCase() }, //$NON-NLS-1$
                { "javaUnicodeIdentifierPart", new LazyJavaUnicodeIdentifierPart() }, //$NON-NLS-1$
                { "javaUnicodeIdentifierStart", new LazyJavaUnicodeIdentifierStart() }, //$NON-NLS-1$
                { "Space", space }, //$NON-NLS-1$
                { "w", new LazyWord() }, //$NON-NLS-1$
                { "W", new LazyNonWord() }, //$NON-NLS-1$
                { "s", space }, //$NON-NLS-1$
                { "S", new LazyNonSpace() }, //$NON-NLS-1$
                { "d", digit }, //$NON-NLS-1$
                { "D", new LazyNonDigit() }, //$NON-NLS-1$
                { "BasicLatin", new LazyRange(0x0000, 0x007F) }, //$NON-NLS-1$
                { "Latin-1Supplement", new LazyRange(0x0080, 0x00FF) }, //$NON-NLS-1$
                { "LatinExtended-A", new LazyRange(0x0100, 0x017F) }, //$NON-NLS-1$
                { "LatinExtended-B", new LazyRange(0x0180, 0x024F) }, //$NON-NLS-1$
                { "IPAExtensions", new LazyRange(0x0250, 0x02AF) }, //$NON-NLS-1$
                { "SpacingModifierLetters", new LazyRange(0x02B0, 0x02FF) }, //$NON-NLS-1$
                { "CombiningDiacriticalMarks", new LazyRange(0x0300, 0x036F) }, //$NON-NLS-1$
                { "Greek", new LazyRange(0x0370, 0x03FF) }, //$NON-NLS-1$
                { "Cyrillic", new LazyRange(0x0400, 0x04FF) }, //$NON-NLS-1$
                { "CyrillicSupplement", new LazyRange(0x0500, 0x052F) }, //$NON-NLS-1$
                { "Armenian", new LazyRange(0x0530, 0x058F) }, //$NON-NLS-1$
                { "Hebrew", new LazyRange(0x0590, 0x05FF) }, //$NON-NLS-1$
                { "Arabic", new LazyRange(0x0600, 0x06FF) }, //$NON-NLS-1$
                { "Syriac", new LazyRange(0x0700, 0x074F) }, //$NON-NLS-1$
                { "ArabicSupplement", new LazyRange(0x0750, 0x077F) }, //$NON-NLS-1$
                { "Thaana", new LazyRange(0x0780, 0x07BF) }, //$NON-NLS-1$
                { "Devanagari", new LazyRange(0x0900, 0x097F) }, //$NON-NLS-1$
                { "Bengali", new LazyRange(0x0980, 0x09FF) }, //$NON-NLS-1$
                { "Gurmukhi", new LazyRange(0x0A00, 0x0A7F) }, //$NON-NLS-1$
                { "Gujarati", new LazyRange(0x0A80, 0x0AFF) }, //$NON-NLS-1$
                { "Oriya", new LazyRange(0x0B00, 0x0B7F) }, //$NON-NLS-1$
                { "Tamil", new LazyRange(0x0B80, 0x0BFF) }, //$NON-NLS-1$
                { "Telugu", new LazyRange(0x0C00, 0x0C7F) }, //$NON-NLS-1$
                { "Kannada", new LazyRange(0x0C80, 0x0CFF) }, //$NON-NLS-1$
                { "Malayalam", new LazyRange(0x0D00, 0x0D7F) }, //$NON-NLS-1$
                { "Sinhala", new LazyRange(0x0D80, 0x0DFF) }, //$NON-NLS-1$
                { "Thai", new LazyRange(0x0E00, 0x0E7F) }, //$NON-NLS-1$
                { "Lao", new LazyRange(0x0E80, 0x0EFF) }, //$NON-NLS-1$
                { "Tibetan", new LazyRange(0x0F00, 0x0FFF) }, //$NON-NLS-1$
                { "Myanmar", new LazyRange(0x1000, 0x109F) }, //$NON-NLS-1$
                { "Georgian", new LazyRange(0x10A0, 0x10FF) }, //$NON-NLS-1$
                { "HangulJamo", new LazyRange(0x1100, 0x11FF) }, //$NON-NLS-1$
                { "Ethiopic", new LazyRange(0x1200, 0x137F) }, //$NON-NLS-1$
                { "EthiopicSupplement", new LazyRange(0x1380, 0x139F) }, //$NON-NLS-1$
                { "Cherokee", new LazyRange(0x13A0, 0x13FF) }, //$NON-NLS-1$
                { "UnifiedCanadianAboriginalSyllabics", //$NON-NLS-1$
                        new LazyRange(0x1400, 0x167F) },
                { "Ogham", new LazyRange(0x1680, 0x169F) }, //$NON-NLS-1$
                { "Runic", new LazyRange(0x16A0, 0x16FF) }, //$NON-NLS-1$
                { "Tagalog", new LazyRange(0x1700, 0x171F) }, //$NON-NLS-1$
                { "Hanunoo", new LazyRange(0x1720, 0x173F) }, //$NON-NLS-1$
                { "Buhid", new LazyRange(0x1740, 0x175F) }, //$NON-NLS-1$
                { "Tagbanwa", new LazyRange(0x1760, 0x177F) }, //$NON-NLS-1$
                { "Khmer", new LazyRange(0x1780, 0x17FF) }, //$NON-NLS-1$
                { "Mongolian", new LazyRange(0x1800, 0x18AF) }, //$NON-NLS-1$
                { "Limbu", new LazyRange(0x1900, 0x194F) }, //$NON-NLS-1$
                { "TaiLe", new LazyRange(0x1950, 0x197F) }, //$NON-NLS-1$
                { "NewTaiLue", new LazyRange(0x1980, 0x19DF) }, //$NON-NLS-1$
                { "KhmerSymbols", new LazyRange(0x19E0, 0x19FF) }, //$NON-NLS-1$
                { "Buginese", new LazyRange(0x1A00, 0x1A1F) }, //$NON-NLS-1$
                { "PhoneticExtensions", new LazyRange(0x1D00, 0x1D7F) }, //$NON-NLS-1$
                { "PhoneticExtensionsSupplement", new LazyRange(0x1D80, 0x1DBF) }, //$NON-NLS-1$
                { "CombiningDiacriticalMarksSupplement", //$NON-NLS-1$
                        new LazyRange(0x1DC0, 0x1DFF) },
                { "LatinExtendedAdditional", new LazyRange(0x1E00, 0x1EFF) }, //$NON-NLS-1$
                { "GreekExtended", new LazyRange(0x1F00, 0x1FFF) }, //$NON-NLS-1$
                { "GeneralPunctuation", new LazyRange(0x2000, 0x206F) }, //$NON-NLS-1$
                { "SuperscriptsandSubscripts", new LazyRange(0x2070, 0x209F) }, //$NON-NLS-1$
                { "CurrencySymbols", new LazyRange(0x20A0, 0x20CF) }, //$NON-NLS-1$
                { "CombiningMarksforSymbols", new LazyRange(0x20D0, 0x20FF) }, //$NON-NLS-1$
                { "LetterlikeSymbols", new LazyRange(0x2100, 0x214F) }, //$NON-NLS-1$
                { "NumberForms", new LazyRange(0x2150, 0x218F) }, //$NON-NLS-1$
                { "Arrows", new LazyRange(0x2190, 0x21FF) }, //$NON-NLS-1$
                { "MathematicalOperators", new LazyRange(0x2200, 0x22FF) }, //$NON-NLS-1$
                { "MiscellaneousTechnical", new LazyRange(0x2300, 0x23FF) }, //$NON-NLS-1$
                { "ControlPictures", new LazyRange(0x2400, 0x243F) }, //$NON-NLS-1$
                { "OpticalCharacterRecognition", new LazyRange(0x2440, 0x245F) }, //$NON-NLS-1$
                { "EnclosedAlphanumerics", new LazyRange(0x2460, 0x24FF) }, //$NON-NLS-1$
                { "BoxDrawing", new LazyRange(0x2500, 0x257F) }, //$NON-NLS-1$
                { "BlockElements", new LazyRange(0x2580, 0x259F) }, //$NON-NLS-1$
                { "GeometricShapes", new LazyRange(0x25A0, 0x25FF) }, //$NON-NLS-1$
                { "MiscellaneousSymbols", new LazyRange(0x2600, 0x26FF) }, //$NON-NLS-1$
                { "Dingbats", new LazyRange(0x2700, 0x27BF) }, //$NON-NLS-1$
                { "MiscellaneousMathematicalSymbols-A", //$NON-NLS-1$
                        new LazyRange(0x27C0, 0x27EF) },
                { "SupplementalArrows-A", new LazyRange(0x27F0, 0x27FF) }, //$NON-NLS-1$
                { "BraillePatterns", new LazyRange(0x2800, 0x28FF) }, //$NON-NLS-1$
                { "SupplementalArrows-B", new LazyRange(0x2900, 0x297F) }, //$NON-NLS-1$
                { "MiscellaneousMathematicalSymbols-B", //$NON-NLS-1$
                        new LazyRange(0x2980, 0x29FF) },
                { "SupplementalMathematicalOperators", //$NON-NLS-1$
                        new LazyRange(0x2A00, 0x2AFF) },
                { "MiscellaneousSymbolsandArrows", //$NON-NLS-1$
                        new LazyRange(0x2B00, 0x2BFF) },
                { "Glagolitic", new LazyRange(0x2C00, 0x2C5F) }, //$NON-NLS-1$
                { "Coptic", new LazyRange(0x2C80, 0x2CFF) }, //$NON-NLS-1$
                { "GeorgianSupplement", new LazyRange(0x2D00, 0x2D2F) }, //$NON-NLS-1$
                { "Tifinagh", new LazyRange(0x2D30, 0x2D7F) }, //$NON-NLS-1$
                { "EthiopicExtended", new LazyRange(0x2D80, 0x2DDF) }, //$NON-NLS-1$
                { "SupplementalPunctuation", new LazyRange(0x2E00, 0x2E7F) }, //$NON-NLS-1$
                { "CJKRadicalsSupplement", new LazyRange(0x2E80, 0x2EFF) }, //$NON-NLS-1$
                { "KangxiRadicals", new LazyRange(0x2F00, 0x2FDF) }, //$NON-NLS-1$
                { "IdeographicDescriptionCharacters", //$NON-NLS-1$
                        new LazyRange(0x2FF0, 0x2FFF) },
                { "CJKSymbolsandPunctuation", new LazyRange(0x3000, 0x303F) }, //$NON-NLS-1$
                { "Hiragana", new LazyRange(0x3040, 0x309F) }, //$NON-NLS-1$
                { "Katakana", new LazyRange(0x30A0, 0x30FF) }, //$NON-NLS-1$
                { "Bopomofo", new LazyRange(0x3100, 0x312F) }, //$NON-NLS-1$
                { "HangulCompatibilityJamo", new LazyRange(0x3130, 0x318F) }, //$NON-NLS-1$
                { "Kanbun", new LazyRange(0x3190, 0x319F) }, //$NON-NLS-1$
                { "BopomofoExtended", new LazyRange(0x31A0, 0x31BF) }, //$NON-NLS-1$
                { "CJKStrokes", new LazyRange(0x31C0, 0x31EF) }, //$NON-NLS-1$
                { "KatakanaPhoneticExtensions", new LazyRange(0x31F0, 0x31FF) }, //$NON-NLS-1$
                { "EnclosedCJKLettersandMonths", new LazyRange(0x3200, 0x32FF) }, //$NON-NLS-1$
                { "CJKCompatibility", new LazyRange(0x3300, 0x33FF) }, //$NON-NLS-1$
                { "CJKUnifiedIdeographsExtensionA", //$NON-NLS-1$
                        new LazyRange(0x3400, 0x4DB5) },
                { "YijingHexagramSymbols", new LazyRange(0x4DC0, 0x4DFF) }, //$NON-NLS-1$
                { "CJKUnifiedIdeographs", new LazyRange(0x4E00, 0x9FFF) }, //$NON-NLS-1$
                { "YiSyllables", new LazyRange(0xA000, 0xA48F) }, //$NON-NLS-1$
                { "YiRadicals", new LazyRange(0xA490, 0xA4CF) }, //$NON-NLS-1$
                { "ModifierToneLetters", new LazyRange(0xA700, 0xA71F) }, //$NON-NLS-1$
                { "SylotiNagri", new LazyRange(0xA800, 0xA82F) }, //$NON-NLS-1$
                { "HangulSyllables", new LazyRange(0xAC00, 0xD7A3) }, //$NON-NLS-1$
                { "HighSurrogates", new LazyRange(0xD800, 0xDB7F) }, //$NON-NLS-1$
                { "HighPrivateUseSurrogates", new LazyRange(0xDB80, 0xDBFF) }, //$NON-NLS-1$
                { "LowSurrogates", new LazyRange(0xDC00, 0xDFFF) }, //$NON-NLS-1$
                { "PrivateUseArea", new LazyRange(0xE000, 0xF8FF) }, //$NON-NLS-1$
                { "CJKCompatibilityIdeographs", new LazyRange(0xF900, 0xFAFF) }, //$NON-NLS-1$
                { "AlphabeticPresentationForms", new LazyRange(0xFB00, 0xFB4F) }, //$NON-NLS-1$
                { "ArabicPresentationForms-A", new LazyRange(0xFB50, 0xFDFF) }, //$NON-NLS-1$
                { "VariationSelectors", new LazyRange(0xFE00, 0xFE0F) }, //$NON-NLS-1$
                { "VerticalForms", new LazyRange(0xFE10, 0xFE1F) }, //$NON-NLS-1$
                { "CombiningHalfMarks", new LazyRange(0xFE20, 0xFE2F) }, //$NON-NLS-1$
                { "CJKCompatibilityForms", new LazyRange(0xFE30, 0xFE4F) }, //$NON-NLS-1$
                { "SmallFormVariants", new LazyRange(0xFE50, 0xFE6F) }, //$NON-NLS-1$
                { "ArabicPresentationForms-B", new LazyRange(0xFE70, 0xFEFF) }, //$NON-NLS-1$
                { "HalfwidthandFullwidthForms", new LazyRange(0xFF00, 0xFFEF) }, //$NON-NLS-1$
                { "all", new LazyRange(0x00, 0x10FFFF) }, //$NON-NLS-1$
                { "Specials", new LazySpecialsBlock() }, //$NON-NLS-1$
                { "Cn", new LazyCategory(Character.UNASSIGNED, true) },
                { "IsL", new LazyCategoryScope(0x3E, true) },
                { "Lu", new LazyCategory(Character.UPPERCASE_LETTER, true) },
                { "Ll", new LazyCategory(Character.LOWERCASE_LETTER, true) },
                { "Lt", new LazyCategory(Character.TITLECASE_LETTER, false) },
                { "Lm", new LazyCategory(Character.MODIFIER_LETTER, false) },
                { "Lo", new LazyCategory(Character.OTHER_LETTER, true) },
                { "IsM", new LazyCategoryScope(0x1C0, true) },
                { "Mn", new LazyCategory(Character.NON_SPACING_MARK, true) },
                { "Me", new LazyCategory(Character.ENCLOSING_MARK, false) },
                { "Mc", new LazyCategory(Character.COMBINING_SPACING_MARK, true) },
                { "N", new LazyCategoryScope(0xE00, true) },
                { "Nd", new LazyCategory(Character.DECIMAL_DIGIT_NUMBER, true) },
                { "Nl", new LazyCategory(Character.LETTER_NUMBER, true) },
                { "No", new LazyCategory(Character.OTHER_NUMBER, true) },
                { "IsZ", new LazyCategoryScope(0x7000, false) },
                { "Zs", new LazyCategory(Character.SPACE_SEPARATOR, false) },
                { "Zl", new LazyCategory(Character.LINE_SEPARATOR, false) },
                { "Zp", new LazyCategory(Character.PARAGRAPH_SEPARATOR, false) },
                { "IsC", new LazyCategoryScope(0xF0000, true, true) },
                { "Cc", new LazyCategory(Character.CONTROL, false) },
                { "Cf", new LazyCategory(Character.FORMAT, true) },
                { "Co", new LazyCategory(Character.PRIVATE_USE, true) },
                { "Cs", new LazyCategory(Character.SURROGATE, false, true) },
                {
                        "IsP",
                        new LazyCategoryScope((1 << Character.DASH_PUNCTUATION) | (1 << Character.START_PUNCTUATION)
                                | (1 << Character.END_PUNCTUATION) | (1 << Character.CONNECTOR_PUNCTUATION)
                                | (1 << Character.OTHER_PUNCTUATION) | (1 << Character.INITIAL_QUOTE_PUNCTUATION)
                                | (1 << Character.FINAL_QUOTE_PUNCTUATION), true) },
                { "Pd", new LazyCategory(Character.DASH_PUNCTUATION, false) },
                { "Ps", new LazyCategory(Character.START_PUNCTUATION, false) },
                { "Pe", new LazyCategory(Character.END_PUNCTUATION, false) },
                { "Pc", new LazyCategory(Character.CONNECTOR_PUNCTUATION, false) },
                { "Po", new LazyCategory(Character.OTHER_PUNCTUATION, true) },
                { "IsS", new LazyCategoryScope(0x7E000000, true) },
                { "Sm", new LazyCategory(Character.MATH_SYMBOL, true) },
                { "Sc", new LazyCategory(Character.CURRENCY_SYMBOL, false) },
                { "Sk", new LazyCategory(Character.MODIFIER_SYMBOL, false) },
                { "So", new LazyCategory(Character.OTHER_SYMBOL, true) },
                { "Pi", new LazyCategory(Character.INITIAL_QUOTE_PUNCTUATION, false) },
                { "Pf", new LazyCategory(Character.FINAL_QUOTE_PUNCTUATION, false) } };

        public Object getObject(String name) {
            for (int i = 0; i < contents.length; ++i) {
                Object[] row = contents[i];
                if (name.equals(row[0])) {
                    return row[1];
                }
            }
            throw new MissingResourceException("", "", name);
        }
    }
}
