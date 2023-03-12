package phasza.futurestream.demo.command;

import me.tongfei.progressbar.ProgressBar;
import org.slf4j.Logger;
import phasza.futurestream.demo.config.ExecutorBean;
import phasza.futurestream.demo.exception.OperationException;
import phasza.futurestream.FutureStream;
import picocli.CommandLine;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Implements the cp command. The command recursively copies the content of a given directory to a destination directory.
 * This command uses multiple threads to execute.
 */
@Singleton
@CommandLine.Command(
        name = "cp",
        description = "Copies files from source to destination using multiple threads.")
public final class CopyCommand implements Runnable {

    /**
     * Source directory of copy.
     */
    @CommandLine.Parameters(description = "Source directory of copy.", index = "0")
    private String source;

    /**
     * Destination directory of copy.
     */
    @CommandLine.Parameters(description = "Destination directory of copy.", index = "1")
    private String destination;

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
    public CopyCommand(
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
     * @param source Source directory of copy
     */
    public void setSource(final String source) {
        this.source = source;
    }

    /**
     * @param destination Destination directory of copy.
     */
    public void setDestination(final String destination) {
        this.destination = destination;
    }

    @Override
    public void run() {
        final Path sourceDir = fileSystem.getPath(source);
        if (!Files.isDirectory(sourceDir)) {
            throw new OperationException(String.format("'%s' source directory does not exist!", source));
        }
        logger.info(String.format("Collecting files from %s", sourceDir));
        try (ProgressBar progress = ProgressBarUtil.transferProgress(
                "Copying...", fileCounter.countSizeOfFiles(sourceDir));
             FutureStream<Path> files = FutureStream.builder(Files.walk(sourceDir).filter(Files::isRegularFile))
                     .executor(executor.get())
                     .build()) {
            final Path destinationDir = fileSystem.getPath(destination); //NOPMD - not a DU
            files.forEachParallel(file -> copyFileWithStatus(
                    file, sourceDir, destinationDir, progress::stepBy), IOException.class);
        } catch (InterruptedException | IOException e) {
            throw new OperationException(e.getMessage(), e);
        }
    }

    private void copyFileWithStatus(
            final Path file,
            final Path sourceDir,
            final Path destinationDir,
            final Consumer<Long> statusListener) throws IOException {
        final Path fullDest = destinationDir.resolve(sourceDir.relativize(file));
        Optional.ofNullable(fullDest.getParent()).ifPresent(i -> {
            try {
                Files.createDirectories(i);
            } catch (IOException e) {
                throw new OperationException(e.getMessage(), e);
            }
        });

        try (InputStream inputStream = Files.newInputStream(file);
             OutputStream outputStream = Files.newOutputStream(fullDest)) {
            final byte[] buffer = new byte[64 * 1024];
            while (true) {
                final int length = inputStream.read(buffer);
                if (length <= 0) {
                    return;
                }

                outputStream.write(buffer, 0, length);
                statusListener.accept((long) length);
            }
        }
    }
}
