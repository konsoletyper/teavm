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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.teavm.common.IntegerArray;

public final class UnicodeSupport {
    private static AtomicBoolean filled = new AtomicBoolean();
    private static volatile CountDownLatch latch = new CountDownLatch(1);
    private static int[] digitValues;
    private static byte[] classes;
    private static int[] titleCaseMapping;
    private static Map<String, Byte> classMap = new HashMap<>();

    static {
        classMap.put("Cn", Character.UNASSIGNED);
        classMap.put("Lu", Character.UPPERCASE_LETTER);
        classMap.put("Ll", Character.LOWERCASE_LETTER);
        classMap.put("Lt", Character.TITLECASE_LETTER);
        classMap.put("Lm", Character.MODIFIER_LETTER);
        classMap.put("Lo", Character.OTHER_LETTER);
        classMap.put("Mn", Character.NON_SPACING_MARK);
        classMap.put("Me", Character.ENCLOSING_MARK);
        classMap.put("Mc", Character.COMBINING_SPACING_MARK);
        classMap.put("Nd", Character.DECIMAL_DIGIT_NUMBER);
        classMap.put("Nl", Character.LETTER_NUMBER);
        classMap.put("No", Character.OTHER_NUMBER);
        classMap.put("Zs", Character.SPACE_SEPARATOR);
        classMap.put("Zl", Character.LINE_SEPARATOR);
        classMap.put("Zp", Character.PARAGRAPH_SEPARATOR);
        classMap.put("Cc", Character.CONTROL);
        classMap.put("Cf", Character.FORMAT);
        classMap.put("Co", Character.PRIVATE_USE);
        classMap.put("Cs", Character.SURROGATE);
        classMap.put("Pd", Character.DASH_PUNCTUATION);
        classMap.put("Ps", Character.START_PUNCTUATION);
        classMap.put("Pe", Character.END_PUNCTUATION);
        classMap.put("Pc", Character.CONNECTOR_PUNCTUATION);
        classMap.put("Po", Character.OTHER_PUNCTUATION);
        classMap.put("Sm", Character.MATH_SYMBOL);
        classMap.put("Sc", Character.CURRENCY_SYMBOL);
        classMap.put("Sk", Character.MODIFIER_SYMBOL);
        classMap.put("So", Character.OTHER_SYMBOL);
        classMap.put("Pi", Character.INITIAL_QUOTE_PUNCTUATION);
        classMap.put("Pf", Character.FINAL_QUOTE_PUNCTUATION);
    }

    private UnicodeSupport() {
    }

    private static void parseUnicodeData() {
        IntegerArray digitValues = new IntegerArray(4096);
        IntegerArray classes = new IntegerArray(65536);
        IntegerArray titleCaseMapping = new IntegerArray(256);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(UnicodeHelper.class
                .getResourceAsStream("UnicodeData.txt")))) {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.isEmpty()) {
                    continue;
                }
                String[] fields = splitLine(line);
                int charCode = parseHex(fields[0]);
                while (classes.size() < charCode) {
                    classes.add(0);
                }
                if (!fields[6].isEmpty()) {
                    int digit = Integer.parseInt(fields[6]);
                    digitValues.add(charCode);
                    digitValues.add(digit);
                }
                Byte charClass = classMap.get(fields[2]);
                classes.add(charClass != null ? charClass.intValue() : 0);

                if (!fields[14].isEmpty()) {
                    int titleCaseCode = parseHex(fields[14]);
                    if (fields[12].isEmpty() || parseHex(fields[12]) != titleCaseCode) {
                        titleCaseMapping.add(charCode);
                        titleCaseMapping.add(titleCaseCode);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading unicode data", e);
        }
        IntegerArray letterDigitValues = new IntegerArray(256);
        for (int i = 'A'; i <= 'Z'; ++i) {
            letterDigitValues.add(i);
            letterDigitValues.add(i - 'A' + 10);
        }
        for (int i = 'a'; i <= 'z'; ++i) {
            letterDigitValues.add(i);
            letterDigitValues.add(i - 'a' + 10);
        }
        for (int i = '\uFF21'; i <= '\uFF3A'; ++i) {
            letterDigitValues.add(i);
            letterDigitValues.add(i - '\uFF21' + 10);
        }
        for (int i = '\uFF41'; i <= '\uFF5A'; ++i) {
            letterDigitValues.add(i);
            letterDigitValues.add(i - '\uFF41' + 10);
        }
        UnicodeSupport.digitValues = mergePairs(digitValues.getAll(), letterDigitValues.getAll());
        UnicodeSupport.classes = new byte[classes.size()];
        for (int i = 0; i < classes.size(); ++i) {
            UnicodeSupport.classes[i] = (byte) classes.get(i);
        }
        UnicodeSupport.titleCaseMapping = titleCaseMapping.getAll();
    }

    private static String[] splitLine(String line) {
        List<String> parts = new ArrayList<>();
        int index = 0;
        while (true) {
            int next = line.indexOf(';', index);
            if (next == -1) {
                break;
            }
            parts.add(line.substring(index, next));
            index = next + 1;
        }
        parts.add(line.substring(index));
        return parts.toArray(new String[parts.size()]);
    }

    private static int[] mergePairs(int[] a, int[] b) {
        int[] result = new int[a.length + b.length];
        int i = 0;
        int j = 0;
        int t = 0;
        while (true) {
            if (j == b.length) {
                while (i < a.length) {
                    result[t++] = a[i++];
                }
                break;
            } else if (i == a.length) {
                while (j < b.length) {
                    result[t++] = b[j++];
                }
                break;
            }
            if (a[i] < b[j]) {
                result[t++] = a[i++];
                result[t++] = a[i++];
            } else {
                result[t++] = b[j++];
                result[t++] = b[j++];
            }
        }
        return result;
    }

    private static int parseHex(String text) {
        int value = 0;
        for (int i = 0; i < text.length(); ++i) {
            value = value << 4 | UnicodeHelper.valueOfHexDigit(text.charAt(i));
        }
        return value;
    }

    private static void ensureUnicodeData() {
        if (filled.compareAndSet(false, true)) {
            parseUnicodeData();
            latch.countDown();
            latch = null;
        } else {
            CountDownLatch latchCopy = latch;
            if (latchCopy != null) {
                try {
                    latchCopy.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public static int[] getDigitValues() {
        ensureUnicodeData();
        return digitValues;
    }

    public static byte[] getClasses() {
        ensureUnicodeData();
        return classes;
    }

    public static int[] getTitleCaseMapping() {
        ensureUnicodeData();
        return titleCaseMapping;
    }
}
