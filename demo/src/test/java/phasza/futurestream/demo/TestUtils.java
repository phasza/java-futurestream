package phasza.futurestream.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility for generating files for tests
 */
public final class TestUtils {

    /**
     * rand instance
     */
    private static final Random RANDOM = new Random();

    /**
     * Generates a fix number of subdirectories with children files under the given
     * source directory and returns the list of generated file paths
     * @param dir Directory to generate children subdirectories and files
     * @return List of generated file paths
     * @throws IOException Exception during generation
     */
    public static List<Path> createSourceFiles(final Path dir) throws IOException {
        final List<Path> result = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 5; j++) {
                final Path filePath = dir
                        .resolve(String.format("dir_%d", i))
                        .resolve(String.format("file_%d", j));
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, generateContent(RANDOM.nextInt(150 * 1024)));
                result.add(filePath);
            }
        }
        return result;
    }

    private static byte[] generateContent(final int size) {
        final byte[] buffer = new byte[size];
        RANDOM.nextBytes(buffer);
        return buffer;
    }

    private TestUtils() {

    }
}
