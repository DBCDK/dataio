/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.diff;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DiffKindDetectorTest {
    @Test
    public void onNull() {
        assertThat(DiffKindDetector.getKind(null), is(ExternalToolDiffGenerator.Kind.PLAINTEXT));
    }

    @Test
    public void onJsonObject() {
        assertThat(DiffKindDetector.getKind("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8)),
                is(ExternalToolDiffGenerator.Kind.JSON));
    }

    @Test
    public void onJsonArray() {
        assertThat(DiffKindDetector.getKind("[\"one\",\"two\"]".getBytes(StandardCharsets.UTF_8)),
                is(ExternalToolDiffGenerator.Kind.JSON));
    }

    @Test
    public void onXml() {
        assertThat(DiffKindDetector.getKind("<test/>".getBytes(StandardCharsets.UTF_8)),
                is(ExternalToolDiffGenerator.Kind.XML));
    }

    @Test
    public void onPlainText() {
        assertThat(DiffKindDetector.getKind("plain text".getBytes(StandardCharsets.UTF_8)),
                is(ExternalToolDiffGenerator.Kind.PLAINTEXT));
    }
}