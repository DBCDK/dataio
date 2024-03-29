package dk.dbc.dataio.gatekeeper;

import dk.dbc.invariant.InvariantUtil;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FileFinder {
    /**
     * Finds all files in given directory ending with the specified
     * file extension(s) ordered by ascending creation time (one second granularity)
     * and then by filename
     *
     * @param dir        directory to search
     * @param extensions only match files with given extensions (dot is not required)
     * @return list of matching files
     * @throws NullPointerException if given null-valued argument
     * @throws IOException          if directory does not exists or similar error condition
     */
    public static List<Path> findFilesWithExtension(Path dir, Set<String> extensions)
            throws NullPointerException, IOException {
        InvariantUtil.checkNotNullOrThrow(dir, "dir");
        InvariantUtil.checkNotNullOrThrow(extensions, "extensions");
        final FindFilesWithExtensionVisitor findFilesWithExtensionVisitor =
                new FindFilesWithExtensionVisitor(extensions);
        Files.walkFileTree(dir, findFilesWithExtensionVisitor);
        return findFilesWithExtensionVisitor.getMatchingFilesFound();
    }

    private static class FindFilesWithExtensionVisitor extends SimpleFileVisitor<Path> {
        private final List<Path> matchingFilesFound;
        private final Set<String> extensions;

        FindFilesWithExtensionVisitor(Set<String> extensions) {
            this.matchingFilesFound = new ArrayList<>();
            this.extensions = extensions;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            matchingFilesFound.addAll(extensions.stream()
                    .filter(extension -> file.toString().endsWith(extension))
                    .map(extension -> file)
                    .collect(Collectors.toList()));

            return FileVisitResult.CONTINUE;
        }

        List<Path> getMatchingFilesFound() {
            Collections.sort(matchingFilesFound, new ByFileCreationTimeThenByFileName());
            return Collections.unmodifiableList(matchingFilesFound);
        }

        private static class ByFileCreationTimeThenByFileName implements Comparator<Path> {
            @Override
            public int compare(Path p1, Path p2) {
                // Note: FileTime granularity is one second
                int compareTo = getCreationTime(p1).compareTo(getCreationTime(p2));
                if (compareTo == 0) {
                    compareTo = p1.getFileName().compareTo(p2.getFileName());
                }
                return compareTo;
            }

            private FileTime getCreationTime(Path path) {
                try {
                    return Files.readAttributes(path, BasicFileAttributes.class).creationTime();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }
}
