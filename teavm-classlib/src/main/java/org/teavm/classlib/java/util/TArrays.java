/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.classlib.java.util;

import org.teavm.classlib.java.lang.TMath;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.TString;
import org.teavm.classlib.java.lang.TStringBuilder;

/**
 *
 * @author Alexey Andreev
 */
public class TArrays extends TObject {
    public static char[] copyOf(char[] array, int length) {
        char[] result = new char[length];
        int sz = TMath.min(length, array.length);
        for (int i = 0; i < sz; ++i) {
            result[i] = array[i];
        }
        return result;
    }

    public static byte[] copyOf(byte[] array, int length) {
        byte[] result = new byte[length];
        int sz = TMath.min(length, array.length);
        for (int i = 0; i < sz; ++i) {
            result[i] = array[i];
        }
        return result;
    }

    public static TString toString(TObject[] a) {
        TStringBuilder sb = new TStringBuilder();
        sb.append(TString.wrap("["));
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(TString.wrap(", "));
            }
            sb.append(a[i]);
        }
        sb.append(TString.wrap("]"));
        return TString.wrap(sb.toString());
    }

    public static TString toString(boolean[] a) {
        TStringBuilder sb = new TStringBuilder();
        sb.append(TString.wrap("["));
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(TString.wrap(", "));
            }
            sb.append(a[i]);
        }
        sb.append(TString.wrap("]"));
        return TString.wrap(sb.toString());
    }

    public static TString toString(byte[] a) {
        TStringBuilder sb = new TStringBuilder();
        sb.append(TString.wrap("["));
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(TString.wrap(", "));
            }
            sb.append(a[i]);
        }
        sb.append(TString.wrap("]"));
        return TString.wrap(sb.toString());
    }

    public static TString toString(short[] a) {
        TStringBuilder sb = new TStringBuilder();
        sb.append(TString.wrap("["));
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(TString.wrap(", "));
            }
            sb.append(a[i]);
        }
        sb.append(TString.wrap("]"));
        return TString.wrap(sb.toString());
    }

    public static TString toString(char[] a) {
        TStringBuilder sb = new TStringBuilder();
        sb.append(TString.wrap("["));
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(TString.wrap(", "));
            }
            sb.append(a[i]);
        }
        sb.append(TString.wrap("]"));
        return TString.wrap(sb.toString());
    }

    public static TString toString(int[] a) {
        TStringBuilder sb = new TStringBuilder();
        sb.append(TString.wrap("["));
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(TString.wrap(", "));
            }
            sb.append(a[i]);
        }
        sb.append(TString.wrap("]"));
        return TString.wrap(sb.toString());
    }

    public static TString toString(long[] a) {
        TStringBuilder sb = new TStringBuilder();
        sb.append(TString.wrap("["));
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(TString.wrap(", "));
            }
            sb.append(a[i]);
        }
        sb.append(TString.wrap("]"));
        return TString.wrap(sb.toString());
    }

    public static TString toString(float[] a) {
        TStringBuilder sb = new TStringBuilder();
        sb.append(TString.wrap("["));
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(TString.wrap(", "));
            }
            sb.append(a[i]);
        }
        sb.append(TString.wrap("]"));
        return TString.wrap(sb.toString());
    }

    public static TString toString(double[] a) {
        TStringBuilder sb = new TStringBuilder();
        sb.append(TString.wrap("["));
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(TString.wrap(", "));
            }
            sb.append(a[i]);
        }
        sb.append(TString.wrap("]"));
        return TString.wrap(sb.toString());
    }
}
