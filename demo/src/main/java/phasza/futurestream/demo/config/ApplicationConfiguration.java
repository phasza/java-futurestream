package phasza.futurestream.demo.config;

import javax.inject.Singleton;

/**
 * Configures the application running e.g. number of threads to use.
 * These configurations map to one of the command line options available for the users.
 */
@Singleton
public class ApplicationConfiguration {

    /**
     * Number of threads to use for operation
     */
    private int numberOfThreads;

    /**
     * @return Number of threads to use for operation
     */
    public int getNumberOfThreads() {
        return this.numberOfThreads;
    }

    /**
     * @param numberOfThreads Number of threads to use for operation
     */
    public void setNumberOfThreads(final int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    /**
     *
     */
    public ApplicationConfiguration() {
        this.numberOfThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    }

    /**
     * @param numberOfThreads Number of threads to use for operation
     */
    public ApplicationConfiguration(final int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }
}
