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

package dk.dbc.dataio.gatekeeper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class FileFinderTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void findFilesWithExtension_dirArgIsNull_throws() throws IOException {
        FileFinder.findFilesWithExtension(null, new HashSet<>(Collections.singletonList(".trans")));
    }

    @Test(expected = NullPointerException.class)
    public void findFilesWithExtension_extensionArgIsNull_throws() throws IOException {
        FileFinder.findFilesWithExtension(testFolder.getRoot().toPath(), null);
    }

    @Test(expected = IOException.class)
    public void findFilesWithExtension_dirDoesNotExist_throws() throws IOException {
        FileFinder.findFilesWithExtension(Paths.get("no-such-dir"), new HashSet<>(Collections.singletonList(".trans")));
    }

    @Test
    public void findFilesWithExtension_extensionArgIsEmpty_findsAllFiles() throws IOException {
        final List<Path> files = Arrays.asList(
                testFolder.newFile("file1").toPath(),
                testFolder.newFile("file2").toPath());
        final List<Path> matchingFiles = FileFinder.findFilesWithExtension(testFolder.getRoot().toPath(), new HashSet<>(Collections.singletonList("")));
        assertThat(matchingFiles, containsInAnyOrder(files.toArray()));
    }

    @Test
    public void findFilesWithExtension_extensionArgIsNonEmpty_findsAllFilesMatchingExtension() throws IOException {
        final List<Path> files = Arrays.asList(
                testFolder.newFile("file0.dat").toPath(),
                testFolder.newFile("file1.trans").toPath(),
                testFolder.newFile("file2.trans").toPath(),
                testFolder.newFile("file3.trs").toPath());
        final List<Path> matchingFiles = FileFinder.findFilesWithExtension(testFolder.getRoot().toPath(), new HashSet<>(Arrays.asList(".trans", ".trs")));
        assertThat(matchingFiles.size(), is(3));
        assertThat(matchingFiles, containsInAnyOrder(files.subList(1, 4).toArray()));
    }

    @Test
    public void findFilesWithExtension_returnsUnmodifiableList() throws IOException {
        final List<Path> matchingFiles = FileFinder.findFilesWithExtension(testFolder.getRoot().toPath(), new HashSet<>(Collections.singletonList("")));
        try {
            matchingFiles.add(testFolder.newFile().toPath());
            fail("No exception thrown");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void findFilesWithExtension_returnsListOrderedByFileCreationTimeThenByFileName() throws IOException {
        final Path first = testFolder.newFile("2.trans").toPath();
        final Path second = testFolder.newFile("3.trans").toPath();
        final Path third = testFolder.newFile("1.trans").toPath();

        final List<Path> matchingFiles = FileFinder.findFilesWithExtension(
                testFolder.getRoot().toPath(), new HashSet<>(Collections.singletonList(".trans")));

        assertThat("first", matchingFiles.get(0), is(third));
        assertThat("second", matchingFiles.get(1), is(first));
        assertThat("third", matchingFiles.get(2), is(second));
    }

    @Test
    public void findFilesWithExtension_returnsListOrderedByFileCreationTime() throws IOException {
        if (SystemUtil.isOsX()) { // setting file create time does not work on OS X - https://bugs.openjdk.java.net/browse/JDK-8151430
            return;
        }

        final Path first = testFolder.newFile("2.trans").toPath();
        final Path second = testFolder.newFile("3.trans").toPath();
        final Path third = testFolder.newFile("1.trans").toPath();
        setFileCreationTime(second, Files.getLastModifiedTime(first).toMillis() + 1000);
        setFileCreationTime(third, Files.getLastModifiedTime(first).toMillis() + 2000);

        final List<Path> matchingFiles = FileFinder.findFilesWithExtension(
                testFolder.getRoot().toPath(), new HashSet<>(Collections.singletonList(".trans")));

        assertThat("first", matchingFiles.get(0), is(first));
        assertThat("second", matchingFiles.get(1), is(second));
        assertThat("third", matchingFiles.get(2), is(third));
    }

    private static void setFileCreationTime(Path file, long creationTime) {
        final BasicFileAttributeView attributes = Files.getFileAttributeView(file, BasicFileAttributeView.class);
        final FileTime time = FileTime.fromMillis(creationTime);
        try {
            attributes.setTimes(time, time, time);
        } catch (IOException e) {
            throw new IllegalStateException();
        }
    }
}