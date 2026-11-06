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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class DecimalFormatContract {
    private static boolean failed;

    private DecimalFormatContract() {
    }

    public static void main(String[] args) {
        DecimalFormat format = new DecimalFormat("#,##0.###", DecimalFormatSymbols.getInstance(Locale.US));
        check("".equals(format.getPositivePrefix()), "omitted positive prefix defaults to empty");
        check("".equals(format.getPositiveSuffix()), "omitted positive suffix defaults to empty");
        check("".equals(format.getNegativeSuffix()), "omitted negative suffix defaults to empty");
        check("-".equals(format.getNegativePrefix()), "omitted negative prefix defaults to minus sign");

        format.applyPattern("#,##0.###;(#,##0.###)");
        check("".equals(format.getPositiveSuffix()), "explicit negative subpattern keeps empty positive suffix");
        check("(".equals(format.getNegativePrefix()), "explicit negative prefix");
        check(")".equals(format.getNegativeSuffix()), "explicit negative suffix");

        format.applyPattern("#,##0.###");
        check("".equals(format.getPositiveSuffix()), "reapplied omitted positive suffix defaults to empty");
        check("".equals(format.getNegativeSuffix()), "reapplied omitted negative suffix defaults to empty");
        check("-1,234.5".equals(format.format(-1234.5)), "default negative formatting has no suffix");

        format.setPositivePrefix("pos:");
        format.setPositiveSuffix(":end");
        format.setNegativePrefix("neg:");
        format.setNegativeSuffix(":end");
        check("pos:".equals(format.getPositivePrefix()), "positive prefix setter");
        check(":end".equals(format.getPositiveSuffix()), "positive suffix setter");
        check("neg:".equals(format.getNegativePrefix()), "negative prefix setter");
        check(":end".equals(format.getNegativeSuffix()), "negative suffix setter");

        format.setPositivePrefix(null);
        check(format.getPositivePrefix() == null, "null positive prefix setter remains null");

        format.applyPattern("#,##0.###");
        format.setPositiveSuffix(null);
        check(format.getPositiveSuffix() == null, "null positive suffix setter remains null");

        format.applyPattern("#,##0.###");
        format.setNegativePrefix(null);
        check(format.getNegativePrefix() == null, "null negative prefix setter remains null");

        format.applyPattern("#,##0.###");
        format.setNegativeSuffix(null);
        check(format.getNegativeSuffix() == null, "null negative suffix setter remains null");

        if (failed) return;
        System.out.println("DecimalFormat affix contract passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            failed = true;
            System.out.println("DecimalFormat affix contract failed: " + message);
        }
    }
}
