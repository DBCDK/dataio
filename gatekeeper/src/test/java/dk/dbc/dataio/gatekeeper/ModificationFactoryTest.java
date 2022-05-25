package dk.dbc.dataio.gatekeeper;

import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.utils.test.model.GatekeeperDestinationBuilder;
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
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ModificationFactoryTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void constructor_transfileArgIsNull_throws() {
        new ModificationFactory(null);
    }

    @Test
    public void getModifications_transfileHasNoLines_returnsModifications() throws IOException {
        final TransFile transfile = createTransfile("123456.trans", "slut");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications = modificationFactory.getModifications();
        assertThat("Number of modifications", modifications.size(), is(1));
        assertThat("Modification opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_INVALID_TRANSFILE_NOTIFICATION));
        assertThat("Modification arg", modifications.get(0).getArg(), is("Transfilen har intet indhold"));
        assertThat("Modification trans file name", modifications.get(0).getTransfileName(), is(transfile.getPath().getFileName().toString()));

    }

    @Test
    public void getModifications_transfileIsInvalid_returnsModifications() throws IOException {
        final TransFile transfile = createTransfile("123456.trans", "b=danbib,t=lin,c=latin-1,o=marc2");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
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
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications = modificationFactory.getModifications();
        assertThat("Number of modifications", modifications.size(), is(2));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_INVALID_TRANSFILE_NOTIFICATION));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.DELETE_FILE));

        assertThat("Modification 1 arg", modifications.get(0).getArg(), is("Datafil angivelse mangler i transfilen"));
        assertThat("Modification 2 arg", modifications.get(1).getArg(), is(transfile.getPath().getFileName().toString()));
    }

    @Test
    public void getModifications_singleDataioExclusiveLineWithDatafile_returnsModifications() throws IOException {
        testFolder.newFile("820011.file");
        final String line = "b=danbib,f=820011.file,t=lin,c=latin-1,o=marc2";
        final TransFile transfile = createTransfile("820011.trans", line + "\nslut");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
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
    public void processLine_dataioExclusiveContainsDatafile_returnsModifications() throws IOException {
        final TransFile transfile = createTransfile("820011.trans", "b=danbib,f=820011.file,t=lin,c=latin-1,o=marc2");
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        final List<Modification> modifications = modificationFactory.processLine(transfile.getLines().get(0));
        assertThat("Number of modifications", modifications.size(), is(2));
        assertThat("Modification 1 opcode", modifications.get(0).getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("Modification 2 opcode", modifications.get(1).getOpcode(), is(Opcode.DELETE_FILE));
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

    /*
     * Package private methods
     */
    static List<GatekeeperDestination> getGatekeeperDestinationsForTest() {
        final GatekeeperDestination gatekeeperDestinationIsDataioExclusive = new GatekeeperDestinationBuilder()
                .setId(0L)
                .setSubmitterNumber("820011")
                .setDestination("danbib")
                .setPackaging("lin")
                .setFormat("marc2")
                .build();

        return Collections.singletonList(gatekeeperDestinationIsDataioExclusive);
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
