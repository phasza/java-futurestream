package phasza.futurestream.demo.command;

import me.tongfei.progressbar.ConsoleProgressBarConsumer;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

/**
 * Utility class which sets up progress bars for data transfer
 */
public final class ProgressBarUtil {

    /**
     * Creates a simple progress bar for a task with transfer speed
     * e.g. >> Copying '../data/records' 480.9 MB 540 MB/s
     * @param taskName Task name (e.g. "Copying '../data/records'")
     * @param size size of transfer
     * @return created progress bar
     */
    public static ProgressBar transferProgress(final String taskName, final long size) {
        return createDefaultBuilder()
                .setTaskName(taskName)
                .setUnit("MB", 1000 * 1000L)
                .setInitialMax(size)
                .showSpeed()
                .build();
    }

    /**
     * Creates a progress bar for file counting without knowing the number of elements upfront
     * @return created progress bar
     */
    public static ProgressBar fileCountProgress() {
        return createDefaultBuilder()
                .setTaskName("Collecting files...")
                .build();
    }

    private static ProgressBarBuilder createDefaultBuilder() {
        return new ProgressBarBuilder()
                .setStyle(ProgressBarStyle.ASCII)
                .setConsumer(new ConsoleProgressBarConsumer(System.out));
    }

    private ProgressBarUtil() {
    }


}
