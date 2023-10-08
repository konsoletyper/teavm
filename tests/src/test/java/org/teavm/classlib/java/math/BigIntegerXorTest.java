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
/**
 * @author Elena Semukhina
 */

package org.teavm.classlib.java.math;

import static org.junit.Assert.assertTrue;
import java.math.BigInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class BigIntegerXorTest {
    /**
     * Xor for zero and a positive number
     */
    @Test
    public void testZeroPos() {
        String numA = "0";
        String numB = "27384627835298756289327365";
        String res = "27384627835298756289327365";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }

    /**
     * Xor for zero and a negative number
     */
    @Test
    public void testZeroNeg() {
        String numA = "0";
        String numB = "-27384627835298756289327365";
        String res = "-27384627835298756289327365";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }

    /**
     * Xor for a positive number and zero
     */
    @Test
    public void testPosZero() {
        String numA = "27384627835298756289327365";
        String numB = "0";
        String res = "27384627835298756289327365";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }

    /**
     * Xor for a negative number and zero
     */
    @Test
    public void testNegPos() {
        String numA = "-27384627835298756289327365";
        String numB = "0";
        String res = "-27384627835298756289327365";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }

    /**
     * Xor for zero and zero
     */
    @Test
    public void testZeroZero() {
        String numA = "0";
        String numB = "0";
        String res = "0";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }

    /**
     * Xor for zero and one
     */
    @Test
    public void testZeroOne() {
        String numA = "0";
        String numB = "1";
        String res = "1";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }

    /**
     * Xor for one and one
     */
    @Test
    public void testOneOne() {
        String numA = "1";
        String numB = "1";
        String res = "0";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }

    /**
     * Xor for two positive numbers of the same length
     */
    @Test
    public void testPosPosSameLength() {
        String numA = "283746278342837476784564875684767";
        String numB = "293478573489347658763745839457637";
        String res = "71412358434940908477702819237626";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }

    /**
     * Xor for two positive numbers; the first is longer
     */
    @Test
    public void testPosPosFirstLonger() {
        String numA = "2837462783428374767845648748973847593874837948575684767";
        String numB = "293478573489347658763745839457637";
        String res = "2837462783428374767845615168483972194300564226167553530";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }

    /**
     * Xor for two positive numbers; the first is shorter
     */
    @Test
    public void testPosPosFirstShorter() {
        String numA = "293478573489347658763745839457637";
        String numB = "2837462783428374767845648748973847593874837948575684767";
        String res = "2837462783428374767845615168483972194300564226167553530";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }

    /**
     * Xor for two negative numbers of the same length
     */
    @Test
    public void testNegNegSameLength() {
        String numA = "-283746278342837476784564875684767";
        String numB = "-293478573489347658763745839457637";
        String res = "71412358434940908477702819237626";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }

    /**
     * Xor for two negative numbers; the first is longer
     */
    @Test
    public void testNegNegFirstLonger() {
        String numA = "-2837462783428374767845648748973847593874837948575684767";
        String numB = "-293478573489347658763745839457637";
        String res = "2837462783428374767845615168483972194300564226167553530";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }

    /**
     * Xor for two negative numbers; the first is shorter
     */
    @Test
    public void testNegNegFirstShorter() {
        String numA = "293478573489347658763745839457637";
        String numB = "2837462783428374767845648748973847593874837948575684767";
        String res = "2837462783428374767845615168483972194300564226167553530";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }

    /**
     * Xor for two numbers of different signs and the same length
     */
    @Test
    public void testPosNegSameLength() {
        String numA = "283746278342837476784564875684767";
        String numB = "-293478573489347658763745839457637";
        String res = "-71412358434940908477702819237628";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }

    /**
     * Xor for two numbers of different signs and the same length
     */
    @Test
    public void testNegPosSameLength() {
        String numA = "-283746278342837476784564875684767";
        String numB = "293478573489347658763745839457637";
        String res = "-71412358434940908477702819237628";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }

    /**
     * Xor for a negative and a positive numbers; the first is longer
     */
    @Test
    public void testNegPosFirstLonger() {
        String numA = "-2837462783428374767845648748973847593874837948575684767";
        String numB = "293478573489347658763745839457637";
        String res = "-2837462783428374767845615168483972194300564226167553532";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }

    /**
     * Xor for two negative numbers; the first is shorter
     */
    @Test
    public void testNegPosFirstShorter() {
        String numA = "-293478573489347658763745839457637";
        String numB = "2837462783428374767845648748973847593874837948575684767";
        String res = "-2837462783428374767845615168483972194300564226167553532";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }

    /**
     * Xor for a positive and a negative numbers; the first is longer
     */
    @Test
    public void testPosNegFirstLonger() {
        String numA = "2837462783428374767845648748973847593874837948575684767";
        String numB = "-293478573489347658763745839457637";
        String res = "-2837462783428374767845615168483972194300564226167553532";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }

    /**
     * Xor for a positive and a negative number; the first is shorter
     */
    @Test
    public void testPosNegFirstShorter() {
        String numA = "293478573489347658763745839457637";
        String numB = "-2837462783428374767845648748973847593874837948575684767";
        String res = "-2837462783428374767845615168483972194300564226167553532";
        BigInteger aNumber = new BigInteger(numA);
        BigInteger bNumber = new BigInteger(numB);
        BigInteger result = aNumber.xor(bNumber);
        assertTrue(res.equals(result.toString()));
    }
}
