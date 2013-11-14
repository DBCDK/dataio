package dk.dbc.dataio.flowstore.entity;

import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.test.json.SinkContentJsonBuilder;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
  * Sink unit tests
  *
  * The test methods of this class uses the following naming convention:
  *
  *  unitOfWork_stateUnderTest_expectedBehavior
  */
public class SinkTest {
    @Test
    public void setContent_jsonDataArgIsValidFlowContentJson_setsNameIndexValue() throws Exception {
        final String name = "testSink";
        final String sinkContent = new SinkContentJsonBuilder()
                .setName(name)
                .build();

        final Sink sink = getSinkEntity();
        sink.setContent(sinkContent);
        assertThat(sink.getNameIndexValue(), is(name));
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsInvalidSinkContent_throws() throws Exception {
        getSinkEntity().setContent("{}");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsInvalidJson_throws() throws Exception {
        getSinkEntity().setContent("{");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setContent_jsonDataArgIsEmpty_throws() throws Exception {
        getSinkEntity().setContent("");
    }

    @Test(expected = NullPointerException.class)
    public void setContent_jsonDataArgIsNull_throws() throws Exception {
        getSinkEntity().setContent(null);
    }

    public static Sink getSinkEntity() {
        return new Sink();
    }
}
