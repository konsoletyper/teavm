/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.classlib.java.nio.charset;

import java.util.*;
import org.teavm.classlib.java.nio.TByteBuffer;
import org.teavm.classlib.java.nio.TCharBuffer;
import org.teavm.classlib.java.nio.charset.impl.TUTF8Charset;

public abstract class TCharset implements Comparable<TCharset> {
    private String canonicalName;
    private String[] aliases;
    private Set<String> aliasSet;

    protected TCharset(String canonicalName, String[] aliases) {
        checkCanonicalName(canonicalName);
        for (String alias : aliases) {
            checkCanonicalName(alias);
        }
        this.canonicalName = canonicalName;
        this.aliases = aliases.clone();
    }

    private static void checkCanonicalName(String name) {
        if (name.isEmpty()) {
            throw new TIllegalCharsetNameException(name);
        }
        if (!isValidCharsetStart(name.charAt(0))) {
            throw new TIllegalCharsetNameException(name);
        }
        for (int i = 1; i < name.length(); ++i) {
            char c = name.charAt(i);
            switch (c) {
                case '-':
                case '+':
                case '.':
                case ':':
                case '_':
                    break;
                default:
                    if (!isValidCharsetStart(c)) {
                        throw new TIllegalCharsetNameException(name);
                    }
                    break;
            }
        }
    }

    private static boolean isValidCharsetStart(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    public static TCharset forName(String charsetName) {
        if (charsetName == null) {
            throw new IllegalArgumentException("charsetName is null");
        }
        checkCanonicalName(charsetName);
        TCharset charset = Charsets.value.get(charsetName.toUpperCase());
        if (charset == null) {
            throw new TUnsupportedCharsetException(charsetName);
        }
        return charset;
    }

    public static TCharset defaultCharset() {
        return Charsets.value.get("UTF-8");
    }

    public final String name() {
        return canonicalName;
    }

    public final Set<String> aliases() {
        if (aliasSet == null) {
            aliasSet = new HashSet<>();
            for (String alias : aliases) {
                aliasSet.add(alias);
            }
            aliasSet = Collections.unmodifiableSet(aliasSet);
        }
        return aliasSet;
    }

    public String displayName() {
        return canonicalName;
    }

    public abstract boolean contains(TCharset cs);

    public abstract TCharsetDecoder newDecoder();

    public abstract TCharsetEncoder newEncoder();

    public boolean canEncode() {
        return true;
    }

    public final TCharBuffer decode(TByteBuffer bb) {
        try {
            return newDecoder()
                    .onMalformedInput(TCodingErrorAction.REPLACE)
                    .onUnmappableCharacter(TCodingErrorAction.REPLACE)
                    .decode(bb);
        } catch (TCharacterCodingException e) {
            throw new AssertionError("Should never been thrown", e);
        }
    }

    public final TByteBuffer encode(TCharBuffer cb) {
        try {
            return newEncoder()
                    .onMalformedInput(TCodingErrorAction.REPLACE)
                    .onUnmappableCharacter(TCodingErrorAction.REPLACE)
                    .encode(cb);
        } catch (TCharacterCodingException e) {
            throw new AssertionError("Should never been thrown", e);
        }
    }

    public final TByteBuffer encode(String str) {
        return encode(TCharBuffer.wrap(str));
    }

    @Override
    public final int compareTo(TCharset that) {
        return canonicalName.compareToIgnoreCase(that.canonicalName);
    }

    static class Charsets {
        private static final Map<String, TCharset> value = new HashMap<>();

        static {
            value.put("UTF-8", new TUTF8Charset());
        }
    }
}
