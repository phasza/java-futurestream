# java-futurestream

A simple library to wrap the `java.util.Stream` with the following additional functionality:
* Parallel mapping and terminating streams using custom executor service
* Ability to define the number of parallel threads programmatically
* Propagate exception to the caller of the stream termination
* Defining a number of times an operation can fail and retry

The intention of the library is only to provide the minimal functionality to achieve the above. Re-implementing the whole Stream interface and ReferencePipeline is not intended. However, requests for operations are welcome.

## Basic Usage

```java
public Map<Path, String> calcChecksumsForFiles(Path sourceDir) {
    try (FutureStream<Path> files = FutureStream
            .of(Files.walk(sourceDir).filter(Files::isRegularFile))) {
        return files.mapJoining(this::getChecksumForFile, IOException.class).boxed().collect(
                    Collectors.toMap(i -> i.getKey(), i -> i.getValue()));
    }
}

private AbstractMap.SimpleEntry<Path, String> getChecksumForFile(Path file) throws IOException {
...
}
```
The code example above calculates the SHA checksum value for all files under a directory recursively and returns the map of `<Path, String>` pairs using parallel threads.

The `FutureStream.of(Stream)` creates a new `FutureStream` from the given stream, with the default executor (fixed thread pool executor, with availableProcessors() threads) and no retry set.

The `mapJoining` maps each element of the stream to a `CompletableFuture` task which gets executed and joined when we terminate the stream with the `collect(Collectors.toMap)` method.

Finally when we return from the `try-resource` the file stream is closed and the terminator is shutdown.

## Background

Java streams are a powerful tool to create multi-threaded applications in Java if you stick to simple, stateless operations chained together. If you avoid side-effects and take care about synchronization (if needed) the `parallel()` or `parallelStream` can turn any function chain into parallel execution. However, there are a few specialized needs which are not handled by the stream interface:

1. Parallelism cannot be controlled:
    Once the `parallel()` is used on a stream, at termination the stream will be evaluated in multiple threads, using the common `ForkJoinPool`. You can specify the parallelism for the common forkJoinPool (`java.util.concurrent.ForkJoinPool.common.parallelism=20`), but there is no possibility to feed a custom executor service to the reference pipeline, which executes the stream pipeline

    `FutureStream` tries to overcome this by providing a simple multi-map operation, where you can provide a custom executor service.

2. Exceptions must be handled locally for a lambda:
    It is not just the Stream interfaces fault, exceptions are thread-local in Java. We can argue about whether checked exceptions are friend or foe, but they can be used as sanity check along the program execution. But if you code throws checked exceptions, they must be handled in each lambda locally when using stream.

    `FutureStream` provides a `forEachParallel(Consumer, Exception)` terminal operation, which propagates the exception to the caller method, and `mapJoining(Func, Exception)` which wraps the exceptions in an unchecked exception.

3. When an exception during an operation during the pipeline processing is re-thrown but the running tasks are finished.
    Since the exceptions are thread-local the whole operation does not know about the exception, so it will continue to execute.

    `FutureStream` via `ExceptionReference` provides a thread-safe synchronization of checked exceptions, and when an exception occurs, the rest of the processing is stopped. Also the user can define a number of retries of the current operation in case of exception.

## Using FutureStream

In the simplest way, the `FutureStream` can simply wrap any stream given, execute parallel mapping or terminal (forEach) operation and close the stream as well as the executor which was instantiated for the execution.

```java
try (FutureStream<R> futureStream = FutureStream.of(anyStream<R>())) {
    ...
}
```

`FutureStream` is `AutoClosable`, the `close()` method will close the wrapped stream, as well as the default executor service.

`FutureStream` provides two operations:
* `mapJoining(Function)`: maps the elements of the stream to something else by applying the given function on each element. The mapping function might be something computationally extensive (like checksum calculation), which is worth executing in parallel. The mapping operation is an **intermediate operation**, meaning that it is lazy-executed, when a terminal operation is called on the stream.
* `forEachParallel(Consumer)`: calls the consumer action on each element of the stream. This is a terminal operation, which means that the pipeline is evaluated and all operations all executed.

### Controlling properties with `builder()`

Besides the `FutureStream.of(Stream)` builder method, a `FutureStreamBuilder` class is available for building a custom `FutureStream` with the following properties:
* `executor(ExecutorService)`: Sets the executor for the FutureStream. If not present then a new FixedThreadPool is instantiated with the number of available cores.
* `shutdownExecutor(boolean)`: Indicates whether the executor should be managed by the `FeatureStream` or the user. If set `True` then the stream will shutdown and terminate the executor when its `close()` method is called. If `False` the user is responsible for terminating the executor.
* `retries`: Sets the number of times an operation should be retried, before re-throwing a checked exception.

