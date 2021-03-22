package phasza.taskstream.executor.demo;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

/**
 * Tests the higher level functionality of the application like context creation and logging.
 */
class ApplicationTest {

    /**
     * Mock filesystem
     */
    private final FileSystem fileSystem;
    /**
     * Messages queued via logging during the test
     */
    private final ConcurrentLinkedQueue<String> logMessages;
    /**
     * AUD
     */
    private final Application appUnderTest;

    /**
     * Instantiates test objects and mocks
     */
    ApplicationTest() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        logMessages = new ConcurrentLinkedQueue<>();
        final Logger mockLogger = mock(Logger.class);
        Mockito.doAnswer(i -> logMessages.add(i.getArgument(0))).when(mockLogger).info(anyString());
        appUnderTest = new Application(fileSystem, mockLogger);
    }

    /**
     * Tests that base command, without arguments runs successfully and prints out information
     */
    @Test
    void testExecuteBaseCommand() {
        assertEquals(0, appUnderTest.execute(), "exit code does not match");
        assertTrue(logMessages.stream().anyMatch(i -> i.contains("futureStream-demo")), "log does not contain help");
    }

    /**
     * Tests that copy command executes successfully
     * @throws IOException e
     */
    @Test
    void testExecuteCopyCommand() throws IOException {
        final Path sourceDir = fileSystem.getPath("/home/user/directory");
        final List<Path> sourceFiles = createSourceDirectory(sourceDir);
        final Path destination = fileSystem.getPath("/usr/bin/");

        assertEquals(0, appUnderTest.execute(
                String.format("cp %s %s", sourceDir, destination).split(" ")), "exit code does not match");
        for (final Path sourceFile : sourceFiles) {
            final Path relative = sourceDir.relativize(sourceFile);
            final Path destinationFile = destination.resolve(relative);
            assertTrue(Files.isRegularFile(destinationFile), String.format("%s does not exist", destinationFile));
            assertEquals(
                    Files.size(sourceFile),
                    Files.size(destinationFile),
                    "Source and destination size don't match");
        }
    }

    /**
     * Tests that delete command executes successfully
     * @throws IOException e
     */
    @Test
    void testExecuteDeleteCommand() throws IOException {
        final Path sourceDir = fileSystem.getPath("/home/user/directory");
        final List<Path> sourceFiles = createSourceDirectory(sourceDir);

        assertEquals(0, appUnderTest.execute(
                String.format("del %s", sourceDir).split(" ")), "exit code does not match");
        for (final Path sourceFile : sourceFiles) {
            assertFalse(Files.isRegularFile(sourceFile), String.format("%s file still exists", sourceFile));
        }

        assertFalse(Files.isDirectory(sourceDir), "source directory still exists");
    }

    private List<Path> createSourceDirectory(final Path dir) throws IOException {
        return Arrays.asList(
                createFileWithContent(dir, "file1.txt"),
                createFileWithContent(dir, "subDir/file1.txt")
        );
    }

    private Path createFileWithContent(final Path dir, final String relativePath) throws IOException {
        final Path file = dir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        final byte[] content = new byte[64 * 1024];
        new Random().nextBytes(content);
        Files.write(file, content);
        return file;
    }
}