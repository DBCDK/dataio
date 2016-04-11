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

package dk.dbc.dataio.gatekeeper.operation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class FileMoveOperationTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void constructor_sourceArgIsNull_throws() {
        new FileMoveOperation(null, Paths.get("destination"));
    }

    @Test(expected = NullPointerException.class)
    public void constructor_destinationArgIsNull_throws() {
        new FileMoveOperation(Paths.get("source"), null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final Path source = Paths.get("source");
        final Path destination = Paths.get("destination");
        final FileMoveOperation fileMoveOperation = new FileMoveOperation(source, destination);
        assertThat("instance", fileMoveOperation, is(notNullValue()));
        assertThat("getSource()", fileMoveOperation.getSource(), is(source));
        assertThat("getDestination()", fileMoveOperation.getDestination(), is(destination));
    }

    @Test
    public void execute_sourcePathDoesNotExist_returns() throws OperationExecutionException {
        final Path source = Paths.get("source");
        final Path destination = Paths.get("destination");
        final FileMoveOperation fileMoveOperation = new FileMoveOperation(source, destination);
        fileMoveOperation.execute();
    }

    @Test
    public void execute_sourcePathExists_movesToDestination() throws IOException, OperationExecutionException {
        final Path source = testFolder.newFile("source").toPath();
        final Path destination = Files.createFile(testFolder.newFolder().toPath().resolve("destination"));
        final FileMoveOperation fileMoveOperation = new FileMoveOperation(source, destination);
        fileMoveOperation.execute();
        assertThat("Source file exists after move", Files.exists(source), is(false));
        assertThat("Destination file exists after move", Files.exists(destination), is(true));
    }

    @Test
    public void execute_moveFails_throws() throws IOException, OperationExecutionException {
        final Path source = testFolder.newFile("file").toPath();
        final Path inaccessibleDestination = FileSystems.getDefault().getPath(System.getProperty("user.home")).getParent();
        final FileMoveOperation fileMoveOperation = new FileMoveOperation(source, inaccessibleDestination);
        assertThat(fileMoveOperation::execute, isThrowing(OperationExecutionException.class));
    }
}