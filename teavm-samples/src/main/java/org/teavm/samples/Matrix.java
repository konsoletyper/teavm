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
// This file is based on the original source code from Bck2Brwsr
/**
 * Back 2 Browser Bytecode Translator
 * Copyright (C) 2012 Jaroslav Tulach <jaroslav.tulach@apidesign.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://opensource.org/licenses/GPL-2.0.
 */
package org.teavm.samples;

import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author Alexey Andreev
 */
public class Matrix {
    private final int rank;
    private final float data[][];

    public Matrix(int r) {
        this(r, new float[r][r]);
    }

    private Matrix(int r, float[][] data) {
        this.rank = r;
        this.data = data;
    }

    public void setElement(int i, int j, float value) {
        data[i][j] = value;
    }
    public float getElement(int i, int j) {
        return data[i][j];
    }

    public void generateData() {
        //final Random rand = new Random();
        //final int x = 10;
        for (int i = 0; i < rank; i++) {
            for (int j = 0; j < rank; j++) {
                data[i][j] = 1 / (1 + i + j);
            }
        }
    }

    public Matrix multiply(Matrix m) {
        if (rank != m.rank) {
            throw new IllegalArgumentException("Rank doesn't match");
        }

        final float res[][] = new float[rank][rank];
        for (int i = 0; i < rank; i++) {
            for (int j = 0; j < rank; j++) {
                float ij = 0;
                for (int q = 0; q < rank; q++) {
                    ij += data[i][q] * m.data[q][j];
                }
                res[i][j] = ij;
            }
        }
        return new Matrix(rank, res);
    }

    public void printOn(Appendable s) throws IOException {
        for (int i = 0; i < rank; i++) {
            String sep = "";
            for (int j = 0; j < rank; j++) {
                s.append(sep + data[i][j]);
                sep = " ";
            }
            s.append("\n");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Matrix) {
            Matrix snd = (Matrix)obj;
            if (snd.rank != rank) {
                return false;
            }
            for (int i = 0; i < rank; i++) {
                for (int j = 0; j < rank; j++) {
                    if (data[i][j] != snd.data[i][j]) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + this.rank;
        hash = 97 * hash + Arrays.deepHashCode(this.data);
        return hash;
    }
}
