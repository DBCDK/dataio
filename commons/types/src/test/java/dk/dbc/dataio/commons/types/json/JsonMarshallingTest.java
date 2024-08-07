package dk.dbc.dataio.commons.types.json;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkItemTest;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowBinderContentTest;
import dk.dbc.dataio.commons.types.FlowBinderIdent;
import dk.dbc.dataio.commons.types.FlowBinderTest;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.FlowContentTest;
import dk.dbc.dataio.commons.types.FlowTest;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.types.GatekeeperDestinationTest;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.JavaScriptTest;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobSpecificationTest;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.SinkContentTest;
import dk.dbc.dataio.commons.types.SinkTest;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.SubmitterContentTest;
import dk.dbc.dataio.commons.types.SubmitterTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class JsonMarshallingTest {

    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void verify_jsonMarshallingForSubmitter() throws Exception {
        String json = jsonbContext.marshall(SubmitterTest.newSubmitterInstance());
        jsonbContext.unmarshall(json, Submitter.class);
    }

    @Test
    public void verify_jsonMarshallingForSubmitterContent() throws Exception {
        String json = jsonbContext.marshall(SubmitterContentTest.newSubmitterContentInstance());
        jsonbContext.unmarshall(json, SubmitterContent.class);
    }

    @Test
    public void verify_jsonMarshallingForJavaScript() throws Exception {
        String json = jsonbContext.marshall(JavaScriptTest.newJavaScriptInstance());
        jsonbContext.unmarshall(json, JavaScript.class);
    }

    @Test
    public void verify_jsonMarshallingForFlow() throws Exception {
        String json = jsonbContext.marshall(FlowTest.newFlowInstance());
        jsonbContext.unmarshall(json, Flow.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowContent() throws Exception {
        String json = jsonbContext.marshall(FlowContentTest.newFlowContentInstance());
        jsonbContext.unmarshall(json, FlowContent.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowContentWithoutTimeOfFlowComponentUpdate() throws Exception {
        String json = jsonbContext.marshall(FlowContentTest.newFlowContentInstance());
        jsonbContext.unmarshall(json, FlowContent.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowBinder() throws Exception {
        String json = jsonbContext.marshall(FlowBinderTest.newFlowBinderInstance());
        jsonbContext.unmarshall(json, FlowBinder.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowBinderContent() throws Exception {
        String json = jsonbContext.marshall(FlowBinderContentTest.newFlowBinderContentInstance());
        jsonbContext.unmarshall(json, FlowBinderContent.class);
    }

    @Test
    public void verify_jsonMarshallingForJobSpecification() throws Exception {
        String json = jsonbContext.marshall(JobSpecificationTest.newJobSpecificationInstance());
        jsonbContext.unmarshall(json, JobSpecification.class);
    }

    @Test
    public void verify_jsonMarshallingForSink() throws Exception {
        String json = jsonbContext.marshall(SinkTest.newSinkInstance());
        jsonbContext.unmarshall(json, Sink.class);
    }

    @Test
    public void verify_jsonMarshallingForSinkContent() throws Exception {
        String json = jsonbContext.marshall(SinkContentTest.newSinkContentInstance());
        jsonbContext.unmarshall(json, SinkContent.class);
    }

    @Test
    public void verify_jsonMarshallingForSinkContentWithType() throws Exception {
        String json = jsonbContext.marshall(SinkContentTest.newSinkContentWithTypeInstance());
        jsonbContext.unmarshall(json, SinkContent.class);
    }

    @Test
    public void verify_jsonMarshallingForSinkContentWithTypeAndConfig() throws Exception {
        String json = jsonbContext.marshall(SinkContentTest.newSinkContentWithTypeAndConfigInstance());
        jsonbContext.unmarshall(json, SinkContent.class);
    }

    @Test
    public void verify_jsonMarshallingForChunkItem() throws Exception {
        String json = jsonbContext.marshall(ChunkItemTest.newChunkItemInstance());
        jsonbContext.unmarshall(json, ChunkItem.class);
    }

    @Test
    public void verify_jsonMarshallingForChunkItemWithTypeAndEncoding() throws Exception {
        String json = jsonbContext.marshall(ChunkItem.successfulChunkItem()
                .withId(42)
                .withData("data")
                .withType(ChunkItem.Type.UNKNOWN)
                .withEncoding(StandardCharsets.UTF_8));
        jsonbContext.unmarshall(json, ChunkItem.class);
    }

    @Test
    public void verify_jsonMarshallingForSavedGatekeeperDestination() throws Exception {
        String json = jsonbContext.marshall(GatekeeperDestinationTest.newGatekeeperDestinationInstance());
        jsonbContext.unmarshall(json, GatekeeperDestination.class);
    }

    @Test
    public void verify_jsonMarshallingForFlowBinderWithSubmitter() throws JSONBException {
        FlowBinderIdent flowBinderIdent =
                new FlowBinderIdent("name", 1L);
        String json = jsonbContext.marshall(flowBinderIdent);
        jsonbContext.unmarshall(json, FlowBinderIdent.class);
    }
}
