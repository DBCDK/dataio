package dk.dbc.dataio.gatekeeper.transfile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TransFileTest {
    @TempDir
    public Path testFolder;

    @Test
    public void constructor_transfileArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new TransFile(null));
    }

    @Test
    public void constructor_transfileContainsEmptyLines_emptyLinesAreSkipped() throws IOException {
        Files.createFile(testFolder.resolve("123456.%001"));
        final String line1 = "b=base1,f=123456.%001,i=æøå";
        final String line2 = "b=base2,f=123456.%001";
        Path file = Files.createFile(testFolder.resolve("123456.trans"));
        String content = line1 + "\n" +
                "\n" +
                "\n" +
                line2 + "\n" +
                "\n" +
                "slut";
        Files.writeString(file, content);
        TransFile transFile = new TransFile(file);
        assertThat("Number of transfile lines", transFile.getLines().size(), is(2));
        assertThat("Transfile line 1", transFile.getLines().get(0).getLine(), is(line1));
        assertThat("Transfile line 2", transFile.getLines().get(1).getLine(), is(line2));
        assertThat("Transfile is complete", transFile.isComplete(), is(true));
        assertThat("Transfile is valid", transFile.isValid(), is(true));
    }

    @Test
    public void isComplete_transfileIsNotComplete_returnsFalse() throws IOException {
        TransFile transFile = makeTransfile("");
        assertThat(transFile.isComplete(), is(false));
        assertThat("Transfile is invalid", transFile.isValid(), is(false));
        assertThat("Invalidation cause", transFile.getCauseForInvalidation(),
                is("Transfil har intet indhold"));
    }

    @Test
    public void isValid_transfileNameContainsWhitespace_returnsFalse() throws IOException {
        TransFile transFile = makeTransfile("123456.my file.trans", "slut");
        assertThat("Transfile is invalid", transFile.isValid(), is(false));
        assertThat("Invalidation cause", transFile.getCauseForInvalidation(),
                is("Transfilnavn indeholder blanktegn"));
    }

    @Test
    public void isValid_datafileNameContainsIllegalCharacter_returnsFalse() throws IOException {
        TransFile transFile = makeTransfile("b=base,f=123456.[VAR].abc\nslut");
        assertThat("Transfile is invalid", transFile.isValid(), is(false));
        assertThat("Invalidation cause", transFile.getCauseForInvalidation(),
                is("Datafilnavn <123456.[VAR].abc> indeholder ulovlige tegn"));
    }

    @Test
    public void isValid_datafileNameMissing_returnsFalse() throws IOException {
        TransFile transFile = makeTransfile("b=base\nslut");
        assertThat("Transfile is invalid", transFile.isValid(), is(false));
        assertThat("Invalidation cause", transFile.getCauseForInvalidation(),
                is("Datafil angivelse mangler i transfilen"));
    }

    @Test
    public void isValid_datafileNotFound_returnsFalse() throws IOException {
        TransFile transFile = makeTransfile("b=base,f=424242\nslut");
        assertThat("Transfile is invalid", transFile.isValid(), is(false));
        assertThat("Invalidation cause", transFile.getCauseForInvalidation(),
                is("Kan ikke finde datafilen: 424242"));
    }

    @Test
    public void isValid_transfileNameWithoutSubmitter_returnsFalse() throws IOException {
        TransFile transFile = makeTransfile("marc21.trans", "b=base,f=424242\nslut");
        assertThat("Transfile is invalid", transFile.isValid(), is(false));
        assertThat("Invalidation cause", transFile.getCauseForInvalidation(),
                is("Transfilnavn indeholder ugyldigt biblioteksnummer 'marc21'"));
    }

    @Test
    public void isValid_transfileNameNumberFormatException_returnsFalse() throws IOException {
        TransFile transFile = makeTransfile("123abc.trans", "b=base,f=424242\nslut");
        assertThat("Transfile is invalid", transFile.isValid(), is(false));
        assertThat("Invalidation cause", transFile.getCauseForInvalidation(),
                is("Transfilnavn indeholder ugyldigt biblioteksnummer '123abc'"));
    }

    @Test
    public void isValid_transfileNameLeadingZeros_returnsTrue() throws IOException {
        makeTransfile("424242", "");
        TransFile transFile = makeTransfile("000123.trans", "b=base,f=424242\nslut");
        assertThat("Transfile is valid", transFile.isValid(), is(true));
    }

    @Test
    public void isComplete_transfileIsCompletedWithSlut_returnsTrue() throws IOException {
        TransFile transFile =makeTransfile("slut");
        assertThat(transFile.isComplete(), is(true));
    }

    @Test
    public void isComplete_transfileIsCompletedWithFinish_returnsTrue() throws IOException {
        TransFile transFile = makeTransfile("finish");
        assertThat(transFile.isComplete(), is(true));
    }

    @Test
    public void getLines_whenLinesExist_returnsUnmodifiableList() throws IOException {
        TransFile transFile = makeTransfile("slut");
        assertThrows(UnsupportedOperationException.class, () -> transFile.getLines().add(null));
    }

    @Test
    public void exists_transfileExistsOnTheFileSystem_returnsTrue() throws IOException {
        TransFile transFile = makeTransfile("");
        assertThat(transFile.exists(), is(true));
    }

    @Test
    public void ascii() {
        TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.ascii.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    @Test
    public void utf8() {
        TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf8.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001,i=æøå"));
    }

    @Test
    public void utf8Bom() {
        TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf8.bom.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    @Test
    public void utf16BigEngine() {
        TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf16.be.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    @Test
    public void utf16BigEngineBom() {
        TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf16.be.bom.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    @Test
    public void utf16LittleEngine() {
        TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf16.le.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    @Test
    public void utf16LittleEngineBom() {
        TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf16.le.bom.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    @Test
    public void utf32BigEngine() {
        TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf32.be.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    @Test
    public void utf32BigEngineBom() {
        TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf32.be.bom.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    @Test
    public void utf32LittleEngine() {
        TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf32.le.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    @Test
    public void utf32LittleEngineBom() {
        TransFile transFile = new TransFile(Paths.get("src/test/resources/123456.utf32.le.bom.trans"));
        assertThat("transfile isValid", transFile.isValid(), is(true));
        assertThat("transfile content", transFile.getLines().get(0).getLine(),
                is("b=base,f=123456.001"));
    }

    private TransFile makeTransfile(String filename, String content) throws IOException {
        Path file = Files.createFile(testFolder.resolve(filename));
        Files.writeString(file, content);
        return new TransFile(file);
    }

    private TransFile makeTransfile(String content) throws IOException {
        return makeTransfile("123456.trans", content);
    }
}
