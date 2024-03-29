package phasza.futurestream.demo;

import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import org.slf4j.Logger;
import phasza.futurestream.demo.command.DeleteCommand;
import phasza.futurestream.demo.config.ParallelOptionConsumer;
import phasza.futurestream.demo.exception.OperationException;
import phasza.futurestream.demo.command.CopyCommand;
import picocli.CommandLine;
import picocli.CommandLine.Help;

import java.nio.file.FileSystem;

/**
 * Main class of futureStream-demo application. This class creates the context of the application, registering the outer
 * services (filesystem, logger etc.).
 */
@CommandLine.Command(
        name = "futureStream-demo",
        subcommands = {
                CopyCommand.class,
                DeleteCommand.class
        })
public final class Application implements Runnable {

    /**
     * CLI coloring scheme is not colored and ANSI escape codes are turned off for easier integration
     */
    private static final Help.ColorScheme CLI_COLOR = Help.defaultColorScheme(CommandLine.Help.Ansi.OFF);

    /**
     * -v --version option
     */
    @CommandLine.Option(
            names = {"-v", "--version"},
            versionHelp = true,
            description = "Displays the version information.")
    private boolean version;


    /**
     * -h --help option
     */
    @CommandLine.Option(names = {"-h", "--help"},
            usageHelp = true,
            description = "Displays the help information.")
    private boolean help;

    /**
     * Number of threads for operation.
     */
    @CommandLine.Option(
            names = {"-p", "--parallel"},
            parameterConsumer = ParallelOptionConsumer.class,
            description = "Number of threads for operation. Default: logical cores - 1")
    private int parallel;

    /**
     * Injected filesystem wrapper to operate on.
     */
    private final FileSystem fileSystem;

    /**
     * Injected logger.
     */
    private final Logger logger;

    /**
     * On running the base command alone, the application prints the usage message for the user. This message will
     * be generated by the picocli framework runtime.
     */
    private String usageMessage = "";

    /**
     * @param fileSystem Injected filesystem wrapper to operate on.
     * @param logger Injected logger.
     */
    public Application(final FileSystem fileSystem, final Logger logger) {

        this.fileSystem = fileSystem;
        this.logger = logger;
    }

    /**
     * @param usageMessage usage message
     */
    private void setUsageMessage(final String usageMessage) {
        this.usageMessage = usageMessage;
    }

    /**
     * Executes the given command line commands.
     * @param args commands to execute
     * @return return code (0 success, 1 error)
     */
    public int execute(final String... args) {
        try (ApplicationContext context = ApplicationContext
                .builder(Application.class, Environment.CLI)
                .singletons(fileSystem, logger)
                .start()) {
            final CommandLine commandLine = new CommandLine(this, new MicronautFactory(context))
                    .setTrimQuotes(true)
                    .setColorScheme(CLI_COLOR)
                    .setUsageHelpWidth(80)
                    .setUsageHelpAutoWidth(true);
            this.setUsageMessage(commandLine.getUsageMessage(CLI_COLOR));
            return commandLine.execute(args);
        } catch (OperationException e) {
            logger.error(e.getMessage());
            logger.debug(e.getMessage(), e);
            return 1;
        }

    }

    @Override
    public void run() {
        logger.info(usageMessage);
    }
}
