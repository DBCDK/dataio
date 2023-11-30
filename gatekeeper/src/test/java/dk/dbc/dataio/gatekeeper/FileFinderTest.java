package dk.dbc.dataio.gatekeeper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
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
        Path first = files.get(0);
        Path second = files.get(1);
        Path third = files.get(2);
        long now = System.currentTimeMillis();
        setFileCreationTime(first, now);
        setFileCreationTime(second, now);
        setFileCreationTime(third, now);

        List<Path> matchingFiles = FileFinder.findFilesWithExtension(testFolder, Set.of(".trans"));

        assertThat("first", matchingFiles.get(0), is(third));
        assertThat("second", matchingFiles.get(1), is(first));
        assertThat("third", matchingFiles.get(2), is(second));
    }

    @Test
    public void findFilesWithExtension_returnsListOrderedByFileCreationTime() throws IOException {
        if (SystemUtil.isOsX()) { // setting file create time does not work on OS X - https://bugs.openjdk.java.net/browse/JDK-8151430
            return;
        }
        List<Path> files = createFiles("2.trans", "3.trans", "1.trans");
        Path first = files.get(0);
        Path second = files.get(1);
        Path third = files.get(2);
        setFileCreationTime(second, Files.getLastModifiedTime(first).toMillis() + 1000);
        setFileCreationTime(third, Files.getLastModifiedTime(first).toMillis() + 2000);

        List<Path> matchingFiles = FileFinder.findFilesWithExtension(testFolder, Set.of(".trans"));

        assertThat("first", matchingFiles.get(0), is(first));
        assertThat("second", matchingFiles.get(1), is(second));
        assertThat("third", matchingFiles.get(2), is(third));
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
}
