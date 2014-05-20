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
package org.teavm.classlib.impl.unicode;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class CLDRHelper {
    public static String getCode(String language, String country) {
        return !country.isEmpty() ? language + "-" + country : language;
    }

    public static String getLikelySubtags(String localeCode) {
        readLikelySubtagsFromCLDR();
        String subtags = getLikelySubtagsImpl(localeCode);
        return subtags != null ? subtags : localeCode;
    }

    // Defined by JCLPlugin
    private static native void readLikelySubtagsFromCLDR();

    // TODO: implement using CLDR
    private static native String getLikelySubtagsImpl(String localeCode);
}
