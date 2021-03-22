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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * More detailed tests for delete command
 */
class DeleteCommandTest implements AutoCloseable {

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
    DeleteCommandTest() {
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
    void testRunDeleteMultipleFiles() throws IOException {
        final Path sourceDir = fileSystem.getPath("/home/user/source");
        TestUtils.createSourceFiles(sourceDir);
        final DeleteCommand testCommand = testContext.findOrInstantiateBean(DeleteCommand.class).get();
        testCommand.setDir(sourceDir.toString());
        testCommand.run();
        assertFalse(Files.exists(sourceDir), "source directory should not exist");
    }

    @Test
    void testRunSourceDirectoryNotExists() {
        final DeleteCommand testCommand = testContext.findOrInstantiateBean(DeleteCommand.class).get();
        testCommand.setDir(fileSystem.getPath("not_existing").toString());
        assertThrows(OperationException.class, testCommand::run, "not existing directory should have thrown");
    }

    @Test
    void testRunDeleteEmptyDirectory() throws IOException {
        final Path sourceDir = fileSystem.getPath("/home/user/source");
        final DeleteCommand testCommand = testContext.findOrInstantiateBean(DeleteCommand.class).get();
        Files.createDirectories(sourceDir);
        testCommand.setDir(sourceDir.toString());
        testCommand.run();
        assertFalse(Files.exists(sourceDir), "source directory should not exist");
    }
}