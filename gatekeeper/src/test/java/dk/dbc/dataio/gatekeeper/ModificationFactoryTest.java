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

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.utils.test.model.GatekeeperDestinationBuilder;
import dk.dbc.dataio.gatekeeper.operation.Opcode;
import dk.dbc.dataio.gatekeeper.transfile.TransFile;
import dk.dbc.dataio.gatekeeper.wal.Modification;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModificationFactoryTest {
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final List<GatekeeperDestination> gatekeeperDestinations = getGatekeeperDestinationsForTest();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setupMocks() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.findAllGatekeeperDestinations()).thenReturn(gatekeeperDestinations);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_transfileArgIsNull_throws() {
        new ModificationFactory(null, flowStoreServiceConnector);
    }

    @Test
    public void constructor_flowStoreServiceConnectorArgIsNull_throws() throws IOException {
        final TransFile transfile = createTransfile("123456.trans", "slut");
        try {
            new ModificationFactory(transfile, null);
            fail("IOException not thrown");
        } catch (NullPointerException e) { }
    }

    @Test
    public void getModifications_transfileHasNoLines_returnsModifications() throws IOException {
        final TransFile transfile = createTransfile("123456.trans", "slut");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications = modificationFactory.getModifications();
        assertThat("Number of modifications", modifications.size(), is(1));
        assertThat("Modification opcode", modifications.get(0).getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("Modification arg", modifications.get(0).getArg(), is(transfile.getPath().getFileName().toString()));
    }

    @Test
    public void getModifications_transfileIsInvalid_returnsModifications() throws IOException {
        final TransFile transfile = createTransfile("123456.trans", "b=danbib,t=lin,c=latin-1,o=marc2");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications = modificationFactory.getModifications();
        assertThat("Number of modifications", modifications.size(), is(2));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_INVALID_TRANSFILE_NOTIFICATION));
        assertThat("Modification 1 arg", modifications.get(0).getArg(), is("Transfil mangler slut-linje"));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.DELETE_FILE));
        assertThat("Modification 2 arg", modifications.get(1).getArg(), is(transfile.getPath().getFileName().toString()));
    }

    @Test
    public void getModifications_singleDataioExclusiveLineWithoutDatafile_returnsModifications() throws IOException {
        final String line = "b=danbib,t=lin,c=latin-1,o=marc2";
        final TransFile transfile = createTransfile("820011.trans", line + "\nslut");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications = modificationFactory.getModifications();
        assertThat("Number of modifications", modifications.size(), is(2));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_INVALID_TRANSFILE_NOTIFICATION));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.DELETE_FILE));

        assertThat("Modification 1 arg", modifications.get(0).getArg(), is("Datafil angivelse mangler i transfilen"));
        assertThat("Modification 2 arg", modifications.get(1).getArg(), is(transfile.getPath().getFileName().toString()));
    }

   @Test
   public void getModifications_singleParallelWithDataioNotificationsLineWithDatafile_returnsModifications() throws IOException {
        testFolder.newFile("820009.file");
        final String line = "b=danbib,f=820009.file,t=lin,c=latin-1,o=marc2";
        final TransFile transfile = createTransfile("820009.trans", line + "\nslut");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications = modificationFactory.getModifications();
        assertThat("Number of modifications", modifications.size(), is(4));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("Modification 3 opcode", modifications.get(2).getOpcode(), is(Opcode.CREATE_TRANSFILE));
        assertThat("Modification 4 opcode", modifications.get(3).getOpcode(), is(Opcode.DELETE_FILE));

        assertThat("Modification 1 arg", modifications.get(0).getArg(), is(line));
        assertThat("Modification 2 arg", modifications.get(1).getArg(), is("820009.file"));
        assertThat("Modification 4 arg", modifications.get(3).getArg(), is(transfile.getPath().getFileName().toString()));

        final String createTransfileArg = modifications.get(2).getArg();
        assertThat("Transfile contains", createTransfileArg, containsString("M=datain-io@dbc.dk,b=danbib,c=latin-1,f=820009.file,m=datain-io@dbc.dk,o=marc2,t=lin\n"));
        assertThat("Transfile has end marker", createTransfileArg, containsString("slut"));
    }

    @Test
    public void getModifications_singleParallelWithPosthusNotificationsLineWithDatafile_returnsModifications() throws IOException, FlowStoreServiceConnectorException {
        testFolder.newFile("820010.file");
        final String line = "b=danbib,f=820010.file,t=lin,c=latin-1,o=marc2";
        final TransFile transfile = createTransfile("820010.trans", line + "\nslut");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications = modificationFactory.getModifications();
        assertThat("Number of modifications", modifications.size(), is(4));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("Modification 3 opcode", modifications.get(2).getOpcode(), is(Opcode.CREATE_TRANSFILE));
        assertThat("Modification 4 opcode", modifications.get(3).getOpcode(), is(Opcode.DELETE_FILE));

        assertThat("Modification 1 arg", modifications.get(0).getArg(), is("M=,b=danbib,c=latin-1,f=820010.file,m=,o=marc2,t=lin"));
        assertThat("Modification 2 arg", modifications.get(1).getArg(), is("820010.file"));
        assertThat("Modification 4 arg", modifications.get(3).getArg(), is(transfile.getPath().getFileName().toString()));

        final String createTransfileArg = modifications.get(2).getArg();
        assertThat("Transfile contains", createTransfileArg, containsString(line + "\n"));
        assertThat("Transfile has end marker", createTransfileArg, containsString("slut"));
    }

    @Test
    public void getModifications_singleDataioExclusiveLineWithDatafile_returnsModifications() throws IOException {
        testFolder.newFile("820011.file");
        final String line = "b=danbib,f=820011.file,t=lin,c=latin-1,o=marc2";
        final TransFile transfile = createTransfile("820011.trans", line + "\nslut");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications = modificationFactory.getModifications();
        assertThat("Number of modifications", modifications.size(), is(3));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.DELETE_FILE));
        assertThat("Modification 3 opcode", modifications.get(2).getOpcode(), is(Opcode.DELETE_FILE));

        assertThat("Modification 1 arg", modifications.get(0).getArg(), is(line));
        assertThat("Modification 2 arg", modifications.get(1).getArg(), is("820011.file"));
        assertThat("Modification 3 arg", modifications.get(2).getArg(), is(transfile.getPath().getFileName().toString()));
    }

    @Test
    public void getModifications_multipleParallelLines_returnsModifications() throws IOException {
        testFolder.newFile("820009.file1");
        testFolder.newFile("820009.file2");
        final String line1 = "b=danbib,f=820009.file1,t=lin,c=latin-1,o=marc2";
        final String line2 = "b=danbib,f=820009.file2,t=lin,c=utf-8,o=marc2";
        final TransFile transfile = createTransfile("820009.trans", line1 + "\n" + line2 + "\nslut");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications = modificationFactory.getModifications();
        assertThat("Number of modifications", modifications.size(), is(6));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("Modification 3 opcode", modifications.get(2).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 4 opcode", modifications.get(3).getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("Modification 5 opcode", modifications.get(4).getOpcode(), is(Opcode.CREATE_TRANSFILE));
        assertThat("Modification 6 opcode", modifications.get(5).getOpcode(), is(Opcode.DELETE_FILE));

        assertThat("Modification 1 arg", modifications.get(0).getArg(), is(line1));
        assertThat("Modification 2 arg", modifications.get(1).getArg(), is("820009.file1"));
        assertThat("Modification 3 arg", modifications.get(2).getArg(), is(line2));
        assertThat("Modification 4 arg", modifications.get(3).getArg(), is("820009.file2"));
        assertThat("Modification 6 arg", modifications.get(5).getArg(), is(transfile.getPath().getFileName().toString()));

        final String createTransfileArg = modifications.get(4).getArg();
        assertThat("Transfile contains", createTransfileArg, containsString(
                "M=datain-io@dbc.dk,b=danbib,c=latin-1,f=820009.file1,m=datain-io@dbc.dk,o=marc2,t=lin\n" +
                "M=datain-io@dbc.dk,b=danbib,c=utf-8,f=820009.file2,m=datain-io@dbc.dk,o=marc2,t=lin\n" +
                "slut"
        ));
    }

    @Test
    public void getModifications_multipleMixedTypes_returnsModification() throws IOException {
        testFolder.newFile("820010.danbib.file");
        testFolder.newFile("820010.dfa.file");
        final String line1 = "b=danbib,f=820010.danbib.file,t=lin,c=latin-1,o=marc2";
        final String line2 = "b=dfa,f=820010.dfa.file,t=lin,c=utf-8,o=marc2";
        final TransFile transfile = createTransfile("820010.trans", line1 + "\n" + line2 + "\nslut");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications = modificationFactory.getModifications();
        assertThat("Number of modifications", modifications.size(), is(5));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("Modification 3 opcode", modifications.get(2).getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("Modification 4 opcode", modifications.get(3).getOpcode(), is(Opcode.CREATE_TRANSFILE));
        assertThat("Modification 5 opcode", modifications.get(4).getOpcode(), is(Opcode.DELETE_FILE));

        assertThat("Modification 1 arg", modifications.get(0).getArg(), is("M=,b=danbib,c=latin-1,f=820010.danbib.file,m=,o=marc2,t=lin"));
        assertThat("Modification 2 arg", modifications.get(1).getArg(), is("820010.danbib.file"));
        assertThat("Modification 3 arg", modifications.get(2).getArg(), is("820010.dfa.file"));
        assertThat("Modification 5 arg", modifications.get(4).getArg(), is(transfile.getPath().getFileName().toString()));

        final String createTransfileArg = modifications.get(3).getArg();
        assertThat("Transfile contains", createTransfileArg, containsString(line1 + System.lineSeparator()));
        assertThat("Transfile contains", createTransfileArg, containsString(line2 + System.lineSeparator()));
        assertThat("Transfile has end marker", createTransfileArg, containsString("slut"));
    }

    @Test
    public void processLine_parallelLineContainsNoDataFile_returnsModifications() throws IOException, FlowStoreServiceConnectorException {
        final TransFile transfile = createTransfile("820010.trans", "b=danbib,t=lin,c=latin-1,o=marc2");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications = modificationFactory.processLine(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(1));
        assertThat("Modification opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
    }

    @Test
    public void processLine_parallelLineContainsDatafile_returnsModifications() throws IOException {
        final TransFile transfile = createTransfile("820010.trans", "b=danbib,f=820010.file,t=lin,c=latin-1,o=marc2");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications = modificationFactory.processLine(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(2));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.MOVE_FILE));
    }

    @Test
    public void processLine_dataioExclusiveContainsDatafile_returnsModifications() throws IOException, FlowStoreServiceConnectorException {
        final TransFile transfile = createTransfile("820011.trans", "b=danbib,f=820011.file,t=lin,c=latin-1,o=marc2");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications = modificationFactory.processLine(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(2));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.DELETE_FILE));
    }

    @Test
    public void processLine_posthusExclusiveLineContainsNoDatafile_returnsModification() throws IOException {
        final TransFile transfile = createTransfile("123456.trans", "b=folk,t=lin,c=latin-1,o=marc2");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications = modificationFactory.processLine(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(0));
    }

    @Test
    public void processLine_posthusExclusiveLineContainsDatafile_returnsModifications() throws IOException {
        final TransFile transfile = createTransfile("123456.trans", "b=folk,f=123456.file,t=lin,c=latin-1,o=marc2");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications = modificationFactory.processLine(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(1));
        assertThat("Modification opcode", modifications.get(0).getOpcode(), is(Opcode.MOVE_FILE));
    }

    @Test
    public void processLine_posthusExclusiveLineWithoutPackaging_returnsModifications() throws IOException {
        testFolder.newFile("820010.file");
        final TransFile transfile = createTransfile("820010.trans", "b=folk,f=820010.file,c=latin-1,o=marc2\nslut");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications = modificationFactory.getModifications();
        assertThat("Number of modifications", modifications.size(), is(3));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.CREATE_TRANSFILE));
        assertThat("Modification 3 opcode", modifications.get(2).getOpcode(), is(Opcode.DELETE_FILE));
    }

    @Test
    public void getDataioExclusiveModifications_lineContainsNoDatafile_returnsModifications() throws IOException {
        final TransFile transfile = createTransfile("820011.trans", "b=danbib,t=lin,c=latin-1,o=marc2\nslut");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications =
                modificationFactory.getDataioExclusiveModifications(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(1));
        assertThat("Modification opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification arg", modifications.get(0).getArg(), is(transfile.getLines().get(0).getLine()));
    }

    @Test
    public void getDataioExclusiveModifications_lineContainsDatafile_returnsModifications() throws IOException {
        final TransFile transfile = createTransfile("820011.trans", "b=danbib,f=820011.file,t=lin,c=latin-1,o=marc2");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications =
                modificationFactory.getDataioExclusiveModifications(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(2));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 1 arg", modifications.get(0).getArg(), is(transfile.getLines().get(0).getLine()));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.DELETE_FILE));
        assertThat("Modification 2 arg", modifications.get(1).getArg(), is("820011.file"));
    }

    @Test
    public void getPosthusExclusiveModifications_lineContainsNoDatafile_returnsNoModifications() throws IOException {
        final TransFile transfile = createTransfile("123456.trans", "b=folk,t=lin,c=latin-1,o=marc2");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications =
                modificationFactory.getPosthusExclusiveModifications(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(0));
    }

    @Test
    public void getPosthusExclusiveModifications_lineContainsDatafile_returnsModifications() throws IOException {
        final TransFile transfile = createTransfile("123456.trans", "b=folk,f=123456.file,t=lin,c=latin-1,o=marc2");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications =
                modificationFactory.getPosthusExclusiveModifications(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(1));
        assertThat("Modification opcode", modifications.get(0).getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("Modification arg", modifications.get(0).getArg(), is("123456.file"));
    }

    @Test
    public void getParallelWithDataioNotificationsModifications_lineContainsNoDatafile_returnsModifications() throws IOException {
        final TransFile transfile = createTransfile("820010.trans", "b=danbib,t=lin,c=latin-1,o=marc2");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications =
                modificationFactory.getParallelWithDataioNotificationsModifications(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(1));
        assertThat("Modification opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification arg", modifications.get(0).getArg(), is(transfile.getLines().get(0).getLine()));
    }

    @Test
    public void getParallelWithDataioNotificationsModifications_lineContainsDatafile_returnsModifications() throws IOException {
        final TransFile transfile = createTransfile("820010.trans", "b=danbib,f=820010.file,t=lin,c=latin-1,o=marc2");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications =
                modificationFactory.getParallelWithDataioNotificationsModifications(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(2));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 1 arg", modifications.get(0).getArg(), is(transfile.getLines().get(0).getLine()));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("Modification 2 arg", modifications.get(1).getArg(), is("820010.file"));
    }

    @Test
    public void getParallelWithPosthusNotificationsModifications_lineContainsNoDatafile_returnsModifications() throws IOException {
        final TransFile transfile = createTransfile("820010.trans", "b=danbib,t=lin,c=latin-1,o=marc2");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications =
                modificationFactory.getParallelWithPosthusNotificationsModifications(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(1));
        assertThat("Modification opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification arg", modifications.get(0).getArg(), is("M=,b=danbib,c=latin-1,m=,o=marc2,t=lin"));
    }

    @Test
    public void getParallelWithPosthusNotificationsModifications_lineContainsDatafile_returnsModifications() throws IOException {
        final TransFile transfile = createTransfile("820010.trans", "b=danbib,f=820010.file,t=lin,c=latin-1,o=marc2");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final List<Modification> modifications =
                modificationFactory.getParallelWithPosthusNotificationsModifications(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(2));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 1 arg", modifications.get(0).getArg(), is("M=,b=danbib,c=latin-1,f=820010.file,m=,o=marc2,t=lin"));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("Modification 2 arg", modifications.get(1).getArg(), is("820010.file"));
    }

    @Test
    public void determineType_missingSubmitter_returnsDataioExclusive() throws IOException {
        final TransFile transfile = createTransfile("missing_submitter.trans", "b=danbib,t=lin,c=latin-1,o=marc2");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        assertThat(modificationFactory.determineType(transfile.getLines().get(0)), is(ModificationFactory.Type.DATAIO_EXCLUSIVE));
    }

    @Test
    public void getDataFilename_lineContainsNoDatafile_returnsEmptyString() {
        final TransFile.Line line = new TransFile.Line("foo");
        final ModificationFactory modificationFactory = new ModificationFactory(flowStoreServiceConnector);
        assertThat(modificationFactory.getDataFilename(line), is(""));
    }

    @Test
    public void getDataFilename_lineContainsEmptyDatafile_returnsEmptyString() {
        final TransFile.Line line = new TransFile.Line("f=  ,g=foo");
        final ModificationFactory modificationFactory = new ModificationFactory(flowStoreServiceConnector);
        assertThat(modificationFactory.getDataFilename(line), is(""));
    }

    @Test
    public void getDataFilename_lineContainsDatafile_returnsDatafile() {
        final TransFile.Line line = new TransFile.Line("f=foo");
        final ModificationFactory modificationFactory = new ModificationFactory(flowStoreServiceConnector);
        assertThat(modificationFactory.getDataFilename(line), is("foo"));
    }

    @Test
    public void getFileDeleteModification_returnsModification() throws IOException {
        final Path transfilePath = testFolder.newFile().toPath();
        final TransFile transfile = new TransFile(transfilePath);
        final String filename = "file";

        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
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

        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
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

        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
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

        final ModificationFactory modificationFactory = new ModificationFactory(transfile, flowStoreServiceConnector);
        final Modification createTransfileModification = modificationFactory.getCreateTransfileModification(arg);
        assertThat("createTransfileModification", createTransfileModification, is(notNullValue()));
        assertThat("createTransfileModification.getOpcode()", createTransfileModification.getOpcode(), is(Opcode.CREATE_TRANSFILE));
        assertThat("createTransfileModification.getArg()", createTransfileModification.getArg(), is(arg));
        assertThat("createTransfileModification.getTransfileName()", createTransfileModification.getTransfileName(),
                is(transfilePath.getFileName().toString()));
    }

    /*
     * Package private methods
     */
    static List<GatekeeperDestination> getGatekeeperDestinationsForTest() {
        final GatekeeperDestination gatekeeperDestinationIsParallelWithDataioNotifications = new GatekeeperDestinationBuilder()
                .setId(0L)
                .setSubmitterNumber("820009")
                .setDestination("danbib")
                .setPackaging("lin")
                .setFormat("marc2")
                .setCopyToPosthus(true)
                .setNotifyFromPosthus(false)
                .build();

        final GatekeeperDestination gatekeeperDestinationIsParallelWithPosthusNotifications = new GatekeeperDestinationBuilder()
                .setId(0L)
                .setSubmitterNumber("820010")
                .setDestination("danbib")
                .setPackaging("lin")
                .setFormat("marc2")
                .setCopyToPosthus(true)
                .setNotifyFromPosthus(true)
                .build();

        final GatekeeperDestination gatekeeperDestinationIsDataioExclusive = new GatekeeperDestinationBuilder()
                .setId(0L)
                .setSubmitterNumber("820011")
                .setDestination("danbib")
                .setPackaging("lin")
                .setFormat("marc2")
                .setCopyToPosthus(false)
                .build();

        return Arrays.asList(
                gatekeeperDestinationIsParallelWithDataioNotifications,
                gatekeeperDestinationIsParallelWithPosthusNotifications,
                gatekeeperDestinationIsDataioExclusive);
    }

    /*
     * Private methods
     */
    private void writeFile(Path filename, String content) {
        try {
            Files.write(filename, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private TransFile createTransfile(String transfileName, String content) throws IOException {
        final Path transfilePath = testFolder.newFile(transfileName).toPath();
        writeFile(transfilePath, content);
        return new TransFile(transfilePath);
    }
}