/*
 *  Copyright 2020 Alexey Andreev.
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
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.threeten.bp.format;

/**
 * Enumeration of the style of text formatting and parsing.
 * <p>
 * Text styles define three sizes for the formatted text - 'full', 'short' and 'narrow'.
 * Each of these three sizes is available in both 'standard' and 'stand-alone' variations.
 * <p>
 * The difference between the three sizes is obvious in most languages.
 * For example, in English the 'full' month is 'January', the 'short' month is 'Jan'
 * and the 'narrow' month is 'J'. Note that the narrow size is often not unique.
 * For example, 'January', 'June' and 'July' all have the 'narrow' text 'J'.
 * <p>
 * The difference between the 'standard' and 'stand-alone' forms is trickier to describe
 * as there is no difference in English. However, in other languages there is a difference
 * in the word used when the text is used alone, as opposed to in a complete date.
 * For example, the word used for a month when used alone in a date picker is different
 * to the word used for month in association with a day and year in a date.
 *
 * <h3>Specification for implementors</h3>
 * This is immutable and thread-safe enum.
 */
public enum TextStyle {
    // ordered from large to small

    /**
     * Full text, typically the full description.
     * For example, day-of-week Monday might output "Monday".
     */
    FULL,
    /**
     * Full text for stand-alone use, typically the full description.
     * For example, day-of-week Monday might output "Monday".
     */
    FULL_STANDALONE,
    /**
     * Short text, typically an abbreviation.
     * For example, day-of-week Monday might output "Mon".
     */
    SHORT,
    /**
     * Short text for stand-alone use, typically an abbreviation.
     * For example, day-of-week Monday might output "Mon".
     */
    SHORT_STANDALONE,
    /**
     * Narrow text, typically a single letter.
     * For example, day-of-week Monday might output "M".
     */
    NARROW,
    /**
     * Narrow text for stand-alone use, typically a single letter.
     * For example, day-of-week Monday might output "M".
     */
    NARROW_STANDALONE;

    /**
     * Checks if the style is stand-alone.
     * 
     * @return true if the style is stand-alone
     */
    public boolean isStandalone() {
        return (ordinal() & 1) == 1;
    }

    /**
     * Converts the style to the equivalent stand-alone style.
     * 
     * @return the matching stand-alone style
     */
    public TextStyle asStandalone() {
        return TextStyle.values()[ordinal()  | 1];
    }

    /**
     * Converts the style to the equivalent normal style.
     *
     * @return the matching normal style
     */
    public TextStyle asNormal() {
        return TextStyle.values()[ordinal() & ~1];
    }

}
