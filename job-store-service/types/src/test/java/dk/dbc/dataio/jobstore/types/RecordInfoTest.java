package dk.dbc.dataio.jobstore.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.SinkContent;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class RecordInfoTest {
    private final String id = "42";
    private final SinkContent.SequenceAnalysisOption sequenceAnalysisOption = SinkContent.SequenceAnalysisOption.ALL;

    @Test
    public void marshalling() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final RecordInfo recordInfo = new RecordInfo("42");
        recordInfo.withPid("pid");
        final RecordInfo unmarshalled = jsonbContext.unmarshall(jsonbContext.marshall(recordInfo), RecordInfo.class);
        assertThat(unmarshalled, is(recordInfo));
    }

    @Test
    public void removesWhitespaces() {
        final RecordInfo recordInfo = new RecordInfo(" 4 2 ");
        assertThat(recordInfo.getId(), is(id));
    }

    @Test
    public void getKeys_idIsNull_returnsEmptySet() {
        RecordInfo recordInfo = new RecordInfo(null);
        assertThat(recordInfo.getKeys(sequenceAnalysisOption), is(Collections.emptySet()));
    }

    @Test
    public void getKeys_idIsNotNull_returnsSetContainingId() {
        RecordInfo recordInfo = new RecordInfo(id);
        assertThat(recordInfo.getKeys(sequenceAnalysisOption).size(), is(1));
        assertThat(recordInfo.getKeys(sequenceAnalysisOption).contains(id), is(true));
    }
}
