package phasza.taskstream.executor.demo;

import org.slf4j.LoggerFactory;

import java.nio.file.FileSystems;

/**
 * Wrapper around taskstream-demo app, injecting system default filesystem and factory created logger
 */
public final class Main {

    /**
     * @param args command line arguments to execute
     */
    public static void main(final String[] args) {
        System.exit(new Application(
                FileSystems.getDefault(),
                LoggerFactory.getLogger("taskstream-demo")).execute(args));
    }

    private Main() {

    }
}
