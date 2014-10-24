
package dk.dbc.dataio.gui.types;

import dk.dbc.dataio.commons.types.SubmitterContent;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;


/**
 * FlowBinderContentViewDataTest unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowBinderContentGenericViewDataTest {
    private static final Long FLOW_BINDER_ID = 33L;
    private static final Long VERSION = 1L;
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String PACKAGING = "packaging";
    private static final String FORMAT = "format";
    private static final String CHARSET = "charset";
    private static final String DESTINATION = "destination";
    private static final String RECORD_SPLITTER = "recordSplitter";
    private static final Long FLOW_ID = 42L;
    private static final String FLOW_NAME = "flowname";
    private static final List<Long> SUBMITTER_IDS = Arrays.asList(54L);
    private static final List<SubmitterContent> SUBMITTER_CONTENTS = Arrays.asList(new SubmitterContent(123L, "submittername", "submitterdescription"));
    private static final Long SINK_ID = 31L;
    private static final String SINK_NAME = "sinkname";

    @Test(expected = NullPointerException.class)
    public void constructor_flowNameArgIsNull_throws() {
        new FlowBinderContentViewData(FLOW_BINDER_ID, VERSION, NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, RECORD_SPLITTER, FLOW_ID, null, SUBMITTER_IDS, SUBMITTER_CONTENTS, SINK_ID, SINK_NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_flowNameArgIsEmpty_throws() {
        new FlowBinderContentViewData(FLOW_BINDER_ID, VERSION, NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, RECORD_SPLITTER, FLOW_ID, "", SUBMITTER_IDS, SUBMITTER_CONTENTS, SINK_ID, SINK_NAME);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_submitterContentsArgIsNull_throws() {
        new FlowBinderContentViewData(FLOW_BINDER_ID, VERSION, NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, RECORD_SPLITTER, FLOW_ID, FLOW_NAME, SUBMITTER_IDS, null, SINK_ID, SINK_NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_submitterContentsArgListIsEmpty_throws() {
        new FlowBinderContentViewData(FLOW_BINDER_ID, VERSION, NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, RECORD_SPLITTER, FLOW_ID, FLOW_NAME, SUBMITTER_IDS, new ArrayList<SubmitterContent>(), SINK_ID, SINK_NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_submitterNameArgIsEmpty_throws() {
        new FlowBinderContentViewData(FLOW_BINDER_ID, VERSION, NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, RECORD_SPLITTER, FLOW_ID, FLOW_NAME, SUBMITTER_IDS, Arrays.asList(new SubmitterContent(123L, "", "submitterdescription")), SINK_ID, SINK_NAME);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_sinkNameArgIsNull_throws() {
        new FlowBinderContentViewData(FLOW_BINDER_ID, VERSION, NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, RECORD_SPLITTER, FLOW_ID, FLOW_NAME, SUBMITTER_IDS, SUBMITTER_CONTENTS, SINK_ID, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_sinkNameArgIsEmpty_throws() {
        new FlowBinderContentViewData(FLOW_BINDER_ID, VERSION, NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, RECORD_SPLITTER, FLOW_ID, FLOW_NAME, SUBMITTER_IDS, SUBMITTER_CONTENTS, SINK_ID, "");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final FlowBinderContentViewData instance = new FlowBinderContentViewData(FLOW_BINDER_ID, VERSION, NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, RECORD_SPLITTER, FLOW_ID, FLOW_NAME, SUBMITTER_IDS, SUBMITTER_CONTENTS, SINK_ID, SINK_NAME);
        assertThat(instance, is(notNullValue()));
    }

}
