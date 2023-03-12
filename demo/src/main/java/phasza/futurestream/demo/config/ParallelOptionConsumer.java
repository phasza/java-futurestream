package phasza.futurestream.demo.config;

import picocli.CommandLine;

import javax.inject.Singleton;
import java.util.Stack;

/**
 * Consumes the -p --parallel option and sets the corresponding application configuration
 */
@Singleton
public final class ParallelOptionConsumer implements CommandLine.IParameterConsumer {

    /**
     * Application configuration bean
     */
    private final ApplicationConfiguration config;

    /**
     * @param config Injected configuration
     */
    public ParallelOptionConsumer(final ApplicationConfiguration config) {
        this.config = config;
    }

    /**
     * @param args        the command line arguments
     * @param argSpec     the option or positional parameter for which to consume command line arguments
     * @param commandSpec the command that the option or positional parameter belongs to
     */
    @Override
    public void consumeParameters(
            final Stack<String> args,
            final CommandLine.Model.ArgSpec argSpec,
            final CommandLine.Model.CommandSpec commandSpec) {
        try {
            config.setNumberOfThreads(Integer.parseInt(args.pop()));
        } catch (NumberFormatException ex) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(), "Invalid number of threads!", ex);
        }
    }
}
