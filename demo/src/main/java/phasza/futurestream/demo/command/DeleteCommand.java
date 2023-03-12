package phasza.futurestream.demo.command;

import me.tongfei.progressbar.ProgressBar;
import org.slf4j.Logger;
import phasza.futurestream.demo.config.ExecutorBean;
import phasza.futurestream.demo.exception.OperationException;
import phasza.futurestream.FutureStream;
import picocli.CommandLine;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Implements the del command. The command recursively deletes the content of a given directory and the directory itself.
 * This command uses multiple threads to execute.
 */
@Singleton
@CommandLine.Command(
        name = "del",
        description = "Deletes a directory recursively using multiple threads.")
public final class DeleteCommand implements Runnable {

    /**
     * Directory to delete
     */
    @CommandLine.Parameters(description = "Directory to delete.", index = "0")
    private String dir;

    /**
     * Injected filesystem
     */
    private final FileSystem fileSystem;

    /**
     * Injected executor bean
     */
    private final ExecutorBean executor;

    /**
     * Injected file counter service
     */
    private final FileCounter fileCounter;

    /**
     * Injected logger
     */
    private final Logger logger;

    /**
     * @param fileSystem Injected filesystem
     * @param executor Injected executor bean
     * @param fileCounter Injected file counter service
     * @param logger Injected logger
     */
    public DeleteCommand(
            final FileSystem fileSystem,
            final ExecutorBean executor,
            final FileCounter fileCounter,
            final Logger logger
    ) {
        this.fileSystem = fileSystem;
        this.executor = executor;
        this.fileCounter = fileCounter;
        this.logger = logger;
    }

    /**
     * @param dir Directory to delete
     */
    public void setDir(final String dir) {
        this.dir = dir;
    }

    @Override
    public void run() {
        final Path sourceDir = fileSystem.getPath(dir);
        if (!Files.isDirectory(sourceDir)) {
            throw new OperationException(String.format("'%s' source directory does not exist!", dir));
        }

        logger.info(String.format("Collecting files from %s", sourceDir));
        try (ProgressBar progressBar = ProgressBarUtil.transferProgress(
                "Deleting...", fileCounter.countSizeOfFiles(sourceDir));
             FutureStream<Path> files = FutureStream.builder(Files.walk(sourceDir).filter(Files::isRegularFile))
                     .executor(executor.get())
                     .build()) {
            files.forEachParallel(file -> {
                progressBar.stepBy(Files.size(file));
                Files.delete(file);
            }, IOException.class);
        } catch (InterruptedException | IOException e) {
            throw new OperationException(e.getMessage(), e);
        }
        deleteDirectories(sourceDir);
    }

    /**
     * Deletes all the remaining content of a directory. The multi-thread deletion will only delete regular files
     * for simplicity (and assuming that we will delete mostly files). The rest (directories, symlinks etc.) will remain
     * intact. This method wipes off the rest of such content.
     *
     * @param sourceDir Directory to delete
     */
    private void deleteDirectories(final Path sourceDir) {
        try {
            Files.walk(sourceDir).sorted(Comparator.reverseOrder()).forEach(i -> {
                try {
                    Files.delete(i);
                } catch (IOException e) {
                    throw new OperationException(e.getMessage(), e);
                }
            });
            Files.deleteIfExists(sourceDir);
        } catch (IOException e) {
            throw new OperationException(e.getMessage(), e);
        }
    }
}