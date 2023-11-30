package dk.dbc.dataio.commons.javascript;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DirectoriesContainingJavascriptFinderTest {
    @TempDir
    public Path folder;

    @Test
    public void testUnusedFinder_emptyResultList() {
        DirectoriesContainingJavascriptFinder finder = new DirectoriesContainingJavascriptFinder();
        assertThat(finder.getJavascriptDirectories().isEmpty(), is(true));
    }

    @Test
    public void testFinderWithDirectoriesContainingJavascriptFiles_findsAllDirectories() throws IOException {
        createTestDirectoryStructure();
        DirectoriesContainingJavascriptFinder finder = new DirectoriesContainingJavascriptFinder();
        Files.walkFileTree(folder, finder);
        List<Path> javascriptDirectories = finder.getJavascriptDirectories();

        assertThat(javascriptDirectories.size(), is(4));

        List<String> javascriptDirectoriesAsString = new ArrayList<>();
        for (Path p : javascriptDirectories) {
            javascriptDirectoriesAsString.add(p.toString());
        }
        assertThat("Could not find dir: /root/dir1", javascriptDirectoriesAsString.contains(folder + "/root/dir1"), is(true));
        assertThat("Could not find dir: /root/dir2", javascriptDirectoriesAsString.contains(folder + "/root/dir2"), is(true));
        assertThat("Could not find dir: /root/dir2/dir3", javascriptDirectoriesAsString.contains(folder + "/root/dir2/dir3"), is(true));
        assertThat("Could not find dir: /root/dir5/dir6", javascriptDirectoriesAsString.contains(folder + "/root/dir5/dir6"), is(true));
    }

    /*
     * The following file/directory structure is created:
     *
     * root
     *  |-dir1
     *  |   |-file1.js
     *  |-dir2
     *  |   |-file2.txt
     *  |   |-file3.js
     *  |   |-dir3
     *  |   |   |-file4.js
     *  |-dir4.js   // this is a trick
     *  |   |-file5.txt
     *  |-dir5
     *      |-dir6
     *          |-file6.js
     *
     * This should generate the following directories:
     *  root/dir1
     *  root/dir2
     *  root/dir2/dir3
     *  root/dir5/dir6
     */
    private void createTestDirectoryStructure() throws IOException {
        Path dir1 = Files.createDirectories(folder.resolve("root/dir1"));
        Path dir2 = Files.createDirectories(folder.resolve("root/dir2"));
        Path dir3 = Files.createDirectories(dir2.resolve("dir3"));
        Path dir4 = Files.createDirectories(folder.resolve("root/dir4.js"));
        Path dir5 = Files.createDirectories(folder.resolve("root/dir5"));
        Path dir6 = Files.createDirectories(dir5.resolve("dir6"));

        Files.createFile(dir1.resolve("file1.js"));
        Files.createFile(dir2.resolve("file2.txt"));
        Files.createFile(dir2.resolve("file3.js"));
        Files.createFile(dir3.resolve("file4.js"));
        Files.createFile(dir4.resolve("file5.txt"));
        Files.createFile(dir6.resolve("file6.js"));
    }
}
