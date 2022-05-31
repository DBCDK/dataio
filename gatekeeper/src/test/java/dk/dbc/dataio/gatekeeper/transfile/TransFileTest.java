package dk.dbc.dataio.gatekeeper.transfile;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class TransFileTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void constructor_transfileArgIsNull_throws() {
        new TransFile(null);
    }

    @Test
    public void constructor_transfileContainsEmptyLines_emptyLinesAreSkipped() throws IOException {
        testFolder.newFile("123456.%001");
        final String line1 = "b=base1,f=123456.%001,i=æøå";
        final String line2 = "b=base2,f=123456.%001";
        final Path file = testFolder.newFile("123456.trans").toPath();
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
        assertThat("Transfile is invalid", transFile.isValid(), is(false));
        assertThat("Invalidation cause", transFile.getCauseForInvalidation(),
                is("Transfil mangler slut-linje"));
    }

    @Test
    public void isValid_transfileNameContainsWhitespace_returnsFalse() throws IOException {
        final Path file = testFolder.newFile("123456.my file.trans").toPath();
        Files.write(file, "slut".getBytes());
        final TransFile transFile = new TransFile(file);
        assertThat("Transfile is invalid", transFile.isValid(), is(false));
        assertThat("Invalidation cause", transFile.getCauseForInvalidation(),
                is("Transfilnavn indeholder blanktegn"));
    }

    @Test
    public void isValid_datafileNameContainsIllegalCharacter_returnsFalse() throws IOException {
        final Path file = testFolder.newFile("123456.trans").toPath();
        Files.write(file, "b=base,f=123456.[VAR].abc\nslut".getBytes(StandardCharsets.UTF_8));
        final TransFile transFile = new TransFile(file);
        assertThat("Transfile is invalid", transFile.isValid(), is(false));
        assertThat("Invalidation cause", transFile.getCauseForInvalidation(),
                is("Datafilnavn <123456.[VAR].abc> indeholder ulovlige tegn"));
    }

    @Test
    public void isValid_datafileNameMissing_returnsFalse() throws IOException {
        final Path file = testFolder.newFile("123456.trans").toPath();
        Files.write(file, "b=base\nslut".getBytes(StandardCharsets.UTF_8));
        final TransFile transFile = new TransFile(file);
        assertThat("Transfile is invalid", transFile.isValid(), is(false));
        assertThat("Invalidation cause", transFile.getCauseForInvalidation(),
                is("Datafil angivelse mangler i transfilen"));
    }

    @Test
    public void isValid_datafileNotFound_returnsFalse() throws IOException {
        final Path file = testFolder.newFile("123456.trans").toPath();
        Files.write(file, "b=base,f=424242\nslut".getBytes(StandardCharsets.UTF_8));
        final TransFile transFile = new TransFile(file);
        assertThat("Transfile is invalid", transFile.isValid(), is(false));
        assertThat("Invalidation cause", transFile.getCauseForInvalidation(),
                is("Kan ikke finde datafilen: 424242"));
    }

    @Test
    public void isValid_transfileNameWithoutSubmitter_returnsFalse() throws IOException {
        final Path file = testFolder.newFile("marc21.trans").toPath();
        Files.write(file, "b=base,f=424242\nslut".getBytes(StandardCharsets.UTF_8));
        final TransFile transFile = new TransFile(file);
        assertThat("Transfile is invalid", transFile.isValid(), is(false));
        assertThat("Invalidation cause", transFile.getCauseForInvalidation(),
                is("Transfilnavn indeholder ugyldigt biblioteksnummer 'marc21'"));
    }

    @Test
    public void isValid_transfileNameNumberFormatException_returnsFalse() throws IOException {
        final Path file = testFolder.newFile("123abc.trans").toPath();
        Files.write(file, "b=base,f=424242\nslut".getBytes(StandardCharsets.UTF_8));
        final TransFile transFile = new TransFile(file);
        assertThat("Transfile is invalid", transFile.isValid(), is(false));
        assertThat("Invalidation cause", transFile.getCauseForInvalidation(),
                is("Transfilnavn indeholder ugyldigt biblioteksnummer '123abc'"));
    }

    @Test
    public void isValid_transfileNameLeadingZeros_returnsTrue() throws IOException {
        final Path file = testFolder.newFile("000123.trans").toPath();
        Files.write(file, "b=base,f=424242\nslut".getBytes(StandardCharsets.UTF_8));
        final Path data = testFolder.newFile("424242").toPath();
        final TransFile transFile = new TransFile(file);
        assertThat("Transfile is valid", transFile.isValid(), is(true));
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

    @Test
    public void ascii() {
        final TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.ascii.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    @Test
    public void utf8() {
        final TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf8.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001,i=æøå"));
    }

    @Test
    public void utf8Bom() {
        final TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf8.bom.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    @Test
    public void utf16BigEngine() {
        final TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf16.be.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    @Test
    public void utf16BigEngineBom() {
        final TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf16.be.bom.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    @Test
    public void utf16LittleEngine() {
        final TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf16.le.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    @Test
    public void utf16LittleEngineBom() {
        final TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf16.le.bom.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    @Test
    public void utf32BigEngine() {
        final TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf32.be.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    @Test
    public void utf32BigEngineBom() {
        final TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf32.be.bom.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    @Test
    public void utf32LittleEngine() {
        final TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf32.le.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    @Test
    public void utf32LittleEngineBom() {
        final TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf32.le.bom.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }
}
