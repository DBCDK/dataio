package dk.dbc.dataio.commons.types.json.mixins;

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
import dk.dbc.dataio.commons.types.NewJob;
import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides access to all JSON mixins
 */
public class MixIns {
    private static Map<Class<?>, Class<?>> mixIns = new HashMap<>();
    static {
        mixIns.put(ChunkResult.class, ChunkResultMixIn.class);
        mixIns.put(Flow.class, FlowJsonMixIn.class);
        mixIns.put(FlowBinder.class, FlowBinderJsonMixIn.class);
        mixIns.put(FlowBinderContent.class, FlowBinderContentJsonMixIn.class);
        mixIns.put(FlowContent.class, FlowContentJsonMixIn.class);
        mixIns.put(FlowComponent.class, FlowComponentJsonMixIn.class);
        mixIns.put(FlowComponentContent.class, FlowComponentContentJsonMixIn.class);
        mixIns.put(JavaScript.class, JavaScriptJsonMixIn.class);
        mixIns.put(Submitter.class, SubmitterJsonMixIn.class);
        mixIns.put(SubmitterContent.class, SubmitterContentJsonMixIn.class);
        mixIns.put(JobInfo.class, JobInfoJsonMixIn.class);
        mixIns.put(JobSpecification.class, JobSpecificationJsonMixIn.class);
        mixIns.put(Sink.class, SinkJsonMixIn.class);
        mixIns.put(SinkChunkResult.class, SinkChunkResultMixIn.class);
        mixIns.put(SinkContent.class, SinkContentJsonMixIn.class);
        mixIns.put(PingResponse.class, PingResponseJsonMixIn.class);
        mixIns.put(NewJob.class, NewJobJsonMixIn.class);
    }

    private MixIns() { }

    public static Map<Class<?>, Class<?>> getMixIns() {
        return mixIns;
    }
}
