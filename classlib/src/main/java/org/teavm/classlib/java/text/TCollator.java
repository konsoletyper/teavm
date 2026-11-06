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
package org.teavm.classlib.java.text;

import java.util.Objects;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.java.lang.TCloneable;
import org.teavm.classlib.java.lang.TUnsupportedOperationException;
import org.teavm.classlib.java.util.TComparator;
import org.teavm.classlib.java.util.TLocale;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/** Locale-aware string ordering backed by the JavaScript host's ICU collation data. */
public abstract class TCollator implements TComparator<Object>, TCloneable {
    protected TCollator() {
    }

    public static TCollator getInstance() {
        return getInstance(TLocale.getDefault());
    }

    public static TCollator getInstance(TLocale locale) {
        Objects.requireNonNull(locale);
        if (!PlatformDetector.isJavaScript()) {
            throw new TUnsupportedOperationException("Collation requires JavaScript Intl.Collator");
        }
        return new JavaScriptCollator(locale);
    }

    public static TLocale[] getAvailableLocales() {
        return TLocale.getAvailableLocales();
    }

    public abstract int compare(String source, String target);

    @Override
    public int compare(Object source, Object target) {
        return compare((String) source, (String) target);
    }

    @Override
    public abstract Object clone();

    private static final class JavaScriptCollator extends TCollator {
        private final TLocale locale;
        private final JSObject delegate;

        JavaScriptCollator(TLocale locale) {
            this.locale = locale;
            String languageTag = locale.toLanguageTag().toString();
            if (languageTag.startsWith("x-")) {
                languageTag = "und-" + languageTag;
            }
            delegate = create(languageTag);
        }

        @Override
        public int compare(String source, String target) {
            return compareIntl(delegate, Objects.requireNonNull(source), Objects.requireNonNull(target));
        }

        @Override
        public Object clone() {
            return new JavaScriptCollator(locale);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof JavaScriptCollator && locale.equals(((JavaScriptCollator) other).locale);
        }

        @Override
        public int hashCode() {
            return locale.hashCode();
        }
    }

    @JSBody(params = "languageTag", script = "return new Intl.Collator(languageTag, "
            + "{ usage: 'sort', sensitivity: 'variant' });")
    private static native JSObject create(String languageTag);

    @JSBody(params = { "collator", "source", "target" }, script = "return collator.compare(source, target);")
    private static native int compareIntl(JSObject collator, String source, String target);
}
