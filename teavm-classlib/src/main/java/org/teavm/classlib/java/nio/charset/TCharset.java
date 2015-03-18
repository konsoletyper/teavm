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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Alexey Andreev
 */
public abstract class TCharset implements Comparable<TCharset> {
    private String canonicalName;
    private String[] aliases;
    private Set<String> aliasSet;

    protected TCharset(String canonicalName, String[] aliases) {
        this.canonicalName = canonicalName;
        this.aliases = aliases.clone();
    }

    public static TCharset forName(String charsetName) {
        return null;
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

    public abstract TCharsetDecoder newDecoder();

    public abstract TCharsetEncoder newEncoder();

    public boolean canEncode() {
        return true;
    }
}
