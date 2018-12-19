/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class Promise<T> {
    public static final Promise<Void> VOID = Promise.of(null);

    private T value;
    private Promise<T> promise;
    private Throwable error;
    private State state = State.PENDING;
    private List<Then<T>> thenList;
    private List<Catch> catchList;

    Promise() {
    }

    public static <T> Promise<T> of(T value) {
        Promise<T> promise = new Promise<>();
        promise.complete(value);
        return promise;
    }

    public static Promise<?> error(Throwable e) {
        Promise<?> promise = new Promise<>();
        promise.completeWithError(e);
        return promise;
    }

    public static Promise<Void> allVoid(Collection<Promise<Void>> promises) {
        if (promises.isEmpty()) {
            return Promise.VOID;
        }
        AllVoidFunction all = new AllVoidFunction(promises.size());

        for (Promise<?> promise : promises) {
            promise.then(all.thenF).catchError(all.catchF);
        }

        return all.result;
    }


    public static <T> Promise<List<T>> all(Collection<Promise<T>> promises) {
        if (promises.isEmpty()) {
            return Promise.of(Collections.emptyList());
        }
        AllFunction<T> all = new AllFunction<>(promises.size());

        int i = 0;
        for (Promise<T> promise : promises) {
            promise.then(all.thenF(i++)).catchError(all.catchF);
        }

        return all.result;
    }

    static class AllVoidFunction {
        Promise<Void> result = new Promise<>();
        int count;
        boolean error;

        AllVoidFunction(int count) {
            this.result = result;
            this.count = count;
        }

        Function<Object, Void> thenF = v -> {
            if (!error && --count == 0) {
                result.complete(null);
            }
            return null;
        };

        Function<Throwable, Void> catchF = e -> {
            if (!error) {
                error = true;
                result.completeWithError(e);
            }
            return null;
        };
    }


    static class AllFunction<T> {
        Promise<List<T>> result = new Promise<>();
        List<T> list = new ArrayList<>();
        int count;
        boolean error;

        AllFunction(int count) {
            this.result = result;
            this.count = count;
            list.addAll(Collections.nCopies(count, null));
        }

        Function<T, Void> thenF(int index) {
            return v -> {
                if (!error) {
                    list.set(index, v);
                    if (--count == 0) {
                        result.complete(list);
                    }
                }
                return null;
            };
        };

        Function<Throwable, Void> catchF = e -> {
            if (!error) {
                error = true;
                result.completeWithError(e);
            }
            return null;
        };
    }

    public <S> Promise<S> then(Function<? super T, S> f) {
        Promise<S> result = new Promise<>();
        if (state == State.PENDING || state == State.WAITING_PROMISE) {
            if (thenList == null) {
                thenList = new ArrayList<>();
                thenList.add(new Then<>(f, result, false));
            }
        } else {
            passValue(f, result);
        }
        return result;
    }

    public Promise<Void> thenVoid(Consumer<T> f) {
        return then(r -> {
            f.accept(r);
            return null;
        });
    }

    public <S> Promise<S> thenAsync(Function<T, Promise<S>> f) {
        Promise<S> result = new Promise<>();
        if (state == State.PENDING || state == State.WAITING_PROMISE) {
            if (thenList == null) {
                thenList = new ArrayList<>();
                thenList.add(new Then<>(f, result, true));
            }
        } else if (state == State.COMPLETED) {
            passValueAsync(f, result);
        }
        return result;
    }

    public <S> Promise<S> catchError(Function<Throwable, S> f) {
        Promise<S> result = new Promise<>();
        if (state == State.PENDING || state == State.WAITING_PROMISE) {
            if (catchList == null) {
                catchList = new ArrayList<>();
                catchList.add(new Catch(f, result));
            }
        } else if (state == State.ERRORED) {
            passError(f, result);
        }
        return result;
    }


    public Promise<Void> catchVoid(Consumer<Throwable> f) {
        return catchError(e -> {
            f.accept(e);
            return null;
        });
    }

    <S> void passValue(Function<? super T, S> f, Promise<? super S> target) {
        if (state == State.COMPLETED) {
            S next;
            try {
                next = f.apply(value);
            } catch (Throwable e) {
                target.completeWithError(e);
                return;
            }
            target.complete(next);
        } else {
            target.completeWithError(error);
        }
    }

    <S> void passValueAsync(Function<T, Promise<S>> f, Promise<S> target) {
        if (state == State.COMPLETED) {
            target.completeAsync(f.apply(value));
        } else {
            target.completeWithError(error);
        }
    }

    <S> void passError(Function<Throwable, S> f, Promise<? super S> target) {
        S next;
        try {
            next = f.apply(error);
        } catch (Throwable e) {
            target.completeWithError(e);
            return;
        }
        target.complete(next);
    }

    void complete(T value) {
        if (state != State.PENDING) {
            throw new IllegalStateException("Already completed");
        }
        completeImpl(value);
    }

    void completeAsync(Promise<T> value) {
        if (state != State.PENDING) {
            throw new IllegalStateException("Already completed");
        }
        state = State.WAITING_PROMISE;

        value
                .then(result -> {
                    completeImpl(result);
                    return null;
                })
                .catchError(e -> {
                    completeWithErrorImpl(e);
                    return null;
                });
    }

    private void completeImpl(T value) {
        state = State.COMPLETED;
        this.value = value;

        if (thenList != null) {
            List<Then<T>> list = thenList;
            thenList = null;
            for (Then<T> then : list) {
                if (then.promise) {
                    passValueAsync((Function<T, Promise<Object>>) then.f, (Promise<Object>) then.target);
                } else {
                    passValue(then.f, (Promise<Object>) then.target);
                }
            }
        }
        catchList = null;
    }

    void completeWithError(Throwable e) {
        if (state != State.PENDING) {
            throw new IllegalStateException("Already completed");
        }
        completeWithErrorImpl(e);
    }

    void completeWithErrorImpl(Throwable e) {
        state = State.ERRORED;
        this.error = e;

        if (catchList != null) {
            List<Catch> list = catchList;
            thenList = null;
            for (Catch c : list) {
                passError(c.f, (Promise<Object>) c.target);
            }
        } else {
            e.printStackTrace();
        }
        thenList = null;
    }

    enum State {
        PENDING,
        WAITING_PROMISE,
        COMPLETED,
        ERRORED
    }

    static class Then<T> {
        Function<? super T, ?> f;
        Promise<?> target;
        boolean promise;

        Then(Function<? super T, ?> f, Promise<?> target, boolean promise) {
            this.f = f;
            this.target = target;
            this.promise = promise;
        }
    }

    static class Catch {
        Function<Throwable, ?> f;
        Promise<?> target;

        Catch(Function<Throwable, ?> f, Promise<?> target) {
            this.f = f;
            this.target = target;
        }
    }
}
