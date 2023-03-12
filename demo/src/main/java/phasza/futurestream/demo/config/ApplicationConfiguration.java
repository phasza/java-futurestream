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

    public int getNumberOfThreads() {
        return this.numberOfThreads;
    }

    public void setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    public ApplicationConfiguration() {
        this.numberOfThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    }

    public ApplicationConfiguration(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }
}
