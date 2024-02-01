package dk.dbc.dataio.commons.types;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FlowBinderContent unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowBinderContentTest {
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String PACKAGING = "packaging";
    private static final String FORMAT = "format";
    private static final String CHARSET = "charset";
    private static final String DESTINATION = "destination";
    private static final Priority PRIORITY = Priority.NORMAL;
    private static final RecordSplitter RECORD_SPLITTER = RecordSplitter.XML;
    private static final Long FLOW_ID = 42L;
    private static final List<Long> SUBMITTER_IDS = Collections.singletonList(42L);
    private static final Long SINK_ID = 31L;
    private static final String QUEUE_PROVIDER = "queue provider";

    @Test
    public void constructor_nameArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new FlowBinderContent(null, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, PRIORITY, RECORD_SPLITTER, FLOW_ID, SUBMITTER_IDS, SINK_ID, QUEUE_PROVIDER));
    }

    @Test
    public void constructor_nameArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new FlowBinderContent("", DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, PRIORITY, RECORD_SPLITTER, FLOW_ID, SUBMITTER_IDS, SINK_ID, QUEUE_PROVIDER));
    }

    @Test
    public void constructor_descriptionArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new FlowBinderContent(NAME, null, PACKAGING, FORMAT, CHARSET, DESTINATION, PRIORITY, RECORD_SPLITTER, FLOW_ID, SUBMITTER_IDS, SINK_ID, QUEUE_PROVIDER));
    }

    @Test
    public void constructor_descriptionArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new FlowBinderContent(NAME, "", PACKAGING, FORMAT, CHARSET, DESTINATION, PRIORITY, RECORD_SPLITTER, FLOW_ID, SUBMITTER_IDS, SINK_ID, QUEUE_PROVIDER));
    }

    @Test
    public void constructor_packagingArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new FlowBinderContent(NAME, DESCRIPTION, null, FORMAT, CHARSET, DESTINATION, PRIORITY, RECORD_SPLITTER, FLOW_ID, SUBMITTER_IDS, SINK_ID, QUEUE_PROVIDER));
    }

    @Test
    public void constructor_packagingArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new FlowBinderContent(NAME, DESCRIPTION, "", FORMAT, CHARSET, DESTINATION, PRIORITY, RECORD_SPLITTER, FLOW_ID, SUBMITTER_IDS, SINK_ID, QUEUE_PROVIDER));
    }

    @Test
    public void constructor_formatArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new FlowBinderContent(NAME, DESCRIPTION, PACKAGING, null, CHARSET, DESTINATION, PRIORITY, RECORD_SPLITTER, FLOW_ID, SUBMITTER_IDS, SINK_ID, QUEUE_PROVIDER));
    }

    @Test
    public void constructor_formatArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new FlowBinderContent(NAME, DESCRIPTION, PACKAGING, "", CHARSET, DESTINATION, PRIORITY, RECORD_SPLITTER, FLOW_ID, SUBMITTER_IDS, SINK_ID, QUEUE_PROVIDER));
    }

    @Test
    public void constructor_charsetArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new FlowBinderContent(NAME, DESCRIPTION, PACKAGING, FORMAT, null, DESTINATION, PRIORITY, RECORD_SPLITTER, FLOW_ID, SUBMITTER_IDS, SINK_ID, QUEUE_PROVIDER));
    }

    @Test
    public void constructor_charsetArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new FlowBinderContent(NAME, DESCRIPTION, PACKAGING, FORMAT, "", DESTINATION, PRIORITY, RECORD_SPLITTER, FLOW_ID, SUBMITTER_IDS, SINK_ID, QUEUE_PROVIDER));
    }

    @Test
    public void constructor_destinationArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new FlowBinderContent(NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, null, PRIORITY, RECORD_SPLITTER, FLOW_ID, SUBMITTER_IDS, SINK_ID, QUEUE_PROVIDER));
    }

    @Test
    public void constructor_destinationArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new FlowBinderContent(NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, "", PRIORITY, RECORD_SPLITTER, FLOW_ID, SUBMITTER_IDS, SINK_ID, QUEUE_PROVIDER));
    }

    @Test
    public void constructor_priorityArgIsNull_normalPriority() {
        FlowBinderContent instance = new FlowBinderContent(NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, null, RECORD_SPLITTER, FLOW_ID, SUBMITTER_IDS, SINK_ID, QUEUE_PROVIDER);
        assertThat(instance.getPriority(), is(Priority.NORMAL));
    }

    @Test
    public void constructor_recordSplitterArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new FlowBinderContent(NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, PRIORITY, null, FLOW_ID, SUBMITTER_IDS, SINK_ID, QUEUE_PROVIDER));
    }

    @Test
    public void constructor_flowIdArgIsLessThanLowerBound_throws() {
        assertThrows(IllegalArgumentException.class, () -> new FlowBinderContent(NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, PRIORITY, RECORD_SPLITTER, Constants.PERSISTENCE_ID_LOWER_BOUND - 1, SUBMITTER_IDS, SINK_ID, QUEUE_PROVIDER));
    }

    @Test
    public void constructor_submitterIdsArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new FlowBinderContent(NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, PRIORITY, RECORD_SPLITTER, FLOW_ID, null, SINK_ID, QUEUE_PROVIDER));
    }

    @Test
    public void constructor_submitterIdsArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new FlowBinderContent(NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, PRIORITY, RECORD_SPLITTER, FLOW_ID, new ArrayList<>(0), SINK_ID, QUEUE_PROVIDER));
    }

    @Test
    public void constructor_sinkIdArgIsLessThanLowerBound_throws() {
        assertThrows(IllegalArgumentException.class, () -> new FlowBinderContent(NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, PRIORITY, RECORD_SPLITTER, FLOW_ID, SUBMITTER_IDS, Constants.PERSISTENCE_ID_LOWER_BOUND - 1, QUEUE_PROVIDER));
    }

    @Test
    public void constructor_queueProviderArgIsNull_throwsNot() {
        new FlowBinderContent(NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, PRIORITY, RECORD_SPLITTER, FLOW_ID, SUBMITTER_IDS, SINK_ID, null);
    }

    @Test
    public void constructor_queueProviderArgIsEmpty_throwsNot() {
        new FlowBinderContent(NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, PRIORITY, RECORD_SPLITTER, FLOW_ID, SUBMITTER_IDS, SINK_ID, "");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        FlowBinderContent instance = new FlowBinderContent(NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, PRIORITY, RECORD_SPLITTER, FLOW_ID, SUBMITTER_IDS, SINK_ID, QUEUE_PROVIDER);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void verify_defensiveCopyingOfSubmitterIdsList() {
        List<Long> submitterIds = new ArrayList<>();
        submitterIds.add(42L);
        FlowBinderContent instance = new FlowBinderContent(NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, PRIORITY, RECORD_SPLITTER, FLOW_ID, submitterIds, SINK_ID, QUEUE_PROVIDER);
        assertThat(instance.getSubmitterIds().size(), is(1));
        submitterIds.add(null);
        List<Long> returnedSubmitterIds = instance.getSubmitterIds();
        assertThat(returnedSubmitterIds.size(), is(1));
        returnedSubmitterIds.add(null);
        assertThat(instance.getSubmitterIds().size(), is(1));
    }

    public static FlowBinderContent newFlowBinderContentInstance() {
        return new FlowBinderContent(NAME, DESCRIPTION, PACKAGING, FORMAT, CHARSET, DESTINATION, PRIORITY, RECORD_SPLITTER, FLOW_ID, SUBMITTER_IDS, SINK_ID, QUEUE_PROVIDER);
    }
}
