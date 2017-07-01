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

/**
 * User defined character classes ([abef]). See AbstractCharClass documentation
 * for more details.
 *
 * @author Nikolay A. Kuznetsov
 */
class TCharClass extends TAbstractCharClass {
    // Flag indicates if we add supplement upper/lower case
    boolean ci;

    boolean uci;

    // Flag indicates if there are unicode supplements
    boolean hasUCI;

    boolean invertedSurrogates;

    boolean inverted;

    boolean hideBits;

    BitSet bits = new BitSet();

    TAbstractCharClass nonBitSet;

    public TCharClass() {
    }

    public TCharClass(boolean ci, boolean uci) {
        this.ci = ci;
        this.uci = uci;
    }

    public TCharClass(boolean negative, boolean ci, boolean uci) {
        this(ci, uci);
        setNegative(negative);
    }

    /*
     * We can use this method safely even if nonBitSet != null due to specific
     * of range constructions in regular expressions.
     */
    public TCharClass add(int ch) {
        if (ci) {
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                if (!inverted) {
                    bits.set(TPattern.getSupplement((char) ch));
                } else {
                    bits.clear(TPattern.getSupplement((char) ch));
                }
            } else if (uci && ch > 128) {
                hasUCI = true;
                ch = Character.toLowerCase(Character.toUpperCase(ch));
                // return this;
            }
        }

        if (TLexer.isHighSurrogate(ch) || TLexer.isLowSurrogate(ch)) {
            if (!invertedSurrogates) {
                lowHighSurrogates.set(ch - Character.MIN_SURROGATE);
            } else {
                lowHighSurrogates.clear(ch - Character.MIN_SURROGATE);
            }
        }

        if (!inverted) {
            bits.set(ch);
        } else {
            bits.clear(ch);
        }

        if (!mayContainSupplCodepoints && Character.isSupplementaryCodePoint(ch)) {
            mayContainSupplCodepoints = true;
        }

