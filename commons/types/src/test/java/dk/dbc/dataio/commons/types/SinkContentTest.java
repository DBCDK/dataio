package dk.dbc.dataio.commons.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SinkContent unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class SinkContentTest {
    private static final String NAME = "name";
    private static final String QUEUE = "queue";
    private static final String DESCRIPTION = "description";
    private static final SinkContent.SinkType SINK_TYPE = SinkContent.SinkType.OPENUPDATE;
    private static final SinkConfig SINK_CONFIG = OpenUpdateSinkConfigTest.config();

    public static SinkContent newSinkContentInstance() {
        return new SinkContent(NAME, QUEUE, DESCRIPTION);
    }

    public static SinkContent newSinkContentWithTypeInstance() {
        return new SinkContent(NAME, QUEUE, DESCRIPTION, SINK_TYPE, null, 1);
    }

    public static SinkContent newSinkContentWithTypeAndConfigInstance() {
        return new SinkContent(NAME, QUEUE, DESCRIPTION, SINK_TYPE, SINK_CONFIG, 1);
    }

    @Test
    public void constructor_nameArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new SinkContent(null, QUEUE, DESCRIPTION));
    }

    @Test
    public void constructor_nameArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new SinkContent("", QUEUE, DESCRIPTION));
    }

    @Test
    public void constructor_descriptionArgIsEmpty_returnsNewInstance() {
        new SinkContent(NAME, QUEUE, "");
    }

    @Test
    public void constructor_descriptionArgIsNull_returnsNewInstance() {
        new SinkContent(NAME, QUEUE, "");
    }

    @Test
    public void constructor_sinkTypeArgIsNull_returnsNewInstance() {
        new SinkContent(NAME, QUEUE, DESCRIPTION);
    }

    @Test
    public void constructor_sinkConfigArgIsNull_returnsNewInstance() {
        new SinkContent(NAME, QUEUE, DESCRIPTION, SINK_TYPE, null, 1);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        SinkContent instance = new SinkContent(NAME, QUEUE, DESCRIPTION, SINK_TYPE, SINK_CONFIG, 1);
        assertThat(instance, is(notNullValue()));
    }

    /**
     * Sink definitions are stored in flow-store as the JSON string they were posted as, and are
     * never rewritten, so rows written before sequence analysis was removed still carry a
     * sequenceAnalysisOption member. Unmarshalling has to ignore it rather than fail: flow-store
     * unmarshalls to this class to validate every sink it is asked to store, so a strict
     * unmarshaller would reject both an existing definition and one posted by a client still
     * sending the member. What makes this work is the class-level JsonIgnoreProperties, and
     * removing that annotation breaks every stored sink at once.
     */
    @Test
    void unmarshall_jsonCarriesSequenceAnalysisOption_memberIsIgnored() throws JSONBException {
        String json = "{\"name\":\"" + NAME + "\",\"queue\":\"" + QUEUE + "\",\"description\":\""
                + DESCRIPTION + "\",\"sinkType\":\"DUMMY\",\"sinkConfig\":null,"
                + "\"sequenceAnalysisOption\":\"ALL\",\"timeout\":1}";
        SinkContent sinkContent = new JSONBContext().unmarshall(json, SinkContent.class);
        assertThat(sinkContent.getName(), is(NAME));
        assertThat(sinkContent.getSinkType(), is(SinkContent.SinkType.DUMMY));
    }
}
