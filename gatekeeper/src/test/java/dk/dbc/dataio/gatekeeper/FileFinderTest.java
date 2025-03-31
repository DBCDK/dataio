package dk.dbc.dataio.gatekeeper;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileFinderTest {
    @TempDir
    public Path testFolder;

    @Test
    public void findFilesWithExtension_dirArgIsNull_throws() throws IOException {
        assertThrows(NullPointerException.class, () -> FileFinder.findFilesWithExtension(null, Set.of(".trans")));
    }

    @Test
    public void findFilesWithExtension_extensionArgIsNull_throws() throws IOException {
        assertThrows(NullPointerException.class, () -> FileFinder.findFilesWithExtension(testFolder, null));
    }

    @Test
    public void findFilesWithExtension_dirDoesNotExist_throws() throws IOException {
        assertThrows(IOException.class, () -> FileFinder.findFilesWithExtension(Paths.get("no-such-dir"), Set.of(".trans")));
    }

    @Test
    public void findFilesWithExtension_extensionArgIsEmpty_findsAllFiles() throws IOException {
        List<Path> files = createFiles("file1", "file2");
        List<Path> matchingFiles = FileFinder.findFilesWithExtension(testFolder, Set.of(""));
        assertThat(matchingFiles, containsInAnyOrder(files.toArray()));
    }

    @Test
    public void findFilesWithExtension_extensionArgIsNonEmpty_findsAllFilesMatchingExtension() throws IOException {
        List<Path> files = createFiles("file0.dat", "file1.trans", "file2.trans", "file3.trs");
        List<Path> matchingFiles = FileFinder.findFilesWithExtension(testFolder, Set.of(".trans", ".trs"));
        assertThat(matchingFiles.size(), is(3));
        assertThat(matchingFiles, containsInAnyOrder(files.subList(1, 4).toArray()));
    }

    @Test
    public void findFilesWithExtension_returnsUnmodifiableList() throws IOException {
        List<Path> matchingFiles = FileFinder.findFilesWithExtension(testFolder, Set.of(""));
        try {
            matchingFiles.add(testFolder);
            fail("No exception thrown");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void findFilesWithExtension_returnsListOrderedByFileCreationTimeThenByFileName() throws IOException {
        List<Path> files = createFiles("2.trans", "3.trans", "1.trans");
        long now = System.currentTimeMillis();
        files.forEach(f -> setFileCreationTime(f, now));
        List<Path> matchingFiles = FileFinder.findFilesWithExtension(testFolder, Set.of(".trans"));
        List<String> names = matchingFiles.stream().map(p -> p.toFile().getName()).toList();
        assertThat("File order", names, is(List.of("1.trans", "2.trans", "3.trans")));
    }

    @Test @Disabled("set creation time ignored on build server")
    public void findFilesWithExtension_returnsListOrderedByFileCreationTime() throws IOException {
        List<Path> files = createFiles("2.trans", "3.trans", "1.trans");
        Path first = files.get(0);
        setFileCreationTime(files.get(1), Files.getLastModifiedTime(first).toMillis() + 1000);
        setFileCreationTime(files.get(2), Files.getLastModifiedTime(first).toMillis() + 2000);
        List<Path> matchingFiles = FileFinder.findFilesWithExtension(testFolder, Set.of(".trans"));
        List<String> names = matchingFiles.stream().map(p -> p.toFile().getName()).toList();
        assertThat("File order", names, is(List.of("2.trans", "3.trans", "1.trans")));
    }

    private static void setFileCreationTime(Path file, long creationTime) {
        BasicFileAttributeView attributes = Files.getFileAttributeView(file, BasicFileAttributeView.class);
        FileTime time = FileTime.fromMillis(creationTime);
        try {
            attributes.setTimes(time, time, time);
        } catch (IOException e) {
            throw new IllegalStateException();
        }
    }

    private List<Path> createFiles(String... filenames) {
        return Arrays.stream(filenames).map(f -> testFolder.resolve(f)).map(f -> {
            try {
                return Files.createFile(f);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    private List<Path> createFiles(List<String> filenames) {
        return filenames.stream().map(f -> testFolder.resolve(f)).map(f -> {
            try {
                return Files.createFile(f);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }
}
