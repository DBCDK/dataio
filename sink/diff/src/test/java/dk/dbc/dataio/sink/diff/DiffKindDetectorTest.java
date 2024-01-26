package dk.dbc.dataio.sink.diff;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DiffKindDetectorTest {
    @Test
    public void onNull() {
        assertThat(Kind.detect(null), is(Kind.PLAINTEXT));
    }

    @Test
    public void onJsonObject() {
        assertThat(Kind.detect("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8)),
                is(Kind.JSON));
    }

    @Test
    public void onJsonArray() {
        assertThat(Kind.detect("[\"one\",\"two\"]".getBytes(StandardCharsets.UTF_8)),
                is(Kind.JSON));
    }

    @Test
    public void onXml() {
        assertThat(Kind.detect("<test/>".getBytes(StandardCharsets.UTF_8)),
                is(Kind.XML));
    }

    @Test
    public void onPlainText() {
        assertThat(Kind.detect("plain text".getBytes(StandardCharsets.UTF_8)),
                is(Kind.PLAINTEXT));
    }
}
