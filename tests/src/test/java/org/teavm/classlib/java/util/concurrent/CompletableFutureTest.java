/*
 *  Copyright 2025 konsoletyper.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
@SkipPlatform(TestPlatform.WASI)
public class CompletableFutureTest {

    @Test
    public void completedFuture() throws Exception {
        CompletableFuture<String> future = CompletableFuture.completedFuture("hello");
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        assertEquals("hello", future.get());
        assertEquals("hello", future.join());
        assertEquals("hello", future.getNow(null));
    }

    @Test
    public void failedFuture() {
        CompletableFuture<String> future = CompletableFuture.failedFuture(new RuntimeException("error"));
        assertTrue(future.isDone());
        assertTrue(future.isCompletedExceptionally());
        try {
            future.join();
            assertTrue("Expected CompletionException", false);
        } catch (CompletionException e) {
            assertEquals("error", e.getCause().getMessage());
        }
    }

    @Test
    public void completeValue() {
        CompletableFuture<String> future = new CompletableFuture<>();
        assertFalse(future.isDone());
        assertTrue(future.complete("done"));
        assertTrue(future.isDone());
        assertEquals("done", future.getNow(null));
        assertFalse(future.complete("other"));
    }

    @Test
    public void completeExceptionally() {
        CompletableFuture<String> future = new CompletableFuture<>();
        assertTrue(future.completeExceptionally(new RuntimeException("fail")));
        assertTrue(future.isCompletedExceptionally());
        assertFalse(future.completeExceptionally(new RuntimeException("other")));
    }

    @Test
    public void cancel() {
        CompletableFuture<String> future = new CompletableFuture<>();
        assertTrue(future.cancel(true));
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
    }

    @Test
    public void thenApply() throws Exception {
        CompletableFuture<Integer> future = CompletableFuture.completedFuture("hello")
                .thenApply(String::length);
        assertEquals(Integer.valueOf(5), future.get());
    }

    @Test
    public void thenAccept() throws Exception {
        int[] result = { 0 };
        CompletableFuture<Void> future = CompletableFuture.completedFuture("hello")
                .thenAccept(s -> result[0] = s.length());
        future.get();
        assertEquals(5, result[0]);
    }

    @Test
    public void thenRun() throws Exception {
        boolean[] ran = { false };
        CompletableFuture<Void> future = CompletableFuture.completedFuture("hello")
                .thenRun(() -> ran[0] = true);
        future.get();
        assertTrue(ran[0]);
    }

    @Test
    public void exceptionallyNormal() throws Exception {
        CompletableFuture<String> future = CompletableFuture.completedFuture("hello")
                .exceptionally(e -> "fallback");
        assertEquals("hello", future.get());
    }

    @Test
    public void exceptionallyError() throws Exception {
        CompletableFuture<String> future = CompletableFuture.<String>failedFuture(new RuntimeException("error"))
                .exceptionally(e -> "fallback");
        assertEquals("fallback", future.get());
    }

    @Test
    public void handleNormal() throws Exception {
        CompletableFuture<Integer> future = CompletableFuture.completedFuture("hello")
                .handle((v, ex) -> v != null ? v.length() : -1);
        assertEquals(Integer.valueOf(5), future.get());
    }

    @Test
    public void handleError() throws Exception {
        CompletableFuture<Integer> future = CompletableFuture.<String>failedFuture(new RuntimeException("error"))
                .handle((v, ex) -> ex != null ? -1 : v.length());
        assertEquals(Integer.valueOf(-1), future.get());
    }

    @Test
    public void whenCompleteNormal() throws Exception {
        boolean[] completed = { false };
        CompletableFuture<String> future = CompletableFuture.completedFuture("hello")
                .whenComplete((v, ex) -> completed[0] = (v != null && ex == null));
        assertEquals("hello", future.get());
        assertTrue(completed[0]);
    }

    @Test
    public void thenCompose() throws Exception {
        CompletableFuture<Integer> future = CompletableFuture.completedFuture("hello")
                .thenCompose(s -> CompletableFuture.completedFuture(s.length()));
        assertEquals(Integer.valueOf(5), future.get());
    }

    @Test
    public void supplyAsync() throws Exception {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "async result");
        assertEquals("async result", future.get());
    }

    @Test
    public void runAsync() throws Exception {
        boolean[] ran = { false };
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> ran[0] = true);
        future.get();
        assertTrue(ran[0]);
    }

    @Test
    public void allOf() throws Exception {
        CompletableFuture<String> f1 = CompletableFuture.completedFuture("a");
        CompletableFuture<String> f2 = CompletableFuture.completedFuture("b");
        CompletableFuture<Void> all = CompletableFuture.allOf(f1, f2);
        all.get();
        assertTrue(f1.isDone());
        assertTrue(f2.isDone());
    }

    @Test
    public void anyOf() throws Exception {
        CompletableFuture<String> f1 = CompletableFuture.completedFuture("first");
        CompletableFuture<String> f2 = new CompletableFuture<>();
        CompletableFuture<Object> any = CompletableFuture.anyOf(f1, f2);
        assertEquals("first", any.get());
    }

    @Test
    public void toStringTest() {
        CompletableFuture<String> done = CompletableFuture.completedFuture("hello");
        assertTrue(done.toString().contains("Completed normally"));
        CompletableFuture<String> pending = new CompletableFuture<>();
        assertTrue(pending.toString().contains("Not completed"));
    }
}
