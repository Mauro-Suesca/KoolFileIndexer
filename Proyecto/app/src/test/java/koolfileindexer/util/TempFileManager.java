
package koolfileindexer.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TempFileManager {
    public Path createFile(Path dir, String fileName, String content) throws IOException {
        Path filePath = dir.resolve(fileName);
        return Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
    }
}
