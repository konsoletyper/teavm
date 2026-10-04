/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.classlib.java.util.regex;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashMap;

/**
 * Factory methods that reconstruct patterns compiled in build time. Calls to these methods are generated
 * by {@code org.teavm.classlib.impl.regex.PatternCompileTransformer} from descriptions produced by
 * {@link TPatternWriter}. Method names must be unique, since the transformer looks them up by name.
 */
final class TPatternFactory {
    private TPatternFactory() {
    }

    static TPattern pattern(String source, int flags, TAbstractSet start, int groupCount, int compCount,
            int consCount) {
        var pattern = new TPattern();
        pattern.source = source;
        pattern.flags = flags;
        pattern.start = start;
        pattern.globalGroupIndex = groupCount;
        pattern.compCount = compCount;
        pattern.consCount = consCount;
        pattern.namedGroups = Collections.emptyMap();
        return pattern;
    }

    static void namedGroup(TPattern pattern, String name, int group) {
        var namedGroups = new LinkedHashMap<>(pattern.namedGroups);
        namedGroups.put(name, group);
        pattern.namedGroups = Collections.unmodifiableMap(namedGroups);
    }

    static void link(TAbstractSet set, TAbstractSet next) {
        set.next = next;
    }

    static void addChild(TJointSet set, TAbstractSet child) {
        set.children.add(child);
    }

    static TQuantifier quantifier(int min, int max) {
        return new TQuantifier(min, max);
    }

    static TAbstractLineTerminator lineTerminator(boolean unix) {
        return TAbstractLineTerminator.getInstance(unix ? TPattern.UNIX_LINES : 0);
    }

    static TAbstractCharClass bitSetCharClass(boolean negative) {
        return new TCharClass.BitSetCharClass(new BitSet()).setNegative(negative);
    }

    static TCharClass charClass(boolean ci, boolean uci, boolean invertedSurrogates, boolean inverted,
            boolean hideBits, boolean alt, boolean altSurrogates, boolean mayContainSupplCodepoints) {
        var charClass = new TCharClass(ci, uci);
        charClass.invertedSurrogates = invertedSurrogates;
        charClass.inverted = inverted;
        charClass.hideBits = hideBits;
        charClass.alt = alt;
        charClass.altSurrogates = altSurrogates;
        charClass.mayContainSupplCodepoints = mayContainSupplCodepoints;
        return charClass;
    }

    static void addCharClassRange(TAbstractCharClass charClass, int from, int to) {
        if (charClass instanceof TCharClass.BitSetCharClass) {
            ((TCharClass.BitSetCharClass) charClass).bs.set(from, to);
        } else {
            ((TCharClass) charClass).bits.set(from, to);
        }
    }

    static TAbstractCharClass surrogatesCharClass(boolean negative) {
        return new TAbstractCharClass.SurrogatesCharClass(new BitSet()).setNegative(negative);
    }

    static TAbstractCharClass withoutSurrogatesCharClass(TAbstractCharClass base, boolean negative,
            boolean mayContainSupplCodepoints) {
        var charClass = new TAbstractCharClass.WithoutSurrogatesCharClass(new BitSet(), base);
        charClass.setNegative(negative);
        charClass.mayContainSupplCodepoints = mayContainSupplCodepoints;
        return charClass;
    }

    static void addSurrogateRange(TAbstractCharClass charClass, int from, int to) {
        BitSet bits;
        if (charClass instanceof TAbstractCharClass.SurrogatesCharClass) {
            bits = ((TAbstractCharClass.SurrogatesCharClass) charClass).surrogates;
        } else if (charClass instanceof TAbstractCharClass.WithoutSurrogatesCharClass) {
            bits = ((TAbstractCharClass.WithoutSurrogatesCharClass) charClass).surrogates;
        } else {
            bits = charClass.lowHighSurrogates;
        }
        bits.set(from, to);
    }

    static TCompositeRangeSet compositeRangeSet(TAbstractSet withoutSurrogates, TAbstractSet withSurrogates) {
        return new TCompositeRangeSet(withoutSurrogates, withSurrogates);
    }

    static TLowHighSurrogateRangeSet lowHighSurrogateRangeSet(TAbstractCharClass surrChars) {
        return new TLowHighSurrogateRangeSet(surrChars);
    }

    static TUnicodeCategory unicodeCategory(int category, boolean negative, boolean mayContainSupplCodepoints,
            boolean allSurrogates) {
        return initCategory(new TUnicodeCategory(category), negative, mayContainSupplCodepoints, allSurrogates);
    }

    static TUnicodeCategory unicodeCategoryScope(int category, boolean negative,
            boolean mayContainSupplCodepoints, boolean allSurrogates) {
        return initCategory(new TUnicodeCategoryScope(category), negative, mayContainSupplCodepoints,
                allSurrogates);
    }