        return this;
    }

    /*
     * The difference between add(AbstractCharClass) and
     * union(AbstractCharClass) is that add() is used for constructions like
     * "[^abc\\d]" (this pattern doesn't match "1") while union is used for
     * constructions like "[^abc[\\d]]" (this pattern matches "1").
     */
    public TCharClass add(final TAbstractCharClass cc) {

        if (!mayContainSupplCodepoints && cc.mayContainSupplCodepoints) {
            mayContainSupplCodepoints = true;
        }

        if (!invertedSurrogates) {

            // A | !B = ! ((A ^ B) & B)
            if (cc.altSurrogates) {
                lowHighSurrogates.xor(cc.getLowHighSurrogates());
                lowHighSurrogates.and(cc.getLowHighSurrogates());
                altSurrogates = !altSurrogates;
                invertedSurrogates = true;

                // A | B
            } else {
                lowHighSurrogates.or(cc.getLowHighSurrogates());
            }
        } else {

            // !A | !B = !(A & B)
            if (cc.altSurrogates) {
                lowHighSurrogates.and(cc.getLowHighSurrogates());

                // !A | B = !(A & !B)
            } else {
                lowHighSurrogates.andNot(cc.getLowHighSurrogates());
            }
        }

        if (!hideBits && cc.getBits() != null) {
            if (!inverted) {

                // A | !B = ! ((A ^ B) & B)
                if (cc.isNegative()) {
                    bits.xor(cc.getBits());
                    bits.and(cc.getBits());
                    alt = !alt;
                    inverted = true;

                    // A | B
                } else {
                    bits.or(cc.getBits());
                }
            } else {

                // !A | !B = !(A & B)
                if (cc.isNegative()) {
                    bits.and(cc.getBits());

                    // !A | B = !(A & !B)
                } else {
                    bits.andNot(cc.getBits());
                }
            }
        } else {
            final boolean curAlt = alt;

            if (nonBitSet == null) {

                if (curAlt && !inverted && bits.isEmpty()) {
                    nonBitSet = new TAbstractCharClass() {
                        @Override
                        public boolean contains(int ch) {
                            return cc.contains(ch);
                        }
                    };
                    // alt = true;
                } else {

                    /*
                     * We keep the value of alt unchanged for constructions like
                     * [^[abc]fgb] by using the formula a ^ b == !a ^ !b.
                     */
                    if (curAlt) {
                        nonBitSet = new TAbstractCharClass() {
                            @Override
                            public boolean contains(int ch) {
                                return !((curAlt ^ bits.get(ch)) || ((curAlt ^ inverted) ^ cc.contains(ch)));
                            }
                        };
                        // alt = true
                    } else {
                        nonBitSet = new TAbstractCharClass() {
                            @Override
                            public boolean contains(int ch) {
                                return (curAlt ^ bits.get(ch)) || ((curAlt ^ inverted) ^ cc.contains(ch));
                            }
                        };
                        // alt = false
                    }
                }

                hideBits = true;
            } else {
                final TAbstractCharClass nb = nonBitSet;

                if (curAlt) {
                    nonBitSet = new TAbstractCharClass() {
                        @Override
                        public boolean contains(int ch) {
                            return !(curAlt ^ (nb.contains(ch) || cc.contains(ch)));
                        }
                    };
                    // alt = true
                } else {
                    nonBitSet = new TAbstractCharClass() {
                        @Override
                        public boolean contains(int ch) {
                            return curAlt ^ (nb.contains(ch) || cc.contains(ch));
                        }
                    };
                    // alt = false
                }
            }
        }

        return this;
    }

    public TCharClass add(int st, int end) {
        if (st > end) {
            throw new IllegalArgumentException();
        }
        if (!ci

        // no intersection with surrogate characters
                &&
                (end < Character.MIN_SURROGATE || st > Character.MAX_SURROGATE)) {
            if (!inverted) {
                bits.set(st, end + 1);
            } else {
                bits.clear(st, end + 1);
            }
        } else {
            for (int i = st; i < end + 1; i++) {
                add(i);
            }
        }
        return this;
    }

    // OR operation
    public void union(final TAbstractCharClass clazz) {
        if (!mayContainSupplCodepoints && clazz.mayContainSupplCodepoints) {
            mayContainSupplCodepoints = true;
        }

        if (clazz.hasUCI()) {
            this.hasUCI = true;
        }

        if (altSurrogates ^ clazz.altSurrogates) {

            // !A | B = !(A & !B)
            if (altSurrogates) {
                lowHighSurrogates.andNot(clazz.getLowHighSurrogates());

                // A | !B = !((A ^ B) & B)
            } else {
                lowHighSurrogates.xor(clazz.getLowHighSurrogates());
                lowHighSurrogates.and(clazz.getLowHighSurrogates());
                altSurrogates = true;
            }

        } else {

            // !A | !B = !(A & B)
            if (altSurrogates) {
                lowHighSurrogates.and(clazz.getLowHighSurrogates());

                // A | B
            } else {
                lowHighSurrogates.or(clazz.getLowHighSurrogates());
            }
        }

        if (!hideBits && clazz.getBits() != null) {
            if (alt ^ clazz.isNegative()) {

                // !A | B = !(A & !B)
                if (alt) {
                    bits.andNot(clazz.getBits());

                    // A | !B = !((A ^ B) & B)
                } else {
                    bits.xor(clazz.getBits());
                    bits.and(clazz.getBits());
                    alt = true;
                }

            } else {

                // !A | !B = !(A & B)
                if (alt) {
                    bits.and(clazz.getBits());

                    // A | B
                } else {
                    bits.or(clazz.getBits());
                }
            }
        } else {
            final boolean curAlt = alt;

            if (nonBitSet == null) {

                if (!inverted && bits.isEmpty()) {
                    if (curAlt) {
                        nonBitSet = new TAbstractCharClass() {
                            @Override
                            public boolean contains(int ch) {
                                return !clazz.contains(ch);
                            }
                        };
                        // alt = true
                    } else {
                        nonBitSet = new TAbstractCharClass() {
                            @Override
                            public boolean contains(int ch) {
                                return clazz.contains(ch);
                            }
                        };
                        // alt = false
                    }
                } else {

                    if (curAlt) {
                        nonBitSet = new TAbstractCharClass() {
                            @Override
                            public boolean contains(int ch) {
                                return !(clazz.contains(ch) || (curAlt ^ bits.get(ch)));
                            }
                        };
                        // alt = true
                    } else {
                        nonBitSet = new TAbstractCharClass() {
                            @Override
                            public boolean contains(int ch) {
                                return clazz.contains(ch) || (curAlt ^ bits.get(ch));
                            }
                        };
                        // alt = false
                    }
                }
                hideBits = true;
            } else {
                final TAbstractCharClass nb = nonBitSet;

                if (curAlt) {
                    nonBitSet = new TAbstractCharClass() {
                        @Override
                        public boolean contains(int ch) {
                            return !((curAlt ^ nb.contains(ch)) || clazz.contains(ch));
                        }
                    };
                    // alt = true
                } else {
                    nonBitSet = new TAbstractCharClass() {
                        @Override
                        public boolean contains(int ch) {
                            return (curAlt ^ nb.contains(ch)) || clazz.contains(ch);
                        }
                    };
                    // alt = false
                }
            }
        }
    }

    // AND operation
    public void intersection(final TAbstractCharClass clazz) {
        if (!mayContainSupplCodepoints && clazz.mayContainSupplCodepoints) {
            mayContainSupplCodepoints = true;
        }

        if (clazz.hasUCI()) {
            this.hasUCI = true;
        }

        if (altSurrogates ^ clazz.altSurrogates) {

            // !A & B = ((A ^ B) & B)
            if (altSurrogates) {
                lowHighSurrogates.xor(clazz.getLowHighSurrogates());
                lowHighSurrogates.and(clazz.getLowHighSurrogates());
                altSurrogates = false;

                // A & !B
            } else {
                lowHighSurrogates.andNot(clazz.getLowHighSurrogates());
            }
        } else {

            // !A & !B = !(A | B)
            if (altSurrogates) {
                lowHighSurrogates.or(clazz.getLowHighSurrogates());

                // A & B
            } else {
                lowHighSurrogates.and(clazz.getLowHighSurrogates());
            }
        }

        if (!hideBits && clazz.getBits() != null) {

            if (alt ^ clazz.isNegative()) {

                // !A & B = ((A ^ B) & B)
                if (alt) {
                    bits.xor(clazz.getBits());
                    bits.and(clazz.getBits());
                    alt = false;

                    // A & !B
                } else {
                    bits.andNot(clazz.getBits());
                }
            } else {

                // !A & !B = !(A | B)
                if (alt) {
                    bits.or(clazz.getBits());

                    // A & B
                } else {
                    bits.and(clazz.getBits());
                }
            }
        } else {
            final boolean curAlt = alt;

            if (nonBitSet == null) {

                if (!inverted && bits.isEmpty()) {
                    if (curAlt) {
                        nonBitSet = new TAbstractCharClass() {
                            @Override
                            public boolean contains(int ch) {
                                return !clazz.contains(ch);
                            }
                        };
                        // alt = true
                    } else {
                        nonBitSet = new TAbstractCharClass() {
                            @Override
                            public boolean contains(int ch) {
                                return clazz.contains(ch);
                            }
                        };
                        // alt = false
                    }
                } else {

                    if (curAlt) {
                        nonBitSet = new TAbstractCharClass() {
                            @Override
                            public boolean contains(int ch) {
                                return !(clazz.contains(ch) && (curAlt ^ bits.get(ch)));
                            }
                        };
                        // alt = true
                    } else {
                        nonBitSet = new TAbstractCharClass() {
                            @Override
                            public boolean contains(int ch) {
                                return clazz.contains(ch) && (curAlt ^ bits.get(ch));
                            }
                        };
                        // alt = false
                    }
                }
                hideBits = true;
            } else {
                final TAbstractCharClass nb = nonBitSet;

                if (curAlt) {
                    nonBitSet = new TAbstractCharClass() {
                        @Override
                        public boolean contains(int ch) {
                            return !((curAlt ^ nb.contains(ch)) && clazz.contains(ch));
                        }
                    };
                    // alt = true
                } else {
                    nonBitSet = new TAbstractCharClass() {
                        @Override
                        public boolean contains(int ch) {
                            return (curAlt ^ nb.contains(ch)) && clazz.contains(ch);
                        }
                    };
                    // alt = false
                }
            }
        }
    }

    /**
     * Returns <code>true</code> if character class contains symbol specified,
     * <code>false</code> otherwise. Note: #setNegative() method changes the
     * meaning of contains method;
     *
     * @return <code>true</code> if character class contains symbol specified;
     *
     *         TODO: currently <code>character class</code> implementation based
     *         on BitSet, but this implementation possibly will be turned to
     *         combined BitSet(for first 256 symbols) and Black/Red tree for the
     *         rest of UTF.
     */
    @Override
    public boolean contains(int ch) {
        if (nonBitSet == null) {
            return this.alt ^ bits.get(ch);
        } else {
            return alt ^ nonBitSet.contains(ch);
        }
    }

    @Override
    protected BitSet getBits() {
        if (hideBits) {
            return null;
        }
        return bits;
    }

    @Override
    protected BitSet getLowHighSurrogates() {
        return lowHighSurrogates;
    }

    @Override
    public TAbstractCharClass getInstance() {

        if (nonBitSet == null) {
            final BitSet bs = getBits();

            TAbstractCharClass res = new TAbstractCharClass() {
                @Override
                public boolean contains(int ch) {
                    return this.alt ^ bs.get(ch);
                }

                @Override
                public String toString() {
                    StringBuilder temp = new StringBuilder();
                    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
                        temp.append(Character.toChars(i));
                        temp.append('|');
                    }

                    if (temp.length() > 0) {
                        temp.deleteCharAt(temp.length() - 1);
                    }

                    return temp.toString();
                }

            };
            return res.setNegative(isNegative());
        } else {
            return this;
        }
    }

    // for debugging purposes only
    @Override
    public String toString() {
        StringBuilder temp = new StringBuilder();
        for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
            temp.append(Character.toChars(i));
            temp.append('|');
        }

        if (temp.length() > 0) {
            temp.deleteCharAt(temp.length() - 1);
        }

        return temp.toString();
    }

    @Override
    public boolean hasUCI() {
        return hasUCI;
    }
}