Example:
```java
try (FutureStream<Path> files = FutureStream
            .builder(Files.walk(dir)
            .executor(Executors.newFixedThreadPool(12)
            .shutdownExecutor(true)
            .retries(10)
            .build()))) {
                ...
}
```

### Throwing vs non-throwing operations

Both `mapJoining` and `forEachParallel` provides an overload, which accepts an exception type. This exception can be thrown by the functional parameters (function and consumer) given to the methods. The `retries` property controls the behavior when a checked exception like this is thrown by one of the methods. If the `retries > 0`, then the checked exception is ignored and that single operation is executed again, util successful or until the number of retries exceeds.

In case a checked exception occurs and the number of retries reached, the exception is propagated to the whole operations and all parallel tasks are terminated. (Already started tasks are finished, but new ones won't be started!)

```java
try (FutureStream<Path> files = FutureStream
        .builder(Files.walk(dir).filter(Files::isRegularFile))
        .retries(3)
        .build()) {
    files.forEachParallel(Files::delete, IOException.class);
} catch (IOException e) {
    throw new RuntimeException("Something went wrong");
}
```
The snippet above deletes all files under a directory recursively, and retries each failed operation 3 times. (Of course it does not make much sense to retry such IO operation but let it be an example).
The `IOException` is propagated to the caller and will be caught in the `catch` block.

In case of a `forEachParallel` operation the behavior is quiet straight forward. As it is a terminal operation we can propagate the exception right back to the caller method. However, the `mapJoining` operation is intermediate, so the exception won't be thrown when we cal `mapJoining` but at the terminal operation later. So `mapJoining` cannot propagate back the exception.

To avoid implementing a complex exception event bus, with which we still could not verify the checked exception compile time, the `mapJoining` will retry the operation `retries` number of times, before wrapping the checked exception in an `UncheckedExecutionException` and throwing the exception. In this case all remaining operations are cancelled just like with the previous method.

```java
try (FutureStream<Path> files = FutureStream
        .builder(Files.walk(dir).filter(Files::isRegularFile))
        .retries(3)
        .build()) {
    return files.mapJoining(Files::size, IOException.class).boxed().mapToLong(i -> i).sum();
} catch (IOException e) {
    //Ocurred during Files.walk
} catch (UncheckedExecutionException e) {
    //Ocurred during mapJoining 
}
```
The code snippet above calculates the cumulative size of all files under a folder recursively. When the `File.size` method throws an `IOException` and the `retries` counter is exceeded it will throw the `UncheckedExecutionException`.

### Chaining multiple operations

As `mapJoining` is an intermediate operation, you can chain multiple calls in a single `FutureStream` to achieve the needed functionality.

```java
try (FutureStream<String> fileURIs = FutureStream.of(getFileStreamFromServer())) {
    return fileURIs.mapJoining(this::validateFilOneServer, HTTPErrorException.class)
        .mapJoining((file) -> {
            if (file.isValid()) {
                return Optional.of(download(file));
            } else {
                return Optional.empty();
            }
        }, HTTPErrorException.class)
        .boxed().filter(Optional::isPresent).collect(Collectors.toList());
}
```
The code above:
* validates all files on the sever (e.g. checksum)
* downloads the valid files
* returns the list of downloaded files which are valid

The natural order for processing a stream is the following:
1. open a stream for the content, filter and preprocess to select the items which are needed for the parallel computation
2. open a new `FutureStream` and add the parallel operations to the pipeline
3. close the stream with a terminal operation or the `forEachParallel` method

### A simplification of implementation and function

The implementation uses `CompletableFuture`s for assigning a task to a thread on the executor. To avoid a very complex implementation and states which can be easily messed up each `mapJoining` operation appends the `CompletableFuture::join` to the pipeline. Which means that chaining two `mapJoining` together would look like in code like:
```java
myStream
    .map((elem) -> CompletableFuture.supplyAsync(() -> firstMapper.apply(elem)))
    .map(CompletableFuture::join)
    .map((resultOfFirst) -> CompletableFuture.supplyAsync(() -> secondMapper.apply(resultOfFirst)))
```

To further simplify the implementation, the rest of the `Stream` interface's functions are not re-implemented. These can be appended to the pipeline either before creating the `FutureStream` out of it, or after the parallel mapping operation (e.g. by terminating the stream). 