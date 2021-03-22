package phasza.taskstream.executor.demo.config;

import lombok.Data;

import javax.inject.Singleton;

/**
 * Configures the application running e.g. number of threads to use.
 * These configurations map to one of the command line options available for the users.
 */
@Singleton
@Data
public class ApplicationConfiguration {

    /**
     * Number of threads to use for operation
     */
    private int numberOfThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
}
