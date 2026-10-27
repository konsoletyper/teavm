/*
 *  Copyright 2023 ihromant.
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

@TeaVMTest
public class RandomTest {
    @Test
    public void testDoubles() {
        Random random = new Random();
        double[] doubles = IntStream.range(0, 1000).mapToDouble(i -> random.nextDouble()).toArray();
        for (double d : doubles) {
            assertTrue(d >= 0.0 && d < 1.0);
        }
        try {
            random.nextDouble(-1.0);
            fail();
        } catch (IllegalArgumentException e) {
            // normal
        }
        doubles = IntStream.range(0, 1000).mapToDouble(i -> random.nextDouble(20.0)).toArray();
        for (double d : doubles) {
            assertTrue(d >= 0.0 && d < 20.0);
        }
        try {
            random.nextDouble(-1.0, -2.0);
            fail();
        } catch (IllegalArgumentException e) {
            // normal
        }
        doubles = IntStream.range(0, 1000).mapToDouble(i -> random.nextDouble(-2.0, -1.0)).toArray();
        for (double d : doubles) {
            assertTrue(d >= -2.0 && d < 1.0);
        }
    }

    @Test
    public void testIntegers() {
        Random random = new Random();
        int[] ints = IntStream.range(0, 10000).map(i -> random.nextInt())
                .toArray(); // 10 000 enough for almost 100% probability
        int ones = IntStream.of(ints).reduce(0, (id, i) -> id | i);
        Set<Integer> unique = Arrays.stream(ints).boxed().collect(Collectors.toSet());
        assertEquals(-1, ones); // all ones present
        assertTrue(unique.size() > 9900);
        try {
            random.nextInt(-5);
            fail();
        } catch (IllegalArgumentException e) {
            // normal
        }
        ints = IntStream.range(0, 1000).map(i -> random.nextInt(512)).toArray();
        for (int i : ints) {
            assertTrue(i >= 0 && i < 512);
        }
        ints = IntStream.range(0, 1000).map(i -> random.nextInt(20)).toArray();
        for (int i : ints) {
            assertTrue(i >= 0 && i < 20);
        }
        try {
            random.nextInt(-3, -5);
            fail();
        } catch (IllegalArgumentException e) {
            // normal
        }
        ints = IntStream.range(0, 1000).map(i -> random.nextInt(Integer.MIN_VALUE / 3 * 2, Integer.MAX_VALUE / 3 * 2))
                .toArray();
        for (int i : ints) {
            assertTrue(i >= Integer.MIN_VALUE / 3 * 2 && i < Integer.MAX_VALUE / 3 * 2);
        }
        Arrays.stream(ints).anyMatch(i -> i < Integer.MIN_VALUE / 2);
        Arrays.stream(ints).anyMatch(i -> i > Integer.MAX_VALUE / 2);
    }

    @Test
    public void testLongs() {
        Random random = new Random();
        long[] longs = IntStream.range(0, 10000).mapToLong(i -> random.nextLong())
                .toArray(); // 10 000 enough for almost 100% probability
        long ones = LongStream.of(longs).reduce(0L, (id, i) -> id | i);
        Set<Long> unique = Arrays.stream(longs).boxed().collect(Collectors.toSet());
        assertEquals(-1L, ones); // all ones present
        assertTrue(unique.size() > 9900);
        try {
            random.nextLong(-5);
            fail();
        } catch (IllegalArgumentException e) {
            // normal
        }
        longs = IntStream.range(0, 1000).mapToLong(i -> random.nextLong(512L)).toArray();
        for (long l : longs) {
            assertTrue(l >= 0L && l < 512L);
        }
        longs = IntStream.range(0, 1000).mapToLong(i -> random.nextLong(20L)).toArray();
        for (long l : longs) {
            assertTrue(l >= 0 && l < 20);
        }
        try {
            random.nextLong(-3, -5);
            fail();
        } catch (IllegalArgumentException e) {
            // normal
        }
        longs = IntStream.range(0, 1000).mapToLong(i -> random.nextLong(Long.MIN_VALUE / 3 * 2, Long.MAX_VALUE / 3 * 2))
                .toArray();
        for (long l : longs) {
            assertTrue(l >= Long.MIN_VALUE / 3 * 2 && l < Long.MAX_VALUE / 3 * 2);
        }
        Arrays.stream(longs).anyMatch(l -> l < Long.MIN_VALUE / 2);
        Arrays.stream(longs).anyMatch(l -> l > Long.MAX_VALUE / 2);
    }

    @Test
    public void testNextBytes() {
        Random rand = new Random();
        rand.nextBytes(new byte[0]);
        assertTrue(IntStream.range(0, 1000).anyMatch(i -> testNonZero(rand, 5)));
        assertTrue(IntStream.range(0, 1000).anyMatch(i -> testNonZero(rand, 6)));
        assertTrue(IntStream.range(0, 1000).anyMatch(i -> testNonZero(rand, 7)));
        assertTrue(IntStream.range(0, 1000).anyMatch(i -> testNonZero(rand, 8)));
        byte[] bytes = new byte[1000];
        rand.nextBytes(bytes);
        assertEquals(0xFF, IntStream.range(0, bytes.length).map(i -> 0xFF & bytes[i]).reduce(0, (id, i) -> id | i));
    }

    private boolean testNonZero(Random rand, int size) {
        byte[] bytes = new byte[size];
        rand.nextBytes(bytes);
        for (byte b : bytes) {
            if (b == 0) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void testGaussian() {
        Random rand = new Random();
        double[] doubles = IntStream.range(0, 10000).mapToDouble(i -> rand.nextGaussian(30, 10)).toArray();
        assertTrue(DoubleStream.of(doubles).filter(d -> d < 0.0 || d > 60.0).count() < 100);
    }
    
    @Test
    public void testLong() {
        var rand = new Random();
        var positive = 0;
        var negative = 0;
        for (var i = 0; i < 1000; ++i) {
            var v = rand.nextLong();
            if (v >= 0) {
                positive++;
            } else {
                negative++;
            }
        }
        assertTrue(Math.abs(positive - negative) < 100);
    }

    @Test
    public void intsWithSeed() {
        var rand = new Random(1);
        var array = new int[10];
        for (var i = 0; i < 10; ++i) {
            array[i] = rand.nextInt();
        }
        assertArrayEquals(new int[] { -1155869325, 431529176, 1761283695, 1749940626, 892128508, 155629808,
                1429008869, -1465154083, -138487339, -1242363800 }, array);
    }

    @Test
    public void longsWithSeed() {
        var rand = new Random(1);
        var array = new long[10];
        for (var i = 0; i < 10; ++i) {
            array[i] = rand.nextLong();
        }
        assertArrayEquals(new long[] { -4964420948893066024L, 7564655870752979346L, 3831662765844904176L,
                6137546356583794141L, -594798593157429144L, 112842269129291794L, -669528114487223426L,
                -1109287713991315740L, -974081879987450628L, -1160629452687687109L }, array);
    }

    @Test
    public void doublesWithSeed() {
        var rand = new Random(1);
        assertEquals(0.7308781907032909, rand.nextDouble(), 1e-5);
        assertEquals(0.41008081149220166, rand.nextDouble(), 1e-5);
    }

    @Test
    public void seededBoundedIntsAreTheJdkOnes() {
        Random rand = new Random(42);
        assertArrayEquals(new int[] { 6, 41, 34, 54, 2, 59, 3, 32 },
                IntStream.range(0, 8).map(i -> rand.nextInt(62)).toArray());
        Random rand2 = new Random(42);
        assertArrayEquals(new int[] { 130, 763, 248, 884, 970 },
                IntStream.range(0, 5).map(i -> rand2.nextInt(1000)).toArray());
        Random rand3 = new Random(42);
        assertArrayEquals(new int[] { 46, 3, 43, 3, 19 }, IntStream.range(0, 5).map(i -> rand3.nextInt(64)).toArray());
        Random rand4 = new Random(42);
        assertArrayEquals(new int[] { 7, 8, 5, 13, 11 },
                IntStream.range(0, 5).map(i -> rand4.nextInt(5, 17)).toArray());
        Random rand5 = new Random(42);
        assertArrayEquals(new int[] { 234785527, 205897768, 1325939940 },
                IntStream.range(0, 3).map(i -> rand5.nextInt(-3, Integer.MAX_VALUE)).toArray());
    }

    @Test
    public void seededBoundedLongsAreTheJdkOnes() {
        Random rand = new Random(42);
        assertArrayEquals(new long[] { 891, 940, 997, 398, 717 },
                IntStream.range(0, 5).mapToLong(i -> rand.nextLong(1000)).toArray());
        Random rand2 = new Random(42);
        assertArrayEquals(new long[] { 227868052215L, 966573539368L, 979003751243L },
                IntStream.range(0, 3).mapToLong(i -> rand2.nextLong(1L << 40)).toArray());
        Random rand3 = new Random(42);
        assertArrayEquals(new long[] { 340114562, 114286899, 86493332 },
                IntStream.range(0, 3).mapToLong(i -> rand3.nextLong(-5, 1_000_000_007L)).toArray());
    }

    // Differential tests against the JDK's sequences. Each expected value folds the results of one call pattern
    // over all SEEDS; the JVM run of this test checks the values against the JDK itself.
    //
    // Random's Javadoc pins its algorithms: "particular algorithms are specified for the class Random", and
    // next(bits), nextInt(), nextInt(bound), nextLong(), nextBoolean(), nextBytes, nextFloat() and nextDouble()
    // are each "implemented by class Random as if by" the code it shows. Random.longs(origin, bound) is "as if"
    // RandomSupport.boundedNextLong. nextInt(origin, bound), nextLong(bound) and nextLong(origin, bound) are only
    // specified to invoke nextInt() / nextLong() "one or more times to ensure a uniform distribution"; matching
    // the JDK (RandomSupport.boundedNextInt / boundedNextLong) there is OpenJDK behaviour. The JDK's
    // Random.ints(origin, bound) also uses boundedNextInt, though its Javadoc shows nextInt(n) + origin; the two
    // differ when n is a power of two.

    private static final int[] INT_BOUNDS = { 1, 2, 3, 5, 62, 64, 100, 1000, 1 << 16, (1 << 30) - 1, 1 << 30,
            (1 << 30) + 1, 0x55555556, Integer.MAX_VALUE - 1, Integer.MAX_VALUE };
    private static final int[][] INT_RANGES = { { 5, 17 }, { 0, 64 }, { -64, 0 }, { -1000, 1000 },
            { -3, Integer.MAX_VALUE }, { Integer.MIN_VALUE, Integer.MAX_VALUE }, { Integer.MIN_VALUE, 0 },
            { Integer.MIN_VALUE, -1 }, { Integer.MIN_VALUE, Integer.MIN_VALUE + 1 },
            { Integer.MAX_VALUE - 1, Integer.MAX_VALUE }, { -(1 << 30), (1 << 30) + 1 },
            { Integer.MIN_VALUE / 3 * 2, Integer.MAX_VALUE / 3 * 2 } };
    private static final long[] LONG_BOUNDS = { 1, 2, 3, 1000, 1L << 40, (1L << 40) + 1, (1L << 62) - 1, 1L << 62,
            (1L << 62) + 1, 0x5555555555555556L, Long.MAX_VALUE - 1, Long.MAX_VALUE };
    private static final long[][] LONG_RANGES = { { -5, 1_000_000_007L }, { 0, 64 }, { -(1L << 40), 1L << 40 },
            { Long.MIN_VALUE, Long.MAX_VALUE }, { Long.MIN_VALUE, 0 }, { 0, Long.MAX_VALUE },
            { -(1L << 62), (1L << 62) + 1 }, { Long.MIN_VALUE, Long.MIN_VALUE + 1 },
            { Long.MAX_VALUE - 1, Long.MAX_VALUE } };

    private static final long[] SEEDS = new long[32];

    static {
        long[] special = { 0, 1, -1, 42, Long.MIN_VALUE, Long.MAX_VALUE, 0x5DEECE66DL, 1L << 48 };
        System.arraycopy(special, 0, SEEDS, 0, special.length);
        for (int i = special.length; i < SEEDS.length; ++i) {
            SEEDS[i] = i * 0x9E3779B97F4A7C15L;
        }
    }

    private static final class Case {
        final String name;
        final ToLongFunction<RandomGenerator> op;

        Case(String name, ToLongFunction<RandomGenerator> op) {
            this.name = name;
            this.op = op;
        }
    }

    private static long mix(long h, long v) {
        h = (h ^ v) * 0x9E3779B97F4A7C15L;
        return h ^ (h >>> 29);
    }

    private static List<Case> cases() {
        List<Case> cases = new ArrayList<>();
        cases.add(new Case("nextInt()", r -> r.nextInt()));
        for (int bound : INT_BOUNDS) {
            cases.add(new Case("nextInt(" + bound + ")", r -> r.nextInt(bound)));
        }
        cases.add(new Case("nextInt(2^k), k = 0..30", r -> {
            long h = 0;
            for (int k = 0; k < 31; ++k) {
                h = mix(h, r.nextInt(1 << k));
            }
            return h;
        }));
        for (int[] range : INT_RANGES) {
            cases.add(new Case("nextInt(" + range[0] + ", " + range[1] + ")", r -> r.nextInt(range[0], range[1])));
            cases.add(new Case("ints(" + range[0] + ", " + range[1] + ")",
                    r -> r.ints(range[0], range[1]).limit(5).asLongStream().reduce(0, RandomTest::mix)));
        }
        cases.add(new Case("nextLong()", r -> r.nextLong()));
        for (long bound : LONG_BOUNDS) {
            cases.add(new Case("nextLong(" + bound + ")", r -> r.nextLong(bound)));
        }
        cases.add(new Case("nextLong(2^k), k = 0..62", r -> {
            long h = 0;
            for (int k = 0; k < 63; ++k) {
                h = mix(h, r.nextLong(1L << k));
            }
            return h;
        }));
        for (long[] range : LONG_RANGES) {
            cases.add(new Case("nextLong(" + range[0] + ", " + range[1] + ")",
                    r -> r.nextLong(range[0], range[1])));
            cases.add(new Case("longs(" + range[0] + ", " + range[1] + ")",
                    r -> r.longs(range[0], range[1]).limit(5).reduce(0, RandomTest::mix)));
        }
        cases.add(new Case("ints()", r -> r.ints().limit(5).asLongStream().reduce(0, RandomTest::mix)));
        cases.add(new Case("longs()", r -> r.longs().limit(5).reduce(0, RandomTest::mix)));
        cases.add(new Case("nextBoolean()", r -> r.nextBoolean() ? 1 : 0));
        cases.add(new Case("nextBytes, lengths 0..17", r -> {
            long h = 0;
            for (int len = 0; len < 18; ++len) {
                byte[] bytes = new byte[len];
                r.nextBytes(bytes);
                for (byte b : bytes) {
                    h = mix(h, b);
                }
                h = mix(h, len);
            }
            return h;
        }));
        cases.add(new Case("nextFloat()", r -> Float.floatToIntBits(r.nextFloat())));
        cases.add(new Case("nextFloat(10)", r -> Float.floatToIntBits(r.nextFloat(10f))));
        cases.add(new Case("nextDouble()", r -> Double.doubleToLongBits(r.nextDouble())));
        cases.add(new Case("nextDouble(10)", r -> Double.doubleToLongBits(r.nextDouble(10))));
        cases.add(new Case("nextDouble(-3.5, 1e9)", r -> Double.doubleToLongBits(r.nextDouble(-3.5, 1e9))));
        cases.add(new Case("doubles(-3.5, 1e9)", r -> r.doubles(-3.5, 1e9).limit(5)
                .mapToLong(Double::doubleToLongBits).reduce(0, RandomTest::mix)));
        cases.add(new Case("Collections.shuffle(ArrayList)", r -> {
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < 50; ++i) {
                list.add(i);
            }
            Collections.shuffle(list, r);
            return list.stream().mapToLong(i -> i).reduce(0, RandomTest::mix);
        }));
        cases.add(new Case("Collections.shuffle(LinkedList)", r -> {
            List<Integer> list = new LinkedList<>();
            for (int i = 0; i < 50; ++i) {
                list.add(i);
            }
            Collections.shuffle(list, r);
            return list.stream().mapToLong(i -> i).reduce(0, RandomTest::mix);
        }));
        return cases;
    }

    /** Runs each case 20 times on each seed, then all cases interleaved in an order picked per seed. */
    private static List<String> differences(LongFunction<RandomGenerator> factory, List<Case> cases, long[] expected) {
        long[] actual = new long[cases.size() + 1];
        for (int i = 0; i < cases.size(); ++i) {
            long h = 0;
            for (long seed : SEEDS) {
                RandomGenerator rng = factory.apply(seed);
                for (int j = 0; j < 20; ++j) {
                    h = mix(h, cases.get(i).op.applyAsLong(rng));
                }
            }
            actual[i] = h;
        }
        long h = 0;
        for (long seed : SEEDS) {
            RandomGenerator rng = factory.apply(seed);
            Random order = new Random(~seed);
            for (int j = 0; j < 300; ++j) {
                h = mix(h, cases.get(order.nextInt(cases.size())).op.applyAsLong(rng));
            }
        }
        actual[cases.size()] = h;

        List<String> differences = new ArrayList<>();
        if (expected.length != actual.length) {
            differences.add("expected " + expected.length + " values, got " + actual.length);
        }
        for (int i = 0; i < actual.length; ++i) {
            if (i >= expected.length || expected[i] != actual[i]) {
                differences.add((i < cases.size() ? cases.get(i).name : "interleaved") + ": " + actual[i] + "L");
            }
        }
        return differences;
    }

    @Test
    public void seededSequencesAreTheJdkOnes() {
        assertEquals(List.of(), differences(Random::new, cases(), new long[] {
            4529353865962221036L, 0L, -2504160376756047863L, -353363204274166157L, -5092111782408733453L,
            4077749102819885206L, -2580989598730039L, -8467571757336159337L, 6141274626011482530L,
            2166102863127719818L, 2395295448308844145L, -1085292172803383829L, 7365638679364422373L,
            -96670341204145160L, -2414449149304302728L, -2414449149304302728L, -5129201363997932463L,
            -5238808638438049235L, -6418652285760414716L, -4164681898600228915L, -5800628148650733619L,
            -4379263567782442843L, -6301342723725463675L, 1060252303980444242L, -5332291380479198300L,
            2388949231823934089L, -1137203978638962761L, 4529353865962221036L, 3990270203283207669L,
            2565273566855447447L, -4863013368414082832L, 8164835386939849891L, -2005541966488363836L,
            -5213286283685095313L, -1328737955519158934L, -2930148022470354089L, -5264890015016234294L,
            -2677919025247917138L, -2812565371954306149L, 8194969695264696345L, 6465830622219214145L,
            1543962710355137417L, 0L, 1131867106437678841L, -361187649986094925L, 8641431810447881092L,
            -3008320627476485877L, 2933192545364083411L, -7256271660680400619L, -3628552951599441748L,
            3440667149893674204L, 6017878616778533713L, -8231230909662994461L, -8231230909662994461L,
            7743451246447589136L, 8285464371745057010L, -2628127744482002881L, 8398530587706359491L,
            -797400341892104812L, 6229808396981914528L, -9108795046697596020L, 1543962710355137417L,
            1861545585668790673L, -1118690172686870676L, -7627271566463740362L, -8231230909662994461L,
            8118156968681733295L, 326091616430805622L, 6399078108128027829L, -5424009259929872390L,
            1850452231801460615L, -8618975167235098492L, 6583859206089444241L, 3990270203283207669L,
            1861545585668790673L, -2504160376756047863L, 3141676630336869806L, 6989823646738448321L,
            1220688400463952000L, 7163386658913266702L, -3559670521533580822L,
            -8893207830046932283L, 3329874321981195754L, -5812881382817162980L, -5812881382817162980L,
            -7455366193750841500L
        }));
    }

    private static List<Case> floatRangeCases() {
        return List.of(new Case("nextFloat(-3, 7)", r -> Float.floatToIntBits(r.nextFloat(-3f, 7f))),
                new Case("nextFloat(1, 1.5)", r -> Float.floatToIntBits(r.nextFloat(1f, 1.5f))));
    }

    // nextFloat(origin, bound) computes nextFloat() * (bound - origin) + origin, which Java rounds to float twice.
    // The JS backend computes it in double precision and rounds once, and C compilers may fuse it into an fma
    // (clang on arm64 does), so those backends give other floats.
    @Test
    @SkipPlatform({ TestPlatform.JAVASCRIPT, TestPlatform.C })
    public void seededFloatRangesAreTheJdkOnes() {
        assertEquals(List.of(), differences(Random::new, floatRangeCases(), new long[] {
                8296115877821257134L, -3865287433310974952L, -3868585526305170685L
        }));
        assertEquals(List.of(), differences(RandomTest::splitMix, floatRangeCases(), new long[] {
                -392800601372430420L, -1243114819351536307L, 1059347380194785349L
        }));
    }

    /** SplitMix64, as a RandomGenerator that only implements nextLong(). */
    private static RandomGenerator splitMix(long seed) {
        long[] state = { seed };
        return () -> {
            state[0] += 0x9E3779B97F4A7C15L;
            long z = state[0];
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            return z ^ (z >>> 31);
        };
    }

    // RandomGenerator's defaults are specified only loosely (e.g. nextInt(bound) "invokes nextInt() one or more
    // times to ensure a uniform distribution"); these are the sequences of OpenJDK's RandomSupport.
    @Test
    public void randomGeneratorDefaultsGiveTheJdkSequences() {
        assertEquals(List.of(), differences(RandomTest::splitMix, cases(), new long[] {
            4689174034723483047L, 0L, -4279955162694099647L, 7682993040954019456L, 3334772236648341504L,
            -9026538771285071855L, 4510244221467991216L, -7863172811109156737L, -3817992064959131461L,
            1386249689626035050L, 3745065420672353561L, 3360164561540774369L, 3559773974199647285L,
            5177229232404148488L, 7097019088731473220L, 7097019088731473220L, 5589908627860020316L,
            5658496699191684700L, 2366954116483798100L, 4510244221467991216L, 4258661352526057861L,
            -8398171997465240900L, -5587569545505145734L, 7169995142416366770L, -5359509884811058889L,
            -4117001149260225662L, 1241948834388866219L, 4689174034723483047L, 2037514973475106599L,
            -4313955036430230751L, 506006529056659094L, -5772365608655201978L, 8077159555735931084L,
            -5213286283685095313L, -1328737955519158934L, -2930148022470354089L, -5264890015016234294L,
            -5544141204971887736L, -3561894162652769851L, -3944442962236911155L, -4330970163865758738L,
            8278277062763941372L, 0L, 8161779103281395385L, 2569218504588036636L, -4680967276327096463L,
            4014880791115168134L, 3285302532498410981L, -1419107680770271068L, 5034113259581954398L,
            8556676985066311232L, -7865441567420566736L, 6384603434109135977L, 6384603434109135977L,
            -1518421645253047079L, -8753632202749684244L, 5021269850811305129L, -3148555651659688701L,
            7605741495140252011L, 4453762511966346278L, 4716485533244402920L, 8278277062763941372L,
            5496898327268425341L, -4483099121892941633L, 1529342359815106682L, 6384603434109135977L,
            -8137808293695290486L, -6915501332424258580L, -8575088639404040906L, -5424009259929872390L,
            1850452231801460615L, -8618975167235098492L, 6583859206089444241L, 2037514973475106599L,
            5496898327268425341L, -6681751262709370903L, -3679960056726414692L, -39690823136208252L,
            -5923130736614380692L, -5504435408518899702L, -5834176291143180896L,
            6570750829028324042L, 8259136572764428713L, -129424836835301903L, -129424836835301903L,
            6019567786430672236L
        }));
    }

    @Test
    @Disabled("RandomGenerator.nextGaussian() uses the polar method, not the JDK's McFarland ziggurat")
    public void randomGeneratorDefaultGaussiansAreTheJdkOnes() {
        RandomGenerator rng = splitMix(42);
        assertArrayEquals(new double[] { -0.7171651812264004, 1.026760016036332, 1.0366872928889295,
                0.9597335727977285 }, DoubleStream.generate(rng::nextGaussian).limit(4).toArray());
    }

    /** A Random whose next(bits) records bits and returns low-order bits of a counter, unlike Random's LCG. */
    private static final class RecordingRandom extends Random {
        final StringBuilder calls = new StringBuilder();
        long counter;

        @Override
        protected int next(int bits) {
            calls.append(bits).append(' ');
            long z = ++counter * 0x9E3779B97F4A7C15L;
            z ^= z >>> 32;
            return bits == 32 ? (int) z : (int) z & ((1 << bits) - 1);
        }
    }

    private static String record(ToLongFunction<Random> op) {
        RecordingRandom rnd = new RecordingRandom();
        long result = op.applyAsLong(rnd);
        return rnd.calls + "-> " + result;
    }

    // A subclass that overrides next(bits) gets the as-if algorithms: e.g. nextBoolean() is "return next(1) != 0;",
    // nextInt(bound) starts with next(31), and nextBytes takes the bytes of nextInt(), low byte first.
    @Test
    public void subclassesGetTheSpecifiedCallsToNext() {
        assertEquals(List.of("32 -> -511900244", "31 -> 48", "31 -> 404", "32 -> 15",
                "32 32 -> -2198594807818220712", "32 32 -> 452", "1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 -> 13107",
                "24 -> 1056574296", "26 27 -> 4600374632668993200", "32 32 -> 464692425"), List.of(
                record(r -> r.nextInt()),
                record(r -> r.nextInt(64)),
                record(r -> r.nextInt(1000)),
                record(r -> r.nextInt(5, 17)),
                record(r -> r.nextLong()),
                record(r -> r.nextLong(1000)),
                record(r -> {
                    long bits = 0;
                    for (int i = 0; i < 16; ++i) {
                        bits = bits * 2 + (r.nextBoolean() ? 1 : 0);
                    }
                    return bits;
                }),
                record(r -> Float.floatToIntBits(r.nextFloat())),
                record(r -> Double.doubleToLongBits(r.nextDouble())),
                record(r -> {
                    byte[] bytes = new byte[7];
                    r.nextBytes(bytes);
                    return Arrays.hashCode(bytes);
                })));
    }
}
