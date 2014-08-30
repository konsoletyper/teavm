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
package org.teavm.samples;

import java.io.IOException;

/**
 *
 * @author Alexey Andreev
 */
public final class MatrixMultiplication {
    private MatrixMultiplication() {
    }

    public static void main(String[] args) throws IOException {
        for (int k = 0; k < 20; ++k) {
            long startTime = System.currentTimeMillis();

            Matrix m1 = new Matrix(5);
            Matrix m2 = new Matrix(5);

            m1.generateData();
            m2.generateData();

            Matrix res = null;
            for (int i = 0; i < 10000; i++) {
                res = m1.multiply(m2);
                m1 = res;
            }
            StringBuilder sb = new StringBuilder();
            res.printOn(sb);
            long timeSpent = System.currentTimeMillis() - startTime;
            System.out.println(sb.toString());
            System.out.println("Time spent: " + timeSpent);
        }
    }
}
