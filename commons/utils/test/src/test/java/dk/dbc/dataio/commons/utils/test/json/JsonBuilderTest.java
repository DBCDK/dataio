package dk.dbc.dataio.commons.utils.test.json;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import org.junit.Test;

public class JsonBuilderTest {

    @Test
    public void ChunkItemJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new ChunkItemJsonBuilder().build(), ChunkItem.class);
    }

    @Test
    public void FlowBinderContentJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new FlowBinderContentJsonBuilder().build(), FlowBinderContent.class);
    }

    @Test
    public void FlowBinderJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new FlowBinderJsonBuilder().build(), FlowBinder.class);
    }

    @Test
    public void FlowComponentContentJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new FlowComponentContentJsonBuilder().build(), FlowComponentContent.class);
    }

    @Test
    public void FlowComponentJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new FlowComponentJsonBuilder().build(), FlowComponent.class);
    }

    @Test
    public void FlowContentJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new FlowContentJsonBuilder().build(), FlowContent.class);
    }

    @Test
    public void FlowJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new FlowJsonBuilder().build(), Flow.class);
    }

    @Test
    public void JavaScriptJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new JavaScriptJsonBuilder().build(), JavaScript.class);
    }

    @Test
    public void JobSpecificationJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new JobSpecificationJsonBuilder().build(), JobSpecification.class);
    }

    @Test
    public void SinkContentJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new SinkContentJsonBuilder().build(), SinkContent.class);
    }

    @Test
    public void SinkJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new SinkJsonBuilder().build(), Sink.class);
    }

    @Test
    public void SubmitterContentJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new SubmitterContentJsonBuilder().build(), SubmitterContent.class);
    }

    @Test
    public void SubmitterJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new SubmitterJsonBuilder().build(), Submitter.class);
    }

    @Test
    public void SupplementaryProcessDataJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new SupplementaryProcessDataJsonBuilder().build(), SupplementaryProcessData.class);
    }

    @Test
    public void test() {
        System.out.println(new JobSpecificationJsonBuilder().build());
    }
}
