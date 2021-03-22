package phasza.futurestream.demo;

import org.slf4j.LoggerFactory;

import java.nio.file.FileSystems;

/**
 * Wrapper around futureStream-demo app, injecting system default filesystem and factory created logger
 */
public final class Main {

    /**
     * @param args command line arguments to execute
     */
    public static void main(final String[] args) {
        System.exit(new Application(
                FileSystems.getDefault(),
                LoggerFactory.getLogger("futureStream-demo")).execute(args));
    }

    private Main() {

    }
}