    private static TUnicodeCategory initCategory(TUnicodeCategory charClass, boolean negative,
            boolean mayContainSupplCodepoints, boolean allSurrogates) {
        if (allSurrogates) {
            charClass.lowHighSurrogates.set(0, TAbstractCharClass.SURROGATE_CARDINALITY);
        }
        charClass.mayContainSupplCodepoints = mayContainSupplCodepoints;
        if (negative) {
            charClass.setNegative(true);
        }
        return charClass;
    }

    static TAbstractSet posFSet() {
        return TFSet.posFSet;
    }

    static TCharSet charSet(char ch) {
        return new TCharSet(ch);
    }

    static TCICharSet ciCharSet(char ch) {
        return new TCICharSet(ch);
    }

    static TSupplCharSet supplCharSet(int ch) {
        return new TSupplCharSet(ch);
    }

    static TLowSurrogateCharSet lowSurrogateCharSet(char low) {
        return new TLowSurrogateCharSet(low);
    }

    static THighSurrogateCharSet highSurrogateCharSet(char high) {
        return new THighSurrogateCharSet(high);
    }

    static TSequenceSet sequenceSet(String string) {
        return new TSequenceSet(new StringBuffer(string));
    }

    static TCISequenceSet ciSequenceSet(String string) {
        return new TCISequenceSet(new StringBuffer(string));
    }

    static TRangeSet rangeSet(TAbstractCharClass chars) {
        return new TRangeSet(chars);
    }

    static TSupplRangeSet supplRangeSet(TAbstractCharClass chars) {
        return new TSupplRangeSet(chars);
    }

    static TDotSet dotSet(TAbstractLineTerminator lt) {
        return new TDotSet(lt);
    }

    static TDotAllSet dotAllSet() {
        return new TDotAllSet();
    }

    static TEmptySet emptySet() {
        return new TEmptySet(null);
    }

    static TEOISet eoiSet() {
        return new TEOISet();
    }

    static TSOLSet solSet() {
        return new TSOLSet();
    }

    static TPreviousMatch previousMatch() {
        return new TPreviousMatch();
    }

    static TEOLSet eolSet(int consCounter) {
        return new TEOLSet(consCounter);
    }

    static TMultiLineEOLSet multiLineEOLSet(int consCounter) {
        return new TMultiLineEOLSet(consCounter);
    }

    static TUEOLSet ueolSet(int consCounter) {
        return new TUEOLSet(consCounter);
    }

    static TUMultiLineEOLSet uMultiLineEOLSet(int consCounter) {
        return new TUMultiLineEOLSet(consCounter);
    }

    static TMultiLineSOLSet multiLineSOLSet(TAbstractLineTerminator lt) {
        return new TMultiLineSOLSet(lt);
    }

    static TWordBoundary wordBoundary(boolean positive) {
        return new TWordBoundary(positive);
    }

    static TFSet fSet(int groupIndex) {
        return new TFSet(groupIndex);
    }

    static TFinalSet finalSet() {
        return new TFinalSet();
    }

    static TAheadFSet aheadFSet() {
        return new TAheadFSet();
    }

    static TNonCapFSet nonCapFSet(int groupIndex) {
        return new TNonCapFSet(groupIndex);
    }

    static TBehindFSet behindFSet(int groupIndex) {
        return new TBehindFSet(groupIndex);
    }

    static TAtomicFSet atomicFSet(int groupIndex) {
        return new TAtomicFSet(groupIndex);
    }

    static TJointSet jointSet(TFSet fSet) {
        return new TJointSet(new ArrayList<>(), fSet);
    }

    static TNonCapJointSet nonCapJointSet(TFSet fSet) {
        return new TNonCapJointSet(new ArrayList<>(), fSet);
    }

    static TAtomicJointSet atomicJointSet(TFSet fSet) {
        return new TAtomicJointSet(new ArrayList<>(), fSet);
    }

    static TPositiveLookAhead positiveLookAhead(TFSet fSet) {
        return new TPositiveLookAhead(new ArrayList<>(), fSet);
    }

    static TNegativeLookAhead negativeLookAhead(TFSet fSet) {
        return new TNegativeLookAhead(new ArrayList<>(), fSet);
    }

    static TPositiveLookBehind positiveLookBehind(TFSet fSet) {
        return new TPositiveLookBehind(new ArrayList<>(), fSet);
    }

    static TNegativeLookBehind negativeLookBehind(TFSet fSet) {
        return new TNegativeLookBehind(new ArrayList<>(), fSet);
    }

