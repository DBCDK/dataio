/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.commons.javascript;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DirectoriesContainingJavascriptFinderTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testUnusedFinder_emptyResultList() {
        DirectoriesContainingJavascriptFinder finder = new DirectoriesContainingJavascriptFinder();
        assertThat(finder.getJavascriptDirectories().isEmpty(), is(true));
    }

    @Test
    public void testFinderWithDirectoriesContainingJavascriptFiles_findsAllDirectories() throws IOException {
        createTestDirectoryStructure();
        DirectoriesContainingJavascriptFinder finder = new DirectoriesContainingJavascriptFinder();
        Files.walkFileTree(folder.getRoot().toPath(), finder);
        List<Path> javascriptDirectories = finder.getJavascriptDirectories();

        assertThat(javascriptDirectories.size(), is(4));

        List<String> javascriptDirectoriesAsString = new ArrayList<>();
        for(Path p : javascriptDirectories) {
            javascriptDirectoriesAsString.add(p.toString());
        }
        assertThat("Could not find dir: /root/dir1", javascriptDirectoriesAsString.contains(folder.getRoot()+"/root/dir1"), is(true));
        assertThat("Could not find dir: /root/dir2", javascriptDirectoriesAsString.contains(folder.getRoot()+"/root/dir2"), is(true));
        assertThat("Could not find dir: /root/dir2/dir3", javascriptDirectoriesAsString.contains(folder.getRoot()+"/root/dir2/dir3"), is(true));
        assertThat("Could not find dir: /root/dir5/dir6", javascriptDirectoriesAsString.contains(folder.getRoot()+"/root/dir5/dir6"), is(true));
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
        folder.newFolder("root");
        folder.newFolder("root", "dir1");
        folder.newFolder("root", "dir2");
        folder.newFolder("root", "dir2", "dir3");
        folder.newFolder("root", "dir4.js");
        folder.newFolder("root", "dir5");
        folder.newFolder("root", "dir5", "dir6");

        folder.newFile("root/dir1/file1.js");
        folder.newFile("root/dir2/file2.txt");
        folder.newFile("root/dir2/file3.js");
        folder.newFile("root/dir2/dir3/file4.js");
        folder.newFile("root/dir4.js/file5.txt");
        folder.newFile("root/dir5/dir6/file6.js");
    }
}