package dk.dbc.dataio.gatekeeper;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileFinder {
    /**
     * Finds all files in given directory ending with the specified
     * file extension
     * @param dir directory to search
     * @param extension only match files with given extension (dot is not required)
     * @return list of matching files
     * @throws NullPointerException if given null-valued argument
     * @throws IOException if directory does not exists or similar error condition
     */
    public static List<Path> findFilesWithExtension(Path dir, String extension)
            throws NullPointerException, IOException {
        InvariantUtil.checkNotNullOrThrow(dir, "dir");
        InvariantUtil.checkNotNullOrThrow(extension, "extension");
        final FindFilesWithExtensionVisitor findFilesWithExtensionVisitor =
                new FindFilesWithExtensionVisitor(extension);
        Files.walkFileTree(dir, findFilesWithExtensionVisitor);
        return findFilesWithExtensionVisitor.getMatchingFilesFound();
    }

    static class FindFilesWithExtensionVisitor extends SimpleFileVisitor<Path> {
        private final List<Path> matchingFilesFound;
        private final String extension;

        public FindFilesWithExtensionVisitor(String extension) {
            this.matchingFilesFound = new ArrayList<>();
            this.extension = extension;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toString().endsWith(extension)) {
                matchingFilesFound.add(file);
            }
            return FileVisitResult.CONTINUE;
        }

        public List<Path> getMatchingFilesFound() {
            return Collections.unmodifiableList(matchingFilesFound);
        }
    }
}
