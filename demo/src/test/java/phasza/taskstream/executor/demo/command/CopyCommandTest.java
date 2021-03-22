package phasza.taskstream.executor.demo.command;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import phasza.taskstream.executor.demo.TestUtils;
import phasza.taskstream.executor.demo.exception.OperationException;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * More detailed tests of the copy command
 */
class CopyCommandTest implements AutoCloseable {

    /**
     * Mock filesystem
     */
    private final FileSystem fileSystem;
    /**
     * Context for the tests
     */
    private final ApplicationContext testContext;

    /**
     * Sets up testcases
     */
    CopyCommandTest() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        testContext = ApplicationContext
                .builder()
                .singletons(fileSystem, mock(Logger.class))
                .start();
    }

    @Override
    public void close() {
        testContext.close();
    }

    @Test
    void testRunCopyMultipleFiles() throws IOException {
        final Path sourceDir = fileSystem.getPath("/home/user/source");
        final Path destination = fileSystem.getPath("/var/lib");
        final List<Path> testFiles = TestUtils.createSourceFiles(sourceDir);
        final CopyCommand testCommand = testContext.findOrInstantiateBean(CopyCommand.class).get();
        testCommand.setSource(sourceDir.toString());
        testCommand.setDestination(destination.toString());
        testCommand.run();
        assertAllFilesCopied(sourceDir, destination, testFiles);
    }

    @Test
    void testRunDestinationAlreadyExists() throws IOException {
        final Path sourceDir = fileSystem.getPath("/home/user/source");
        final Path destination = fileSystem.getPath("/var/lib");
        final List<Path> testFiles = TestUtils.createSourceFiles(sourceDir);
        TestUtils.createSourceFiles(destination); //generate files to overwrite
        final CopyCommand testCommand = testContext.findOrInstantiateBean(CopyCommand.class).get();
        testCommand.setSource(sourceDir.toString());
        testCommand.setDestination(destination.toString());
        testCommand.run();
        assertAllFilesCopied(sourceDir, destination, testFiles);
    }

    @Test
    void testRunSourceDirectoryNotExists() {
        final Path sourceDir = fileSystem.getPath("/home/user/not_existing");
        final CopyCommand testCommand = testContext.findOrInstantiateBean(CopyCommand.class).get();
        testCommand.setSource(sourceDir.toString());
        testCommand.setDestination(sourceDir.toString());
        assertThrows(
                OperationException.class,
                () -> testContext.findOrInstantiateBean(CopyCommand.class).get().run());
    }

    private static void assertAllFilesCopied(
            final Path source,
            final Path destination,
            final List<Path> sourceFiles) throws IOException {
        for (final Path file : sourceFiles) {
            assertTrue(Files.exists(file), String.format("%s source file does not exist", file));
            final Path destFile = destination.resolve(source.relativize(file));
            assertTrue(Files.exists(destFile), String.format("%s destination file does not exist", destFile));
            assertEquals(Files.size(file), Files.size(destFile), "destination and source size don't match");
        }
    }
}