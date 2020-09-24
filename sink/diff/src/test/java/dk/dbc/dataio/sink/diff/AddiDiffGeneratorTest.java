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

package dk.dbc.dataio.sink.diff;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class AddiDiffGeneratorTest extends AbstractDiffGeneratorTest {
    private final AddiDiffGenerator addiDiffGenerator = new AddiDiffGenerator();
    {
        addiDiffGenerator.externalToolDiffGenerator = newExternalToolDiffGenerator();
    }

    @Test
    public void noDiff() throws DiffGeneratorException {
        final byte[] addiBytes = "9\nmetadata1\n8\ncontent1\n9\nmetadata2\n8\ncontent2\n".getBytes(StandardCharsets.UTF_8);
        assertThat(addiDiffGenerator.getDiff(addiBytes, addiBytes), is(NO_DIFF));
    }

    @Test
    public void plaintextDiff() throws DiffGeneratorException {
        final byte[] currentAddiBytes = "9\nmetadata1\n8\ncontent1\n9\nmetadata2\n8\ncontent2\n".getBytes(StandardCharsets.UTF_8);
        final byte[] nextAddiBytes = "9\nmetadataA\n8\ncontentA\n9\nmetadata2\n8\ncontent2\n".getBytes(StandardCharsets.UTF_8);
        final String diff = addiDiffGenerator.getDiff(currentAddiBytes, nextAddiBytes);
        assertThat(diff, containsString("-metadata1"));
        assertThat(diff, containsString("+metadataA"));
        assertThat(diff, containsString("-content1"));
        assertThat(diff, containsString("+contentA"));
    }

    @Test
    public void jsonDiff() throws DiffGeneratorException {
        final byte[] currentAddiBytes = "14\n{\"metadata\":1}\n13\n{\"content\":1}\n14\n{\"metadata\":2}\n13\n{\"content\":2}\n".getBytes(StandardCharsets.UTF_8);
        final byte[] nextAddiBytes = "14\n{\"metadata\":1}\n13\n{\"content\":1}\n16\n{\"metadata\":\"B\"}\n15\n{\"content\":\"B\"}\n".getBytes(StandardCharsets.UTF_8);
        final String diff = addiDiffGenerator.getDiff(currentAddiBytes, nextAddiBytes);
        assertThat(diff, containsString("-  \"metadata\": 2"));
        assertThat(diff, containsString("+  \"metadata\": \"B\""));
        assertThat(diff, containsString("-  \"content\": 2"));
        assertThat(diff, containsString("+  \"content\": \"B\""));
    }

    @Test
    public void xmlDiff() throws DiffGeneratorException {
        final byte[] currentAddiBytes = "22\n<metadata>1</metadata>\n20\n<content>1</content>\n22\n<metadata>2</metadata>\n20\n<content>2</content>\n".getBytes(StandardCharsets.UTF_8);
        final byte[] nextAddiBytes = "22\n<metadata>1</metadata>\n20\n<content>1</content>\n22\n<metadata>B</metadata>\n20\n<content>B</content>\n".getBytes(StandardCharsets.UTF_8);
        final String diff = addiDiffGenerator.getDiff(currentAddiBytes, nextAddiBytes);
        assertThat(diff, containsString("-<metadata>2</metadata>"));
        assertThat(diff, containsString("+<metadata>B</metadata>"));
        assertThat(diff, containsString("-<content>2</content>"));
        assertThat(diff, containsString("+<content>B</content>"));
    }

    @Test
    public void multipleDocTypesDiff() throws DiffGeneratorException {
        final byte[] currentAddiBytes = "14\n{\"metadata\":1}\n20\n<content>1</content>\n14\n{\"metadata\":2}\n20\n<content>2</content>\n".getBytes(StandardCharsets.UTF_8);
        final byte[] nextAddiBytes = "14\n{\"metadata\":1}\n20\n<content>1</content>\n16\n{\"metadata\":\"B\"}\n20\n<content>B</content>\n".getBytes(StandardCharsets.UTF_8);
        final String diff = addiDiffGenerator.getDiff(currentAddiBytes, nextAddiBytes);
        assertThat(diff, containsString("-  \"metadata\": 2"));
        assertThat(diff, containsString("+  \"metadata\": \"B\""));
        assertThat(diff, containsString("-<content>2</content>"));
        assertThat(diff, containsString("+<content>B</content>"));
    }

    @Test
    public void multipleRecordsDiff() throws DiffGeneratorException {
        final byte[] currentAddiBytes = "14\n{\"metadata\":1}\n20\n<content>1</content>\n14\n{\"metadata\":2}\n20\n<content>2</content>\n".getBytes(StandardCharsets.UTF_8);
        final byte[] nextAddiBytes = "16\n{\"metadata\":\"A\"}\n20\n<content>A</content>\n16\n{\"metadata\":\"B\"}\n20\n<content>B</content>\n".getBytes(StandardCharsets.UTF_8);
        final String diff = addiDiffGenerator.getDiff(currentAddiBytes, nextAddiBytes);
        assertThat(diff, containsString("-  \"metadata\": 1"));
        assertThat(diff, containsString("+  \"metadata\": \"A\""));
        assertThat(diff, containsString("-<content>1</content>"));
        assertThat(diff, containsString("+<content>A</content>"));
        assertThat(diff, containsString("-  \"metadata\": 2"));
        assertThat(diff, containsString("+  \"metadata\": \"B\""));
        assertThat(diff, containsString("-<content>2</content>"));
        assertThat(diff, containsString("+<content>B</content>"));
    }

    @Test
    public void currentRecordMissingFromNextDiff() throws DiffGeneratorException {
        final byte[] currentAddiBytes = "14\n{\"metadata\":1}\n20\n<content>1</content>\n14\n{\"metadata\":2}\n20\n<content>2</content>\n".getBytes(StandardCharsets.UTF_8);
        final byte[] nextAddiBytes = "14\n{\"metadata\":1}\n20\n<content>1</content>\n".getBytes(StandardCharsets.UTF_8);
        final String diff = addiDiffGenerator.getDiff(currentAddiBytes, nextAddiBytes);
        assertThat(diff, containsString("-{\"metadata\":2}"));
        assertThat(diff, containsString("-<content>2</content>"));
    }

    @Test
    public void nextRecordNotInCurrentDiff() throws DiffGeneratorException {
        final byte[] currentAddiBytes = "14\n{\"metadata\":1}\n20\n<content>1</content>\n".getBytes(StandardCharsets.UTF_8);
        final byte[] nextAddiBytes = "14\n{\"metadata\":1}\n20\n<content>1</content>\n14\n{\"metadata\":2}\n20\n<content>2</content>\n".getBytes(StandardCharsets.UTF_8);
        final String diff = addiDiffGenerator.getDiff(currentAddiBytes, nextAddiBytes);
        assertThat(diff, containsString("+{\"metadata\":2}"));
        assertThat(diff, containsString("+<content>2</content>"));
    }
}
