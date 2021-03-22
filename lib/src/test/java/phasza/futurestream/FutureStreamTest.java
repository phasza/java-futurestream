package phasza.futurestream;

import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves that FutureStream behaves correctly
 */
@NoArgsConstructor
class FutureStreamTest {

    // Tests the of() creation and termination
    @Test
    void testOfDefaultExecutorSuccessful() throws InterruptedException {
        final int numberOfTasks = 20;
        try (FutureStream<Integer> futureStream = FutureStream.of(IntStream.range(0, numberOfTasks).boxed())) {
            final ConcurrentHashMap<Long, Integer> threads = new ConcurrentHashMap<>();
            final int result = futureStream.mapJoining(i -> {
                threads.putIfAbsent(Thread.currentThread().getId(), 0);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new UncheckedExecutionException(e);
                }
                return i;
            }).boxed().collect(Collectors.toList()).size();
            assertEquals(numberOfTasks, result, "Not all tasks have completed");
            assertEquals(
                    Runtime.getRuntime().availableProcessors(), threads.size(),
                    "Parallel threads count is not right");
        }
    }

    @Test
    void testOfDefaultExecutorExceptionDuringExecution() throws InterruptedException {
        final int numberOfTasks = 100;
        try (FutureStream<Integer> futureStream = FutureStream.of(IntStream.range(0, numberOfTasks).boxed())) {
            assertThrows(UncheckedExecutionException.class, () -> {
                futureStream.mapJoining(i -> {
                    throw new UncheckedExecutionException(String.format("%s %d", Thread.currentThread().getId(), i));
                }).boxed().collect(Collectors.toList());
            }, "Should have thrown an exception");
        }
    }
    //End of tests for of()

    //Tests for default executor and freeing
    @Test
    void testForEachParallelDefaultExecutor() throws InterruptedException {
        final int numberOfTasks = 20;
        try (FutureStream<Integer> futureStream = FutureStream.of(IntStream.range(0, numberOfTasks).boxed())) {
            final ConcurrentHashMap<Long, Integer> threads = new ConcurrentHashMap<>();
            futureStream.forEachParallel(i -> {
                threads.putIfAbsent(Thread.currentThread().getId(), 0);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new UncheckedExecutionException(e);
                }
            });
            assertEquals(
                    Runtime.getRuntime().availableProcessors(), threads.size(),
                    "Parallel threads count is not right");
        }
    }

    @Test
    void testForEachParallelDefaultExecutorThrowNoRetry() throws InterruptedException {
        final int numberOfTasks = 20;
        try (FutureStream<Integer> futureStream = FutureStream.of(IntStream.range(0, numberOfTasks).boxed())) {
            assertThrows(IOException.class, () -> futureStream.forEachParallel(i -> {
                throw new IOException("test exception");
            }, IOException.class), "Should have thrown an exception");
        }
    }

    @Test
    void testMapJoiningDefaultExecutorThrowNoRetry() throws InterruptedException {
        final int numberOfTasks = 20;
        try (FutureStream<Integer> futureStream = FutureStream.of(IntStream.range(0, numberOfTasks).boxed())) {
            assertThrows(UncheckedExecutionException.class,
                    () -> futureStream.mapJoining(i -> {
                        throw new IOException("test exception");
                    }, IOException.class).boxed().collect(Collectors.toList()), "Should have thrown an exception");
        }
    }

    @Test
    void testForEachParallelDefaultExecutorThrowWithRetry() throws InterruptedException, IOException {
        final int numberOfTasks = 20;
        AtomicInteger timesRetried = new AtomicInteger(0);
        try (FutureStream<Integer> futureStream = FutureStream
                .builder(IntStream.range(0, numberOfTasks).boxed())
                .retries(50)
                .build()) {
            futureStream.forEachParallel(i -> {
                if (timesRetried.get() < 10) {
                    timesRetried.incrementAndGet();
                    throw new IOException("test exception");
                }
            }, IOException.class);
            assertEquals(10, timesRetried.get());
        }
    }
    //End of test for default executor

    //Tests for custom executor and freeing
    @Test
    void testMapJoiningCustomExecutor() throws InterruptedException {
        final int numberOfTasks = 20;
        final int numberOfThreads = 3;
        try (FutureStream<Integer> futureStream = FutureStream
                .builder(IntStream.range(0, numberOfTasks).boxed())
                .executor(Executors.newFixedThreadPool(numberOfThreads))
                .shutdownExecutor(true)
                .build()) {
            final ConcurrentHashMap<Long, Integer> threads = new ConcurrentHashMap<>();
            final int result = futureStream.mapJoining(i -> {
                threads.putIfAbsent(Thread.currentThread().getId(), 0);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new UncheckedExecutionException(e);
                }
                return i;
            }).boxed().collect(Collectors.toList()).size();
            assertEquals(numberOfTasks, result, "Not all tasks have completed");
            assertEquals(numberOfThreads, threads.size(), "Parallel threads count is not right");
        }
    }

    @Test
    void testForEachParallelCustomExecutor() throws InterruptedException {
        final int numberOfTasks = 50;
        final int numberOfThreads = 11;
        try (FutureStream<Integer> futureStream = FutureStream
                .builder(IntStream.range(0, numberOfTasks).boxed())
                .executor(Executors.newFixedThreadPool(numberOfThreads))
                .build()) {
            final ConcurrentHashMap<Long, Integer> threads = new ConcurrentHashMap<>();
            futureStream.forEachParallel(i -> {
                threads.putIfAbsent(Thread.currentThread().getId(), 0);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new UncheckedExecutionException(e);
                }
            });
            assertEquals(numberOfThreads, threads.size(), "Parallel threads count is not right");
        }
    }

    @Test
    void testForEachParallelCustomExecutorThrowNoRetry() throws InterruptedException {
        final int numberOfTasks = 20;
        try (FutureStream<Integer> futureStream = FutureStream
                .builder(IntStream.range(0, numberOfTasks).boxed())
                .executor(Executors.newSingleThreadExecutor())
                .build()) {
            assertThrows(IOException.class, () -> futureStream.forEachParallel(i -> {
                throw new IOException("test exception");
            }, IOException.class), "Should have thrown an exception");
        }
    }

    @Test
    void testForEachParallelCustomExecutorThrowWithRetry() throws InterruptedException, IOException {
        final int numberOfTasks = 20;
        AtomicInteger timesRetried = new AtomicInteger(0);
        try (FutureStream<Integer> futureStream = FutureStream
                .builder(IntStream.range(0, numberOfTasks).boxed())
                .executor(Executors.newWorkStealingPool(11))
                .retries(50)
                .build()) {
            futureStream.forEachParallel(i -> {
                if (timesRetried.get() < 10) {
                    timesRetried.incrementAndGet();
                    throw new IOException("test exception");
                }
            }, IOException.class);
            assertEquals(10, timesRetried.get());
        }
    }

    @Test
    void testMapJoiningCustomExecutorThrowWithRetry() throws InterruptedException, IOException {
        final int numberOfTasks = 20;
        AtomicInteger timesRetried = new AtomicInteger(0);
        try (FutureStream<Integer> futureStream = FutureStream
                .builder(IntStream.range(0, numberOfTasks).boxed())
                .executor(Executors.newWorkStealingPool(11))
                .retries(50)
                .build()) {
            final long result = futureStream.mapJoining(i -> {
                if (timesRetried.get() < 10) {
                    timesRetried.incrementAndGet();
                    throw new IOException("test exception");
                }
                return 5;
            }, IOException.class).boxed().collect(Collectors.toList()).size();
            assertEquals(10, timesRetried.get());
            assertEquals(numberOfTasks, result, "The number of tasks executed was not correct");
        }
    }

    @Test
    void testMapJoiningCustomExecutorThrowWithExceeding() throws InterruptedException {
        final int numberOfTasks = 20;
        AtomicInteger timesRetried = new AtomicInteger(0);
        try (FutureStream<Integer> futureStream = FutureStream
                .builder(IntStream.range(0, numberOfTasks).boxed())
                .executor(Executors.newWorkStealingPool(11))
                .retries(2)
                .build()) {
            assertThrows(UncheckedExecutionException.class, () -> futureStream.mapJoining(i -> {
                timesRetried.incrementAndGet();
                throw new IOException("test exception");
            }, IOException.class).boxed().collect(Collectors.toList()));
            assertTrue(timesRetried.get() > 1);
        }
    }
    //End of test for custom executor

    //Tests for shutdownExecutor
    @Test
    void testCustomExecutorShutDown() throws InterruptedException {
        ExecutorService customService = Executors.newSingleThreadExecutor();
        try (FutureStream<Integer> ignored = FutureStream
                .builder(IntStream.range(0, 25).boxed())
                .shutdownExecutor(true)
                .executor(customService)
                .build()) {
            //Not doing here anything
        }

        assertTrue(customService.isTerminated(), "Executor should be terminated");
    }

    @Test
    void testCustomExecutorNotShutDown() throws InterruptedException {
        ExecutorService customService = Executors.newSingleThreadExecutor();
        try (FutureStream<Integer> ignored = FutureStream
                .builder(IntStream.range(0, 25).boxed())
                .shutdownExecutor(false)
                .executor(customService)
                .build()) {
            //Not doing here anything
        }

        assertFalse(customService.isTerminated(), "Executor should be terminated");
        customService.shutdownNow();
        customService.awaitTermination(100, TimeUnit.MILLISECONDS);
    }

    @Test
    void testDefaultExecutorNotShutDown() throws InterruptedException {
        FutureStream<Integer> futureStream = FutureStream
                .builder(IntStream.range(0, 25).boxed())
                .shutdownExecutor(false)
                .build();

        assertFalse(futureStream.getExecutor().isTerminated(), "Executor should be terminated");
        futureStream.getExecutor().shutdownNow();
        futureStream.getExecutor().awaitTermination(100, TimeUnit.MILLISECONDS);
    }
    //End of tests for shutdownExecutor

    //Tests for retry
    @Test
    void testNegativeRetryNoException() throws InterruptedException {
        try (FutureStream<Integer> futureStream = FutureStream
                .builder(IntStream.range(0, 25).boxed())
                .retries(-1)
                .build()) {
            final long result = futureStream.mapJoining(i -> i).boxed()
                    .collect(Collectors.toList()).size();
            assertEquals(25, result, "Result does not match the expected runs");
        }
    }

    @Test
    void testNegativeRetryException() throws InterruptedException {
        try (FutureStream<Integer> futureStream = FutureStream
                .builder(IntStream.range(0, 25).boxed())
                .retries(-1)
                .build()) {
            assertThrows(
                    IOException.class,
                    () -> futureStream.forEachParallel(i -> {
                        throw new IOException("e");
                    }, IOException.class), "Should have thrown");
        }
    }

    @Test
    void testZeroRetry() throws InterruptedException {
        try (FutureStream<Integer> futureStream = FutureStream
                .builder(IntStream.range(0, 25).boxed())
                .retries(0)
                .build()) {
            final long result = futureStream.mapJoining(i -> i).boxed()
                    .collect(Collectors.toList()).size();
            assertEquals(25, result, "Result does not match the expected runs");
        }
    }

    @Test
    void testZeroRetryException() throws InterruptedException {
        try (FutureStream<Integer> futureStream = FutureStream
                .builder(IntStream.range(0, 25).boxed())
                .retries(0)
                .build()) {
            assertThrows(
                    IOException.class,
                    () -> futureStream.forEachParallel(i -> {
                        throw new IOException("e");
                    }, IOException.class), "Should have thrown");
        }
    }

    @Test
    void testRetryWithNotSpecifiedException() throws InterruptedException {
        try (FutureStream<Integer> futureStream = FutureStream
                .builder(IntStream.range(0, 25).boxed())
                .retries(0)
                .build()) {
            assertThrows(
                    UncheckedExecutionException.class,
                    () -> futureStream.forEachParallel(i -> {
                        throw new IllegalStateException("e");
                    }, IOException.class), "Should have thrown");
        }
    }
    //End of tests for retry

    //Test for combining operations
    @Test
    void testChainMultipleMapJoining() throws InterruptedException {
        final int numberOfTasks = 20;
        try (FutureStream<Integer> futureStream = FutureStream.of(IntStream.range(0, numberOfTasks).boxed())) {
            final ConcurrentHashMap<Long, Integer> threads = new ConcurrentHashMap<>();
            final long result = futureStream.mapJoining(i -> {
                threads.putIfAbsent(Thread.currentThread().getId(), 0);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new UncheckedExecutionException(e);
                }
                return i;
            }).mapJoining(i -> {
                threads.putIfAbsent(Thread.currentThread().getId(), 0);
                return 50;
            }).boxed().mapToLong(i -> i).sum();
            assertEquals(numberOfTasks * 50, result, "Not all tasks have completed successfully");
            assertEquals(
                    Runtime.getRuntime().availableProcessors(), threads.size(),
                    "Parallel threads count is not right");
        }
    }

    @Test
    void testChainMapJoiningWithForeach() throws InterruptedException {
        final int numberOfTasks = 20;
        try (FutureStream<Integer> futureStream = FutureStream.of(IntStream.range(0, numberOfTasks).boxed())) {
            final ConcurrentHashMap<Long, Integer> threads = new ConcurrentHashMap<>();
            final ConcurrentLinkedQueue<Long> finalResult = new ConcurrentLinkedQueue<>();
            futureStream.mapJoining(i -> {
                threads.putIfAbsent(Thread.currentThread().getId(), 0);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new UncheckedExecutionException(e);
                }
                return i;
            }).forEachParallel(i -> {
                threads.putIfAbsent(Thread.currentThread().getId(), 0);
                finalResult.add(50L);
            });
            assertEquals(numberOfTasks, finalResult.size(), "Not all tasks have completed successfully");
            assertEquals(
                    Runtime.getRuntime().availableProcessors(), threads.size(),
                    "Parallel threads count is not right");
        }
    }

    //End of tests for combining operations
}