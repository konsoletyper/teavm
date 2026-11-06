/*
 *  Copyright 2026 TeaVM contributors.
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
package repro;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

public final class LocaleContract {
    private LocaleContract() {
    }

    public static void main(String[] args) {
        localeContract();
        collatorContract();
        System.out.println("Locale contract passed");
    }

    private static void localeContract() {
        Locale locale = Locale.forLanguageTag("en-Latn-us-posix-u-co-search");
        check("en".equals(locale.getLanguage()), "language parsed");
        check("Latn".equals(locale.getScript()), "script parsed with title case");
        check("US".equals(locale.getCountry()), "region parsed with upper case");
        check("posix".equals(locale.getVariant()), "variant parsed");
        check("co-search".equals(locale.getExtension('u')), "Unicode locale extension preserved");
        check("en-Latn-US-posix-u-co-search".equals(locale.toLanguageTag()), "locale tag canonicalized");

        Locale extlang = Locale.forLanguageTag("zh-cmn-Hans-CN");
        check("cmn".equals(extlang.getLanguage()) && "Hans".equals(extlang.getScript())
                && "CN".equals(extlang.getCountry()), "extlang preferred value and script parsed");
        check("cmn-Hans-CN".equals(extlang.toLanguageTag()), "extlang preferred tag canonicalized");
        check("cmn".equals(Locale.forLanguageTag("zh-cmn-yue-Hans-CN").getLanguage()),
                "first extlang determines the preferred language");
        check("POSIX".equals(Locale.forLanguageTag("en-US-POSIX").getVariant()), "variant case is preserved");
        check("rozaj_BISKE".equals(Locale.forLanguageTag("sl-rozaj-BISKE").getVariant()),
                "variant sequence and case are preserved");
        Locale privateVariant = Locale.forLanguageTag("en-US-x-lvariant-foo");
        check("foo".equals(privateVariant.getVariant())
                && "en-US-x-lvariant-foo".equals(privateVariant.toLanguageTag()),
                "legacy private variant encoding round-trips");
        check("en-US-x-lvariant-foo".equals(new Locale("en", "US", "foo").toLanguageTag()),
                "invalid constructor variants use the private lvariant encoding");
        Locale genericExtlang = Locale.forLanguageTag("en-abc-US");
        check("abc".equals(genericExtlang.getLanguage()) && "abc-US".equals(genericExtlang.toLanguageTag()),
                "extlang is exposed as the preferred language");
        check("en-u-ca-gregory-nu-latn".equals(
                Locale.forLanguageTag("en-u-nu-latn-ca-gregory").toLanguageTag()),
                "Unicode extension keys sort canonically");
        check("ca-gregory-nu-latn".equals(
                Locale.forLanguageTag("en-u-nu-latn-ca-gregory").getExtension('u')),
                "Unicode extension lookup uses canonical key order");
        check("tlh".equals(Locale.forLanguageTag("i-klingon").getLanguage()),
                "grandfathered language tag maps to its preferred language");

        Locale malformed = Locale.forLanguageTag("en-US-u-ca-gregory-bad!");
        check("en-US-u-ca-gregory".equals(malformed.toLanguageTag()), "malformed suffix truncates to valid prefix");
        Locale privateUse = Locale.forLanguageTag("x-private");
        check(privateUse.getLanguage().isEmpty() && "x-private".equals(privateUse.toLanguageTag()),
                "private use tag without language is retained");
        check("private".equals(privateUse.getExtension('x')), "private use extension can be queried");
        check(Locale.forLanguageTag("und").equals(Locale.ROOT), "und maps to root locale");

        try {
            Locale.forLanguageTag(null);
            throw new AssertionError("null language tag must fail");
        } catch (NullPointerException expected) {
            // Expected.
        }
    }

    private static void collatorContract() {
        Collator german = Collator.getInstance(Locale.forLanguageTag("de-DE"));
        check(german.compare("ä", "z") < 0, "German collation places umlauted a before z");
        Collator english = Collator.getInstance(Locale.forLanguageTag("en-US"));
        check(english.compare("e", "é") < 0, "tertiary collation distinguishes accents");
        check(((Comparator<Object>) english).compare("a", "b") < 0, "Comparator<Object> bridge compares strings");
        Collator turkish = Collator.getInstance(Locale.forLanguageTag("tr-TR"));
        check(turkish.compare("I", "ı") > 0, "Turkish collation honors dotted and dotless I");

        Collator filipino = Collator.getInstance(Locale.forLanguageTag("fil-PH-u-co-search"));
        check(filipino.compare("a", "b") < 0, "collator accepts a locale with a Unicode extension");
        Collator rusyn = Collator.getInstance(Locale.forLanguageTag("rue-SK-u-kr-cyrl-latn-digit"));
        check(rusyn.compare("a", "b") < 0, "collator accepts a locale with a reorder extension");
        Collator privateUse = Collator.getInstance(Locale.forLanguageTag("x-private"));
        check(privateUse.compare("a", "b") < 0, "collator accepts a private-use-only locale");

        try {
            Collator.getInstance(null);
            throw new AssertionError("null locale must fail");
        } catch (NullPointerException expected) {
            // Expected.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
