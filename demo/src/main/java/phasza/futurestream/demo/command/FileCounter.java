package phasza.futurestream.demo.command;

import phasza.futurestream.demo.config.ExecutorBean;
import phasza.futurestream.demo.exception.OperationException;
import phasza.futurestream.FutureStream;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Service for counting the size of the content to delete/copy
 */
@Singleton
public class FileCounter {

    /**
     * Injected executor bean
     */
    private final ExecutorBean executor;

    /**
     * @param executor Injected executor bean
     */
    public FileCounter(final ExecutorBean executor) {
        this.executor = executor;
    }

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
            return files.mapJoining(Files::size, IOException.class).boxed().mapToLong(i -> i).sum();
        } catch (IOException | InterruptedException e) {
            throw new OperationException(e.getMessage(), e);
        }
    }
}
