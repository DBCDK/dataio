package dk.dbc.dataio.gatekeeper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class EncodingDetectorTest {
    private final EncodingDetector encodingDetector = new EncodingDetector();

    @Test
    public void fileAscii() {
        assertThat(encodingDetector.detect(Paths.get("src/test/resources/123456.ascii.trans")).get(),
                is(Charset.forName("ISO-8859-9")));
    }

    @Test
    public void fileUtf8() {
        assertThat(encodingDetector.detect(Paths.get("src/test/resources/123456.utf8.trans")).get(),
                is(Charset.forName("UTF-8")));
    }

    @Test
    public void fileUtf8Bom() {
        assertThat(encodingDetector.detect(Paths.get("src/test/resources/123456.utf8.bom.trans")).get(),
                is(Charset.forName("UTF-8")));
    }

    @Test
    public void fileUtf16BigEngine() {
        assertThat(encodingDetector.detect(Paths.get("src/test/resources/123456.utf16.be.trans")).get(),
                is(Charset.forName("UTF-16BE")));
    }

    @Test
    public void fileUtf16BigEngineBom() {
        assertThat(encodingDetector.detect(Paths.get("src/test/resources/123456.utf16.be.bom.trans")).get(),
                is(Charset.forName("UTF-16BE")));
    }

    @Test
    public void fileUtf16LittleEngine() {
        assertThat(encodingDetector.detect(Paths.get("src/test/resources/123456.utf16.le.trans")).get(),
                is(Charset.forName("UTF-16LE")));
    }

    @Test
    public void fileUtf16LittleEngineBom() {
        assertThat(encodingDetector.detect(Paths.get("src/test/resources/123456.utf16.le.bom.trans")).get(),
                is(Charset.forName("UTF-16LE")));
    }

    @Test
    public void fileUtf32BigEngine() {
        assertThat(encodingDetector.detect(Paths.get("src/test/resources/123456.utf32.be.trans")).get(),
                is(Charset.forName("UTF-32BE")));
    }

    @Test
    public void fileUtf32BigEngineBom() {
        assertThat(encodingDetector.detect(Paths.get("src/test/resources/123456.utf32.be.bom.trans")).get(),
                is(Charset.forName("UTF-32BE")));
    }

    @Test
    public void fileUtf32LittleEngine() {
        assertThat(encodingDetector.detect(Paths.get("src/test/resources/123456.utf32.le.trans")).get(),
                is(Charset.forName("UTF-32LE")));
    }

    @Test
    public void fileUtf32LittleEngineBom() {
        assertThat(encodingDetector.detect(Paths.get("src/test/resources/123456.utf32.le.bom.trans")).get(),
                is(Charset.forName("UTF-32LE")));
    }

    @Test
    public void ascii() {
        assertThat(encodingDetector.detect(readResource("src/test/resources/123456.ascii.trans")).get(),
                is(Charset.forName("ISO-8859-9")));
    }

    @Test
    public void utf8() {
        assertThat(encodingDetector.detect(readResource("src/test/resources/123456.utf8.trans")).get(),
                is(Charset.forName("UTF-8")));
    }

    @Test
    public void utf8Bom() {
        assertThat(encodingDetector.detect(readResource("src/test/resources/123456.utf8.bom.trans")).get(),
                is(Charset.forName("UTF-8")));
    }

    @Test
    public void utf16BigEngine() {
        assertThat(encodingDetector.detect(readResource("src/test/resources/123456.utf16.be.trans")).get(),
                is(Charset.forName("UTF-16BE")));
    }

    @Test
    public void utf16BigEngineBom() {
        assertThat(encodingDetector.detect(readResource("src/test/resources/123456.utf16.be.bom.trans")).get(),
                is(Charset.forName("UTF-16BE")));
    }

    @Test
    public void utf16LittleEngine() {
        assertThat(encodingDetector.detect(readResource("src/test/resources/123456.utf16.le.trans")).get(),
                is(Charset.forName("UTF-16LE")));
    }

    @Test
    public void utf16LittleEngineBom() {
        assertThat(encodingDetector.detect(readResource("src/test/resources/123456.utf16.le.bom.trans")).get(),
                is(Charset.forName("UTF-16LE")));
    }

    @Test
    public void utf32BigEngine() {
        assertThat(encodingDetector.detect(readResource("src/test/resources/123456.utf32.be.trans")).get(),
                is(Charset.forName("UTF-32BE")));
    }

    @Test
    public void utf32BigEngineBom() {
        assertThat(encodingDetector.detect(readResource("src/test/resources/123456.utf32.be.bom.trans")).get(),
                is(Charset.forName("UTF-32BE")));
    }

    @Test
    public void utf32LittleEngine() {
        assertThat(encodingDetector.detect(readResource("src/test/resources/123456.utf32.le.trans")).get(),
                is(Charset.forName("UTF-32LE")));
    }

    @Test
    public void utf32LittleEngineBom() {
        assertThat(encodingDetector.detect(readResource("src/test/resources/123456.utf32.le.bom.trans")).get(),
                is(Charset.forName("UTF-32LE")));
    }

    private byte[] readResource(String resource) {
        try {
            return Files.readAllBytes(Paths.get(resource));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
