package dk.dbc.dataio.commons.utils.test.json;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import org.junit.Test;

public class JsonBuilderTest {
    @Test
    public void ChunkJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new ChunkJsonBuilder().build(), Chunk.class, MixIns.getMixIns());
    }

    @Test
    public void ChunkItemJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new ChunkItemJsonBuilder().build(), ChunkItem.class, MixIns.getMixIns());
    }

    @Test
    public void ChunkResultJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new ChunkResultJsonBuilder().build(), ChunkResult.class, MixIns.getMixIns());
    }

    @Test
    public void FlowBinderContentJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new FlowBinderContentJsonBuilder().build(), FlowBinderContent.class, MixIns.getMixIns());
    }

    @Test
    public void FlowBinderJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new FlowBinderJsonBuilder().build(), FlowBinder.class, MixIns.getMixIns());
    }

    @Test
    public void FlowComponentContentJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new FlowComponentContentJsonBuilder().build(), FlowComponentContent.class, MixIns.getMixIns());
    }

    @Test
    public void FlowComponentJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new FlowComponentJsonBuilder().build(), FlowComponent.class, MixIns.getMixIns());
    }

    @Test
    public void FlowContentJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new FlowContentJsonBuilder().build(), FlowContent.class, MixIns.getMixIns());
    }

    @Test
    public void FlowJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new FlowJsonBuilder().build(), Flow.class, MixIns.getMixIns());
    }

    @Test
    public void JavaScriptJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new JavaScriptJsonBuilder().build(), JavaScript.class, MixIns.getMixIns());
    }

    @Test
    public void JobInfoJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new JobInfoJsonBuilder().build(), JobInfo.class, MixIns.getMixIns());
    }

    @Test
    public void JobSpecificationJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new JobSpecificationJsonBuilder().build(), JobSpecification.class, MixIns.getMixIns());
    }

    @Test
    public void SinkChunkResultJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new SinkChunkResultJsonBuilder().build(), SinkChunkResult.class, MixIns.getMixIns());
    }

    @Test
    public void SinkContentJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new SinkContentJsonBuilder().build(), SinkContent.class, MixIns.getMixIns());
    }

    @Test
    public void SinkJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new SinkJsonBuilder().build(), Sink.class, MixIns.getMixIns());
    }

    @Test
    public void SubmitterContentJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new SubmitterContentJsonBuilder().build(), SubmitterContent.class, MixIns.getMixIns());
    }

    @Test
    public void SubmitterJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new SubmitterJsonBuilder().build(), Submitter.class, MixIns.getMixIns());
    }

    @Test
    public void SupplementaryProcessDataJsonBuilderProducesValidJson() throws JsonException {
        JsonUtil.fromJson(new SupplementaryProcessDataJsonBuilder().build(), SupplementaryProcessData.class, MixIns.getMixIns());
    }

    @Test
    public void test() {
        System.out.println(new JobSpecificationJsonBuilder().build());
    }
}
