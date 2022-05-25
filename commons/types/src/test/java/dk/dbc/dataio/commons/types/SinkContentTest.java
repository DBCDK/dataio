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
    private static final String RESOURCE = "resource";
    private static final String DESCRIPTION = "description";
    private static final SinkContent.SinkType SINK_TYPE = SinkContent.SinkType.OPENUPDATE;
    private static final SinkConfig SINK_CONFIG = OpenUpdateSinkConfigTest.newOpenUpdateSinkConfigInstance();
    private static final SinkContent.SequenceAnalysisOption SEQUENCE_ANALYSIS_OPTION = SinkContent.SequenceAnalysisOption.ALL;

    @Test(expected = NullPointerException.class)
    public void constructor_nameArgIsNull_throws() {
        new SinkContent(null, RESOURCE, DESCRIPTION, SEQUENCE_ANALYSIS_OPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nameArgIsEmpty_throws() {
        new SinkContent("", RESOURCE, DESCRIPTION, SEQUENCE_ANALYSIS_OPTION);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_resourceArgIsNull_throws() {
        new SinkContent(NAME, null, DESCRIPTION, SEQUENCE_ANALYSIS_OPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_resourceArgIsEmpty_throws() {
        new SinkContent(NAME, "", DESCRIPTION, SEQUENCE_ANALYSIS_OPTION);
    }

    @Test
    public void constructor_descriptionArgIsEmpty_returnsNewInstance() {
        new SinkContent(NAME, RESOURCE, "", SEQUENCE_ANALYSIS_OPTION);
    }

    @Test
    public void constructor_descriptionArgIsNull_returnsNewInstance() {
        new SinkContent(NAME, RESOURCE, null, SEQUENCE_ANALYSIS_OPTION);
    }

    @Test
    public void constructor_sinkTypeArgIsNull_returnsNewInstance() {
        new SinkContent(NAME, RESOURCE, DESCRIPTION, null, SEQUENCE_ANALYSIS_OPTION);
    }

    @Test
    public void constructor_sinkConfigArgIsNull_returnsNewInstance() {
        new SinkContent(NAME, RESOURCE, DESCRIPTION, SINK_TYPE, null, SEQUENCE_ANALYSIS_OPTION);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_sequenceAnalysisOptionArgIsNull_throws() {
        new SinkContent(NAME, RESOURCE, DESCRIPTION, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final SinkContent instance = new SinkContent(NAME, RESOURCE, DESCRIPTION, SINK_TYPE, SINK_CONFIG, SEQUENCE_ANALYSIS_OPTION);
        assertThat(instance, is(notNullValue()));
    }

    public static SinkContent newSinkContentInstance() {
        return new SinkContent(NAME, RESOURCE, DESCRIPTION, SEQUENCE_ANALYSIS_OPTION);
    }

    public static SinkContent newSinkContentWithTypeInstance() {
        return new SinkContent(NAME, RESOURCE, DESCRIPTION, SINK_TYPE, SEQUENCE_ANALYSIS_OPTION);
    }

    public static SinkContent newSinkContentWithTypeAndConfigInstance() {
        return new SinkContent(NAME, RESOURCE, DESCRIPTION, SINK_TYPE, SINK_CONFIG, SEQUENCE_ANALYSIS_OPTION);
    }
}
