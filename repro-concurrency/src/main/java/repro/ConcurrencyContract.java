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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class ConcurrencyContract {
    private ConcurrencyContract() {
    }

    public static void main(String[] args) throws Exception {
        queueContract();
        readWriteLockContract();
        System.out.println("Concurrency contract passed");
    }

    private static void queueContract() throws Exception {
        LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>(1);
        AtomicInteger taken = new AtomicInteger();
        AtomicInteger entered = new AtomicInteger();
        Thread consumer = new Thread(() -> {
            entered.set(1);
            try {
                taken.set(queue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "queue-consumer");
        consumer.start();
        awaitFlag(entered, "consumer started");
        Thread.sleep(2);
        check(taken.get() == 0, "take waits until an item arrives");
        check(queue.offer(1), "queue accepts item after consumer waits");
        consumer.join();
        check(queue.offer(2), "queue accepts another item after take frees capacity");
        check(taken.get() == 1 && queue.peek() == 2, "take preserves FIFO order");
        check(queue.poll(1, TimeUnit.MILLISECONDS) == 2, "timed poll returns available item");
        check(queue.poll(1, TimeUnit.MILLISECONDS) == null, "timed poll returns null on timeout");

        queue.put(3);
        check(!queue.offer(4) && queue.remainingCapacity() == 0, "bounded queue rejects over-capacity offer");
        check(!queue.offer(4, 1, TimeUnit.MILLISECONDS), "timed offer returns false while full");
        check(queue.poll() == 3, "poll removes head");

        AtomicInteger interruptEntered = new AtomicInteger();
        AtomicInteger interruptObserved = new AtomicInteger();
        AtomicInteger interruptFlagAfterThrow = new AtomicInteger();
        Thread interruptedConsumer = new Thread(() -> {
            interruptEntered.set(1);
            try {
                new LinkedBlockingQueue<Integer>().take();
            } catch (InterruptedException expected) {
                interruptFlagAfterThrow.set(Thread.currentThread().isInterrupted() ? 1 : 0);
                interruptObserved.set(1);
            }
        }, "interrupted-consumer");
        interruptedConsumer.start();
        awaitFlag(interruptEntered, "interruptible consumer started");
        Thread.sleep(2);
        interruptedConsumer.interrupt();
        interruptedConsumer.join();
        check(interruptObserved.get() == 1 && interruptFlagAfterThrow.get() == 0,
                "interrupted take clears interrupt status (observed=" + interruptObserved.get()
                        + ", flag=" + interruptFlagAfterThrow.get() + ")");

        LinkedBlockingQueue<Integer> empty = new LinkedBlockingQueue<>();
        AtomicInteger taskStarted = new AtomicInteger();
        FutureTask<Integer> blocked = new FutureTask<>(() -> {
            taskStarted.set(1);
            return empty.take();
        });
        Thread queueWorker = new Thread(blocked, "cancelled-queue-worker");
        queueWorker.start();
        awaitFlag(taskStarted, "queue task started");
        Thread.sleep(2);
        check(blocked.cancel(true), "blocked queue future cancels");
        queueWorker.join();
        try {
            blocked.get();
            throw new AssertionError("cancelled queue future should fail");
        } catch (CancellationException expected) {
            // Expected.
        }

        LinkedBlockingQueue<Integer> collectionQueue = new LinkedBlockingQueue<>(Arrays.asList(5, 6, 7));
        check(collectionQueue.contains(6) && collectionQueue.remove(6), "contains and remove");
        check(collectionQueue.drainTo(new ArrayList<>(), 1) == 1, "bounded drainTo");
        check(Arrays.equals(collectionQueue.toArray(), new Object[] { 7 }), "toArray preserves remaining order");

        LinkedBlockingQueue<Integer> duplicateQueue = new LinkedBlockingQueue<>(Arrays.asList(8, 8));
        java.util.Iterator<Integer> iterator = duplicateQueue.iterator();
        check(iterator.next() == 8, "iterator returns snapshot head");
        check(duplicateQueue.poll() == 8, "concurrent removal detaches iterator node");
        iterator.remove();
        check(duplicateQueue.size() == 1, "iterator removal does not remove a different equal node");
    }

    private static void readWriteLockContract() throws Exception {
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        lock.writeLock().lock();
        lock.writeLock().lock();
        check(lock.getWriteHoldCount() == 2 && lock.isWriteLockedByCurrentThread(), "write lock reentrancy");
        lock.readLock().lock();
        check(lock.getReadHoldCount() == 1 && lock.getReadLockCount() == 1,
                "writer may also acquire the read lock");
        lock.readLock().unlock();
        lock.writeLock().unlock();
        lock.writeLock().unlock();

        lock.readLock().lock();
        check(lock.readLock().tryLock() && lock.getReadHoldCount() == 2, "read lock reentrancy");
        lock.readLock().unlock();
        AtomicInteger writerEntered = new AtomicInteger();
        AtomicInteger writerAcquired = new AtomicInteger();
        Thread writer = new Thread(() -> {
            writerEntered.set(1);
            lock.writeLock().lock();
            try {
                writerAcquired.set(1);
            } finally {
                lock.writeLock().unlock();
            }
        }, "read-held-writer");
        writer.start();
        awaitFlag(writerEntered, "writer started");
        Thread.sleep(2);
        check(writerAcquired.get() == 0 && !lock.writeLock().tryLock(), "writer waits for active readers");
        lock.readLock().unlock();
        writer.join();
        check(writerAcquired.get() == 1, "writer acquires after reader release");

        lock.writeLock().lock();
        AtomicInteger lockInterruptStarted = new AtomicInteger();
        AtomicInteger lockInterruptCaught = new AtomicInteger();
        AtomicInteger lockInterruptStatus = new AtomicInteger(1);
        Thread interruptedWaiter = new Thread(() -> {
            lockInterruptStarted.set(1);
            try {
                lock.writeLock().lockInterruptibly();
                lock.writeLock().unlock();
            } catch (InterruptedException expected) {
                lockInterruptStatus.set(Thread.currentThread().isInterrupted() ? 1 : 0);
                lockInterruptCaught.set(1);
            }
        }, "interrupted-lock-waiter");
        interruptedWaiter.start();
        awaitFlag(lockInterruptStarted, "interruptible lock waiter started");
        awaitQueueLength(lock, 1);
        interruptedWaiter.interrupt();
        interruptedWaiter.join();
        check(lockInterruptCaught.get() == 1 && lockInterruptStatus.get() == 0 && lock.getQueueLength() == 0,
                "interruptible lock acquisition removes its waiter and clears interrupt status");
        lock.writeLock().unlock();

        ReentrantReadWriteLock fair = new ReentrantReadWriteLock(true);
        fair.writeLock().lock();
        List<Integer> order = new ArrayList<>();
        Thread firstWriter = queuedWriter(fair, order, 1);
        firstWriter.start();
        awaitQueueLength(fair, 1);
        Thread middleReader = new Thread(() -> {
            fair.readLock().lock();
            try {
                order.add(2);
            } finally {
                fair.readLock().unlock();
            }
        }, "fair-middle-reader");
        middleReader.start();
        awaitQueueLength(fair, 2);
        Thread lastWriter = queuedWriter(fair, order, 3);
        lastWriter.start();
        awaitQueueLength(fair, 3);
        fair.writeLock().unlock();
        firstWriter.join();
        middleReader.join();
        lastWriter.join();
        check(order.equals(Arrays.asList(1, 2, 3)), "fair mode honors waiter order across readers and writers");

        fair.writeLock().lock();
        AtomicInteger timedResult = new AtomicInteger(1);
        Thread timedWriter = new Thread(() -> {
            try {
                timedResult.set(fair.writeLock().tryLock(2, TimeUnit.MILLISECONDS) ? 2 : 0);
                if (timedResult.get() == 2) {
                    fair.writeLock().unlock();
                }
            } catch (InterruptedException e) {
                timedResult.set(-1);
            }
        }, "timed-writer");
        timedWriter.start();
        timedWriter.join();
        check(timedResult.get() == 0, "timed writer expires");
        fair.writeLock().unlock();

        Condition condition = lock.writeLock().newCondition();
        lock.writeLock().lock();
        lock.writeLock().lock();
        AtomicInteger signallerReady = new AtomicInteger();
        Thread signaller = new Thread(() -> {
            signallerReady.set(1);
            lock.writeLock().lock();
            try {
                condition.signal();
            } finally {
                lock.writeLock().unlock();
            }
        }, "condition-signaller");
        signaller.start();
        awaitFlag(signallerReady, "condition signaller started");
        condition.await();
        check(lock.getWriteHoldCount() == 2, "condition await reacquires the full write hold count");
        lock.writeLock().unlock();
        lock.writeLock().unlock();
        signaller.join();

        lock.writeLock().lock();
        try {
            check(!condition.await(1, TimeUnit.MILLISECONDS), "condition timed await returns false");
        } finally {
            lock.writeLock().unlock();
        }

        AtomicInteger conditionInterruptStarted = new AtomicInteger();
        AtomicInteger conditionInterruptCaught = new AtomicInteger();
        AtomicInteger conditionLockReacquired = new AtomicInteger();
        Thread interruptedConditionWaiter = new Thread(() -> {
            lock.writeLock().lock();
            try {
                conditionInterruptStarted.set(1);
                condition.await();
            } catch (InterruptedException expected) {
                conditionInterruptCaught.set(1);
                conditionLockReacquired.set(lock.isWriteLockedByCurrentThread() ? 1 : 0);
            } finally {
                lock.writeLock().unlock();
            }
        }, "interrupted-condition-waiter");
        interruptedConditionWaiter.start();
        awaitFlag(conditionInterruptStarted, "condition waiter started");
        lock.writeLock().lock();
        lock.writeLock().unlock();
        interruptedConditionWaiter.interrupt();
        interruptedConditionWaiter.join();
        check(conditionInterruptCaught.get() == 1 && conditionLockReacquired.get() == 1,
                "interrupted condition wait reacquires its lock before throwing");
        try {
            lock.readLock().newCondition();
            throw new AssertionError("read lock condition should be unsupported");
        } catch (UnsupportedOperationException expected) {
            // JDK ReadLock.newCondition has this behavior.
        }
    }

    private static Thread queuedWriter(ReentrantReadWriteLock lock, List<Integer> order, int value) {
        return new Thread(() -> {
            lock.writeLock().lock();
            try {
                order.add(value);
            } finally {
                lock.writeLock().unlock();
            }
        }, "fair-writer-" + value);
    }

    private static void awaitFlag(AtomicInteger flag, String description) throws InterruptedException {
        for (int i = 0; i < 100 && flag.get() == 0; ++i) {
            Thread.sleep(1);
        }
        check(flag.get() != 0, description);
    }

    private static void awaitQueueLength(ReentrantReadWriteLock lock, int length) throws InterruptedException {
        for (int i = 0; i < 100 && lock.getQueueLength() < length; ++i) {
            Thread.sleep(1);
        }
        check(lock.getQueueLength() == length, "expected lock queue length " + length);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
