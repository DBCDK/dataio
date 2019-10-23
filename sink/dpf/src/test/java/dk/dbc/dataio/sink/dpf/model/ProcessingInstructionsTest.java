/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf.model;

import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ProcessingInstructionsTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void jsonUnmarshalling() throws JSONBException {
        final String json =
                "{\"submitter\":424242,\"title\":\"A title\",\"updateTemplate\":\"dbcperiodica\",\"recordState\":\"NEW\",\"unknownKey\":\"foo\"}";

        final ProcessingInstructions expected = new ProcessingInstructions()
                .withSubmitter(424242)
                .withTitle("A title")
                .withUpdateTemplate("dbcperiodica")
                .withRecordState(DpfRecord.State.NEW);

        final ProcessingInstructions unmarshalled =
                jsonbContext.unmarshall(json, ProcessingInstructions.class);
        assertThat(unmarshalled, is(expected));
    }

    @Test
    public void jsonUnmarshalling_withErrors() throws JSONBException {
        final String json =
                "{\"submitter\":424242,\"title\":\"A title\",\"updateTemplate\":\"dbcperiodica\",\"recordState\":\"MODIFIED\",\"errors\":[\"err1\",\"err2\"]}";

        final ProcessingInstructions expected = new ProcessingInstructions()
                .withSubmitter(424242)
                .withTitle("A title")
                .withUpdateTemplate("dbcperiodica")
                .withRecordState(DpfRecord.State.MODIFIED)
                .withErrors(Arrays.asList("err1", "err2"));

        final ProcessingInstructions unmarshalled =
                jsonbContext.unmarshall(json, ProcessingInstructions.class);
        assertThat(unmarshalled, is(expected));
    }
}