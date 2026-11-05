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

import java.util.concurrent.atomic.AtomicReferenceArray;

public final class AtomicArrayContract {
    private AtomicArrayContract() {
    }

    public static void main(String[] args) throws InterruptedException {
        String first = new String("first");
        String equalButDistinct = new String("first");
        String[] source = { first, null };
        AtomicReferenceArray<Object> array = new AtomicReferenceArray<>(source);
        source[0] = "changed";
        check(array.length() == 2 && array.get(0) == first, "defensive copy");
        check(!array.compareAndSet(0, equalButDistinct, "wrong"), "CAS uses identity");
        check(array.compareAndSet(0, first, null), "CAS permits null");
        Object arbitraryObject = new Object();
        array.set(1, arbitraryObject);
        check(array.getAndSet(1, first) == arbitraryObject, "array constructor does not retain String[] restriction");
        array.lazySet(0, "a");
        check("a".equals(array.getAndUpdate(0, value -> value + "b")), "getAndUpdate previous");
        check("abc".equals(array.updateAndGet(0, value -> value + "c")), "updateAndGet result");
        check("abc".equals(array.getAndAccumulate(0, "d", (a, b) -> a.toString() + b)), "accumulate previous");
        check("abcde".equals(array.accumulateAndGet(0, "e", (a, b) -> a.toString() + b)), "accumulate result");
        array.setPlain(0, first);
        check(array.getPlain(0) == first, "plain access");
        array.setOpaque(0, equalButDistinct);
        check(array.getOpaque(0) == equalButDistinct, "opaque access");
        array.setRelease(0, first);
        check(array.getAcquire(0) == first, "release/acquire access");
        check(array.compareAndExchange(0, equalButDistinct, null) == first, "exchange mismatch witness");
        check(array.compareAndExchangeAcquire(0, first, null) == first, "exchange acquire witness");
        check(array.compareAndExchangeRelease(0, null, first) == null, "exchange release witness");
        check(array.weakCompareAndSetPlain(0, first, first), "weak plain CAS");
        check(array.weakCompareAndSetAcquire(0, first, first), "weak acquire CAS");
        check(array.weakCompareAndSetRelease(0, first, first), "weak release CAS");
        check(array.weakCompareAndSetVolatile(0, first, first), "weak volatile CAS");
        check("[first, first]".equals(array.toString()), "string representation");
        check("[]".equals(new AtomicReferenceArray<>(0).toString()), "empty array");
        expect(IndexOutOfBoundsException.class, () -> array.get(-1));
        expect(IndexOutOfBoundsException.class, () -> array.set(2, null));
        expect(NegativeArraySizeException.class, () -> new AtomicReferenceArray<>(-1));
        expect(NullPointerException.class, () -> new AtomicReferenceArray<>((Object[]) null));
        expect(NullPointerException.class, () -> array.updateAndGet(0, null));
        AtomicReferenceArray<Integer> counter = new AtomicReferenceArray<>(new Integer[] { 0 });
        Runnable increment = () -> {
            for (int i = 0; i < 20; i++) {
                counter.updateAndGet(0, value -> {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        throw new AssertionError(e);
                    }
                    return value + 1;
                });
            }
        };
        Thread firstWorker = new Thread(increment);
        Thread secondWorker = new Thread(increment);
        firstWorker.start();
        secondWorker.start();
        firstWorker.join();
        secondWorker.join();
        check(counter.get(0) == 40, "concurrent functional updates preserve every increment");
        System.out.println("AtomicReferenceArray contract passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expect(Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
        } catch (Throwable error) {
            check(expected.isInstance(error), "wrong exception: " + error);
            return;
        }
        throw new AssertionError("Missing exception " + expected.getName());
    }
}
