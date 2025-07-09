/**
 * @author Juan Camilo Camargo
 * @version 0.1
 * This file was created with help from ChatGPT.
 * This class is used to create a temporary directory and subdirectories for testing purposes.
 */
package koolfileindexer.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;


public final class TempDirectroryManager {

    public Path createTempDirectory(String prefix) throws IOException{ 
        return Files.createTempDirectory(prefix);
    }

    public Path createSubdirectory(Path parentDir, String subDirName) throws IOException {
        Path subDir = parentDir.resolve(subDirName);
        return Files.createDirectories(subDir);
    }

    public void deleteTempDirectory(Path dir) throws IOException {
        if (Files.exists(dir)){
            try (var paths = Files.walk(dir)) {
                paths.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(dir);
                    } catch (IOException e) {
                        System.err.println("Filed to delete "+ path+" : "+ e.getMessage());
                    }
                });
            }
        }
    }

}
