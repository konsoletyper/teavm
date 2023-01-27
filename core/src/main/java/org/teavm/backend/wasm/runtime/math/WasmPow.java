/*
 *  Copyright 2022 Alexey Andreev.
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

package org.teavm.backend.wasm.runtime.math;

// Ported from musl
public final class WasmPow {
    private static final long OFF = 0x3fe6955500000000L;
    private static final int POW_LOG_TABLE_BITS = 7;
    private static final int POW_N = 1 << POW_LOG_TABLE_BITS;
    private static final double LN2HI = 0x1.62e42fefa3800p-1;
    private static final double LN2LO = 0x1.ef35793c76730p-45;

    private static final int EXP_TABLE_BITS = 7;
    private static final int EXP_POLY_ORDER = 5;
    private static final int EXP_N = 1 << EXP_TABLE_BITS;
    private static final double INVLN2N = 0x1.71547652b82fep0 * EXP_N;
    private static final double NEGLN2HIN = -0x1.62e42fefa0000p-8;
    private static final double NEGLN2LON = -0x1.cf79abc9e3b3ap-47;
    private static final double Shift = 0x1.8p52;

    static class DoubleResult {
        double value;
    }

    private WasmPow() {
    }

    private static int top12(double x) {
        return (int) (Double.doubleToLongBits(x) >> 52);
    }

    private static double logInline(long ix, DoubleResult tail) {
        /* double_t for better performance on targets with FLT_EVAL_METHOD==2.  */
        double z;
        double r;
        double y;
        double invc;
        double logc;
        double logctail;
        double kd;
        double hi;
        double t1;
        double t2;
        double lo;
        double lo1;
        double lo2;
        double p;
        long iz;
        long tmp;
        int k;
        int i;

        /* x = 2^k z; where z is in range [OFF,2*OFF) and exact.
           The range is split into N subintervals.
           The ith subinterval contains z and c is near its center.  */
        tmp = ix - OFF;
        i = (int) ((tmp >>> (52 - POW_LOG_TABLE_BITS)) % POW_N);
        k = (int) (tmp >> 52);
        iz = ix - (tmp & 0xfffL << 52);
        z = Double.longBitsToDouble(iz);
        kd = k;

        /* log(x) = k*Ln2 + log(c) + log1p(z/c-1).  */
        invc = tab[i].invc;
        logc = tab[i].logc;
        logctail = tab[i].logctail;

        /* Split z such that rhi, rlo and rhi*rhi are exact and |rlo| <= |r|.  */
        double zhi = Double.longBitsToDouble((iz + (1L << 31)) & (-1L << 32));
        double zlo = z - zhi;
        double rhi = zhi * invc - 1.0;
        double rlo = zlo * invc;
        r = rhi + rlo;

        /* k*Ln2 + log(c) + r.  */
        t1 = kd * LN2HI + logc;
        t2 = t1 + r;
        lo1 = kd * LN2LO + logctail;
        lo2 = t1 - t2 + r;

        /* Evaluation is optimized assuming superscalar pipelined execution.  */
        double ar;
        double ar2;
        double ar3;
        double lo3;
        double lo4;
        ar = poly[0] * r; /* A[0] = -0.5.  */
        ar2 = r * ar;
        ar3 = r * ar2;
        /* k*Ln2 + log(c) + r + A[0]*r*r.  */
        double arhi = poly[0] * rhi;
        double arhi2 = rhi * arhi;
        hi = t2 + arhi2;
        lo3 = rlo * (ar + arhi);
        lo4 = t2 - hi + arhi2;
        /* p = log1p(r) - r - A[0]*r*r.  */
        p = ar3 * (poly[1] + r * poly[2] + ar2 * (poly[3] + r * poly[4] + ar2 * (poly[5] + r * poly[6])));
        lo = lo1 + lo2 + lo3 + lo4 + p;
        y = hi + lo;
        tail.value = hi - y + lo;
        return y;
    }

    private static double specialcase(double tmp, long sbits, long ki) {
        double scale;
        double y;

        if ((ki & 0x80000000L) == 0) {
            /* k > 0, the exponent of scale might have overflowed by <= 460.  */
            sbits -= 1009L << 52;
            scale = Double.longBitsToDouble(sbits);
            y = 0x1p1009 * (scale + scale * tmp);
            return y;
        }
        /* k < 0, need special care in the subnormal range.  */
        sbits += 1022L << 52;
        /* Note: sbits is signed scale.  */
        scale = Double.longBitsToDouble(sbits);
        y = scale + scale * tmp;
        if (Math.abs(y) < 1.0) {
            /* Round y to the right precision before scaling it into the subnormal
               range to avoid double rounding that can cause 0.5+E/2 ulp error where
               E is the worst-case ulp error outside the subnormal range.  So this
               is only useful if the goal is better than 1 ulp worst-case error.  */
            double hi;
            double lo;
            double one = 1.0;
            if (y < 0.0) {
                one = -1.0;
            }
            lo = scale - y + scale * tmp;
            hi = one + y;
            lo = one - hi + y + lo;
            y = (hi + lo) - one;
            /* Fix the sign of 0.  */
            if (y == 0.0) {
                y = Double.longBitsToDouble(sbits & 0x8000000000000000L);
            }
        }
        y = 0x1p-1022 * y;
        return y;
    }

    /* Computes sign*exp(x+xtail) where |xtail| < 2^-8/N and |xtail| <= |x|.
       The sign_bias argument is SIGN_BIAS or 0 and sets the sign to -1 or 1.  */
    private static double expInline(double x, double xtail, boolean signBias) {
        int abstop;
        long ki;
        long idx;
        long top;
        long sbits;
        /* double_t for better performance on targets with FLT_EVAL_METHOD==2.  */
        double kd;
        double z;
        double r;
        double r2;
        double scale;
        double tail;
        double tmp;

        abstop = top12(x) & 0x7ff;
        if (abstop - top12(0x1p-54) >= top12(512.0) - top12(0x1p-54)) {
            if (abstop - top12(0x1p-54) >= 0x80000000) {
                /* Avoid spurious underflow for tiny x.  */
                /* Note: 0 is common input.  */
                double one = 1.0 + x;
                return signBias ? -one : 1.0 + x;
            }
            if (abstop >= top12(1024.0)) {
                /* Note: inf and nan are already handled.  */
                if ((Double.doubleToLongBits(x) >>> 63) != 0) {
                    return mathUflow(signBias);
                } else {
                    return mathOflow(signBias);
                }
            }
            /* Large x is special cased below.  */
            abstop = 0;
        }

        /* exp(x) = 2^(k/N) * exp(r), with exp(r) in [2^(-1/2N),2^(1/2N)].  */
        /* x = ln2/N*k + r, with int k and r in [-ln2/2N, ln2/2N].  */
        z = INVLN2N * x;

        /* z - kd is in [-1, 1] in non-nearest rounding modes.  */
        kd = z + Shift;
        ki = Double.doubleToLongBits(kd);
        kd -= Shift;

        r = x + kd * NEGLN2HIN + kd * NEGLN2LON;
        /* The code assumes 2^-200 < |xtail| < 2^-8/N.  */
        r += xtail;
        /* 2^(k/N) ~= scale * (1 + tail).  */
        idx = 2 * (ki % EXP_N);
        top = (ki + (signBias ? 1 : 0)) << (52 - EXP_TABLE_BITS);
        tail = Double.longBitsToDouble(EXP_TAB[(int) idx]);
        /* This is only a valid scale when -1023*N < k < 1024*N.  */
        sbits = EXP_TAB[(int) idx + 1] + top;
        /* exp(x) = 2^(k/N) * exp(r) ~= scale + scale * (tail + exp(r) - 1).  */
        /* Evaluation is optimized assuming superscalar pipelined execution.  */
        r2 = r * r;
        /* Without fma the worst case error is 0.25/N ulp larger.  */
        /* Worst case error is less than 0.5+1.11/N+(abs poly error * 2^53) ulp.  */
        tmp = tail + r + r2 * (C2 + r * C3) + r2 * r2 * (C4 + r * C5);
        if (abstop == 0) {
            return specialcase(tmp, sbits, ki);
        }
        scale = Double.longBitsToDouble(sbits);
        /* Note: tmp == 0 or |tmp| > 2^-200 and scale > 2^-739, so there
           is no spurious underflow here even without fma.  */
        return scale + scale * tmp;
    }

    private static double mathOflow(boolean sign) {
        return mathXflow(sign, 0x1p769);
    }

    private static double mathUflow(boolean sign) {
        return mathXflow(sign, 0x1p-767);
    }

    private static double mathXflow(boolean sign, double y) {
        return (sign ? -y : y) * y;
    }

    /* Returns 0 if not int, 1 if odd int, 2 if even int.  The argument is
       the bit representation of a non-zero finite floating-point value.  */
    private static int checkint(long iy) {
        int e = (int) (iy >>> 52 & 0x7ff);
        if (e < 0x3ff) {
            return 0;
        }
        if (e > 0x3ff + 52) {
            return 2;
        }
        if ((iy & ((1L << (0x3ff + 52 - e)) - 1)) != 0) {
            return 0;
        }
        if ((iy & (1L << (0x3ff + 52 - e))) != 0) {
            return 1;
        }
        return 2;
    }

    /* Returns 1 if input is the bit representation of 0, infinity or nan.  */
    private static boolean zeroinfnan(long i) {
        return 2 * i - 1 >= 2 * Double.doubleToLongBits(Double.POSITIVE_INFINITY) - 1;
    }

    public static double pow(double x, double y) {
        boolean signBias = false;
        long ix;
        long iy;
        int topx;
        int topy;

        ix = Double.doubleToLongBits(x);
        iy = Double.doubleToLongBits(y);
        topx = top12(x);
        topy = top12(y);
        if (topx - 0x001 >= 0x7ff - 0x001 || (topy & 0x7ff) - 0x3be >= 0x43e - 0x3be) {
            /* Note: if |y| > 1075 * ln2 * 2^53 ~= 0x1.749p62 then pow(x,y) = inf/0
               and if |y| < 2^-54 / 1075 ~= 0x1.e7b6p-65 then pow(x,y) = +-1.  */
            /* Special cases: (x < 0x1p-126 or inf or nan) or
               (|y| < 0x1p-65 or |y| >= 0x1p63 or nan).  */
            if (zeroinfnan(iy)) {
                if (2 * iy == 0) {
                    return 1.0;
                }
                if (ix == Double.doubleToLongBits(1.0)) {
                    return 1.0;
                }
                if (2 * ix > 2 * Double.doubleToLongBits(Double.POSITIVE_INFINITY)
                        || 2 * iy > 2 * Double.doubleToLongBits(Double.POSITIVE_INFINITY)) {
                    return x + y;
                }
                if (2 * ix == 2 * Double.doubleToLongBits(1.0)) {
                    return 1.0;
                }
                if ((2 * ix < 2 * Double.doubleToLongBits(1.0)) == ((iy >>> 63) == 0)) {
                    return 0.0; /* |x|<1 && y==inf or |x|>1 && y==-inf.  */
                }
                return y * y;
            }
            if (zeroinfnan(ix)) {
                double x2 = x * x;
                if ((ix >>> 63) != 0 && checkint(iy) == 1) {
                    x2 = -x2;
                }
                /* Without the barrier some versions of clang hoist the 1/x2 and
                   thus division by zero exception can be signaled spuriously.  */
                return (iy >>> 63) != 0 ? 1 / x2 : x2;
            }
            /* Here x and y are non-zero finite.  */
            if ((ix >>> 63) != 0) {
                /* Finite x < 0.  */
                int yint = checkint(iy);
                if (yint == 0) {
                    return x;
                }
                if (yint == 1) {
                    signBias = true;
                }
                ix &= 0x7fffffffffffffffL;
                topx &= 0x7ff;
            }
            if ((topy & 0x7ff) - 0x3be >= 0x43e - 0x3be) {
                /* Note: sign_bias == 0 here because y is not odd.  */
                if (ix == Double.doubleToLongBits(1.0)) {
                    return 1.0;
                }
                if ((topy & 0x7ff) < 0x3be) {
                    /* |y| < 2^-65, x^y ~= 1 + y*log(x).  */
                    return 1.0;
                }
                return (ix > Double.doubleToLongBits(1.0)) == (topy < 0x800) ? mathOflow(false) : mathUflow(false);
            }
            if (topx == 0) {
                /* Normalize subnormal x so exponent becomes negative.  */
                ix = Double.doubleToLongBits(x * 0x1p52);
                ix &= 0x7fffffffffffffffL;
                ix -= 52L << 52;
            }
        }

        DoubleResult lo = new DoubleResult();
        double hi = logInline(ix, lo);
        double ehi;
        double elo;
        double yhi = Double.longBitsToDouble(iy & -1L << 27);
        double ylo = y - yhi;
        double lhi = Double.longBitsToDouble(Double.doubleToLongBits(hi) & -1L << 27);
        double llo = hi - lhi + lo.value;
        ehi = yhi * lhi;
        elo = ylo * lhi + y * llo; /* |elo| < |ehi| * 2^-25.  */
        return expInline(ehi, elo, signBias);
    }

    static class TableEntry {
        double invc;
        double pad;
        double logc;
        double logctail;
    }
    
    private static TableEntry a(double a, double b, double c) {
        TableEntry entry = new TableEntry();
        entry.invc = a;
        entry.logc = b;
        entry.logctail = c;
        return entry;
    }

    private static double[] poly = {
            -0x1p-1,
            0x1.555555555556p-2 * -2,
            -0x1.0000000000006p-2 * -2,
            0x1.999999959554ep-3 * 4,
            -0x1.555555529a47ap-3 * 4,
            0x1.2495b9b4845e9p-3 * -8,
            -0x1.0002b8b263fc3p-3 * -8
    };

    /* Algorithm:
        x = 2^k z
        log(x) = k ln2 + log(c) + log(z/c)
        log(z/c) = poly(z/c - 1)
    where z is in [0x1.69555p-1; 0x1.69555p0] which is split into N subintervals
    and z falls into the ith one, then table entries are computed as
        tab[i].invc = 1/c
        tab[i].logc = round(0x1p43*log(c))/0x1p43
        tab[i].logctail = (double)(log(c) - logc)
    where c is chosen near the center of the subinterval such that 1/c has only a
    few precision bits so z/c - 1 is exactly representible as double:
        1/c = center < 1 ? round(N/center)/N : round(2*N/center)/N/2
    Note: |z/c - 1| < 1/N for the chosen c, |log(c) - logc - logctail| < 0x1p-97,
    the last few bits of logc are rounded away so k*ln2hi + logc has no rounding
    error and the interval for z is selected such that near x == 1, where log(x)
    is tiny, large cancellation error is avoided in logc + poly(z/c - 1).  */
    private static TableEntry[] tab = {
            a(0x1.6a00000000000p+0, -0x1.62c82f2b9c800p-2, 0x1.ab42428375680p-48),
            a(0x1.6800000000000p+0, -0x1.5d1bdbf580800p-2, -0x1.ca508d8e0f720p-46),
            a(0x1.6600000000000p+0, -0x1.5767717455800p-2, -0x1.362a4d5b6506dp-45),
            a(0x1.6400000000000p+0, -0x1.51aad872df800p-2, -0x1.684e49eb067d5p-49),
            a(0x1.6200000000000p+0, -0x1.4be5f95777800p-2, -0x1.41b6993293ee0p-47),
            a(0x1.6000000000000p+0, -0x1.4618bc21c6000p-2, 0x1.3d82f484c84ccp-46),
            a(0x1.5e00000000000p+0, -0x1.404308686a800p-2, 0x1.c42f3ed820b3ap-50),
            a(0x1.5c00000000000p+0, -0x1.3a64c55694800p-2, 0x1.0b1c686519460p-45),
            a(0x1.5a00000000000p+0, -0x1.347dd9a988000p-2, 0x1.5594dd4c58092p-45),
            a(0x1.5800000000000p+0, -0x1.2e8e2bae12000p-2, 0x1.67b1e99b72bd8p-45),
            a(0x1.5600000000000p+0, -0x1.2895a13de8800p-2, 0x1.5ca14b6cfb03fp-46),
            a(0x1.5600000000000p+0, -0x1.2895a13de8800p-2, 0x1.5ca14b6cfb03fp-46),
            a(0x1.5400000000000p+0, -0x1.22941fbcf7800p-2, -0x1.65a242853da76p-46),
            a(0x1.5200000000000p+0, -0x1.1c898c1699800p-2, -0x1.fafbc68e75404p-46),
            a(0x1.5000000000000p+0, -0x1.1675cababa800p-2, 0x1.f1fc63382a8f0p-46),
            a(0x1.4e00000000000p+0, -0x1.1058bf9ae4800p-2, -0x1.6a8c4fd055a66p-45),
            a(0x1.4c00000000000p+0, -0x1.0a324e2739000p-2, -0x1.c6bee7ef4030ep-47),
            a(0x1.4a00000000000p+0, -0x1.0402594b4d000p-2, -0x1.036b89ef42d7fp-48),
            a(0x1.4a00000000000p+0, -0x1.0402594b4d000p-2, -0x1.036b89ef42d7fp-48),
            a(0x1.4800000000000p+0, -0x1.fb9186d5e4000p-3, 0x1.d572aab993c87p-47),
            a(0x1.4600000000000p+0, -0x1.ef0adcbdc6000p-3, 0x1.b26b79c86af24p-45),
            a(0x1.4400000000000p+0, -0x1.e27076e2af000p-3, -0x1.72f4f543fff10p-46),
            a(0x1.4200000000000p+0, -0x1.d5c216b4fc000p-3, 0x1.1ba91bbca681bp-45),
            a(0x1.4000000000000p+0, -0x1.c8ff7c79aa000p-3, 0x1.7794f689f8434p-45),
            a(0x1.4000000000000p+0, -0x1.c8ff7c79aa000p-3, 0x1.7794f689f8434p-45),
            a(0x1.3e00000000000p+0, -0x1.bc286742d9000p-3, 0x1.94eb0318bb78fp-46),
            a(0x1.3c00000000000p+0, -0x1.af3c94e80c000p-3, 0x1.a4e633fcd9066p-52),
            a(0x1.3a00000000000p+0, -0x1.a23bc1fe2b000p-3, -0x1.58c64dc46c1eap-45),
            a(0x1.3a00000000000p+0, -0x1.a23bc1fe2b000p-3, -0x1.58c64dc46c1eap-45),
            a(0x1.3800000000000p+0, -0x1.9525a9cf45000p-3, -0x1.ad1d904c1d4e3p-45),
            a(0x1.3600000000000p+0, -0x1.87fa06520d000p-3, 0x1.bbdbf7fdbfa09p-45),
            a(0x1.3400000000000p+0, -0x1.7ab890210e000p-3, 0x1.bdb9072534a58p-45),
            a(0x1.3400000000000p+0, -0x1.7ab890210e000p-3, 0x1.bdb9072534a58p-45),
            a(0x1.3200000000000p+0, -0x1.6d60fe719d000p-3, -0x1.0e46aa3b2e266p-46),
            a(0x1.3000000000000p+0, -0x1.5ff3070a79000p-3, -0x1.e9e439f105039p-46),
            a(0x1.3000000000000p+0, -0x1.5ff3070a79000p-3, -0x1.e9e439f105039p-46),
            a(0x1.2e00000000000p+0, -0x1.526e5e3a1b000p-3, -0x1.0de8b90075b8fp-45),
            a(0x1.2c00000000000p+0, -0x1.44d2b6ccb8000p-3, 0x1.70cc16135783cp-46),
            a(0x1.2c00000000000p+0, -0x1.44d2b6ccb8000p-3, 0x1.70cc16135783cp-46),
            a(0x1.2a00000000000p+0, -0x1.371fc201e9000p-3, 0x1.178864d27543ap-48),
            a(0x1.2800000000000p+0, -0x1.29552f81ff000p-3, -0x1.48d301771c408p-45),
            a(0x1.2600000000000p+0, -0x1.1b72ad52f6000p-3, -0x1.e80a41811a396p-45),
            a(0x1.2600000000000p+0, -0x1.1b72ad52f6000p-3, -0x1.e80a41811a396p-45),
            a(0x1.2400000000000p+0, -0x1.0d77e7cd09000p-3, 0x1.a699688e85bf4p-47),
            a(0x1.2400000000000p+0, -0x1.0d77e7cd09000p-3, 0x1.a699688e85bf4p-47),
            a(0x1.2200000000000p+0, -0x1.fec9131dbe000p-4, -0x1.575545ca333f2p-45),
            a(0x1.2000000000000p+0, -0x1.e27076e2b0000p-4, 0x1.a342c2af0003cp-45),
            a(0x1.2000000000000p+0, -0x1.e27076e2b0000p-4, 0x1.a342c2af0003cp-45),
            a(0x1.1e00000000000p+0, -0x1.c5e548f5bc000p-4, -0x1.d0c57585fbe06p-46),
            a(0x1.1c00000000000p+0, -0x1.a926d3a4ae000p-4, 0x1.53935e85baac8p-45),
            a(0x1.1c00000000000p+0, -0x1.a926d3a4ae000p-4, 0x1.53935e85baac8p-45),
            a(0x1.1a00000000000p+0, -0x1.8c345d631a000p-4, 0x1.37c294d2f5668p-46),
            a(0x1.1a00000000000p+0, -0x1.8c345d631a000p-4, 0x1.37c294d2f5668p-46),
            a(0x1.1800000000000p+0, -0x1.6f0d28ae56000p-4, -0x1.69737c93373dap-45),
            a(0x1.1600000000000p+0, -0x1.51b073f062000p-4, 0x1.f025b61c65e57p-46),
            a(0x1.1600000000000p+0, -0x1.51b073f062000p-4, 0x1.f025b61c65e57p-46),
            a(0x1.1400000000000p+0, -0x1.341d7961be000p-4, 0x1.c5edaccf913dfp-45),
            a(0x1.1400000000000p+0, -0x1.341d7961be000p-4, 0x1.c5edaccf913dfp-45),
            a(0x1.1200000000000p+0, -0x1.16536eea38000p-4, 0x1.47c5e768fa309p-46),
            a(0x1.1000000000000p+0, -0x1.f0a30c0118000p-5, 0x1.d599e83368e91p-45),
            a(0x1.1000000000000p+0, -0x1.f0a30c0118000p-5, 0x1.d599e83368e91p-45),
            a(0x1.0e00000000000p+0, -0x1.b42dd71198000p-5, 0x1.c827ae5d6704cp-46),
            a(0x1.0e00000000000p+0, -0x1.b42dd71198000p-5, 0x1.c827ae5d6704cp-46),
            a(0x1.0c00000000000p+0, -0x1.77458f632c000p-5, -0x1.cfc4634f2a1eep-45),
            a(0x1.0c00000000000p+0, -0x1.77458f632c000p-5, -0x1.cfc4634f2a1eep-45),
            a(0x1.0a00000000000p+0, -0x1.39e87b9fec000p-5, 0x1.502b7f526feaap-48),
            a(0x1.0a00000000000p+0, -0x1.39e87b9fec000p-5, 0x1.502b7f526feaap-48),
            a(0x1.0800000000000p+0, -0x1.f829b0e780000p-6, -0x1.980267c7e09e4p-45),
            a(0x1.0800000000000p+0, -0x1.f829b0e780000p-6, -0x1.980267c7e09e4p-45),
            a(0x1.0600000000000p+0, -0x1.7b91b07d58000p-6, -0x1.88d5493faa639p-45),
            a(0x1.0400000000000p+0, -0x1.fc0a8b0fc0000p-7, -0x1.f1e7cf6d3a69cp-50),
            a(0x1.0400000000000p+0, -0x1.fc0a8b0fc0000p-7, -0x1.f1e7cf6d3a69cp-50),
            a(0x1.0200000000000p+0, -0x1.fe02a6b100000p-8, -0x1.9e23f0dda40e4p-46),
            a(0x1.0200000000000p+0, -0x1.fe02a6b100000p-8, -0x1.9e23f0dda40e4p-46),
            a(0x1.0000000000000p+0, 0x0.0000000000000p+0, 0x0.0000000000000p+0),
            a(0x1.0000000000000p+0, 0x0.0000000000000p+0, 0x0.0000000000000p+0),
            a(0x1.fc00000000000p-1, 0x1.0101575890000p-7, -0x1.0c76b999d2be8p-46),
            a(0x1.f800000000000p-1, 0x1.0205658938000p-6, -0x1.3dc5b06e2f7d2p-45),
            a(0x1.f400000000000p-1, 0x1.8492528c90000p-6, -0x1.aa0ba325a0c34p-45),
            a(0x1.f000000000000p-1, 0x1.0415d89e74000p-5, 0x1.111c05cf1d753p-47),
            a(0x1.ec00000000000p-1, 0x1.466aed42e0000p-5, -0x1.c167375bdfd28p-45),
            a(0x1.e800000000000p-1, 0x1.894aa149fc000p-5, -0x1.97995d05a267dp-46),
            a(0x1.e400000000000p-1, 0x1.ccb73cdddc000p-5, -0x1.a68f247d82807p-46),
            a(0x1.e200000000000p-1, 0x1.eea31c006c000p-5, -0x1.e113e4fc93b7bp-47),
            a(0x1.de00000000000p-1, 0x1.1973bd1466000p-4, -0x1.5325d560d9e9bp-45),
            a(0x1.da00000000000p-1, 0x1.3bdf5a7d1e000p-4, 0x1.cc85ea5db4ed7p-45),
            a(0x1.d600000000000p-1, 0x1.5e95a4d97a000p-4, -0x1.c69063c5d1d1ep-45),
            a(0x1.d400000000000p-1, 0x1.700d30aeac000p-4, 0x1.c1e8da99ded32p-49),
            a(0x1.d000000000000p-1, 0x1.9335e5d594000p-4, 0x1.3115c3abd47dap-45),
            a(0x1.cc00000000000p-1, 0x1.b6ac88dad6000p-4, -0x1.390802bf768e5p-46),
            a(0x1.ca00000000000p-1, 0x1.c885801bc4000p-4, 0x1.646d1c65aacd3p-45),
            a(0x1.c600000000000p-1, 0x1.ec739830a2000p-4, -0x1.dc068afe645e0p-45),
            a(0x1.c400000000000p-1, 0x1.fe89139dbe000p-4, -0x1.534d64fa10afdp-45),
            a(0x1.c000000000000p-1, 0x1.1178e8227e000p-3, 0x1.1ef78ce2d07f2p-45),
            a(0x1.be00000000000p-1, 0x1.1aa2b7e23f000p-3, 0x1.ca78e44389934p-45),
            a(0x1.ba00000000000p-1, 0x1.2d1610c868000p-3, 0x1.39d6ccb81b4a1p-47),
            a(0x1.b800000000000p-1, 0x1.365fcb0159000p-3, 0x1.62fa8234b7289p-51),
            a(0x1.b400000000000p-1, 0x1.4913d8333b000p-3, 0x1.5837954fdb678p-45),
            a(0x1.b200000000000p-1, 0x1.527e5e4a1b000p-3, 0x1.633e8e5697dc7p-45),
            a(0x1.ae00000000000p-1, 0x1.6574ebe8c1000p-3, 0x1.9cf8b2c3c2e78p-46),
            a(0x1.ac00000000000p-1, 0x1.6f0128b757000p-3, -0x1.5118de59c21e1p-45),
            a(0x1.aa00000000000p-1, 0x1.7898d85445000p-3, -0x1.c661070914305p-46),
            a(0x1.a600000000000p-1, 0x1.8beafeb390000p-3, -0x1.73d54aae92cd1p-47),
            a(0x1.a400000000000p-1, 0x1.95a5adcf70000p-3, 0x1.7f22858a0ff6fp-47),
            a(0x1.a000000000000p-1, 0x1.a93ed3c8ae000p-3, -0x1.8724350562169p-45),
            a(0x1.9e00000000000p-1, 0x1.b31d8575bd000p-3, -0x1.c358d4eace1aap-47),
            a(0x1.9c00000000000p-1, 0x1.bd087383be000p-3, -0x1.d4bc4595412b6p-45),
            a(0x1.9a00000000000p-1, 0x1.c6ffbc6f01000p-3, -0x1.1ec72c5962bd2p-48),
            a(0x1.9600000000000p-1, 0x1.db13db0d49000p-3, -0x1.aff2af715b035p-45),
            a(0x1.9400000000000p-1, 0x1.e530effe71000p-3, 0x1.212276041f430p-51),
            a(0x1.9200000000000p-1, 0x1.ef5ade4dd0000p-3, -0x1.a211565bb8e11p-51),
            a(0x1.9000000000000p-1, 0x1.f991c6cb3b000p-3, 0x1.bcbecca0cdf30p-46),
            a(0x1.8c00000000000p-1, 0x1.07138604d5800p-2, 0x1.89cdb16ed4e91p-48),
            a(0x1.8a00000000000p-1, 0x1.0c42d67616000p-2, 0x1.7188b163ceae9p-45),
            a(0x1.8800000000000p-1, 0x1.1178e8227e800p-2, -0x1.c210e63a5f01cp-45),
            a(0x1.8600000000000p-1, 0x1.16b5ccbacf800p-2, 0x1.b9acdf7a51681p-45),
            a(0x1.8400000000000p-1, 0x1.1bf99635a6800p-2, 0x1.ca6ed5147bdb7p-45),
            a(0x1.8200000000000p-1, 0x1.214456d0eb800p-2, 0x1.a87deba46baeap-47),
            a(0x1.7e00000000000p-1, 0x1.2bef07cdc9000p-2, 0x1.a9cfa4a5004f4p-45),
            a(0x1.7c00000000000p-1, 0x1.314f1e1d36000p-2, -0x1.8e27ad3213cb8p-45),
            a(0x1.7a00000000000p-1, 0x1.36b6776be1000p-2, 0x1.16ecdb0f177c8p-46),
            a(0x1.7800000000000p-1, 0x1.3c25277333000p-2, 0x1.83b54b606bd5cp-46),
            a(0x1.7600000000000p-1, 0x1.419b423d5e800p-2, 0x1.8e436ec90e09dp-47),
            a(0x1.7400000000000p-1, 0x1.4718dc271c800p-2, -0x1.f27ce0967d675p-45),
            a(0x1.7200000000000p-1, 0x1.4c9e09e173000p-2, -0x1.e20891b0ad8a4p-45),
            a(0x1.7000000000000p-1, 0x1.522ae0738a000p-2, 0x1.ebe708164c759p-45),
            a(0x1.6e00000000000p-1, 0x1.57bf753c8d000p-2, 0x1.fadedee5d40efp-46),
            a(0x1.6c00000000000p-1, 0x1.5d5bddf596000p-2, -0x1.a0b2a08a465dcp-47)
    };

    // 2^(k/N) ~= H[k]*(1 + T[k]) for int k in [0,N)
    // tab[2*k] = asuint64(T[k])
    // tab[2*k+1] = asuint64(H[k]) - (k << 52)/N
    private static final long[] EXP_TAB = {
            0x0, 0x3ff0000000000000L,
            0x3c9b3b4f1a88bf6eL, 0x3feff63da9fb3335L,
            0xbc7160139cd8dc5dL, 0x3fefec9a3e778061L,
            0xbc905e7a108766d1L, 0x3fefe315e86e7f85L,
            0x3c8cd2523567f613L, 0x3fefd9b0d3158574L,
            0xbc8bce8023f98efaL, 0x3fefd06b29ddf6deL,
            0x3c60f74e61e6c861L, 0x3fefc74518759bc8L,
            0x3c90a3e45b33d399L, 0x3fefbe3ecac6f383L,
            0x3c979aa65d837b6dL, 0x3fefb5586cf9890fL,
            0x3c8eb51a92fdeffcL, 0x3fefac922b7247f7L,
            0x3c3ebe3d702f9cd1L, 0x3fefa3ec32d3d1a2L,
            0xbc6a033489906e0bL, 0x3fef9b66affed31bL,
            0xbc9556522a2fbd0eL, 0x3fef9301d0125b51L,
            0xbc5080ef8c4eea55L, 0x3fef8abdc06c31ccL,
            0xbc91c923b9d5f416L, 0x3fef829aaea92de0L,
            0x3c80d3e3e95c55afL, 0x3fef7a98c8a58e51L,
            0xbc801b15eaa59348L, 0x3fef72b83c7d517bL,
            0xbc8f1ff055de323dL, 0x3fef6af9388c8deaL,
            0x3c8b898c3f1353bfL, 0x3fef635beb6fcb75L,
            0xbc96d99c7611eb26L, 0x3fef5be084045cd4L,
            0x3c9aecf73e3a2f60L, 0x3fef54873168b9aaL,
            0xbc8fe782cb86389dL, 0x3fef4d5022fcd91dL,
            0x3c8a6f4144a6c38dL, 0x3fef463b88628cd6L,
            0x3c807a05b0e4047dL, 0x3fef3f49917ddc96L,
            0x3c968efde3a8a894L, 0x3fef387a6e756238L,
            0x3c875e18f274487dL, 0x3fef31ce4fb2a63fL,
            0x3c80472b981fe7f2L, 0x3fef2b4565e27cddL,
            0xbc96b87b3f71085eL, 0x3fef24dfe1f56381L,
            0x3c82f7e16d09ab31L, 0x3fef1e9df51fdee1L,
            0xbc3d219b1a6fbffaL, 0x3fef187fd0dad990L,
            0x3c8b3782720c0ab4L, 0x3fef1285a6e4030bL,
            0x3c6e149289cecb8fL, 0x3fef0cafa93e2f56L,
            0x3c834d754db0abb6L, 0x3fef06fe0a31b715L,
            0x3c864201e2ac744cL, 0x3fef0170fc4cd831L,
            0x3c8fdd395dd3f84aL, 0x3feefc08b26416ffL,
            0xbc86a3803b8e5b04L, 0x3feef6c55f929ff1L,
            0xbc924aedcc4b5068L, 0x3feef1a7373aa9cbL,
            0xbc9907f81b512d8eL, 0x3feeecae6d05d866L,
            0xbc71d1e83e9436d2L, 0x3feee7db34e59ff7L,
            0xbc991919b3ce1b15L, 0x3feee32dc313a8e5L,
            0x3c859f48a72a4c6dL, 0x3feedea64c123422L,
            0xbc9312607a28698aL, 0x3feeda4504ac801cL,
            0xbc58a78f4817895bL, 0x3feed60a21f72e2aL,
            0xbc7c2c9b67499a1bL, 0x3feed1f5d950a897L,
            0x3c4363ed60c2ac11L, 0x3feece086061892dL,
            0x3c9666093b0664efL, 0x3feeca41ed1d0057L,
            0x3c6ecce1daa10379L, 0x3feec6a2b5c13cd0L,
            0x3c93ff8e3f0f1230L, 0x3feec32af0d7d3deL,
            0x3c7690cebb7aafb0L, 0x3feebfdad5362a27L,
            0x3c931dbdeb54e077L, 0x3feebcb299fddd0dL,
            0xbc8f94340071a38eL, 0x3feeb9b2769d2ca7L,
            0xbc87deccdc93a349L, 0x3feeb6daa2cf6642L,
            0xbc78dec6bd0f385fL, 0x3feeb42b569d4f82L,
            0xbc861246ec7b5cf6L, 0x3feeb1a4ca5d920fL,
            0x3c93350518fdd78eL, 0x3feeaf4736b527daL,
            0x3c7b98b72f8a9b05L, 0x3feead12d497c7fdL,
            0x3c9063e1e21c5409L, 0x3feeab07dd485429L,
            0x3c34c7855019c6eaL, 0x3feea9268a5946b7L,
            0x3c9432e62b64c035L, 0x3feea76f15ad2148L,
            0xbc8ce44a6199769fL, 0x3feea5e1b976dc09L,
            0xbc8c33c53bef4da8L, 0x3feea47eb03a5585L,
            0xbc845378892be9aeL, 0x3feea34634ccc320L,
            0xbc93cedd78565858L, 0x3feea23882552225L,
            0x3c5710aa807e1964L, 0x3feea155d44ca973L,
            0xbc93b3efbf5e2228L, 0x3feea09e667f3bcdL,
            0xbc6a12ad8734b982L, 0x3feea012750bdabfL,
            0xbc6367efb86da9eeL, 0x3fee9fb23c651a2fL,
            0xbc80dc3d54e08851L, 0x3fee9f7df9519484L,
            0xbc781f647e5a3ecfL, 0x3fee9f75e8ec5f74L,
            0xbc86ee4ac08b7db0L, 0x3fee9f9a48a58174L,
            0xbc8619321e55e68aL, 0x3fee9feb564267c9L,
            0x3c909ccb5e09d4d3L, 0x3feea0694fde5d3fL,
            0xbc7b32dcb94da51dL, 0x3feea11473eb0187L,
            0x3c94ecfd5467c06bL, 0x3feea1ed0130c132L,
            0x3c65ebe1abd66c55L, 0x3feea2f336cf4e62L,
            0xbc88a1c52fb3cf42L, 0x3feea427543e1a12L,
            0xbc9369b6f13b3734L, 0x3feea589994cce13L,
            0xbc805e843a19ff1eL, 0x3feea71a4623c7adL,
            0xbc94d450d872576eL, 0x3feea8d99b4492edL,
            0x3c90ad675b0e8a00L, 0x3feeaac7d98a6699L,
            0x3c8db72fc1f0eab4L, 0x3feeace5422aa0dbL,
            0xbc65b6609cc5e7ffL, 0x3feeaf3216b5448cL,
            0x3c7bf68359f35f44L, 0x3feeb1ae99157736L,
            0xbc93091fa71e3d83L, 0x3feeb45b0b91ffc6L,
            0xbc5da9b88b6c1e29L, 0x3feeb737b0cdc5e5L,
            0xbc6c23f97c90b959L, 0x3feeba44cbc8520fL,
            0xbc92434322f4f9aaL, 0x3feebd829fde4e50L,
            0xbc85ca6cd7668e4bL, 0x3feec0f170ca07baL,
            0x3c71affc2b91ce27L, 0x3feec49182a3f090L,
            0x3c6dd235e10a73bbL, 0x3feec86319e32323L,
            0xbc87c50422622263L, 0x3feecc667b5de565L,
            0x3c8b1c86e3e231d5L, 0x3feed09bec4a2d33L,
            0xbc91bbd1d3bcbb15L, 0x3feed503b23e255dL,
            0x3c90cc319cee31d2L, 0x3feed99e1330b358L,
            0x3c8469846e735ab3L, 0x3feede6b5579fdbfL,
            0xbc82dfcd978e9db4L, 0x3feee36bbfd3f37aL,
            0x3c8c1a7792cb3387L, 0x3feee89f995ad3adL,
            0xbc907b8f4ad1d9faL, 0x3feeee07298db666L,
            0xbc55c3d956dcaebaL, 0x3feef3a2b84f15fbL,
            0xbc90a40e3da6f640L, 0x3feef9728de5593aL,
            0xbc68d6f438ad9334L, 0x3feeff76f2fb5e47L,
            0xbc91eee26b588a35L, 0x3fef05b030a1064aL,
            0x3c74ffd70a5fddcdL, 0x3fef0c1e904bc1d2L,
            0xbc91bdfbfa9298acL, 0x3fef12c25bd71e09L,
            0x3c736eae30af0cb3L, 0x3fef199bdd85529cL,
            0x3c8ee3325c9ffd94L, 0x3fef20ab5fffd07aL,
            0x3c84e08fd10959acL, 0x3fef27f12e57d14bL,
            0x3c63cdaf384e1a67L, 0x3fef2f6d9406e7b5L,
            0x3c676b2c6c921968L, 0x3fef3720dcef9069L,
            0xbc808a1883ccb5d2L, 0x3fef3f0b555dc3faL,
            0xbc8fad5d3ffffa6fL, 0x3fef472d4a07897cL,
            0xbc900dae3875a949L, 0x3fef4f87080d89f2L,
            0x3c74a385a63d07a7L, 0x3fef5818dcfba487L,
            0xbc82919e2040220fL, 0x3fef60e316c98398L,
            0x3c8e5a50d5c192acL, 0x3fef69e603db3285L,
            0x3c843a59ac016b4bL, 0x3fef7321f301b460L,
            0xbc82d52107b43e1fL, 0x3fef7c97337b9b5fL,
            0xbc892ab93b470dc9L, 0x3fef864614f5a129L,
            0x3c74b604603a88d3L, 0x3fef902ee78b3ff6L,
            0x3c83c5ec519d7271L, 0x3fef9a51fbc74c83L,
            0xbc8ff7128fd391f0L, 0x3fefa4afa2a490daL,
            0xbc8dae98e223747dL, 0x3fefaf482d8e67f1L,
            0x3c8ec3bc41aa2008L, 0x3fefba1bee615a27L,
            0x3c842b94c3a9eb32L, 0x3fefc52b376bba97L,
            0x3c8a64a931d185eeL, 0x3fefd0765b6e4540L,
            0xbc8e37bae43be3edL, 0x3fefdbfdad9cbe14L,
            0x3c77893b4d91cd9dL, 0x3fefe7c1819e90d8L,
            0x3c5305c14160cc89L, 0x3feff3c22b8f71f1L,
    };

    private static final double[] EXP_POLY = {
            // abs error: 1.555*2^-66
            // ulp error: 0.509 (0.511 without fma)
            // if |x| < ln2/256+eps
            // abs error if |x| < ln2/256+0x1p-15: 1.09*2^-65
            // abs error if |x| < ln2/128: 1.7145*2^-56
            0x1.ffffffffffdbdp-2,
            0x1.555555555543cp-3,
            0x1.55555cf172b91p-5,
            0x1.1111167a4d017p-7,
    };

    private static final double C2 = EXP_POLY[5 - EXP_POLY_ORDER];
    private static final double C3 = EXP_POLY[6 - EXP_POLY_ORDER];
    private static final double C4 = EXP_POLY[7 - EXP_POLY_ORDER];
    private static final double C5 = EXP_POLY[8 - EXP_POLY_ORDER];
}
