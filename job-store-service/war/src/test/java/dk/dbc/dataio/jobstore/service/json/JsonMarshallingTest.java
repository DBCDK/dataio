package dk.dbc.dataio.jobstore.service.json;


import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SupplementaryProcessDataBuilder;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferenceBuilder;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferencesBuilder;
import dk.dbc.dataio.jobstore.test.types.ItemInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jsonb.JSONBContext;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class JsonMarshallingTest {

    final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void verify_jsonMarshallingForJobInfoSnapshot() throws Exception {
        final String json = jsonbContext.marshall(new JobInfoSnapshotBuilder().setFatalError(true).build());
        final JobInfoSnapshot jobInfoSnapshot = jsonbContext.unmarshall(json, JobInfoSnapshot.class);
        assertThat(jobInfoSnapshot.hasFatalError(), is(true)); // Extra test on fatal error because of @JsonProperty
    }

    @Test
    public void verify_jsonMarshallingForItemInfoSnapshot() throws Exception {
        final String json = jsonbContext.marshall(new ItemInfoSnapshotBuilder().build());
        jsonbContext.unmarshall(json, ItemInfoSnapshot.class);
    }

    @Test
    public void verify_jsonMarshallingForResourceBundle() throws Exception {
        final String json = jsonbContext.marshall(new ResourceBundle(new FlowBuilder().build(), new SinkBuilder().build(), new SupplementaryProcessDataBuilder().build()));
        jsonbContext.unmarshall(json, ResourceBundle.class);
    }

    @Test
    public void verify_jsonMarshallingForDiagnostic() throws Exception {
        final String json = jsonbContext.marshall(new Diagnostic(Diagnostic.Level.FATAL, "msg"));
        jsonbContext.unmarshall(json, Diagnostic.class);
    }

    @Test
    public void verify_jsonMarshallingForState() throws Exception {
        final String json = jsonbContext.marshall(new State());
        jsonbContext.unmarshall(json, State.class);
    }

    @Test
    public void verify_jsonMarshallingForItemData() throws Exception {
        final String json = jsonbContext.marshall(new ItemData("data", StandardCharsets.UTF_8));
        jsonbContext.unmarshall(json, ItemData.class);
    }

    @Test
    public void verify_jsonMarshallingForSequenceAnalysisData() throws Exception {
        final String json = jsonbContext.marshall(new SequenceAnalysisData(new HashSet<String>()));
        jsonbContext.unmarshall(json, SequenceAnalysisData.class);
    }

    @Test
    public void verify_jsonMarshallingForJobInputStream() throws Exception {
        final String json = jsonbContext.marshall(new JobInputStream(new JobSpecificationBuilder().build(), true, 123456));
        jsonbContext.unmarshall(json, JobInputStream.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowStoreReference() throws Exception {
        final String json = jsonbContext.marshall(new FlowStoreReferenceBuilder().build());
        jsonbContext.unmarshall(json, FlowStoreReference.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowStoreReferences() throws Exception {
        final String json = jsonbContext.marshall(new FlowStoreReferencesBuilder().build());
        jsonbContext.unmarshall(json, FlowStoreReferences.class);
    }

    @Test
    public void verify_jsonMarshallingForJobError() throws Exception {
        final String json = jsonbContext.marshall(new JobError(JobError.Code.ILLEGAL_CHUNK, "description", "stacktrace"));
        jsonbContext.unmarshall(json, JobError.class);
    }
}
