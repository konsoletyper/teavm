/*
 *  Copyright 2024 Bernd Busse.
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
package org.teavm.samples.promise;

import java.util.Arrays;
import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSString;
import org.teavm.jso.function.JSConsumer;
import org.teavm.jso.function.JSMapping;
import org.teavm.jso.function.JSSupplier;

public final class PromiseExample {
    private static long start = System.currentTimeMillis();

    private PromiseExample() {
    }

    public static void main(String[] args) throws InterruptedException {
        report(Arrays.toString(args));
        report("");

        checkFunctionalInterface();

        runSimplePromise();

        runComplexPromise();

        final var lock = new Object();
        runLongRunningPromise(lock);
        synchronized (lock) {
            report("Lock acquired");
            lock.wait(20000);
        }

        combinePromises(lock);
        synchronized (lock) {
            report("Lock acquired");
            lock.wait(20000);
        }

        testNativePromises(lock);

        report("Finished main thread");
    }

    private static void report(String message) {
        var current = System.currentTimeMillis() - start;
        System.out.println("[" + Thread.currentThread().getName() + "]/" + current + ": " + message);
    }

    private static void checkFunctionalInterface() {
        JSSupplier<Integer> supplier = () -> 23;

        JSMapping<Integer, Integer> addTwenty = value -> value + 20;
        JSMapping<Integer, Integer> subTwenty = value -> value - 20;
        JSMapping<Integer, Boolean> isPositive = value -> value >= 0;

        JSConsumer<Integer> print = value -> report("My value: " + value.toString());
        JSConsumer<Integer> print2 = value -> report("My value plus 10: " + Integer.valueOf(value + 10).toString());

        var value = supplier.get();
        report("Supplied value: " + value);

        value = addTwenty.apply(value);
        report("Value plus 20: " + value.toString());

        value = subTwenty.apply(value);
        report("Value minus 20: " + value.toString());

        var value2 = isPositive.apply(value);
        report("Value is positive: " + value2.toString());

        var subFourty = subTwenty.andThen(subTwenty);
        value = subFourty.apply(value);
        report("Value minus 40: " + value.toString());

        var plusFourty = addTwenty.compose(addTwenty).andThen(addTwenty.compose(subTwenty));
        value = plusFourty.apply(value);
        report("Value plus 40: " + value.toString());

        value2 = subFourty.andThen(isPositive).apply(value);
        report("Value minus 40 is positive: " + value2.toString());

        print.accept(value);
        var printExtended = print.andThen(print2);
        printExtended.accept(value);
    }

    private static void runSimplePromise() {
        new JSPromise<>((resolve, reject) -> {
            report("Simple promise execution");

            report("Resolving with 'success'");
            resolve.accept("success");
        });
    }

    private static void runComplexPromise() {
        new JSPromise<>((resolve, reject) -> {
            report("Complex promise execution");

            report("Resolving with 'step1'");
            resolve.accept("step1");
        })
        .then(value -> {
            report("Resolved with '" + value + "'");
            report("... and resolve with 'step2'");
            return "step2";
        })
        .then(value -> {
            report("Resolved with '" + value + "'");
            report("... and throw exception");
            throw new RuntimeException("Exception in promise handler");
        }, reason -> {
            report("Failed unexpectedly with reason: " + reason.toString());
            return reason.toString();
        })
        .then(value -> {
            report("Resolved unexpectedly with '" + value + "'");
            return value;
        }, reason -> {
            report("Failed expectedly with reason: " + reason.toString());
            return reason.toString();
        })
        .flatThen(value -> {
            report("Resolved with '" + value + "'");
            report("... and resolve with resolved promise");
            return JSPromise.resolve("step3");
        })
        .flatThen(value -> {
            report("Resolved with '" + value + "'");
            report("... and resolve with rejected promise");
            return JSPromise.reject("step4");
        })
        .catchError(reason -> {
            report("Catched reject reason '" + reason.toString() + "'");
            return reason.toString();
        })
        .flatThen(value -> {
            report("Resolved with '" + value + "'");
            report("... and resolve with new promise");
            return new JSPromise<>((resolve, reject) -> {
                report("Inner promise");
                report("Reject with 'step from inner'");
                reject.accept("step from inner");
            });
        })
        .then(value -> {
            report("Resolved unexpectedly with '" + value + "'");
            return value;
        })
        .catchError(reason -> {
            report("Catched reject reason '" + reason.toString() + "'");
            return reason.toString();
        })
        .onSettled(() -> {
            report("Promise has finally settled");
            return null;
        });
    }

    private static void runLongRunningPromise(Object lock) {
        new JSPromise<>((resolve, reject) -> {
            report("Long promise exection");
            report("Wait for a while...");
            Window.setTimeout(() -> {
                report("... and resolve with 'done'");
                resolve.accept("done");
            }, 2000);
        }).then(value -> {
            report("Resolved with '" + value + "'");
            synchronized (lock) {
                lock.notify();
            }
            return value;
        });
    }

    private static void combinePromises(Object lock) throws InterruptedException {
        var promises = new JSArray<JSPromise<String>>(3);

        report("Start 3 successful promises");
        promises.set(0, JSPromise.resolve("success1"));
        promises.set(1, JSPromise.resolve("success2"));
        promises.set(2, JSPromise.resolve("success3"));

        var allPromises = JSPromise.all(promises);
        allPromises.then(value -> {
            report("All promises resolved to: " + value);
            return "success";
        }, reason -> {
            report("At least one promise rejected with: " + reason.toString());
            return "failure";
        }).onSettled(() -> {
            synchronized (lock) {
                lock.notify();
            }
            return null;
        });

        synchronized (lock) {
            report("Lock acquired");
            lock.wait(20000);
        }

        report("Start 1 successful and 2 rejected promise");
        promises.set(0, JSPromise.resolve("success1"));
        promises.set(1, JSPromise.reject("failure2"));
        promises.set(2, JSPromise.reject("failure3"));

        allPromises = JSPromise.all(promises);
        allPromises.then(value -> {
            report("All promises resolved to: " + value);
            return "success";
        }, reason -> {
            report("At least one promise rejected with: " + reason.toString());
            return "failure";
        }).onSettled(() -> {
            synchronized (lock) {
                lock.notify();
            }
            return null;
        });

        synchronized (lock) {
            report("Lock acquired");
            lock.wait(20000);
        }

        var settledPromises = JSPromise.allSettled(promises);
        settledPromises.then(value -> {
            report(value.getLength() + " promises settled to:");
            for (int i = 0; i < value.getLength(); ++i) {
                var item = value.get(i);
                var msg = "-- Promise " + i + " " + item.getStatus() + " with: ";
                if (item.getStatus().stringValue().equals("fulfilled")) {
                    msg += item.getValue();
                } else if (item.getStatus().stringValue().equals("rejected")) {
                    msg += item.getReason().toString();
                }
                report(msg);
            }
            return "success";
        }).onSettled(() -> {
            synchronized (lock) {
                lock.notify();
            }
            return null;
        });

        synchronized (lock) {
            report("Lock acquired");
            lock.wait(20000);
        }

        var anyPromise = JSPromise.any(promises);
        anyPromise.then(value -> {
            report("At least one promise resolved to: " + value);
            return "success";
        }, reason -> {
            report("All promises rejected with: " + reason.toString());
            return "failure";
        }).onSettled(() -> {
            synchronized (lock) {
                lock.notify();
            }
            return null;
        });

        synchronized (lock) {
            report("Lock acquired");
            lock.wait(20000);
        }

        report("Start 3 rejected promises");
        promises.set(0, JSPromise.reject("failure1"));
        promises.set(1, JSPromise.reject("failure2"));
        promises.set(2, JSPromise.reject("failure3"));

        anyPromise = JSPromise.any(promises);
        anyPromise.then(value -> {
            report("At least one promise resolved to: " + value);
            return "success";
        }, reason -> {
            report("All promises rejected with: " + reason.toString());
            return "failure";
        }).onSettled(() -> {
            synchronized (lock) {
                lock.notify();
            }
            return null;
        });

        synchronized (lock) {
            report("Lock acquired");
            lock.wait(20000);
        }

        report("Start 3 delayed promises");
        promises.set(0, new JSPromise<>((resolve, reject) -> Window.setTimeout(() -> resolve.accept("success1"), 200)));
        promises.set(1, new JSPromise<>((resolve, reject) -> Window.setTimeout(() -> reject.accept("failure1"), 100)));
        promises.set(2, new JSPromise<>((resolve, reject) -> Window.setTimeout(() -> resolve.accept("success3"), 50)));

        anyPromise = JSPromise.race(promises);
        anyPromise.then(value -> {
            report("First settled promise resolved to: " + value);
            return "success";
        }, reason -> {
            report("First settled promise rejected with: " + reason.toString());
            return "failure";
        }).onSettled(() -> {
            synchronized (lock) {
                lock.notify();
            }
            return null;
        });

        synchronized (lock) {
            report("Lock acquired");
            lock.wait(20000);
        }

        report("Start 3 delayed promises");
        promises.set(0, new JSPromise<>((resolve, reject) -> Window.setTimeout(() -> resolve.accept("success1"), 200)));
        promises.set(1, new JSPromise<>((resolve, reject) -> Window.setTimeout(() -> reject.accept("failure1"), 50)));
        promises.set(2, new JSPromise<>((resolve, reject) -> Window.setTimeout(() -> resolve.accept("success3"), 100)));

        anyPromise = JSPromise.race(promises);
        anyPromise.then(value -> {
            report("First settled promise resolved to: " + value);
            return "success";
        }, reason -> {
            report("First settled promise rejected with: " + reason.toString());
            return "failure";
        }).onSettled(() -> {
            synchronized (lock) {
                lock.notify();
            }
            return null;
        });
    }

    private static void testNativePromises(Object lock) throws InterruptedException {
        report("Get promise from native method");
        var nativePromise = getNativePromise(JSString.valueOf("success from native"));
        nativePromise.then(value -> {
            report("Native resolved expectedly with '" + value + "'");
            return value;
        }, reason -> {
            report("Native rejected unexpectedly with '" + reason.toString() + "'");
            return reason.toString();
        }).onSettled(() -> {
            synchronized (lock) {
                lock.notify();
            }
            return null;
        });

        synchronized (lock) {
            report("Lock acquired");
            lock.wait(20000);
        }

        nativePromise = getNativeRejectingPromise(JSString.valueOf("failure from native"));
        nativePromise.then(value -> {
            report("Native resolved unexpectedly with '" + value + "'");
            return value;
        }, reason -> {
            report("Native rejected expectedly with '" + reason.toString() + "'");
            return reason.toString();
        }).onSettled(() -> {
            synchronized (lock) {
                lock.notify();
            }
            return null;
        });

        synchronized (lock) {
            report("Lock acquired");
            lock.wait(20000);
        }

        report("Pass promise to native method");
        handlePromise(new JSPromise<>((resolve, reject) -> {
            resolve.accept(JSString.valueOf("Resolved from Java"));
        }));

        handlePromise(new JSPromise<>((resolve, reject) -> {
            reject.accept(JSString.valueOf("Rejected from Java"));
        }));
    }

    @JSBody(params = "msg", script = "return new Promise((resolve, reject) => {"
        + "    setTimeout(() => resolve(msg), 500);"
        + "});")
    private static native JSPromise<JSString> getNativePromise(JSString msg);

    @JSBody(params = "msg", script = "return new Promise((resolve, reject) => {"
        + "    setTimeout(() => reject(msg), 500);"
        + "});")
    private static native JSPromise<JSString> getNativeRejectingPromise(JSString msg);

    @JSBody(params = "promise", script = "promise.then("
        + "    (value) => console.log('success:', value),"
        + "    (reason) => console.log('failure:', reason));")
    private static native void handlePromise(JSPromise<JSString> promise);
}
