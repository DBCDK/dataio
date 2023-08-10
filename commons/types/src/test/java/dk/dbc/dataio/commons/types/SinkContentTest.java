package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

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
    private static final SinkConfig SINK_CONFIG = OpenUpdateSinkConfigTest.newOpenUpdateSinkConfigInstance();
    private static final SinkContent.SequenceAnalysisOption SEQUENCE_ANALYSIS_OPTION = SinkContent.SequenceAnalysisOption.ALL;

    public static SinkContent newSinkContentInstance() {
        return new SinkContent(NAME, QUEUE, DESCRIPTION, SEQUENCE_ANALYSIS_OPTION);
    }

    public static SinkContent newSinkContentWithTypeInstance() {
        return new SinkContent(NAME, QUEUE, DESCRIPTION, SINK_TYPE, null, SEQUENCE_ANALYSIS_OPTION, 1);
    }

    public static SinkContent newSinkContentWithTypeAndConfigInstance() {
        return new SinkContent(NAME, QUEUE, DESCRIPTION, SINK_TYPE, SINK_CONFIG, SEQUENCE_ANALYSIS_OPTION, 1);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_nameArgIsNull_throws() {
        new SinkContent(null, QUEUE, DESCRIPTION, SEQUENCE_ANALYSIS_OPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nameArgIsEmpty_throws() {
        new SinkContent("", QUEUE, DESCRIPTION, SEQUENCE_ANALYSIS_OPTION);
    }

    @Test
    public void constructor_descriptionArgIsEmpty_returnsNewInstance() {
        new SinkContent(NAME, QUEUE, "", SEQUENCE_ANALYSIS_OPTION);
    }

    @Test
    public void constructor_descriptionArgIsNull_returnsNewInstance() {
        new SinkContent(NAME, QUEUE, "", SEQUENCE_ANALYSIS_OPTION);
    }

    @Test
    public void constructor_sinkTypeArgIsNull_returnsNewInstance() {
        new SinkContent(NAME, QUEUE, DESCRIPTION, SEQUENCE_ANALYSIS_OPTION);
    }

    @Test
    public void constructor_sinkConfigArgIsNull_returnsNewInstance() {
        new SinkContent(NAME, QUEUE, DESCRIPTION, SINK_TYPE, null, SEQUENCE_ANALYSIS_OPTION, 1);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_sequenceAnalysisOptionArgIsNull_throws() {
        new SinkContent(NAME, QUEUE, DESCRIPTION, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        SinkContent instance = new SinkContent(NAME, QUEUE, DESCRIPTION, SINK_TYPE, SINK_CONFIG, SEQUENCE_ANALYSIS_OPTION, 1);
        assertThat(instance, is(notNullValue()));
    }
}