    static TSingleSet singleSet(TAbstractSet kid, TFSet fSet) {
        return new TSingleSet(kid, fSet);
    }

    static TLeafQuantifierSet leafQuantifierSet(TLeafSet leaf, int type) {
        return new TLeafQuantifierSet(leaf, null, type);
    }

    static TReluctantQuantifierSet reluctantQuantifierSet(TLeafSet leaf, int type) {
        return new TReluctantQuantifierSet(leaf, null, type);
    }

    static TPossessiveQuantifierSet possessiveQuantifierSet(TLeafSet leaf, int type) {
        return new TPossessiveQuantifierSet(leaf, null, type);
    }

    static TAltQuantifierSet altQuantifierSet(TLeafSet leaf, int type) {
        return new TAltQuantifierSet(leaf, null, type);
    }

    static TPossessiveAltQuantifierSet possessiveAltQuantifierSet(TLeafSet leaf, int type) {
        return new TPossessiveAltQuantifierSet(leaf, null, type);
    }

    static TReluctantAltQuantifierSet reluctantAltQuantifierSet(TLeafSet leaf, int type) {
        return new TReluctantAltQuantifierSet(leaf, null, type);
    }

    static TUnifiedQuantifierSet unifiedQuantifierSet(TLeafSet leaf, int type) {
        return new TUnifiedQuantifierSet(leaf, null, type);
    }

    static TCompositeQuantifierSet compositeQuantifierSet(TQuantifier quantifier, TLeafSet leaf, int type) {
        return new TCompositeQuantifierSet(quantifier, leaf, null, type);
    }

    static TPossessiveCompositeQuantifierSet possessiveCompositeQuantifierSet(TQuantifier quantifier,
            TLeafSet leaf, int type) {
        return new TPossessiveCompositeQuantifierSet(quantifier, leaf, null, type);
    }

    static TReluctantCompositeQuantifierSet reluctantCompositeQuantifierSet(TQuantifier quantifier,
            TLeafSet leaf, int type) {
        return new TReluctantCompositeQuantifierSet(quantifier, leaf, null, type);
    }

    static TGroupQuantifierSet groupQuantifierSet(TAbstractSet innerSet, int type) {
        return new TGroupQuantifierSet(innerSet, null, type);
    }

    static TReluctantGroupQuantifierSet reluctantGroupQuantifierSet(TAbstractSet innerSet, int type) {
        return new TReluctantGroupQuantifierSet(innerSet, null, type);
    }

    static TPosPlusGroupQuantifierSet posPlusGroupQuantifierSet(TAbstractSet innerSet, int type) {
        return new TPosPlusGroupQuantifierSet(innerSet, null, type);
    }

    static TPossessiveGroupQuantifierSet possessiveGroupQuantifierSet(TAbstractSet innerSet, int type) {
        return new TPossessiveGroupQuantifierSet(innerSet, null, type);
    }

    static TAltGroupQuantifierSet altGroupQuantifierSet(TAbstractSet innerSet, int type) {
        return new TAltGroupQuantifierSet(innerSet, null, type);
    }

    static TPosAltGroupQuantifierSet posAltGroupQuantifierSet(TAbstractSet innerSet, int type) {
        return new TPosAltGroupQuantifierSet(innerSet, null, type);
    }

    static TRelAltGroupQuantifierSet relAltGroupQuantifierSet(TAbstractSet innerSet, int type) {
        return new TRelAltGroupQuantifierSet(innerSet, null, type);
    }

    static TCompositeGroupQuantifierSet compositeGroupQuantifierSet(TQuantifier quantifier,
            TAbstractSet innerSet, int type, int setCounter) {
        return new TCompositeGroupQuantifierSet(quantifier, innerSet, null, type, setCounter);
    }

    static TPosCompositeGroupQuantifierSet posCompositeGroupQuantifierSet(TQuantifier quantifier,
            TAbstractSet innerSet, int type, int setCounter) {
        return new TPosCompositeGroupQuantifierSet(quantifier, innerSet, null, type, setCounter);
    }

    static TRelCompositeGroupQuantifierSet relCompositeGroupQuantifierSet(TQuantifier quantifier,
            TAbstractSet innerSet, int type, int setCounter) {
        return new TRelCompositeGroupQuantifierSet(quantifier, innerSet, null, type, setCounter);
    }

    static TDotQuantifierSet dotQuantifierSet(TAbstractSet innerSet, int type, TAbstractLineTerminator lt) {
        return new TDotQuantifierSet(innerSet, null, type, lt);
    }

    static TDotAllQuantifierSet dotAllQuantifierSet(TAbstractSet innerSet, int type) {
        return new TDotAllQuantifierSet(innerSet, null, type);
    }
}
