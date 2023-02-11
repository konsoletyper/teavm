/*
 *  Copyright 2020 by Joerg Hohwiller
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
package org.teavm.classlib.java.util.concurrent;

// https://www.reactive-streams.org/
public class TFlow {

    public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {

    }

    public interface Publisher<T> {
        void subscribe(Subscriber<? super T> s);
    }

    public interface Subscriber<T> {
        void onComplete();

        void onError(Throwable t);

        void onNext(T t);

        void onSubscribe(Subscription s);
    }

    public interface Subscription {
        void cancel();

        void request(long n);
    }

}
