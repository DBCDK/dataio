package dk.dbc.dataio.sink.dpf.model;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ProcessingInstructionsTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void jsonUnmarshalling() throws JSONBException {
        final String json =
                "{\"submitter\":424242,\"title\":\"A title\",\"updateTemplate\":\"dbcperiodica\",\"recordState\":\"NEW\",\"unknownKey\":\"foo\"}";

        ProcessingInstructions expected = new ProcessingInstructions()
                .withSubmitter(424242)
                .withTitle("A title")
                .withUpdateTemplate("dbcperiodica")
                .withRecordState(DpfRecord.State.NEW);

        ProcessingInstructions unmarshalled =
                jsonbContext.unmarshall(json, ProcessingInstructions.class);
        assertThat(unmarshalled, is(expected));
    }

    @Test
    public void jsonUnmarshalling_withErrors() throws JSONBException {
        final String json =
                "{\"submitter\":424242,\"title\":\"A title\",\"updateTemplate\":\"dbcperiodica\",\"recordState\":\"MODIFIED\",\"errors\":[\"err1\",\"err2\"]}";

        ProcessingInstructions expected = new ProcessingInstructions()
                .withSubmitter(424242)
                .withTitle("A title")
                .withUpdateTemplate("dbcperiodica")
                .withRecordState(DpfRecord.State.MODIFIED)
                .withErrors(Arrays.asList("err1", "err2"));

        ProcessingInstructions unmarshalled =
                jsonbContext.unmarshall(json, ProcessingInstructions.class);
        assertThat(unmarshalled, is(expected));
    }

    @Test
    public void jsonMarshalling() throws JSONBException {
        ProcessingInstructions processingInstructions = new ProcessingInstructions()
                .withSubmitter(424242)
                .withId("test")
                .withTitle("A title")
                .withUpdateTemplate("dbcperiodica")
                .withOriginalRecordId("oriId")
                .withRecordState(DpfRecord.State.MODIFIED)
                .withErrors(Arrays.asList("err1", "err2"));

        final String expectedJson =
                "{\"submitter\":424242,\"id\":\"test\",\"title\":\"A title\",\"updateTemplate\":\"dbcperiodica\",\"originalRecordId\":\"oriId\",\"recordState\":\"MODIFIED\",\"errors\":[\"err1\",\"err2\"]}";

        assertThat(jsonbContext.marshall(processingInstructions), is(expectedJson));
    }
}
