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

package dk.dbc.dataio.gatekeeper.transfile;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TransFileTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void constructor_transfileArgIsNull_throws() throws IOException {
        new TransFile(null);
    }

    @Test
    public void constructor_transfileContainsEmptyLines_emptyLinesAreSkipped() throws IOException {
        final String line1 = "b=base1,f=123456.001_ABC-æøå";
        final String line2 = "b=base2";
        final Path file = testFolder.newFile().toPath();
        final StringBuilder content = new StringBuilder()
                .append(line1).append("\n")
                .append("\n")
                .append("\n")
                .append(line2).append("\n")
                .append("\n")
                .append("slut");
        Files.write(file, content.toString().getBytes(StandardCharsets.UTF_8));
        final TransFile transFile = new TransFile(file);
        assertThat("Number of transfile lines", transFile.getLines().size(), is(2));
        assertThat("Transfile line 1", transFile.getLines().get(0).getLine(), is(line1));
        assertThat("Transfile line 2", transFile.getLines().get(1).getLine(), is(line2));
        assertThat("Transfile is complete", transFile.isComplete(), is(true));
        assertThat("Transfile is valid", transFile.isValid(), is(true));
    }

    @Test
    public void isComplete_transfileIsNotComplete_returnsFalse() throws IOException {
        final TransFile transFile = new TransFile(testFolder.newFile().toPath());
        assertThat(transFile.isComplete(), is(false));
    }

    @Test
    public void isValid_transfileIsNotComplete_returnsFalse() throws IOException {
        final TransFile transFile = new TransFile(testFolder.newFile().toPath());
        assertThat(transFile.isValid(), is(false));
    }

    @Test
    public void isValid_transfileNameContainsWhitespace_returnsFalse() throws IOException {
        final Path file = testFolder.newFile("my file.trans").toPath();
        Files.write(file, "slut".getBytes());
        final TransFile transFile = new TransFile(file);
        assertThat(transFile.isValid(), is(false));
    }

    @Test
    public void isValid_datafileNameContainsIllegalCharacter_returnsFalse() throws IOException {
        final Path file = testFolder.newFile().toPath();
        Files.write(file, "b=base,f=123456.[VAR].abc\nslut".getBytes(StandardCharsets.UTF_8));
        final TransFile transFile = new TransFile(file);
        assertThat("Transfile is invalid", transFile.isValid(), is(false));
        assertThat("Invalidation cause", transFile.getCauseForInvalidation(),
                is("Datafilnavn <123456.[VAR].abc> indeholder ulovlige tegn"));
    }

    @Test
    public void isComplete_transfileIsCompletedWithSlut_returnsTrue() throws IOException {
        final Path file = testFolder.newFile().toPath();
        Files.write(file, "slut".getBytes(StandardCharsets.UTF_8));
        final TransFile transFile = new TransFile(file);
        assertThat(transFile.isComplete(), is(true));
    }

    @Test
    public void isComplete_transfileIsCompletedWithFinish_returnsTrue() throws IOException {
        final Path file = testFolder.newFile().toPath();
        Files.write(file, "finish".getBytes(StandardCharsets.UTF_8));
        final TransFile transFile = new TransFile(file);
        assertThat(transFile.isComplete(), is(true));
    }

    @Test
    public void getLines_whenLinesExist_returnsUnmodifiableList() throws IOException {
        final Path file = testFolder.newFile().toPath();
        Files.write(file, "slut".getBytes(StandardCharsets.UTF_8));
        final TransFile transFile = new TransFile(file);
        try {
            transFile.getLines().add(null);
            fail("No exception thrown");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void getPath_returnsPathOfTransfile() throws IOException {
        final Path file = testFolder.newFile().toPath();
        final TransFile transFile = new TransFile(file);
        assertThat(transFile.getPath(), is(file));
    }

    @Test
    public void exists_transfileExistsOnTheFileSystem_returnsTrue() throws IOException {
        final Path file = testFolder.newFile().toPath();
        final TransFile transFile = new TransFile(file);
        assertThat(transFile.exists(), is(true));
    }

    @Test
    public void exists_transfileDoesNotExistOnTheFileSystem_returnsFalse() throws IOException {
        final Path file = testFolder.newFile().toPath();
        final TransFile transFile = new TransFile(file);
        Files.delete(file);
        assertThat(transFile.exists(), is(false));
    }
}