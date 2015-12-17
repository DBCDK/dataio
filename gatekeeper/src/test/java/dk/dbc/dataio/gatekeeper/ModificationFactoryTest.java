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

import dk.dbc.dataio.gatekeeper.operation.Opcode;
import dk.dbc.dataio.gatekeeper.transfile.TransFile;
import dk.dbc.dataio.gatekeeper.wal.Modification;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ModificationFactoryTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void constructor_transfileArgIsNull_throws() {
        new ModificationFactory(null);
    }

    @Test
    public void getModifications_transfileHasNoLines_returnsModifications() throws IOException {
        final Path transfilePath = testFolder.newFile().toPath();
        writeFile(transfilePath, "slut");
        final TransFile transfile = new TransFile(transfilePath);
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications = modificationFactory.getModifications();
        assertThat("Number of modifications", modifications.size(), is(1));
        assertThat("Modification opcode", modifications.get(0).getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("Modification arg", modifications.get(0).getArg(), is(transfilePath.getFileName().toString()));
    }

    @Test
    public void getModifications_transfileIsIncomplete_returnsModifications() throws IOException {
        final String content = "b=danbib,t=lin,c=latin-1,o=marc2" + System.lineSeparator() + "b=danbib";
        final Path transfilePath = testFolder.newFile().toPath();
        writeFile(transfilePath, content + System.lineSeparator());
        final TransFile transfile = new TransFile(transfilePath);
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications = modificationFactory.getModifications();
        assertThat("Number of modifications", modifications.size(), is(2));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_INCOMPLETE_TRANSFILE_NOTIFICATION));
        assertThat("Modification 1 arg", modifications.get(0).getArg(), is(content));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.DELETE_FILE));
        assertThat("Modification 2 arg", modifications.get(1).getArg(), is(transfilePath.getFileName().toString()));
    }

    @Test
    public void getModifications_singleParallelLineWithoutDatafile_returnsModifications() throws IOException {
        final String line = "b=danbib,t=lin,c=latin-1,o=marc2";
        final Path transfilePath = testFolder.newFile().toPath();
        writeFile(transfilePath, line + System.lineSeparator());
        writeFile(transfilePath, "slut");
        final TransFile transfile = new TransFile(transfilePath);
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications = modificationFactory.getModifications();
        assertThat("Number of modifications", modifications.size(), is(3));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.CREATE_TRANSFILE));
        assertThat("Modification 3 opcode", modifications.get(2).getOpcode(), is(Opcode.DELETE_FILE));

        assertThat("Modification 1 arg", modifications.get(0).getArg(), is(line));
        assertThat("Modification 3 arg", modifications.get(2).getArg(), is(transfilePath.getFileName().toString()));

        final String createTransfileArg = modifications.get(1).getArg();
        assertThat("Transfile contains", createTransfileArg, containsString(line + System.lineSeparator()));
        assertThat("Transfile has end marker", createTransfileArg, containsString("slut"));
    }

    @Test
    public void getModifications_singleParallelLineWithDatafile_returnsModifications() throws IOException {
        final String line = "b=danbib,f=820010.file,t=lin,c=latin-1,o=marc2";
        final Path transfilePath = testFolder.newFile().toPath();
        writeFile(transfilePath, line + System.lineSeparator());
        writeFile(transfilePath, "slut");
        final TransFile transfile = new TransFile(transfilePath);
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications = modificationFactory.getModifications();
        assertThat("Number of modifications", modifications.size(), is(4));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("Modification 3 opcode", modifications.get(2).getOpcode(), is(Opcode.CREATE_TRANSFILE));
        assertThat("Modification 4 opcode", modifications.get(3).getOpcode(), is(Opcode.DELETE_FILE));

        assertThat("Modification 1 arg", modifications.get(0).getArg(), is(line));
        assertThat("Modification 2 arg", modifications.get(1).getArg(), is("820010.file"));
        assertThat("Modification 4 arg", modifications.get(3).getArg(), is(transfilePath.getFileName().toString()));

        final String createTransfileArg = modifications.get(2).getArg();
        assertThat("Transfile contains", createTransfileArg, containsString(line + System.lineSeparator()));
        assertThat("Transfile has end marker", createTransfileArg, containsString("slut"));
    }

    @Test
    public void getModifications_multipleParallelLines_returnsModifications() throws IOException {
        final String line1 = "b=danbib,f=820010.file,t=lin,c=latin-1,o=marc2";
        final String line2 = "b=danbib,t=lin,c=utf-8,o=marc2";
        final Path transfilePath = testFolder.newFile().toPath();
        writeFile(transfilePath, line1 + System.lineSeparator());
        writeFile(transfilePath, line2 + System.lineSeparator());
        writeFile(transfilePath, "slut");
        final TransFile transfile = new TransFile(transfilePath);
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications = modificationFactory.getModifications();
        assertThat("Number of modifications", modifications.size(), is(5));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("Modification 3 opcode", modifications.get(2).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 4 opcode", modifications.get(3).getOpcode(), is(Opcode.CREATE_TRANSFILE));
        assertThat("Modification 5 opcode", modifications.get(4).getOpcode(), is(Opcode.DELETE_FILE));

        assertThat("Modification 1 arg", modifications.get(0).getArg(), is(line1));
        assertThat("Modification 2 arg", modifications.get(1).getArg(), is("820010.file"));
        assertThat("Modification 3 arg", modifications.get(2).getArg(), is(line2));
        assertThat("Modification 5 arg", modifications.get(4).getArg(), is(transfilePath.getFileName().toString()));

        final String createTransfileArg = modifications.get(3).getArg();
        assertThat("Transfile contains", createTransfileArg, containsString(line1 + System.lineSeparator()));
        assertThat("Transfile contains", createTransfileArg, containsString(line2 + System.lineSeparator()));
        assertThat("Transfile has end marker", createTransfileArg, containsString("slut"));
    }

    @Test
    public void getModifications_multipleMixedTypes_returnsModifications() throws IOException {
        final String line1 = "b=danbib,f=820010.file,t=lin,c=latin-1,o=marc2";
        final String line2 = "b=dfa,f=654321.file,t=lin,c=utf-8,o=marc2";
        final Path transfilePath = testFolder.newFile().toPath();
        writeFile(transfilePath, line1 + System.lineSeparator());
        writeFile(transfilePath, line2 + System.lineSeparator());
        writeFile(transfilePath, "slut");
        final TransFile transfile = new TransFile(transfilePath);
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications = modificationFactory.getModifications();
        assertThat("Number of modifications", modifications.size(), is(5));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("Modification 3 opcode", modifications.get(2).getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("Modification 4 opcode", modifications.get(3).getOpcode(), is(Opcode.CREATE_TRANSFILE));
        assertThat("Modification 5 opcode", modifications.get(4).getOpcode(), is(Opcode.DELETE_FILE));

        assertThat("Modification 1 arg", modifications.get(0).getArg(), is(line1));
        assertThat("Modification 2 arg", modifications.get(1).getArg(), is("820010.file"));
        assertThat("Modification 3 arg", modifications.get(2).getArg(), is("654321.file"));
        assertThat("Modification 5 arg", modifications.get(4).getArg(), is(transfilePath.getFileName().toString()));

        final String createTransfileArg = modifications.get(3).getArg();
        assertThat("Transfile contains", createTransfileArg, containsString(line1 + System.lineSeparator()));
        assertThat("Transfile contains", createTransfileArg, containsString(line2 + System.lineSeparator()));
        assertThat("Transfile has end marker", createTransfileArg, containsString("slut"));
    }

    @Test
    public void processLine_parallelLineContainsNoDataFile_returnsModifications() throws IOException {
        final Path transfilePath = testFolder.newFile().toPath();
        writeFile(transfilePath, "b=danbib,t=lin,c=latin-1,o=marc2");
        final TransFile transfile = new TransFile(transfilePath);
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications = modificationFactory.processLine(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(1));
        assertThat("Modification opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
    }

    @Test
    public void processLine_parallelLineContainsDatafile_returnsModifications() throws IOException {
        final Path transfilePath = testFolder.newFile().toPath();
        writeFile(transfilePath, "b=danbib,f=820010.file,t=lin,c=latin-1,o=marc2");
        final TransFile transfile = new TransFile(transfilePath);
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications = modificationFactory.processLine(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(2));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.MOVE_FILE));
    }

    @Test
    public void processLine_posthusExclusiveLineContainsNoDatafile_returnsNoModifications() throws IOException {
        final Path transfilePath = testFolder.newFile().toPath();
        writeFile(transfilePath, "b=mybib,t=lin,c=latin-1,o=marc2");
        final TransFile transfile = new TransFile(transfilePath);
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications = modificationFactory.processLine(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(0));
    }

    @Test
    public void processLine_posthusExclusiveLineContainsDatafile_returnsModifications() throws IOException {
        final Path transfilePath = testFolder.newFile().toPath();
        writeFile(transfilePath, "b=mybib,f=123456.file,t=lin,c=latin-1,o=marc2");
        final TransFile transfile = new TransFile(transfilePath);
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications =
                modificationFactory.processLine(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(1));
        assertThat("Modification opcode", modifications.get(0).getOpcode(), is(Opcode.MOVE_FILE));
    }

    @Test
    public void getDataioExclusiveModifications_lineContainsNoDatafile_returnsModifications() throws IOException {
        final Path transfilePath = testFolder.newFile().toPath();
        writeFile(transfilePath, "b=danbib,t=lin,c=latin-1,o=marc2");
        final TransFile transfile = new TransFile(transfilePath);
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications =
                modificationFactory.getDataioExclusiveModifications(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(1));
        assertThat("Modification opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification arg", modifications.get(0).getArg(), is(transfile.getLines().get(0).getLine()));
    }

    @Test
    public void getDataioExclusiveModifications_lineContainsDatafile_returnsModifications() throws IOException {
        final Path transfilePath = testFolder.newFile().toPath();
        writeFile(transfilePath, "b=danbib,f=123456.file,t=lin,c=latin-1,o=marc2");
        final TransFile transfile = new TransFile(transfilePath);
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications =
                modificationFactory.getDataioExclusiveModifications(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(2));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 1 arg", modifications.get(0).getArg(), is(transfile.getLines().get(0).getLine()));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.DELETE_FILE));
        assertThat("Modification 2 arg", modifications.get(1).getArg(), is("123456.file"));
    }

    @Test
    public void getPosthusExclusiveModifications_lineContainsNoDatafile_returnsNoModifications() throws IOException {
        final Path transfilePath = testFolder.newFile().toPath();
        writeFile(transfilePath, "b=mybib,t=lin,c=latin-1,o=marc2");
        final TransFile transfile = new TransFile(transfilePath);
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications =
                modificationFactory.getPosthusExclusiveModifications(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(0));
    }

    @Test
    public void getPosthusExclusiveModifications_lineContainsDatafile_returnsModifications() throws IOException {
        final Path transfilePath = testFolder.newFile().toPath();
        writeFile(transfilePath, "b=mybib,f=123456.file,t=lin,c=latin-1,o=marc2");
        final TransFile transfile = new TransFile(transfilePath);
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications =
                modificationFactory.getPosthusExclusiveModifications(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(1));
        assertThat("Modification opcode", modifications.get(0).getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("Modification arg", modifications.get(0).getArg(), is("123456.file"));
    }

    @Test
    public void getParallelModifications_lineContainsNoDatafile_returnsModifications() throws IOException {
        final Path transfilePath = testFolder.newFile().toPath();
        writeFile(transfilePath, "b=danbib,t=lin,c=latin-1,o=marc2");
        final TransFile transfile = new TransFile(transfilePath);
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications =
                modificationFactory.getParallelModifications(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(1));
        assertThat("Modification opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification arg", modifications.get(0).getArg(), is(transfile.getLines().get(0).getLine()));
    }

    @Test
    public void getParallelModifications_lineContainsDatafile_returnsModifications() throws IOException {
        final Path transfilePath = testFolder.newFile().toPath();
        writeFile(transfilePath, "b=danbib,f=123456.file,t=lin,c=latin-1,o=marc2");
        final TransFile transfile = new TransFile(transfilePath);
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications =
                modificationFactory.getParallelModifications(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(2));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 1 arg", modifications.get(0).getArg(), is(transfile.getLines().get(0).getLine()));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("Modification 2 arg", modifications.get(1).getArg(), is("123456.file"));
    }

    @Test
    public void determineType_lineIsInvalid_returnsPosthusExclusive() {
        final TransFile.Line line = new TransFile.Line("foo");
        final ModificationFactory modificationFactory = new ModificationFactory();
        assertThat(modificationFactory.determineType(line), is(ModificationFactory.Type.POSTHUS_EXCLUSIVE));
    }

    @Test
    public void determineType_danbibMatch_returnsParallel() {
        final TransFile.Line line = new TransFile.Line("b=danbib,t=lin,c=latin-1,o=marc2");
        final ModificationFactory modificationFactory = new ModificationFactory();
        assertThat(modificationFactory.determineType(line), is(ModificationFactory.Type.PARALLEL));
    }

    @Test
    public void determineType_dfaMatch_returnsPosthusExclusive() {
        final TransFile.Line line = new TransFile.Line("b=dfa,t=lin,c=latin-1,o=marc2");
        final ModificationFactory modificationFactory = new ModificationFactory();
        assertThat(modificationFactory.determineType(line), is(ModificationFactory.Type.POSTHUS_EXCLUSIVE));
    }

    @Test
    public void getDataFilename_lineContainsNoDatafile_returnsEmptyString() {
        final TransFile.Line line = new TransFile.Line("foo");
        final ModificationFactory modificationFactory = new ModificationFactory();
        assertThat(modificationFactory.getDataFilename(line), is(""));
    }

    @Test
    public void getDataFilename_lineContainsEmptyDatafile_returnsEmptyString() {
        final TransFile.Line line = new TransFile.Line("f=  ,g=foo");
        final ModificationFactory modificationFactory = new ModificationFactory();
        assertThat(modificationFactory.getDataFilename(line), is(""));
    }

    @Test
    public void getDataFilename_lineContainsDatafile_returnsDatafile() {
        final TransFile.Line line = new TransFile.Line("f=foo");
        final ModificationFactory modificationFactory = new ModificationFactory();
        assertThat(modificationFactory.getDataFilename(line), is("foo"));
    }

    @Test
    public void getFileDeleteModification_returnsModification() throws IOException {
        final Path transfilePath = testFolder.newFile().toPath();
        final TransFile transfile = new TransFile(transfilePath);
        final String filename = "file";

        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final Modification fileDeleteModification = modificationFactory.getFileDeleteModification(filename);
        assertThat("fileDeleteModification", fileDeleteModification, is(notNullValue()));
        assertThat("fileDeleteModification.getOpcode()", fileDeleteModification.getOpcode(), is(Opcode.DELETE_FILE));
        assertThat("fileDeleteModification.getArg()", fileDeleteModification.getArg(), is(filename));
        assertThat("fileDeleteModification.getTransfileName()", fileDeleteModification.getTransfileName(),
                is(transfilePath.getFileName().toString()));
    }

    @Test
    public void getFileMoveModification_returnsModification() throws IOException {
        final Path transfilePath = testFolder.newFile().toPath();
        final TransFile transfile = new TransFile(transfilePath);
        final String filename = "file";

        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final Modification fileMoveModification = modificationFactory.getFileMoveModification(filename);
        assertThat("fileMoveModification", fileMoveModification, is(notNullValue()));
        assertThat("fileMoveModification.getOpcode()", fileMoveModification.getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("fileMoveModification.getArg()", fileMoveModification.getArg(), is(filename));
        assertThat("fileMoveModification.getTransfileName()", fileMoveModification.getTransfileName(),
                is(transfilePath.getFileName().toString()));
    }

    @Test
    public void getCreateJobModification_returnsModification() throws IOException {
        final Path transfilePath = testFolder.newFile().toPath();
        final TransFile transfile = new TransFile(transfilePath);
        final String arg = "data";

        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final Modification createJobModification = modificationFactory.getCreateJobModification(arg);
        assertThat("createJobModification", createJobModification, is(notNullValue()));
        assertThat("createJobModification.getOpcode()", createJobModification.getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("createJobModification.getArg()", createJobModification.getArg(), is(arg));
        assertThat("createJobModification.getTransfileName()", createJobModification.getTransfileName(),
                is(transfilePath.getFileName().toString()));
    }

    @Test
    public void getCreateTransfileModification_returnsModification() throws IOException {
        final Path transfilePath = testFolder.newFile().toPath();
        final TransFile transfile = new TransFile(transfilePath);
        final String arg = "data";

        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final Modification createTransfileModification = modificationFactory.getCreateTransfileModification(arg);
        assertThat("createTransfileModification", createTransfileModification, is(notNullValue()));
        assertThat("createTransfileModification.getOpcode()", createTransfileModification.getOpcode(), is(Opcode.CREATE_TRANSFILE));
        assertThat("createTransfileModification.getArg()", createTransfileModification.getArg(), is(arg));
        assertThat("createTransfileModification.getTransfileName()", createTransfileModification.getTransfileName(),
                is(transfilePath.getFileName().toString()));
    }

    private void writeFile(Path filename, String content) {
        try {
            Files.write(filename, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}