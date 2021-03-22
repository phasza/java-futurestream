package phasza.taskstream.executor.demo.command;

import lombok.RequiredArgsConstructor;
import phasza.taskstream.executor.AtomicExceptionReference;
import phasza.taskstream.executor.ExceptionReference;
import phasza.taskstream.executor.FutureStream;
import phasza.taskstream.executor.demo.config.ExecutorBean;
import phasza.taskstream.executor.demo.exception.OperationException;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Service for counting the size of the content to delete/copy
 */
@RequiredArgsConstructor
@Singleton
public class FileCounter {

    /**
     * Injected executor bean
     */
    private final ExecutorBean executor;

    /**
     * Sums the size of regular files in a directory regularly using multi-threads.
     *
     * @param dir Start directory
     * @return Cumulative size
     */
    public long countSizeOfFiles(final Path dir) {
        try (FutureStream<Path> files = FutureStream.builder(Files.walk(dir).filter(Files::isRegularFile))
                .executor(executor.get())
                .build()) {
            ExceptionReference<IOException> exception = new AtomicExceptionReference<>();
            return files.mapJoining(Files::size, IOException.class).boxed().mapToLong(i -> i).sum();
        } catch (IOException | InterruptedException e) {
            throw new OperationException(e.getMessage(), e);
        }
    }
}
