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

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.UUID;

public final class JdkApiContract {
    private static float staticValue = 6.5f;
    private float sample = 1.25f;
    private byte byteValue = 2;
    private short shortValue = 3;
    private char charValue = 'A';
    private int integer = 7;
    private long longValue = 9L;
    private boolean booleanValue = true;
    private double doubleValue = 2.5;

    private JdkApiContract() {
    }

    public static void main(String[] args) throws Exception {
        check(Charset.isSupported("UTF-8"), "UTF-8 supported");
        check(!Charset.isSupported("x-unsupported-charset"), "unknown charset unsupported");
        try {
            Charset.isSupported("bad name");
            throw new AssertionError("illegal charset name accepted");
        } catch (IllegalCharsetNameException expected) {
            // Expected.
        }
        try {
            Charset.isSupported(null);
            throw new AssertionError("null charset name accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }

        UUID uuid = new UUID(0x123456789abcdef0L, 0xfedcba9876543210L);
        check(uuid.toString().equals("12345678-9abc-def0-fedc-ba9876543210"), "UUID bit layout");
        check(new UUID(0, 0).toString().equals("00000000-0000-0000-0000-000000000000"), "UUID zero padding");

        JdkApiContract target = new JdkApiContract();
        Field field = JdkApiContract.class.getDeclaredField("sample");
        field.setAccessible(true);
        check(field.getFloat(target) == 1.25f, "float field read");
        check(JdkApiContract.class.getDeclaredField("byteValue").getFloat(target) == 2f, "byte widening");
        check(JdkApiContract.class.getDeclaredField("shortValue").getFloat(target) == 3f, "short widening");
        check(JdkApiContract.class.getDeclaredField("charValue").getFloat(target) == 65f, "char widening");
        check(JdkApiContract.class.getDeclaredField("integer").getFloat(target) == 7f, "int widening");
        check(JdkApiContract.class.getDeclaredField("longValue").getFloat(target) == 9f, "long widening");
        check(JdkApiContract.class.getDeclaredField("staticValue").getFloat(null) == 6.5f, "static field read");
        try {
            JdkApiContract.class.getDeclaredField("booleanValue").getFloat(target);
            throw new AssertionError("boolean field converted to float");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
        try {
            field.getFloat(new Object());
            throw new AssertionError("wrong receiver accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
        try {
            field.getFloat(null);
            throw new AssertionError("null receiver accepted");
        } catch (NullPointerException expected) {
            // Expected.
        }
        try {
            JdkApiContract.class.getDeclaredField("doubleValue").getFloat(target);
            throw new AssertionError("double field narrowed to float");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
        System.out.println("JDK API contract passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
