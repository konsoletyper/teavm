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

/*
 *
 *  Portions, Copyright © 1991-2005 Unicode, Inc. The following applies to Unicode.
 *
 *  COPYRIGHT AND PERMISSION NOTICE
 *
 *  Copyright © 1991-2005 Unicode, Inc. All rights reserved. Distributed under
 *  the Terms of Use in http://www.unicode.org/copyright.html. Permission is
 *  hereby granted, free of charge, to any person obtaining a copy of the
 *  Unicode data files and any associated documentation (the "Data Files")
 *  or Unicode software and any associated documentation (the "Software")
 *  to deal in the Data Files or Software without restriction, including without
 *  limitation the rights to use, copy, modify, merge, publish, distribute,
 *  and/or sell copies of the Data Files or Software, and to permit persons
 *  to whom the Data Files or Software are furnished to do so, provided that
 *  (a) the above copyright notice(s) and this permission notice appear with
 *  all copies of the Data Files or Software, (b) both the above copyright
 *  notice(s) and this permission notice appear in associated documentation,
 *  and (c) there is clear notice in each modified Data File or in the Software
 *  as well as in the documentation associated with the Data File(s) or Software
 *  that the data or software has been modified.

 *  THE DATA FILES AND SOFTWARE ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 *  KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT
 *  OF THIRD PARTY RIGHTS. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS
 *  INCLUDED IN THIS NOTICE BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT
 *  OR CONSEQUENTIAL DAMAGES, OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
 *  OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
 *  OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE DATA FILES OR SOFTWARE.
 *
 *  Except as contained in this notice, the name of a copyright holder shall
 *  not be used in advertising or otherwise to promote the sale, use or other
 *  dealings in these Data Files or Software without prior written
 *  authorization of the copyright holder.
 *
 *  2. Additional terms from the Database:
 *
 *  Copyright © 1995-1999 Unicode, Inc. All Rights reserved.
 *
 *  Disclaimer
 *
 *  The Unicode Character Database is provided as is by Unicode, Inc.
 *  No claims are made as to fitness for any particular purpose. No warranties
 *  of any kind are expressed or implied. The recipient agrees to determine
 *  applicability of information provided. If this file has been purchased
 *  on magnetic or optical media from Unicode, Inc., the sole remedy for any claim
 *  will be exchange of defective media within 90 days of receipt. This disclaimer
 *  is applicable for all other data files accompanying the Unicode Character Database,
 *  some of which have been compiled by the Unicode Consortium, and some of which
 *  have been supplied by other sources.
 *
 *  Limitations on Rights to Redistribute This Data
 *
 *  Recipient is granted the right to make copies in any form for internal
 *  distribution and to freely use the information supplied in the creation of
 *  products supporting the UnicodeTM Standard. The files in
 *  the Unicode Character Database can be redistributed to third parties or other
 *  organizations (whether for profit or not) as long as this notice and the disclaimer
 *  notice are retained. Information can be extracted from these files and used
 *  in documentation or programs, as long as there is an accompanying notice
 *  indicating the source.
 */

package org.teavm.classlib.java.util.regex;

/**
 * Represents node accepting single supplementary codepoint.
 */
class TSupplCharSet extends TLeafSet {

    /*
     * UTF-16 encoding of this supplementary codepoint
     */
    private char high;

    private char low;

    // int value of this supplementary codepoint
    private int ch;

    public TSupplCharSet(int ch) {
        charCount = 2;
        this.ch = ch;
        char[] chUTF16 = Character.toChars(ch);
        high = chUTF16[0];

        /*
         * we suppose that SupplCharSet is build over supplementary codepoints
         * only
         */
        low = chUTF16[1];
    }

    @Override
    public int accepts(int strIndex, CharSequence testString) {
        char high = testString.charAt(strIndex++);
        char low = testString.charAt(strIndex);
        return ((this.high == high) && (this.low == low)) ? 2 : -1;
    }

    @Override
    public int find(int strIndex, CharSequence testString, TMatchResultImpl matchResult) {

        if (testString instanceof String) {
            String testStr = (String) testString;
            int strLength = matchResult.getRightBound();

            while (strIndex < strLength) {
                strIndex = testStr.indexOf(high, strIndex);
                if (strIndex < 0) {
                    return -1;
                }

                strIndex++;
                if (strIndex < strLength) {
                    char ch = testStr.charAt(strIndex);

                    if ((low == ch) && (next.matches(strIndex + 1, testString, matchResult) >= 0)) {
                        return --strIndex;
                    }
                    strIndex++;
                }
            }
            return -1;
        }

        return super.find(strIndex, testString, matchResult);
    }

    @Override
    public int findBack(int strIndex, int lastIndex, CharSequence testString, TMatchResultImpl matchResult) {

        if (testString instanceof String) {
            String testStr = (String) testString;

            while (lastIndex >= strIndex) {
                lastIndex = testStr.lastIndexOf(low, lastIndex);
                lastIndex--;
                if (lastIndex < 0 || lastIndex < strIndex) {
                    return -1;
                }

                if ((high == testStr.charAt(lastIndex)) && next.matches(lastIndex + 2, testString, matchResult) >= 0) {
                    return lastIndex;
                }

                lastIndex--;
            }
            return -1;
        }

        return super.findBack(strIndex, lastIndex, testString, matchResult);
    }

    @Override
    protected String getName() {
        return "" + high + low;
    }

    protected int getCodePoint() {
        return ch;
    }

    @Override
    public boolean first(TAbstractSet set) {
        if (set instanceof TSupplCharSet) {
            return ((TSupplCharSet) set).getCodePoint() == ch;
        } else if (set instanceof TSupplRangeSet) {
            return ((TSupplRangeSet) set).contains(ch);
        } else if (set instanceof TCharSet) {
            return false;
        } else if (set instanceof TRangeSet) {
            return false;
        }

        return true;
    }
}
