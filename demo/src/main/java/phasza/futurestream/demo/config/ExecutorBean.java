package phasza.futurestream.demo.config;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Bean for the executor service used by the application.
 * The executor is a fixed thread-pool executor, the number of threads is configurable
 * via the command line parameter.
 * The executor is lazy instantiated the first time it is required, and terminated at
 * the end of the application context.
 */
@Singleton
public final class ExecutorBean implements AutoCloseable {

    /**
     * Injected application configuration
     */
    private final ApplicationConfiguration config;

    /**
     * Executor instance wrapped by this class
     */
    private ExecutorService executor;

    /**
     * @param config Injected config
     */
    public ExecutorBean(final ApplicationConfiguration config) {
        this.config = config;
    }


    /**
     * @return A lazy instantiated, fixed thread-pool executor, with the configured number
     * of parallel threads
     */
    public ExecutorService get() {
        if (!Optional.ofNullable(executor).isPresent()) {
            executor = Executors.newFixedThreadPool(config.getNumberOfThreads());
        }
        return executor;
    }

    @Override
    public void close() throws Exception {
        if (Optional.ofNullable(executor).isPresent()) {
            executor.shutdownNow();
            executor.awaitTermination(100L, TimeUnit.MILLISECONDS);
        }
    }
}
